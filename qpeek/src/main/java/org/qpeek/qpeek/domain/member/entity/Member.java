package org.qpeek.qpeek.domain.member.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.member.enums.MemberStatus;
import org.qpeek.qpeek.common.persistence.converter.ZoneIdAttributeConverter;
import org.qpeek.qpeek.domain.member.value.LoginId;
import org.qpeek.qpeek.domain.member.value.PasswordHash;

import java.time.ZoneId;

/**
 * Member (회원)
 * <p>
 * <도메인 규칙/정책>
 * - loginId: 회원 식별용 로그인 ID. 가입 후 변경 불가(updatable=false), 정책 위반 값은 값 객체(LoginId)에서 즉시 거절.
 * - passwordHash: 항상 해시 문자열만 저장(평문 비밀번호는 절대 받지 않음), 검증·해싱은 서비스/인프라 레이어에서 수행.
 * - memberStatus: 회원 상태 기본 ACTIVE. 탈퇴/정지 등 상태 전이는 비즈니스 규칙 적용.
 * - timeZone: IANA TZ 식별자(예: "Asia/Seoul").
 * - nickname: null/empty/blank 불가, 앞/뒤 공백 불가, 최대 50자.
 * <p>
 * <설계 메모>
 * - 값 객체(LoginId, PasswordHash)가 각자 불변식(검증)을 책임지며, 엔티티는 조립에 집중.
 * - UNIQUE 제약 + CHECK 제약(로그인 ID 패턴 준수, nickname 공백 사용 불가 보장).
 */
@Entity
@Getter
@Table(name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_members_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_members_nickname", columnNames = "nickname")})
@Check(constraints = "login_id ~ '^[a-z0-9]{5,20}$' AND nickname ~ '.*\\S.*'")
@ToString(of = {"id", "loginId", "nickname", "memberStatus", "timeZone"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "member_id")
    private Long id;

    @Embedded
    private LoginId loginId;

    @Embedded
    @JsonIgnore
    private PasswordHash passwordHash;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false)
    private MemberStatus memberStatus;

    @Convert(converter = ZoneIdAttributeConverter.class)
    @Column(name = "time_zone", nullable = false, length = 64)
    private ZoneId timeZone;

    private Member(String loginId, String passwordHash, String nickname, ZoneId timeZone, MemberStatus memberStatus) {
        this.loginId = LoginId.of(loginId);
        this.passwordHash = PasswordHash.of(passwordHash);
        this.nickname = validNickname(nickname);
        this.timeZone = validNullOrBlank(timeZone, "timeZone");
        this.memberStatus = memberStatus != null ? memberStatus : MemberStatus.ACTIVE;
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static Member create(String loginId, String passwordHash, String nickname, ZoneId timeZone) {
        return new Member(loginId, passwordHash, nickname, timeZone, MemberStatus.ACTIVE);
    }

    public static Member create(LoginId loginId, PasswordHash passwordHash, String nickname, ZoneId timeZone) {
        return new Member(loginId.getValue(), passwordHash.getValue(), nickname, timeZone, MemberStatus.ACTIVE);
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    public void changePassword(String newPasswordHash) {
        this.passwordHash = PasswordHash.of(newPasswordHash);
    }

    public void changeNickname(String newNickname) {
        this.nickname = validNickname(newNickname);
    }

    public void changeMemberStatus(MemberStatus status) {
        this.memberStatus = validNullOrBlank(status, "memberStatus");
    }


    // 검증 로직 ----------------------------------------------------------------


    private static String validNickname(String nickname) {
        if (nickname == null)
            throw new IllegalArgumentException("nickname must not be null");
        if (nickname.isEmpty())
            throw new IllegalArgumentException("nickname must not be empty");
        if (nickname.codePoints().allMatch(Character::isWhitespace))
            throw new IllegalArgumentException("nickname must not be blank");
        if (nickname.length() > 50)
            throw new IllegalArgumentException("nickname length must be <= 50");
        if (Character.isWhitespace(nickname.charAt(0)) || Character.isWhitespace(nickname.charAt(nickname.length() - 1))) {
            throw new IllegalArgumentException("nickname must not start or end with whitespace");
        }
        return nickname;
    }

    private static <T> T validNullOrBlank(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        if (value instanceof CharSequence cs && cs.toString().isBlank())
            throw new IllegalArgumentException(name + " is blank");
        return value;
    }
}
