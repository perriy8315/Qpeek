package org.qpeek.qpeek.domain.queue.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.database.entity.Database;
import org.qpeek.qpeek.domain.task.entity.Task;

import java.util.ArrayList;
import java.util.List;


/**
 * Queue (작업 큐)
 * <p>
 * <도메인 규칙/정책>
 * - database: 소속 저장소(필수). 생성 후 변경 불가(updatable=false).
 * - name: 원문 보존(공백 포함). 전부 공백만은 금지, 길이 ≤ 100.
 * - description: 선택(Optional). 원문 보존, 전부 공백만이면 null, 길이 ≤ 500.
 * - maxTasks: 기본 50. addTask 시 개수 제한 검사.
 * - version: 재정렬 낙관적 잠금 버전(@Version). reorder에서 동시성 제어에 사용.
 * <p>
 * <설계 메모>
 * - UNIQUE (database_id, name): 같은 DB 안에서 큐 이름 중복 금지(다른 DB끼리는 허용).
 * - INDEX (database_id): "특정 DB의 큐 목록" 조회/카운트 최적화.
 * - INDEX (database_id, created_at): DB별 목록을 생성일로 정렬/페이징할 때 커버.
 * - Queue(1) — Task(*)
 * - Queue(1) — RecurringRule(0..*)
 */
@Entity
@Getter
@Table(name = "queues",
        uniqueConstraints = @UniqueConstraint(name = "uk_queues_db_name", columnNames = {"database_id", "name"}),
        indexes = {
                @Index(name = "idx_queues_db", columnList = "database_id"),
                @Index(name = "idx_queues_db_created_at", columnList = "database_id, created_at")
        })
@ToString(of = {"id", "name", "description", "maxTasks"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskQueue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "queue_id")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "max_tasks", nullable = false)
    private int maxTasks = 50;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "database_id", nullable = false, updatable = false)
    private Database database;

    @JsonIgnore
    @OneToMany(mappedBy = "queue", fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    private TaskQueue(String name, String description, Integer maxTasks, Database database) {
        this.name = normalizeName(name);
        this.description = normalizeDesc(description);
        if (maxTasks != null) this.maxTasks = validMaxTasks(maxTasks);
        this.database = validDatabaseIsNull(database);
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static TaskQueue create(String name, String description, Database database) {
        return new TaskQueue(name, description, null, database);
    }

    public static TaskQueue createWithLimit(String name, String description, int maxTasks, Database database) {
        return new TaskQueue(name, description, maxTasks, database);
    }


    // 검증 로직 ----------------------------------------------------------------


    private static Database validDatabaseIsNull(Database db) {
        if (db == null || db.getId() == null)
            throw new IllegalArgumentException("database is null or transient");
        return db;
    }

    private static String normalizeName(String raw) {
        if (raw == null) throw new IllegalArgumentException("name is null");
        if (raw.isBlank()) throw new IllegalArgumentException("name is blank");
        if (raw.length() > 100) throw new IllegalArgumentException("name length > 100");
        return raw; // 원문 보존 (trim 하지 않음)
    }

    private static String normalizeDesc(String raw) {
        if (raw == null) return null;
        if (raw.isBlank()) return null; // 전부 공백이면 null 정규화
        if (raw.length() > 500) throw new IllegalArgumentException("description length > 500");
        return raw; // 원문 보존
    }

    private static int validMaxTasks(int n) {
        if (n <= 0) throw new IllegalArgumentException("maxTasks must be > 0");
        return n;
    }
}
