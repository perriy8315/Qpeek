package org.qpeek.qpeek.domain.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.queue.entity.TaskQueue;
import org.qpeek.qpeek.domain.task.enums.DueStatus;
import org.qpeek.qpeek.domain.task.enums.TaskImportance;
import org.qpeek.qpeek.domain.task.enums.TaskStatus;
import org.qpeek.qpeek.domain.task.enums.TaskTemplateType;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Database (저장소)
 * <p>
 * <도메인 규칙/정책>
 * - owner(member): 저장소 소유자. 생성 후 변경 불가(updatable = false).
 * - name: 회원 단위 유니크((member_id, name)). 원문 보존(공백 포함), 전부 공백만은 금지, 길이 ≤ 100.
 * - description: 선택(Optional). 원문 보존, 전부 공백만이면 null로 정규화, 길이 ≤ 500.
 * - deletedAt: 소프트 삭제 시각. OffsetDateTime(timestamptz). null이면 활성 상태.
 * - createdAt/updatedAt: BaseEntity 에서 감사(Auditing)로 관리(UTC 저장 권장).
 * - 권한: rename / deleteByOwner 는 소유자만 가능(ensureOwner로 검증).
 * <p>
 * <설계 메모>
 * - 유니크 제약: (member_id, name)으로 회원별 이름 중복 방지.
 * - 소프트 삭제 후 이름 재사용 필요 시 부분 유니크 인덱스 고려: WHERE deleted_at IS NULL.
 * - 인덱스: member_id(목록/카운트), deleted_at(정리 배치).
 * - 공백 정책: trim 미사용, isBlank()로 공백-only만 차단하여 원문 보존.
 * - 삭제 전략: 소프트 삭제 → 배치 퍼지에서 하위(queues→tasks…) 명시적 삭제.
 */
@Entity
@Getter
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_queue", columnList = "queue_id"),
        @Index(name = "idx_tasks_queue_status_priority", columnList = "queue_id, status, priority_index"),
        @Index(name = "idx_tasks_queue_due", columnList = "queue_id, due_at")
})
@Check(constraints = "progress BETWEEN 0 AND 100 AND btrim(title) <> ''")
@ToString(of = {"id", "title", "status", "progress", "priorityIndex"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "task_id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", length = 20)
    private TaskTemplateType templateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "importance")
    private TaskImportance importance;

    @Column(name = "due_at", columnDefinition = "timestamptz")
    private OffsetDateTime dueAt;

    @Column(name = "completed_at", columnDefinition = "timestamptz")
    private OffsetDateTime completedAt;

    @Column(name = "trashed_at", columnDefinition = "timestamptz")
    private OffsetDateTime trashedAt;

    @Column(name = "progress", nullable = false)
    private int progress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "priority_index")
    private Long priorityIndex;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false, updatable = false)
    private TaskQueue queue;

    private Task(String title, TaskQueue queue) {
        this.title = normalizeTitle(title);
        this.content = null;
        this.templateType = null;
        this.importance = null;
        this.progress = 0;
        this.status = TaskStatus.ACTIVE;
        this.priorityIndex = null;
        this.queue = validQueueIsNull(queue);
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static Task create(String title, TaskQueue queue) {
        return new Task(title, queue);
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    public void editTitle(String newTitle) {
        this.title = normalizeTitle(newTitle);
    }

    public void editContent(String newContent) {
        this.content = normalizeContent(newContent);
    }

    public void setDue(OffsetDateTime dateTime) {
        this.dueAt = dateTime; // 상태 변경은 별도 dueStatus 로직에 따름
    }

    public void changeImportance(TaskImportance level) {
        this.importance = level; // null 허용
    }

    public void updateProgress(int percent) {
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("progress must be 0..100");
        this.progress = percent;
    }

    public void markCompleted(Clock clock) {
        this.completedAt = OffsetDateTime.now(validNull(clock, "clock"));
        this.status = TaskStatus.COMPLETED;
    }

    public void reopen() {
        this.completedAt = null;
        this.status = TaskStatus.ACTIVE;
    }

    public void moveTask(TaskQueue targetQueue, Long newPriorityIndex) {
        TaskQueue target = validQueueIsNull(targetQueue);
        if (!Objects.equals(this.queue.getId(), target.getId())) {
            throw new IllegalStateException("policy: cross-queue move limited");
        }
        this.priorityIndex = newPriorityIndex;
    }

    public void deferTo(OffsetDateTime dateTime) {
        if (dateTime == null) throw new IllegalArgumentException("dateTime is null");
        this.dueAt = dateTime;
    }

    public void deferDays(int days, Clock clock) {
        if (days <= 0) throw new IllegalArgumentException("days must be > 0");
        this.dueAt = Objects.requireNonNullElseGet(this.dueAt, () -> OffsetDateTime.now(validNull(clock, "clock"))).plusDays(days);
    }

    public void softDelete(Clock clock) {
        this.status = TaskStatus.TRASHED;
        this.trashedAt = OffsetDateTime.now(validNull(clock, "clock"));
    }


    /**
     * 수동 삭제 가능 여부
     */
    public boolean canHardDelete(Clock clock) {
        OffsetDateTime now = OffsetDateTime.now(validNull(clock, "clock"));
        return this.status == TaskStatus.TRASHED
                && this.trashedAt != null
                && !now.isBefore(this.trashedAt);
    }

    /**
     * 자동 삭제 가능 여부 - 보존기간이 지난 이후 가능
     */
    public boolean canHardDelete(Clock clock, Duration retention) {
        if (this.status != TaskStatus.TRASHED) return false;
        OffsetDateTime time = validNull(this.trashedAt, "trashedAt");
        OffsetDateTime allowedFrom = time.plus(validNull(retention, "retention"));
        OffsetDateTime now = OffsetDateTime.now(validNull(clock, "clock"));
        return !now.isBefore(allowedFrom);
    }


    public DueStatus checkDueStatus(OffsetDateTime now, int imminentHours) {
        if (dueAt == null) return DueStatus.NORMAL;
        if (now.isAfter(dueAt)) return DueStatus.OVERDUE;
        OffsetDateTime edge = now.plusHours(imminentHours);
        return (dueAt.isAfter(now) && !dueAt.isAfter(edge)) ? DueStatus.IMMINENT : DueStatus.NORMAL;
    }


    // 검증 로직 ----------------------------------------------------------------


    private static TaskQueue validQueueIsNull(TaskQueue queue) {
        if (queue == null || queue.getId() == null) throw new IllegalArgumentException("queue is null or transient");
        return queue;
    }

    private static <T> T validNull(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        return value;
    }

    private static String normalizeTitle(String raw) {
        if (raw == null) throw new IllegalArgumentException("title is null");
        if (raw.isBlank()) throw new IllegalArgumentException("title is blank");
        return raw;
    }

    private static String normalizeContent(String raw) {
        if (raw == null) return null;
        if (raw.isBlank()) return null;
        return raw;
    }
}
