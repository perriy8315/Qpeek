package org.qpeek.qpeek.domain.authentication.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.qpeek.qpeek.common.entity.BaseTimeEntity;
import org.qpeek.qpeek.domain.authentication.enums.VerificationChannelType;
import org.qpeek.qpeek.domain.member.entity.Member;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

import static java.time.OffsetDateTime.*;

/**
 * Verification (인증 엔티티)
 * <p>
 * <도메인 규칙/정책>
 * 1. 인증 만료 정책 : 현재 시간이 expiresAt과 같거나 이후면 만료된 것으로 처리 (== 포함)
 * 2. 이미 검증된 경우(verifiedAt != null) 재검증 불가
 * 3. 검증 시 제공된 코드 해시(rawCodeHashed)가 저장된 codeHash와 일치해야 검증 완료
 * 4. 발급(issue) 시 duration 은 반드시 양수여야 함 (null, 0, 음수 불가)
 * 5. Member 는 영속 상태여야 하며, 식별자(id)가 반드시 존재해야 함
 * <p>
 * <설계 메모>
 * - member:다대일 관계 (여러 Verification 가능), member_id 는 FK
 * - @Version 을 통한 낙관적 락(동시성 제어)
 * - 조회 성능을 위해 (member_id, channel_type, verified_at, expires_at) 복합 인덱스 구성
 * - verifiedAt 이 null → 미검증 상태, 값 존재 → 검증 완료
 * - 한 Member 가 여러 채널(channelType)로 동시에 Verification을 가질 수 있음
 */
@Entity
@Getter
@Table(name = "verifications",
        indexes = {
                @Index(name = "idx_verification_member_channel", columnList = "member_id, channel_type, verified_at, expires_at")
        })
@ToString(of = {"id", "channelType", "expiresAt", "verifiedAt"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "verification_id")
    private Long id;

    @Version
    @JsonIgnore
    @Column(name = "version")
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private VerificationChannelType channelType;

    @JsonIgnore
    @Column(name = "code_hash", nullable = false, length = 200)
    private String codeHash;

    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime expiresAt;

    @Column(name = "verified_at", columnDefinition = "timestamptz")
    private OffsetDateTime verifiedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private Verification(VerificationChannelType channelType, String codeHash, OffsetDateTime expiresAt, Member member) {
        this.channelType = validNullOrBlank(channelType, "channelType");
        this.codeHash = validNullOrBlank(codeHash, "codeHash");
        this.expiresAt = validNullOrBlank(expiresAt, "expiresAt");
        this.member = validMemberIsNull(member);
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    public static Verification issue(VerificationChannelType channelType, String rawCodeHashed, Duration duration, Clock clock, Member member) {
        if (duration == null || duration.isZero() || duration.isNegative())
            throw new IllegalArgumentException("duration must be positive");
        OffsetDateTime now = now(clock);
        return new Verification(channelType, rawCodeHashed, now.plus(duration), member);
    }

    public void verify(String rawCodeHashed, Clock clock) {
        if (isVerified()) throw new IllegalStateException("already verified");

        OffsetDateTime now = now(clock);
        if (!now.isBefore(expiresAt)) throw new IllegalStateException("verification code expired");

        if (!Objects.equals(this.codeHash, rawCodeHashed)) throw new IllegalArgumentException("code mismatch");
        this.verifiedAt = now;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isExpired(Clock clock) {
        return !now(clock).isBefore(expiresAt);
    }


    // 검증 로직 ----------------------------------------------------------------


    private static <T> T validNullOrBlank(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        if (value instanceof CharSequence cs && cs.toString().isBlank())
            throw new IllegalArgumentException(name + " is blank");
        return value;
    }

    private static Member validMemberIsNull(Member member) {
        if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("member is null or transient");
        }
        return member;
    }
}