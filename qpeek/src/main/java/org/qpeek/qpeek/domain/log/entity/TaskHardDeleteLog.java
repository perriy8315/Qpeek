package org.qpeek.qpeek.domain.log.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.task.entity.Task;
import org.qpeek.qpeek.domain.trash.entity.TrashItem;

import java.time.OffsetDateTime;

/**
 * TaskHardDeleteLog (작업 하드 삭제 이력)
 * <p>
 * <도메인 규칙/정책>
 * - taskId: 삭제된 Task의 식별자(원시 값). 실제 Task 행은 이미 삭제되었을 수 있어 FK 미사용.
 * - trashedAt: 해당 Task가 휴지통으로 이동된 시각.
 * - hardDeletedAt: 실제 하드 삭제가 완료된 시각(로그 시각).
 * <p>
 * <설계 메모>
 * - 조회 최적화를 위해 taskId / hardDeletedAt 인덱스 구성.
 * - 무결성 제약: hard_deleted_at >= trashed_at.
 * - 로그 테이블이므로 UPDATE 최소화(대부분 INSERT-only).
 * - 보존 기간(retention) 정책 증빙이 필요 없다면 단순히 trashedAt과 hardDeletedAt만 기록.
 * (정책 추적이 필요하면 별도 테이블/정책 버전으로 관리)
 */
@Entity
@Getter
@Table(name = "task_hard_delete_logs",
        indexes = {
                @Index(name = "idx_delete_log_task_id", columnList = "task_id"),
                @Index(name = "idx_delete_log_hard_deleted_at", columnList = "hard_deleted_at")
        })
@Check(constraints = "hard_deleted_at >= trashed_at")
@ToString(of = {"id", "taskId", "hardDeletedAt"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskHardDeleteLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "task_hard_delete_log_id")
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "trashed_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime trashedAt;

    @Column(name = "hard_deleted_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private OffsetDateTime hardDeletedAt;

    private TaskHardDeleteLog(Long taskId,
                              OffsetDateTime trashedAt,
                              OffsetDateTime hardDeletedAt) {
        this.taskId = requireNonNull(taskId, "taskId");
        this.trashedAt = requireNonNull(trashedAt, "trashedAt");
        this.hardDeletedAt = requireNonNull(hardDeletedAt, "hardDeletedAt");

        if (hardDeletedAt.isBefore(trashedAt)) {
            throw new IllegalArgumentException("hardDeletedAt must be >= trashedAt");
        }
    }

    // 도메인 생성 로직 -----------------------------------------------------------

    public static TaskHardDeleteLog createFromTrashItem(TrashItem trashItem, OffsetDateTime hardDeletedAt) {
        requireNonNull(trashItem, "trashItem");
        requireNonNull(hardDeletedAt, "hardDeletedAt");

        Task task = requireNonNull(trashItem.getTask(), "task");
        Long taskId = requireNonNull(task.getId(), "taskId");

        OffsetDateTime trashedAt = requireNonNull(trashItem.getTrashedAt(), "trashedAt");

        return new TaskHardDeleteLog(taskId, trashedAt, hardDeletedAt);
    }

    public static TaskHardDeleteLog create(Long taskId,
                                           OffsetDateTime trashedAt,
                                           OffsetDateTime hardDeletedAt) {
        return new TaskHardDeleteLog(taskId, trashedAt, hardDeletedAt);
    }

    // 공통 검증 ---------------------------------------------------------------

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        return value;
    }
}