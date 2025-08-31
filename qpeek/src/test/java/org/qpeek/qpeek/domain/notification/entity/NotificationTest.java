package org.qpeek.qpeek.domain.notification.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.member.entity.Member;
import org.qpeek.qpeek.domain.notification.enums.NotificationChannelType;
import org.qpeek.qpeek.domain.notification.enums.NotificationType;
import org.qpeek.qpeek.domain.task.entity.Task;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class NotificationTest {
    private final OffsetDateTime scheduledAt = OffsetDateTime.parse("2025-08-08T00:00:00Z");
    private final OffsetDateTime beforeScheduled = scheduledAt.minusMinutes(5);
    private final OffsetDateTime afterScheduled = scheduledAt.plusMinutes(30);

    private Member memberWithId(Long id) {
        Member mock = Mockito.mock(Member.class);
        Mockito.when(mock.getId()).thenReturn(id);
        return mock;
    }

    private Task taskWithId(Long id) {
        Task mock = Mockito.mock(Task.class);
        Mockito.when(mock.getId()).thenReturn(id);
        return mock;
    }

    // ------------------------------------------------------------------
    // schedule()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("schedule() success test")
    void schedule_success() {
        // given
        Member member = memberWithId(1L);

        // when
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, member);

        // then
        assertThat(notification.getId()).isNull();
        assertThat(notification.getType()).isEqualTo(NotificationType.DUE);
        assertThat(notification.getChannel()).isEqualTo(NotificationChannelType.EMAIL);
        assertThat(notification.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(notification.getSentAt()).isNull();
        assertThat(notification.getMember()).isEqualTo(member);
        assertThat(notification.getTask()).isNull();
    }

    @Test
    @DisplayName("schedule() fail test : null arguments")
    void schedule_fail_null_arguments() {
        //given
        Member member = memberWithId(1L);

        //then
        assertThatThrownBy(() -> Notification.schedule(null, NotificationChannelType.EMAIL, scheduledAt, member)) // type
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("type is null");

        assertThatThrownBy(() -> Notification.schedule(NotificationType.DUE, null, scheduledAt, member)) // channel
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("channel is null");

        assertThatThrownBy(() -> Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, null, member)) // scheduledAt
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scheduledAt is null");

        assertThatThrownBy(() -> Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, null)) // member
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");
    }

    @Test
    @DisplayName("schedule() fail test : member is transient")
    void schedule_fail_transient_member() {
        // given
        Member transientMember = memberWithId(null);

        // then
        assertThatThrownBy(() -> Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, transientMember))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");
    }


    // ------------------------------------------------------------------
    // scheduleForTask()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("scheduleForTask() success test")
    void scheduleForTask_success() {
        // given
        Member member = memberWithId(1L);
        Task task = taskWithId(1L);

        // when
        Notification notification = Notification.scheduleForTask(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, member, task);

        // then
        assertThat(notification.getMember()).isEqualTo(member);
        assertThat(notification.getTask()).isEqualTo(task);
        assertThat(notification.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    @DisplayName("scheduleForTask() success test : task is null")
    void scheduleForTask_success_task_null() {
        // given
        Member member = memberWithId(1L);

        // when
        Notification notification = Notification.scheduleForTask(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, member, null);

        // then
        assertThat(notification.getTask()).isNull();
    }

    @Test
    @DisplayName("scheduleForTask() fail test : member and task is null or transient")
    void scheduleForTask_fail_transient_member_and_task() {
        Member transientMember = memberWithId(null);
        Task transientTask = taskWithId(null);
        Member member = memberWithId(1L);

        assertThatThrownBy(() ->
                Notification.scheduleForTask(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, transientMember, taskWithId(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");

        assertThatThrownBy(() ->
                Notification.scheduleForTask(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, member, transientTask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task is transient");
    }

    @Test
    @DisplayName("scheduleForTask() fail test : null arguments")
    void scheduleForTask_fail_null_arguments() {
        //given
        Member member = memberWithId(1L);
        Task task = taskWithId(1L);

        //then
        assertThatThrownBy(() -> Notification.scheduleForTask(null, NotificationChannelType.EMAIL, scheduledAt, member, task)) // type
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("type is null");

        assertThatThrownBy(() -> Notification.scheduleForTask(NotificationType.DUE, null, scheduledAt, member, task)) // channel
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("channel is null");

        assertThatThrownBy(() -> Notification.scheduleForTask(NotificationType.DUE, NotificationChannelType.EMAIL, null, member, task)) // scheduledAt
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scheduledAt is null");
    }


    // ------------------------------------------------------------------
    // canSend(now)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("canSend() success test : now == scheduledAt")
    void canSend_success_equal_boundary() {
        //when
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));

        //then
        assertThat(notification.canSend(scheduledAt)).isTrue();
    }

    @Test
    @DisplayName("canSend() success test : now > scheduledAt")
    void canSend_success_after() {
        //when
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));

        //then
        assertThat(notification.canSend(afterScheduled)).isTrue();
    }

    @Test
    @DisplayName("canSend() success test : now < scheduledAt")
    void canSend_success_before() {
        //when
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));

        //then
        assertThat(notification.canSend(beforeScheduled)).isFalse();
    }

    @Test
    @DisplayName("canSend() fail test : now is null")
    void canSend_fail_now_null() {
        //when
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));

        //then
        assertThatThrownBy(() -> notification.canSend(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("now is null");
    }

    @Test
    @DisplayName("canSend() fail test : after markSent()")
    void canSend_false_after_sent() {
        //given
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));
        Clock clock = Clock.fixed(afterScheduled.toInstant(), ZoneOffset.UTC);

        //when
        notification.markSent(clock);

        //then
        assertThat(notification.getSentAt()).isEqualTo(OffsetDateTime.ofInstant(afterScheduled.toInstant(), ZoneOffset.UTC));
        assertThat(notification.canSend(afterScheduled.plusMinutes(1))).isFalse();
    }


    // ------------------------------------------------------------------
    // markSent(clock)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("markSent() success test : sentAt == now(clock)")
    void markSent_success() {
        //given
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));
        Instant sendInstant = afterScheduled.toInstant();
        Clock clock = Clock.fixed(sendInstant, ZoneOffset.UTC);

        //when
        notification.markSent(clock);


        //then
        assertThat(notification.getSentAt()).isEqualTo(OffsetDateTime.ofInstant(sendInstant, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("markSent() fail test : clock is null")
    void markSent_fail_null_clock() {
        //when
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));

        //then
        assertThatThrownBy(() -> notification.markSent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clock is null");
    }


    // ------------------------------------------------------------------
    // reschedule(newTime)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("reschedule() success test")
    void reschedule_success() {
        //given
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));
        OffsetDateTime newTime = scheduledAt.plusHours(2);

        //when
        notification.reschedule(newTime);

        //then
        assertThat(notification.getScheduledAt()).isEqualTo(newTime);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    @DisplayName("reschedule() success test")
    void reschedule_success_past_allowed() {
        //given
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));
        OffsetDateTime earlier = scheduledAt.minusHours(1);

        //when
        notification.reschedule(earlier);

        //then
        assertThat(notification.getScheduledAt()).isEqualTo(earlier);
    }

    @Test
    @DisplayName("reschedule() fail test : already sent")
    void reschedule_fail_already_sent() {
        //given
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));
        Clock clock = Clock.fixed(afterScheduled.toInstant(), ZoneOffset.UTC);

        //when
        notification.markSent(clock);

        // then
        assertThatThrownBy(() -> notification.reschedule(afterScheduled.plusHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("already sent");
    }

    @Test
    @DisplayName("reschedule() fail test : newTime is null")
    void reschedule_fail_null_new_time() {
        //when
        Notification notification = Notification.schedule(NotificationType.DUE, NotificationChannelType.EMAIL, scheduledAt, memberWithId(1L));

        //then
        assertThatThrownBy(() -> notification.reschedule(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scheduledAt is null");
    }
}