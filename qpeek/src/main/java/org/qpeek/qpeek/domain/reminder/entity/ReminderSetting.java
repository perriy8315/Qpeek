package org.qpeek.qpeek.domain.reminder.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.member.entity.Member;


/**
 * ReminderSetting (임박/초과 판단에 사용하는 임계값 제공)
 * <p>
 * <도메인 정책>
 * - imminentHours: 마감 임박 알림 시간 (0 ~ 168시간 = 최대 7일)
 * - overdueIntervalHours: 마감 초과 후 반복 알림 주기 (0 = 최초 1회만, 양수 = n 시간마다 반복)
 * - notifyDayBefore: 마감 전날 D-1 알림 여부 (default: true)
 * - notifyOnDueDay: 마감 당일 알림 여부 (default: true)
 * <p>
 * <기본 설정값>
 * - imminentHours = 3h
 * - overdueIntervalHours = 24h
 * - notifyDayBefore = true
 * - notifyOnDueDay = true
 */
@Entity
@Getter
@Check(constraints =
        "imminent_hours >= 0 AND imminent_hours <= 168 AND overdue_interval_hours >= 0")
@Table(name = "reminder_setting")
@ToString(of = {"id", "imminentHours", "overdueIntervalHours", "notifyDayBefore", "notifyOnDueDay"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// TODO: @Cacheable(2차 캐시) 검토.
public class ReminderSetting extends BaseEntity {

    private static final int DEFAULT_IMMINENT_HOURS = 3;
    private static final int DEFAULT_OVERDUE_INTERVAL_HRS = 24;
    private static final boolean DEFAULT_NOTIFY_DAY_BEFORE = true;
    private static final boolean DEFAULT_NOTIFY_ON_DUE = true;

    @Id
    @Column(name = "member_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "imminent_hours", nullable = false)
    private int imminentHours;

    @Column(name = "overdue_interval_hours", nullable = false)
    private int overdueIntervalHours;

    @Column(name = "notify_day_before", nullable = false)
    private boolean notifyDayBefore;

    @Column(name = "notify_on_due_day", nullable = false)
    private boolean notifyOnDueDay;

    @JsonIgnore
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private ReminderSetting(int imminentHours,
                            int overdueIntervalHours,
                            boolean notifyDayBefore,
                            boolean notifyOnDueDay,
                            Member member) {
        validMemberIsNull(member);
        validTimeValues(imminentHours, overdueIntervalHours);
        this.imminentHours = imminentHours;
        this.overdueIntervalHours = overdueIntervalHours;
        this.notifyDayBefore = notifyDayBefore;
        this.notifyOnDueDay = notifyOnDueDay;
        this.member = member;
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static ReminderSetting create(int imminentHours,
                                         boolean notifyDayBefore,
                                         boolean notifyOnDueDay,
                                         int overdueIntervalHours,
                                         Member member) {
        return new ReminderSetting(imminentHours, overdueIntervalHours, notifyDayBefore, notifyOnDueDay, member);
    }

    public static ReminderSetting createDefaultSetting(Member member) {
        return new ReminderSetting(
                DEFAULT_IMMINENT_HOURS,
                DEFAULT_OVERDUE_INTERVAL_HRS,
                DEFAULT_NOTIFY_DAY_BEFORE,
                DEFAULT_NOTIFY_ON_DUE,
                member
        );
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    public void updateAll(int imminentHours,
                          int overdueIntervalHours,
                          boolean notifyDayBefore,
                          boolean notifyOnDueDay) {
        validTimeValues(imminentHours, overdueIntervalHours);
        this.imminentHours = imminentHours;
        this.overdueIntervalHours = overdueIntervalHours;
        this.notifyDayBefore = notifyDayBefore;
        this.notifyOnDueDay = notifyOnDueDay;
    }

    public void changeImminentHours(int value) {
        validImminent(value);
        this.imminentHours = value;
    }

    public void changeOverdueIntervalHours(int value) {
        validOverdueInterval(value);
        this.overdueIntervalHours = value;
    }

    public void enableDayBefore(boolean enabled) {
        this.notifyDayBefore = enabled;
    }

    public void enableOnDueDay(boolean enabled) {
        this.notifyOnDueDay = enabled;
    }


    // 검증 로직 ----------------------------------------------------------------


    private static void validMemberIsNull(Member member) {
        if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("member is null or transient");
        }
    }

    private static void validTimeValues(int imminentHours, int overdueIntervalHours) {
        validImminent(imminentHours);
        validOverdueInterval(overdueIntervalHours);
    }

    private static void validImminent(int imminentHours) {
        if (imminentHours < 0 || imminentHours > 168) {
            throw new IllegalArgumentException("imminentHours must be between 0 and 168");
        }
    }

    private static void validOverdueInterval(int overdueIntervalHours) {
        if (overdueIntervalHours < 0) {
            throw new IllegalArgumentException("overdueIntervalHours must be >= 0");
        }
    }
}