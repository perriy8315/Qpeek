package org.qpeek.qpeek.domain.log.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.task.entity.Task;
import org.qpeek.qpeek.domain.task.enums.TaskStatus;

import java.time.Clock;
import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "completion_logs",
        uniqueConstraints = @UniqueConstraint(name = "uk_completion_task", columnNames = "task_id"),
        indexes = {
                @Index(name = "idx_completion_queue_date", columnList = "queue_id, completed_at"),
                @Index(name = "idx_completion_completed_at", columnList = "completed_at")
        })
@Check(constraints = "progress BETWEEN 0 AND 100 AND btrim(title_snapshot) <> ''")
@ToString(of = {"id", "queueId", "completedAt", "progress"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompletionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "completion_id")
    private Long id;

    @Column(name = "completed_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime completedAt;

    @Column(name = "title_snapshot", nullable = false, length = 225)
    private String titleSnapshot;

    @Column(name = "progress", nullable = false)
    private int progress;

    @Column(name = "queue_id", nullable = false)
    private Long queueId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    private Task task;

    private CompletionLog(OffsetDateTime completedAt, String titleSnapshot, int progress, Long queueId, Task task) {
        this.completedAt = validNonNull(completedAt, "completedAt");
        this.titleSnapshot = normalizeTitle(titleSnapshot);
        this.progress = validProgressSize(progress);
        this.queueId = validQueueIdIsNull(queueId);
        this.task = validTaskIsNull(task);
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static CompletionLog createFromTask(Task task, Clock clock) {
        validNonNull(clock, "clock");
        validTaskIsNull(task);

        if (task.getStatus() != TaskStatus.COMPLETED)
            throw new IllegalStateException("task must be COMPLETED to create CompletionLog");

        if (task.getQueue() == null || task.getQueue().getId() == null)
            throw new IllegalStateException("task.queue is null or transient");

        Long queueId = task.getQueue().getId();
        OffsetDateTime completedAt = task.getCompletedAt() != null ? task.getCompletedAt() : OffsetDateTime.now(clock);
        String titleSnap = task.getTitle();
        int progress = task.getProgress();
        return new CompletionLog(completedAt, titleSnap, progress, queueId, task);
    }

    public static CompletionLog create(Task task,
                                       Long queueId,
                                       OffsetDateTime completedAt,
                                       String titleSnapshot,
                                       int progress) {
        return new CompletionLog(completedAt, titleSnapshot, progress, queueId, task);
    }


    // 검증 로직 ----------------------------------------------------------------


    private static Task validTaskIsNull(Task task) {
        if (task == null || task.getId() == null)
            throw new IllegalArgumentException("task is null or transient");
        return task;
    }

    private static Long validQueueIdIsNull(Long id) {
        if (id == null) throw new IllegalArgumentException("queueId is null");
        if (id <= 0) throw new IllegalArgumentException("queueId must be positive");
        return id;
    }

    private static int validProgressSize(int progress) {
        if (progress < 0 || progress > 100) throw new IllegalArgumentException("progress must be 0 ~ 100");
        return progress;
    }

    private static String normalizeTitle(String raw) {
        if (raw == null) throw new IllegalArgumentException("titleSnapshot is null");
        if (raw.isBlank()) throw new IllegalArgumentException("titleSnapshot is blank");
        if (raw.length() > 255) throw new IllegalArgumentException("titleSnapshot length > 255");
        return raw;
    }

    private static <T> T validNonNull(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        return value;
    }
}