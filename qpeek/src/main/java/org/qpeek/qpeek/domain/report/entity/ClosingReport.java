package org.qpeek.qpeek.domain.report.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.qpeek.qpeek.common.entity.BaseEntity;
import org.qpeek.qpeek.domain.member.entity.Member;

import java.time.LocalDate;
import java.util.Objects;

/**
 * ClosingReport (데일리 클로징 결과)
 * <p>
 * <도메인 규칙/정책>
 * 1. 보고 단위는 회원(Member)×날짜(LocalDate)이며, 회원당 날짜별 최대 1건만 존재한다.
 * - DB 제약: UNIQUE(member_id, report_date)
 * 2. completedCount / incompleteCount 는 0 이상이어야 한다.
 * 3. generatedPdfUrl 은 선택(Optional)이며, 추후 PDF 생성 후 연결할 수 있다.
 * 4. reportDate 는 회원의 타임존 기준으로 산출된 “그 날”의 날짜를 저장한다.
 * <p>
 * <설계 메모>
 * - reportDate / Member 는 생성 후 변경 불가(updatable=false)로 식별
 * - DB 무결성: @Check(completed_count >= 0 AND incomplete_count >= 0)
 * - 합계가 필요할 땐 totalCount() 계산 프로퍼티로 제공
 */
@Entity
@Getter
@Check(constraints = "completed_count >= 0 AND incomplete_count >= 0")
@Table(name = "closing_report",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_closing_member_date", columnNames = {"member_id", "report_date"})
        })
@ToString(of = {"id", "reportDate", "completedCount", "incompleteCount", "generatedPdfUrl"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClosingReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq_gen")
    @Column(name = "closing_report_id")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "report_date", nullable = false, columnDefinition = "date", updatable = false)
    private LocalDate reportDate;

    @Column(name = "completed_count", nullable = false)
    private int completedCount;

    @Column(name = "incomplete_count", nullable = false)
    private int incompleteCount;

    @Column(name = "generated_pdf_url", length = 1024)
    private String generatedPdfUrl;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    private ClosingReport(LocalDate reportDate, int completedCount, int incompleteCount, String generatedPdfUrl, Member member) {
        this.reportDate = validNull(reportDate, "reportDate");
        validCounts(completedCount, incompleteCount);
        this.completedCount = completedCount;
        this.incompleteCount = incompleteCount;
        this.generatedPdfUrl = normalizeUrlOrNull(generatedPdfUrl);
        this.member = validMemberIsNull(member);
    }


    // 도메인 서비스 로직 ----------------------------------------------------------------


    public static ClosingReport create(LocalDate reportDate,
                                       int completedCount,
                                       int incompleteCount, Member member) {
        return new ClosingReport(reportDate, completedCount, incompleteCount, null, member);
    }

    public static ClosingReport createWithPdf(LocalDate reportDate,
                                              int completedCount,
                                              int incompleteCount,
                                              String generatedPdfUrl, Member member) {
        return new ClosingReport(reportDate, completedCount, incompleteCount, generatedPdfUrl, member);
    }


    // 행위(도메인 메서드) ----------------------------------------------------------------


    public void attachPdfUrl(String url) {
        String validUrl = normalizeUrlOrNull(url);
        if (validUrl == null) throw new IllegalArgumentException("generatedPdfUrl is null");
        if (!Objects.equals(this.generatedPdfUrl, validUrl)) {
            this.generatedPdfUrl = validUrl;
        }
    }

    public void removePdfUrl() {
        this.generatedPdfUrl = null;
    }

    public int totalCount() {
        return completedCount + incompleteCount;
    }


    // 검증 로직 ----------------------------------------------------------------


    private static Member validMemberIsNull(Member member) {
        if (member == null || member.getId() == null)
            throw new IllegalArgumentException("member is null or transient");
        return member;
    }

    private static <T> T validNull(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is null");
        return value;
    }

    private static void validCounts(int completed, int incomplete) {
        if (completed < 0) throw new IllegalArgumentException("completedCount must be >= 0");
        if (incomplete < 0) throw new IllegalArgumentException("incompleteCount must be >= 0");
    }

    private static String normalizeUrlOrNull(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }
}