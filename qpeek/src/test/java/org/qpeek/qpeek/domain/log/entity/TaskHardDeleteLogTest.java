package org.qpeek.qpeek.domain.log.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.task.entity.Task;
import org.qpeek.qpeek.domain.trash.entity.TrashItem;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

class TaskHardDeleteLogTest {

    private final OffsetDateTime trashedAt = OffsetDateTime.parse("2025-08-08T00:00:00Z");
    private final OffsetDateTime hardDeleted = trashedAt.plusDays(1);

    private TrashItem trashItemWithTask(Long taskId, OffsetDateTime trashedAt) {
        Task taskMock = Mockito.mock(Task.class);
        Mockito.when(taskMock.getId()).thenReturn(taskId);

        TrashItem trashItemMock = Mockito.mock(TrashItem.class);
        Mockito.when(trashItemMock.getTask()).thenReturn(taskMock);
        Mockito.when(trashItemMock.getTrashedAt()).thenReturn(trashedAt);
        return trashItemMock;
    }

    // ------------------------------------------------------------------
    // createFromTrashItem()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createFromTrashItem() success test")
    void createFromTrashItem_success() {
        // given
        Long taskId = 1L;
        TrashItem trashItem = trashItemWithTask(taskId, trashedAt);

        // when
        TaskHardDeleteLog log = TaskHardDeleteLog.createFromTrashItem(trashItem, hardDeleted);

        // then
        assertThat(log.getTaskId()).isEqualTo(taskId);
        assertThat(log.getTrashedAt()).isEqualTo(trashedAt);
        assertThat(log.getHardDeletedAt()).isEqualTo(hardDeleted);
    }

    @Test
    @DisplayName("createFromTrashItem() fail test : trashItem is null")
    void createFromTrashItem_fail_null_item() {
        //then
        assertThatThrownBy(() -> TaskHardDeleteLog.createFromTrashItem(null, hardDeleted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trashItem is null");
    }

    @Test
    @DisplayName("createFromTrashItem() fail test : hardDeletedAt is null")
    void createFromTrashItem_fail_null_hardDeletedAt() {
        //given
        TrashItem trashItem = trashItemWithTask(1L, trashedAt);

        //then
        assertThatThrownBy(() -> TaskHardDeleteLog.createFromTrashItem(trashItem, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("hardDeletedAt is null");
    }

    @Test
    @DisplayName("createFromTrashItem() fail test : task is null")
    void createFromTrashItem_fail_null_task() {
        //given
        TrashItem trashItem = Mockito.mock(TrashItem.class);
        Mockito.when(trashItem.getTask()).thenReturn(null);
        Mockito.when(trashItem.getTrashedAt()).thenReturn(trashedAt);

        //then
        assertThatThrownBy(() -> TaskHardDeleteLog.createFromTrashItem(trashItem, hardDeleted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task is null");
    }

    @Test
    @DisplayName("createFromTrashItem() fail test : task is transient")
    void createFromTrashItem_fail_transient_task() {
        //given
        Task transientTask = Mockito.mock(Task.class);
        Mockito.when(transientTask.getId()).thenReturn(null);

        TrashItem trashItem = Mockito.mock(TrashItem.class);
        Mockito.when(trashItem.getTask()).thenReturn(transientTask);
        Mockito.when(trashItem.getTrashedAt()).thenReturn(trashedAt);

        //then
        assertThatThrownBy(() -> TaskHardDeleteLog.createFromTrashItem(trashItem, hardDeleted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    @DisplayName("createFromTrashItem() fail test : trashedAt is null")
    void createFromTrashItem_fail_null_trashedAt_from_item() {
        //given
        TrashItem trashItem = trashItemWithTask(1L, null);

        //then
        assertThatThrownBy(() -> TaskHardDeleteLog.createFromTrashItem(trashItem, hardDeleted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trashedAt is null");
    }


    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create() success test")
    void create_success() {
        //when
        TaskHardDeleteLog log = TaskHardDeleteLog.create(1L, trashedAt, hardDeleted);

        //then
        assertThat(log.getTaskId()).isEqualTo(1L);
        assertThat(log.getTrashedAt()).isEqualTo(trashedAt);
        assertThat(log.getHardDeletedAt()).isEqualTo(hardDeleted);
    }

    @Test
    @DisplayName("create() success test : hardDeletedAt == trashedAt")
    void create_success_equal_boundary() {
        //when
        TaskHardDeleteLog log = TaskHardDeleteLog.create(1L, trashedAt, trashedAt);

        //then
        assertThat(log.getHardDeletedAt()).isEqualTo(trashedAt);
    }

    @Test
    @DisplayName("create() fail test: hardDeletedAt < trashedAt")
    void create_fail_hardDeleted_before_trashed() {
        assertThatThrownBy(() -> TaskHardDeleteLog.create(1L, trashedAt, trashedAt.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("hardDeletedAt must be >= trashedAt");
    }

    @Test
    @DisplayName("create() fail test : null arguments (taskId/trashedAt/hardDeletedAt)")
    void create_fail_null_arguments() {
        //then
        assertThatThrownBy(() -> TaskHardDeleteLog.create(null, trashedAt, hardDeleted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId is null");

        assertThatThrownBy(() -> TaskHardDeleteLog.create(1L, null, hardDeleted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trashedAt is null");

        assertThatThrownBy(() -> TaskHardDeleteLog.create(1L, trashedAt, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("hardDeletedAt is null");
    }
}