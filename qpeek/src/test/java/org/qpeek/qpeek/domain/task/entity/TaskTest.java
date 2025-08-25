package org.qpeek.qpeek.domain.task.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.queue.entity.TaskQueue;
import org.qpeek.qpeek.domain.task.enums.DueStatus;
import org.qpeek.qpeek.domain.task.enums.TaskStatus;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

class TaskTest {
    private static final Clock BASE_CLOCK =
            Clock.fixed(Instant.parse("2025-08-08T00:00:00Z"), ZoneOffset.UTC);

    private TaskQueue taskQueueWithId(Long id) {
        TaskQueue mock = Mockito.mock(TaskQueue.class);
        Mockito.when(mock.getId()).thenReturn(id);
        return mock;
    }


    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create() success test")
    void create_success() {
        // given
        TaskQueue queue = taskQueueWithId(1L);
        String rawTitle = "  Title  With  Spaces  "; // 원문 보존 (trim X)

        // when
        Task task = Task.create(rawTitle, queue);

        // then
        assertThat(task.getId()).isNull();
        assertThat(task.getTitle()).isEqualTo(rawTitle);
        assertThat(task.getContent()).isNull();
        assertThat(task.getTemplateType()).isNull();
        assertThat(task.getImportance()).isNull();
        assertThat(task.getDueAt()).isNull();
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getProgress()).isEqualTo(0); // default progress 0
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ACTIVE); // default status ACTIVE
        assertThat(task.getPriorityIndex()).isNull();
        assertThat(task.getQueue()).isEqualTo(queue);
    }

    @Test
    @DisplayName("create() fail test : title is null or blank)")
    void create_fail_title_null() {
        //given
        TaskQueue queue = taskQueueWithId(1L);

        //then
        assertThatThrownBy(() -> Task.create(null, queue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title is null");

        assertThatThrownBy(() -> Task.create("   ", queue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title is blank");
    }

    @Test
    @DisplayName("create() fail test : queue is null or transient")
    void create_fail_transient_queue() {
        //given
        TaskQueue transientQueue = taskQueueWithId(null);

        //then
        assertThatThrownBy(() -> Task.create("Task", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queue is null or transient");

        assertThatThrownBy(() -> Task.create("Task", transientQueue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queue is null or transient");
    }


    // ------------------------------------------------------------------
    // editTitle() / editContent()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("editTitle() success test")
    void editTitle_success() {
        //given
        Task task = Task.create("Old title", taskQueueWithId(1L));
        String newTitle = "New  Title";

        //when
        task.editTitle(newTitle);

        //then
        assertThat(task.getTitle()).isEqualTo(newTitle);
    }

    @Test
    @DisplayName("editTitle() fail test : title is null or blank")
    void editTitle_fail() {
        //given
        Task task = Task.create("Title", taskQueueWithId(1L));

        //then
        assertThatThrownBy(() -> task.editTitle(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title is null");

        assertThatThrownBy(() -> task.editTitle("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title is blank");
    }

    @Test
    @DisplayName("editContent() normalization")
    void editContent_normalization() {
        //given
        Task task = Task.create("Title", taskQueueWithId(1L));

        // null 유지
        task.editContent(null); // when
        assertThat(task.getContent()).isNull(); // then

        // 공백 only → null
        task.editContent("   \t  "); // when
        assertThat(task.getContent()).isNull(); // then

        // 원문 보존
        String c = "  content  ";
        task.editContent(c); // when
        assertThat(task.getContent()).isEqualTo(c); // then
    }


    // ------------------------------------------------------------------
    // setDue()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("setDue() success test")
    void setDue_success() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));
        OffsetDateTime time = OffsetDateTime.now(BASE_CLOCK).plusDays(1);

        //when
        task.setDue(time);

        //then
        assertThat(task.getDueAt()).isEqualTo(time);
    }


    // ------------------------------------------------------------------
    // deferTo(), deferDays()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deferTo() success test")
    void deferTo_success() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));
        OffsetDateTime time1 = OffsetDateTime.now(BASE_CLOCK).plusDays(1);

        task.setDue(time1);
        assertThat(task.getDueAt()).isEqualTo(time1);

        OffsetDateTime time2 = time1.plusDays(2);
        task.deferTo(time2);
        assertThat(task.getDueAt()).isEqualTo(time2);
    }

    @Test
    @DisplayName("deferTo() fail test : dateTime is null")
    void deferTo_fail_null() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        //then
        assertThatThrownBy(() -> task.deferTo(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dateTime is null");
    }

    @Test
    @DisplayName("deferDays() success test : dueAt is null")
    void deferDays_success_null_due() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        //when
        task.deferDays(3, BASE_CLOCK); // dueAt=null 일때, now(clock)+days 로 설정

        //then
        assertThat(task.getDueAt())
                .isEqualTo(OffsetDateTime.now(BASE_CLOCK).plusDays(3));
    }

    @Test
    @DisplayName("deferDays() success test : dueAt is not null")
    void deferDays_success_existing_due() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        //when
        OffsetDateTime time = OffsetDateTime.now(BASE_CLOCK).plusDays(1);
        task.setDue(time);

        task.deferDays(5, null); // clock 미사용 경로

        //then
        assertThat(task.getDueAt()).isEqualTo(time.plusDays(5));
    }

    @Test
    @DisplayName("deferDays() fail test : days<=0 or clock=null")
    void deferDays_fail() {
        //when
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        //then
        assertThatThrownBy(() -> task.deferDays(0, BASE_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("days must be > 0");

        assertThatThrownBy(() -> task.deferDays(-1, BASE_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("days must be > 0");

        assertThatThrownBy(() -> task.deferDays(1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clock is null");

    }


    // ------------------------------------------------------------------
    // updateProgress()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateProgress() success test")
    void updateProgress_success() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        task.updateProgress(0); // when
        assertThat(task.getProgress()).isEqualTo(0); // then

        task.updateProgress(100); // when
        assertThat(task.getProgress()).isEqualTo(100); // then
    }

    @Test
    @DisplayName("updateProgress() fail test : progress is -1, 101")
    void updateProgress_fail_out_of_range() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        //then
        assertThatThrownBy(() -> task.updateProgress(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("progress must be 0..100");

        assertThatThrownBy(() -> task.updateProgress(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("progress must be 0..100");
    }


    // ------------------------------------------------------------------
    // updateProgress(), reOpen()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("markCompleted() success test")
    void markCompleted_success() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        // when
        task.markCompleted(BASE_CLOCK); // completedAt=now(clock)

        //then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt())
                .isEqualTo(OffsetDateTime.now(BASE_CLOCK));

        //when
        task.reopen(); // reopen() 은 상태 되돌림

        //then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ACTIVE);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("markCompleted() fail test : clock is null")
    void markCompleted_fail_null_clock() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        //then
        assertThatThrownBy(() -> task.markCompleted(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clock is null");
    }


    // ------------------------------------------------------------------
    // moveTask()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("moveTask() success test")
    void moveTask_success() {
        //given
        TaskQueue queue = taskQueueWithId(1L);
        Task task = Task.create("Task Title", queue);

        //when
        task.moveTask(queue, 10L);

        //then
        assertThat(task.getPriorityIndex()).isEqualTo(10L);
    }

    @Test
    @DisplayName("moveTask() fail test : cross-queue move")
    void moveTask_fail_cross_queue() {
        //given
        TaskQueue queue1 = taskQueueWithId(1L);
        TaskQueue queue2 = taskQueueWithId(2L);
        Task task = Task.create("Task Title", queue1);

        //then
        assertThatThrownBy(() -> task.moveTask(queue2, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("policy: cross-queue move limited");
    }

    @Test
    @DisplayName("moveTask() fail test : target queue is null or transient")
    void moveTask_fail_transient_queue() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));
        TaskQueue transientQueue = taskQueueWithId(null);

        //then
        assertThatThrownBy(() -> task.moveTask(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queue is null or transient");

        assertThatThrownBy(() -> task.moveTask(transientQueue, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queue is null or transient");
    }


    // ------------------------------------------------------------------
    // softDelete() , canHardDelete()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("softDelete() success test")
    void softDelete_changes_status() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        //when
        task.softDelete(BASE_CLOCK);

        //then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TRASHED);
        assertThat(task.getTrashedAt()).isEqualTo(OffsetDateTime.now(BASE_CLOCK));
    }

    @Test
    @DisplayName("canHardDelete(clock) success test")
    void canHardDelete_withoutRetention_success() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));
        assertThat(task.canHardDelete(BASE_CLOCK)).isFalse(); // TaskStatus.ACTIVE 이면 false

        // when
        task.softDelete(BASE_CLOCK);

        // then
        assertThat(task.canHardDelete(BASE_CLOCK)).isTrue(); //TaskStatus TRASHED 이면 true
        assertThat(task.canHardDelete(Clock.offset(BASE_CLOCK, Duration.ofHours(1)))).isTrue(); // 보존시간과 관계 X
    }

    @Test
    @DisplayName("canHardDelete(clock, retention) success test")
    void canHardDelete_withRetention_success() {
        //given
        Task task = Task.create("Task Title", taskQueueWithId(1L));
        Duration retention = Duration.ofMinutes(10);
        assertThat(task.canHardDelete(BASE_CLOCK, retention)).isFalse();

        // when
        task.softDelete(BASE_CLOCK);

        //then
        assertThat(task.canHardDelete(BASE_CLOCK, retention)).isFalse(); // 보존기간 전 (false)
        Clock atBoundary = Clock.fixed(task.getTrashedAt().plus(retention).toInstant(), ZoneOffset.UTC);
        assertThat(task.canHardDelete(atBoundary, retention)).isTrue(); // 경계: trashedAt + retention (true)

        Clock afterBoundary = Clock.offset(atBoundary, Duration.ofMinutes(1));
        assertThat(task.canHardDelete(afterBoundary, retention)).isTrue(); // 보존기간 후 (true)

        assertThat(task.canHardDelete(BASE_CLOCK, Duration.ZERO)).isTrue(); // 보존기간 0 = trashed 직후에도 true
    }

    @Test
    @DisplayName("canHardDelete() fail test : clock, retention is null")
    void canHardDelete_withRetention_nulls() {
        //given
        Duration retention = Duration.ofMinutes(1);
        Task task = Task.create("Task Title", taskQueueWithId(1L));

        //when
        task.softDelete(BASE_CLOCK);


        // then
        assertThatThrownBy(() -> task.canHardDelete(null, retention))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clock is null");


        assertThatThrownBy(() -> task.canHardDelete(BASE_CLOCK, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retention is null");
    }


    // ------------------------------------------------------------------
    // checkDueStatus()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("checkDueStatus() success test")
    void dueStatus_success() {
        // given
        OffsetDateTime now = OffsetDateTime.now(BASE_CLOCK);
        int imminentHours = 3; // 마감 임박 기준

        // when & then - setDue() X
        Task task1 = Task.create("Task Title", taskQueueWithId(1L));
        assertThat(task1.checkDueStatus(now, imminentHours)).isEqualTo(DueStatus.NORMAL);

        // when & then - setDue() O
        Task task2 = Task.create("Task Title", taskQueueWithId(1L));

        // 1) 미래: NORMAL
        task2.setDue(now.plusHours(10));
        assertThat(task2.checkDueStatus(now, imminentHours)).isEqualTo(DueStatus.NORMAL);

        // 2) 경계: now + H → IMMINENT
        task2.setDue(now.plusHours(imminentHours));
        assertThat(task2.checkDueStatus(now, imminentHours)).isEqualTo(DueStatus.IMMINENT);

        // 3) 윈도우 내부: now + 1h → IMMINENT
        task2.setDue(now.plusHours(1));
        assertThat(task2.checkDueStatus(now, imminentHours)).isEqualTo(DueStatus.IMMINENT);

        // 4) 정확히 now → NORMAL
        task2.setDue(now);
        assertThat(task2.checkDueStatus(now, imminentHours)).isEqualTo(DueStatus.NORMAL);

        // 5) 과거(마감 초과) → OVERDUE
        task2.setDue(now.minusMinutes(1));
        assertThat(task2.checkDueStatus(now, imminentHours)).isEqualTo(DueStatus.OVERDUE);
    }
}