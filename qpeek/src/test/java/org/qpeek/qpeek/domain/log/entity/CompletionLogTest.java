package org.qpeek.qpeek.domain.log.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.queue.entity.TaskQueue;
import org.qpeek.qpeek.domain.task.entity.Task;
import org.qpeek.qpeek.domain.task.enums.TaskStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class CompletionLogTest {

    private final Clock baseClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    private Task taskWithQueue(Long taskId,
                               Long queueId,
                               TaskStatus status,
                               int progress,
                               OffsetDateTime completedAt) {
        Task task = Mockito.mock(Task.class);
        TaskQueue queue = Mockito.mock(TaskQueue.class);

        Mockito.when(task.getId()).thenReturn(taskId);
        Mockito.when(task.getQueue()).thenReturn(queue);
        Mockito.when(task.getStatus()).thenReturn(status);
        Mockito.when(task.getTitle()).thenReturn("Test 코드 작성");
        Mockito.when(task.getProgress()).thenReturn(progress);
        Mockito.when(task.getCompletedAt()).thenReturn(completedAt);
        Mockito.when(queue.getId()).thenReturn(queueId);
        return task;
    }

    @Test
    @DisplayName("createFromTask() success test")
    void createFromTask_success() {
        //given
        Long queueId = 1L;
        OffsetDateTime completedAt = OffsetDateTime.ofInstant(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        Task task = taskWithQueue(1L, queueId, TaskStatus.COMPLETED, 80, completedAt);

        //when
        CompletionLog log = CompletionLog.createFromTask(task, baseClock);

        //then
        assertThat(log.getTask()).isEqualTo(task);
        assertThat(log.getQueueId()).isEqualTo(queueId);
        assertThat(log.getTitleSnapshot()).isEqualTo("Test 코드 작성");
        assertThat(log.getProgress()).isEqualTo(80);
        assertThat(log.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("createFromTask() fail test : task is null or transient")
    void createFromTask_fail_transient_task() {
        //given 
        Task transientTask = Mockito.mock(Task.class);
        Mockito.when(transientTask.getId()).thenReturn(null);

        //then
        assertThatThrownBy(() -> CompletionLog.createFromTask(null, baseClock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task is null or transient");

        assertThatThrownBy(() -> CompletionLog.createFromTask(transientTask, baseClock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task is null or transient");
    }

    @Test
    @DisplayName("createFromTask() fail test : task.status is not COMPLETED")
    void createFromTask_fail_not_completed_task() {
        //given 
        Task task = taskWithQueue(1L, 1L, TaskStatus.ACTIVE, 0, null);

        //then
        assertThatThrownBy(() -> CompletionLog.createFromTask(task, baseClock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("task must be COMPLETED to create CompletionLog");
    }

    @Test
    @DisplayName("createFromTask() fail test : task.queue is null")
    void createFromTask_fail_null_queue() {
        //given 
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getId()).thenReturn(1L);
        Mockito.when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        Mockito.when(task.getQueue()).thenReturn(null);

        //then
        assertThatThrownBy(() -> CompletionLog.createFromTask(task, baseClock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("task.queue is null or transient");
    }

    @Test
    @DisplayName("createFromTask() fail test : task.queue is transient")
    void createFromTask_fail_queue_transient() {
        //given 
        Task task = Mockito.mock(Task.class);
        TaskQueue queue = Mockito.mock(TaskQueue.class);

        Mockito.when(queue.getId()).thenReturn(null);

        Mockito.when(task.getId()).thenReturn(1L);
        Mockito.when(task.getStatus()).thenReturn(TaskStatus.COMPLETED);
        Mockito.when(task.getQueue()).thenReturn(queue);

        //then
        assertThatThrownBy(() -> CompletionLog.createFromTask(task, baseClock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("task.queue is null or transient");
    }


    @Test
    @DisplayName("create() success test")
    void create_success() {
        // given
        Long queueId = 10L;
        Task task = taskWithQueue(1L, queueId, TaskStatus.COMPLETED, 100, null);

        // when
        CompletionLog log = CompletionLog.createFromTask(task, baseClock);

        // then
        assertThat(log.getCompletedAt()).isEqualTo(OffsetDateTime.now(baseClock));
    }

    @Test
    void create_fail_invalid_arguments() {
        //given
        Task task = taskWithQueue(1L, 10L, TaskStatus.COMPLETED, 0, OffsetDateTime.now(baseClock));

        // titleSnapshot : null, blank, 256자 초과
        assertThatThrownBy(() -> CompletionLog.create(task, 10L, OffsetDateTime.now(baseClock), null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("titleSnapshot is null");

        assertThatThrownBy(() -> CompletionLog.create(task, 10L, OffsetDateTime.now(baseClock), "  ", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("titleSnapshot is blank");

        assertThatThrownBy(() -> CompletionLog.create(task, 10L, OffsetDateTime.now(baseClock), "a".repeat(256), 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("titleSnapshot length > 255");


        // progress 범위 : -1 , 101
        assertThatThrownBy(() -> CompletionLog.create(task, 10L, OffsetDateTime.now(baseClock), "Test 코드 작성", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("progress must be 0 ~ 100");

        assertThatThrownBy(() -> CompletionLog.create(task, 10L, OffsetDateTime.now(baseClock), "Test 코드 작성", 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("progress must be 0 ~ 100");


        // queueId : null, 음수, 0
        assertThatThrownBy(() -> CompletionLog.create(task, null, OffsetDateTime.now(baseClock), "Test 코드 작성", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queueId is null");

        assertThatThrownBy(() -> CompletionLog.create(task, 0L, OffsetDateTime.now(baseClock), "Test 코드 작성", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queueId must be positive");

        assertThatThrownBy(() -> CompletionLog.create(task, -1L, OffsetDateTime.now(baseClock), "Test 코드 작성", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queueId must be positive");


        // completedAt : null
        assertThatThrownBy(() -> CompletionLog.create(task, 10L, null, "title", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("completedAt is null");
    }

    @Test
    @DisplayName("create() fail test : task is null or transient")
    void create_transient_task() {
        //given
        Task transientTask = Mockito.mock(Task.class);
        Mockito.when(transientTask.getId()).thenReturn(null);

        //then
        assertThatThrownBy(() -> CompletionLog.create(transientTask, 10L, OffsetDateTime.now(baseClock), "Test 코드 작성", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task is null or transient");

        assertThatThrownBy(() -> CompletionLog.create(null, 10L, OffsetDateTime.now(baseClock), "Test 코드 작성", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task is null or transient");
    }
}