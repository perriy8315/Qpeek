package org.qpeek.qpeek.domain.reminder.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.member.entity.Member;
import org.qpeek.qpeek.domain.reminder.enums.ReminderChannelType;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * ReminderChannelAccount (리마인더 발송 채널 계정 엔티티)
 * <p>
 * <도메인 규칙/정책>
 * 1. 한 Member 는 채널(ChannelType)별로 최대 1개의 계정만 가질 수 있음
 * (DB 제약: UNIQUE(member_id, channel_type))
 * 2. addressOrToken 은 null/빈 문자열 불가, 채널별 형식 검증 필수
 * - EMAIL → 간단 이메일 패턴 검증
 * - KAKAO → 토큰/식별자 길이 최소 10 이상
 * - SLACK → `https://hooks.slack.com/services/` 로 시작해야 함
 * - WEBPUSH → http/https URL 형식 필수
 * 3. 채널 활성/비활성 플래그(channelEnabled)로 발송 가능 여부 제어
 * 4. 외부 검증 완료 시 verifiedAt 을 기록, null 이면 미검증 상태
 * 5. 발송 가능(canSend) 조건: channelEnabled = true 이면서 verifiedAt != null
 * 6. Member 는 반드시 영속 상태여야 하며, 식별자(id)가 존재해야 함
 * 7. 생성 후 채널/소유자 변경 불가
 * <p>
 * <설계 메모>
 * - member:다대일 관계 (여러 채널 계정 보유 가능), member_id 는 FK (NOT NULL)
 * - DB 제약: @Check(address_or_token <> ''), UNIQUE(member_id, channel_type)
 * - 조회 성능: (member_id, channel_type, verified_at) 인덱스 구성
 * - 주소/토큰 변경 시 updateAddressOrToken → 검증 상태 초기화(verifiedAt = null)
 * - enable/disable 로 채널 활성화 상태 전환 가능
 */

@Entity
@Getter
@Check(constraints = "address_or_token <> ''")
@Table(name = "reminder_channel_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_channel_member_type", columnNames = {"member_id", "channel_type"})
        },
        indexes = {
                @Index(name = "idx_member_type_enabled", columnList = "member_id, channel_type, channel_enabled")
        })
@ToString(of = {"id", "channelType", "verifiedAt"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderChannelAccount extends BaseEntity {

    private static final boolean DEFAULT_ENABLED = true;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "channel_account_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ReminderChannelType channelType;

    @JsonIgnore
    @Column(name = "address_or_token", nullable = false, length = 512)
    private String addressOrToken;

    @JsonIgnore
    @Column(name = "channel_enabled", nullable = false)
    private boolean channelEnabled = DEFAULT_ENABLED;

    @Column(name = "verified_at", columnDefinition = "timestamptz")
    private OffsetDateTime verifiedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    private ReminderChannelAccount(ReminderChannelType channelType, String addressOrToken, boolean channelEnabled, OffsetDateTime verifiedAt, Member member) {
        this.channelType = validNullOrBlank(channelType, "channel type");
        this.addressOrToken = normalizeAndValid(channelType, addressOrToken);
        this.channelEnabled = channelEnabled;
        this.verifiedAt = verifiedAt; // 보통 null로 시작, 검증 성공 시 세팅
        this.member = validMemberIsNull(member);
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static ReminderChannelAccount create(ReminderChannelType channelType, String addressOrToken, Member member) {
        return new ReminderChannelAccount(channelType, addressOrToken, DEFAULT_ENABLED, null, member);
    }

    public static ReminderChannelAccount email(String email, Member member) {
        return create(ReminderChannelType.EMAIL, email, member);
    }

    public static ReminderChannelAccount kakao(String kakaoUserIdOrToken, Member member) {
        return create(ReminderChannelType.KAKAO, kakaoUserIdOrToken, member);
    }

    public static ReminderChannelAccount slack(String incomingWebhookUrl, Member member) {
        return create(ReminderChannelType.SLACK, incomingWebhookUrl, member);
    }

    public static ReminderChannelAccount webPush(String endpoint, Member member) {
        return create(ReminderChannelType.WEBPUSH, endpoint, member);
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    public void updateAddressOrToken(String newAddressOrToken) {
        this.addressOrToken = normalizeAndValid(this.channelType, newAddressOrToken);
        this.verifiedAt = null; // 주소/토큰이 바뀌면 검증 상태 초기화
    }

    public void enable() {
        this.channelEnabled = true;
    }

    public void disable() {
        this.channelEnabled = false;
    }

    public void setVerifiedAt(Clock clock) {
        this.verifiedAt = OffsetDateTime.now(validNullOrBlank(clock, "clock"));
    }

    public boolean canSend() {
        return this.channelEnabled && this.verifiedAt != null;
    }


    // 검증 로직 ----------------------------------------------------------------


    private static Member validMemberIsNull(Member member) {
        if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("member is null or transient");
        }
        return member;
    }

    private static <T> T validNullOrBlank(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        if (value instanceof CharSequence cs && cs.toString().isBlank())
            throw new IllegalArgumentException(name + " is blank");
        return value;
    }


    //TODO: SRP 위반 분리 필요
    private static String normalizeAndValid(ReminderChannelType type, String raw) {
        if (raw == null) throw new IllegalArgumentException("addressOrToken is null");
        String v = raw.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("addressOrToken is blank");

        switch (type) {
            case EMAIL -> {
                // 간단 이메일 검증 (RFC 완벽 X, 실무에선 추가 검증/도메인 제한 가능)
                String email = v.toLowerCase();
                if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
                    throw new IllegalArgumentException("invalid email");
                return email;
            }
            case KAKAO -> {
                // 카카오 사용자 식별자/토큰(길이 최소 보장 정도)
                if (v.length() < 10)
                    throw new IllegalArgumentException("kakao token too short");
                return v;
            }
            case SLACK -> {
                // Incoming Webhook URL 형식 체크(대략)
                if (!v.startsWith("https://hooks.slack.com/services/"))
                    throw new IllegalArgumentException("invalid slack webhook url");
                return v;
            }
            case WEBPUSH -> {
                // 웹푸시 엔드포인트(대략 URL 형식 확인)
                if (!v.startsWith("http://") && !v.startsWith("https://"))
                    throw new IllegalArgumentException("invalid webPush endpoint");
                return v;
            }
            default -> throw new IllegalStateException("unsupported channel type");
        }
    }
}