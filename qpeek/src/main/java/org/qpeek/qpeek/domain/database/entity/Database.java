package org.qpeek.qpeek.domain.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.member.entity.Member;
import org.qpeek.qpeek.domain.queue.entity.TaskQueue;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Database (저장소)
 * <p>
 * <도메인 규칙/정책>
 * - owner(member): 저장소 소유자. 생성 후 변경 불가(updatable = false).
 * - name: 회원 단위 유니크((member_id, name)). 원문 보존(공백 포함)하며 전부 공백만은 금지, 길이 ≤ 100.
 * - description: 선택(Optional). 원문 보존, 전부 공백만이면 null로 정규화, 길이 ≤ 500.
 * - deletedAt: 소프트 삭제 시각. OffsetDateTime(timestamptz). null이면 활성 상태.
 * - 권한: rename / deleteByOwner 는 소유자만 가능(ensureOwner로 검증).
 * <p>
 * <설계 메모>
 * - 유니크 제약: (member_id, name)으로 회원별 이름 중복 방지.
 * - 인덱스: member_id, deleted_at(활성 목록/정리 배치 성능 목적). 운영 시 WHERE deleted_at IS NULL 부분 인덱스 고려 가능.
 */
@Entity
@Getter
@Table(name = "databases",
        uniqueConstraints = @UniqueConstraint(name = "uk_databases_member_name", columnNames = {"member_id", "name"}),
        indexes = {
                @Index(name = "idx_databases_member", columnList = "member_id"),
                @Index(name = "idx_databases_deleted_at", columnList = "deleted_at")
        })
@Check(constraints = "name <> ''")
@ToString(of = {"id", "name", "description", "deletedAt"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Database extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "database_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private OffsetDateTime deletedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @JsonIgnore
    @OneToMany(mappedBy = "database", fetch = FetchType.LAZY)
    private List<TaskQueue> queues = new ArrayList<>();

    private Database(String name, String description, Member member) {
        this.name = normalizeName(name);
        this.description = normalizeDesc(description); // 공백만이면 null, 아니면 원문 보존
        this.deletedAt = null;
        this.member = validMemberIsNull(member);
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static Database create(String name, String description, Member member) {
        return new Database(name, description, member);
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    public void rename(String newName, String newDescription, Member actor) {
        ensureOwner(actor);
        this.name = normalizeName(newName);
        this.description = normalizeDesc(newDescription);
    }

    public void deleteByOwner(Clock clock, Member actor) {
        ensureOwner(actor);
        if (clock == null) throw new IllegalArgumentException("clock is null");
        if (this.deletedAt == null) {
            this.deletedAt = OffsetDateTime.now(clock);
        }
    }


    // 검증 로직 ----------------------------------------------------------------


    private static Member validMemberIsNull(Member member) {
        if (member == null || member.getId() == null)
            throw new IllegalArgumentException("member is null or transient");
        return member;
    }

    private void ensureOwner(Member actor) {
        if (actor == null || actor.getId() == null)
            throw new IllegalArgumentException("actor is null or transient");
        if (!Objects.equals(this.member.getId(), actor.getId()))
            throw new SecurityException("actor is not this database owner");
    }

    private static String normalizeName(String raw) {
        if (raw == null) throw new IllegalArgumentException("name is null");
        if (raw.isBlank()) throw new IllegalArgumentException("name is blank");
        if (raw.length() > 100) throw new IllegalArgumentException("name length > 100");
        return raw;
    }

    private static String normalizeDesc(String raw) {
        if (raw == null) return null;
        if (raw.isBlank()) return null;
        if (raw.length() > 500) throw new IllegalArgumentException("description length > 500");
        return raw;
    }
}