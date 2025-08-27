package org.qpeek.qpeek.domain.trash.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.task.entity.Task;
import org.qpeek.qpeek.domain.task.enums.TaskStatus;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

class TrashItemTest {
    private static final Clock BASE_CLOCK =
            Clock.fixed(Instant.parse("2025-08-08T00:00:00Z"), ZoneOffset.UTC);

    private Task taskWithIdAndStatus(Long id, TaskStatus status) {
        Task mock = Mockito.mock(Task.class);
        Mockito.when(mock.getId()).thenReturn(id);
        Mockito.when(mock.getStatus()).thenReturn(status);
        return mock;
    }


    // ------------------------------------------------------------------
    // create(OffsetDateTime trashedAt, Duration retention, Task task)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create()trashedAt, duration, task) success test")
    void create_with_duration_success() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        Duration retention = Duration.ofDays(7);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);

        // when
        TrashItem trash = TrashItem.create(trashedAt, retention, task);

        // then
        assertThat(trash.getId()).isNull();
        assertThat(trash.getTrashedAt()).isEqualTo(trashedAt);
        assertThat(trash.getRetentionUntil()).isEqualTo(trashedAt.plus(retention));
        assertThat(trash.getTask()).isEqualTo(task);
    }

    @Test
    @DisplayName("create(trashedAt, duration, task) fail test : duration <= 0 or null")
    void create_with_duration_fail_invalid_duration() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);

        // then
        assertThatThrownBy(() -> TrashItem.create(trashedAt, Duration.ZERO, task))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duration must be > 0");

        assertThatThrownBy(() -> TrashItem.create(trashedAt, Duration.ofSeconds(-1), task))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duration must be > 0");
    }


    // ------------------------------------------------------------------
    // create(OffsetDateTime trashedAt, OffsetDateTime retentionUntil, Task task)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create(trashedAt, retentionUntil, task) success test")
    void create_with_retentionUntil_success() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        OffsetDateTime retentionUntil = trashedAt.plusDays(3);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);

        // when
        TrashItem trash = TrashItem.create(trashedAt, retentionUntil, task);

        // then
        assertThat(trash.getTrashedAt()).isEqualTo(trashedAt);
        assertThat(trash.getRetentionUntil()).isEqualTo(retentionUntil);
        assertThat(trash.getTask()).isEqualTo(task);
    }

    @Test
    @DisplayName("create(trashedAt, retentionUntil, task) fail test : trashedAt is null")
    void create_with_retentionUntil_fail_trashedAt_null() {
        // given
        OffsetDateTime retentionUntil = OffsetDateTime.now(BASE_CLOCK).plusDays(7);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);

        // then
        assertThatThrownBy(() -> TrashItem.create(null, retentionUntil, task))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trashedAt is null");
    }

    @Test
    @DisplayName("create(trashedAt, retentionUntil, task) fail test : retentionUntil < trashedAt")
    void create_with_retentionUntil_fail_order_violation() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        OffsetDateTime retentionUntil = trashedAt.minusMinutes(1);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);

        // then
        assertThatThrownBy(() -> TrashItem.create(trashedAt, retentionUntil, task))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retentionUntil must be >= trashedAt");
    }

    @Test
    @DisplayName("create( fail test : task is null or transient")
    void create_fail_transient_task() {
        // given
        Task transientTask = taskWithIdAndStatus(null, TaskStatus.TRASHED);
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        OffsetDateTime retentionUntil = trashedAt.plusDays(1);

        // then
        assertThatThrownBy(() -> TrashItem.create(trashedAt, retentionUntil, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task is null or transient");

        assertThatThrownBy(() -> TrashItem.create(trashedAt, retentionUntil, transientTask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task is null or transient");
    }

    @Test
    @DisplayName("create() fail test : task status is not TRASHED")
    void create_fail_task_status_not_trashed() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        OffsetDateTime retentionUntil = trashedAt.plusDays(1);
        Task nonTrashedTask = taskWithIdAndStatus(1L, TaskStatus.ACTIVE);

        // then
        assertThatThrownBy(() -> TrashItem.create(trashedAt, retentionUntil, nonTrashedTask))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("task status must be TRASHED to create TrashItem");
    }


    // ------------------------------------------------------------------
    // canHardDelete()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("canHardDelete() success test")
    void canHardDelete_success() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        OffsetDateTime retentionUntil = trashedAt.plusHours(2);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);
        TrashItem trash = TrashItem.create(trashedAt, retentionUntil, task);

        // then
        assertThat(trash.canHardDelete(retentionUntil.minusMinutes(1))).isFalse(); // before
        assertThat(trash.canHardDelete(retentionUntil)).isTrue();                  // boundary
        assertThat(trash.canHardDelete(retentionUntil.plusSeconds(1))).isTrue();  // after
    }

    @Test
    @DisplayName("canHardDelete(now) fail test : now is null")
    void canHardDelete_fail_null() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);
        TrashItem trash = TrashItem.create(trashedAt, trashedAt.plusDays(1), task);

        // then
        assertThatThrownBy(() -> trash.canHardDelete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("now is null");
    }


    // ------------------------------------------------------------------
    // extendRetentionUntil()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("extendRetentionUntil() success test")
    void extendRetentionUntil_success() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        OffsetDateTime retentionUntil = trashedAt.plusDays(7);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);
        TrashItem trash = TrashItem.create(trashedAt, retentionUntil, task);

        OffsetDateTime later = retentionUntil.plusDays(3);

        // when
        trash.extendRetentionUntil(later);

        // then
        assertThat(trash.getRetentionUntil()).isEqualTo(later);
    }

    @Test
    @DisplayName("extendRetentionUntil() success test : Retention until not be changed when newRetention is not later")
    void extendRetentionUntil_success_no_change_when_not_later() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        OffsetDateTime retentionUntil = trashedAt.plusDays(7);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);
        TrashItem trash = TrashItem.create(trashedAt, retentionUntil, task);

        OffsetDateTime earlierButAfterTrashed = retentionUntil.minusDays(1);

        // when
        trash.extendRetentionUntil(retentionUntil);
        assertThat(trash.getRetentionUntil()).isEqualTo(retentionUntil);

        trash.extendRetentionUntil(earlierButAfterTrashed);
        assertThat(trash.getRetentionUntil()).isEqualTo(retentionUntil);
    }

    @Test
    @DisplayName("extendRetentionUntil(newRetentionUntil) fail test : New retention is null or before trashedAt")
    void extendRetentionUntil_fail_invalid() {
        // given
        OffsetDateTime trashedAt = OffsetDateTime.now(BASE_CLOCK);
        OffsetDateTime retentionUntil = trashedAt.plusDays(2);
        Task task = taskWithIdAndStatus(1L, TaskStatus.TRASHED);
        TrashItem trash = TrashItem.create(trashedAt, retentionUntil, task);

        // then
        assertThatThrownBy(() -> trash.extendRetentionUntil(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("newRetentionUntil is null");

        assertThatThrownBy(() -> trash.extendRetentionUntil(trashedAt.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("newRetentionUntil must be >= trashedAt");
    }
}