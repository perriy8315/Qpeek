package org.qpeek.qpeek.domain.trash.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.task.entity.Task;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.qpeek.qpeek.domain.task.enums.TaskStatus.TRASHED;

/**
 * TrashItem (휴지통 항목)
 * <p>
 * <도메인 규칙/정책>
 * - task: 휴지통에 들어간 작업(필수). 하나의 Task에는 최대 1개의 TrashItem만 존재.
 * - trashedAt: 휴지통에 들어간 시각(timestamptz). null 불가.
 * - retentionUntil: 보관 만료 시각(timestamptz). 일반적으로 trashedAt + 보존기간.
 * - 실제 Task 상태 복구(= ACTIVE 전환)와 TrashItem 삭제는 서비스 계층에서 수행.
 * - canHardDelete(now): now >= retentionUntil 이면 하드 삭제 수행 가능(실제 삭제는 리포지토리/서비스에서).
 * <p>
 * <설계 메모>
 * - UNIQUE(task_id)로 Task당 1건 보장.
 * - INDEX(retention_until)로 만료 스캔 최적화, INDEX(trashed_at)로 보관 관리.
 * - @Check(retention_until >= trashed_at).
 */

@Entity
@Getter
@Table(name = "trash_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_trash_item_task", columnNames = "task_id"),
        indexes = {
                @Index(name = "idx_trash_trashed_at", columnList = "trashed_at"),
                @Index(name = "idx_trash_retention_until", columnList = "retention_until")
        })
@Check(constraints = "retention_until >= trashed_at")
@ToString(of = {"id", "trashedAt", "retentionUntil"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrashItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "trash_item_id")
    private Long id;

    @Column(name = "trashed_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime trashedAt;

    @Column(name = "retention_until", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime retentionUntil;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    private Task task;

    private TrashItem(OffsetDateTime trashedAt, OffsetDateTime retentionUntil, Task task) {
        this.trashedAt = validNonNull(trashedAt, "trashedAt");
        this.retentionUntil = validNonNull(retentionUntil, "retentionUntil");
        this.task = validTaskIsNull(task);
        if (retentionUntil.isBefore(trashedAt)) {
            throw new IllegalArgumentException("retentionUntil must be >= trashedAt");
        }
        if (task.getStatus() != TRASHED) {
            throw new IllegalStateException("task status must be TRASHED to create TrashItem");
        }
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static TrashItem create(OffsetDateTime trashedAt, Duration retention, Task task) {
        if (retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("duration must be > 0");
        }
        return new TrashItem(trashedAt, trashedAt.plus(retention), task);
    }

    public static TrashItem create(OffsetDateTime trashedAt, OffsetDateTime retentionUntil, Task task) {
        return new TrashItem(trashedAt, retentionUntil, task);
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    public boolean canHardDelete(OffsetDateTime now) {
        validNonNull(now, "now");
        return !now.isBefore(this.retentionUntil);
    }

    public void extendRetentionUntil(OffsetDateTime newRetentionUntil) {
        validNonNull(newRetentionUntil, "newRetentionUntil");
        if (newRetentionUntil.isBefore(this.trashedAt)) {
            throw new IllegalArgumentException("newRetentionUntil must be >= trashedAt");
        }
        if (newRetentionUntil.isAfter(this.retentionUntil)) {
            this.retentionUntil = newRetentionUntil;
        }
    }


    // 검증 로직 ----------------------------------------------------------------


    private static Task validTaskIsNull(Task task) {
        if (task == null || task.getId() == null) throw new IllegalArgumentException("task is null or transient");
        return task;
    }

    private static <T> T validNonNull(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        return value;
    }
}