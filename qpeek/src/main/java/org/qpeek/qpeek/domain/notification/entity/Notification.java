package org.qpeek.qpeek.domain.notification.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.member.entity.Member;
import org.qpeek.qpeek.domain.notification.enums.NotificationChannelType;
import org.qpeek.qpeek.domain.notification.enums.NotificationType;
import org.qpeek.qpeek.domain.task.entity.Task;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Notification (알림 이벤트)
 * <p>
 * <도메인 규칙/정책>
 * - member: 알림 대상 회원(필수). 생성 후 변경 불가(updatable=false).
 * - task: 관련 작업(선택). report 타입 등은 null 가능. 생성 후 변경 불가(updatable=false).
 * - type: D-1 / IMMINENT / DUE / OVERDUE / REPORT 등 알림 유형.
 * - channel: EMAIL / KAKAO / SLACK / WEBPUSH 등 발송 채널.
 * - scheduledAt: 발송 예정 시각(timestamptz). null 불가.
 * - sentAt: 실제 발송 시각(timestamptz). null이면 아직 미발송 상태.
 * <설계 메모>
 * - 인덱스: (scheduled_at), (member_id, scheduled_at), (task_id), (sent_at) 권장.
 * - 무결성: sent_at >= scheduled_at 또는 sent_at IS NULL 보장(@Check).
 */
@Entity
@Getter
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_scheduled_at", columnList = "scheduled_at"),
                @Index(name = "idx_notifications_member_scheduled", columnList = "member_id, scheduled_at"),
                @Index(name = "idx_notifications_task", columnList = "task_id"),
                @Index(name = "idx_notifications_sent_at", columnList = "sent_at")
        })
@Check(constraints = "sent_at IS NULL OR sent_at >= scheduled_at")
@ToString(of = {"id", "type", "channel", "scheduledAt", "sentAt"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "notification_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannelType channel;

    @Column(name = "scheduled_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime scheduledAt;

    @Column(name = "sent_at", columnDefinition = "timestamptz")
    private OffsetDateTime sentAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", updatable = false)
    private Task task;

    private Notification(NotificationType type, NotificationChannelType channel, OffsetDateTime scheduledAt, Member member, Task task) {
        this.type = validNullOrBlank(type, "type");
        this.channel = validNullOrBlank(channel, "channel");
        this.scheduledAt = validNullOrBlank(scheduledAt, "scheduledAt");
        this.sentAt = null;
        this.member = validMemberIsNull(member);
        this.task = validTaskIsNull(task);
    }

    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static Notification schedule(NotificationType type, NotificationChannelType channel, OffsetDateTime scheduledAt, Member member) {
        return new Notification(type, channel, scheduledAt, member, null);
    }

    public static Notification scheduleForTask(NotificationType type, NotificationChannelType channel, OffsetDateTime scheduledAt, Member member, Task task) {
        return new Notification(type, channel, scheduledAt, member, task);
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    /**
     * now 기준 발송 가능 여부
     */
    public boolean canSend(OffsetDateTime now) {
        validNullOrBlank(now, "now");
        return sentAt == null && !now.isBefore(scheduledAt); // now >= scheduledAt
    }

    /**
     * 발송 처리
     */
    public void markSent(Clock clock) {
        validNullOrBlank(clock, "clock");
        this.sentAt = OffsetDateTime.now(clock);
    }

    /**
     * 재스케줄(미발송 상태에서만 허용)
     */
    public void reschedule(OffsetDateTime newTime) {
        if (this.sentAt != null) throw new IllegalStateException("already sent");
        this.scheduledAt = validNullOrBlank(newTime, "scheduledAt");
    }


    // 검증 로직 ----------------------------------------------------------------


    private static Member validMemberIsNull(Member member) {
        if (member == null || member.getId() == null)
            throw new IllegalArgumentException("member is null or transient");
        return member;
    }

    private static Task validTaskIsNull(Task task) {
        if (task == null) return null;
        if (task.getId() == null)
            throw new IllegalArgumentException("task is transient");
        return task;
    }

    private static <T> T validNullOrBlank(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        if (value instanceof CharSequence cs && cs.toString().isBlank())
            throw new IllegalArgumentException(name + " is blank");
        return value;
    }
}
