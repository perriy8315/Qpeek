package org.qpeek.qpeek.domain.report.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.member.entity.Member;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ClosingReportTest {

    private static final LocalDate DATE = LocalDate.of(2025, 8, 24);

    private Member memberWithId(Long id) {
        Member member = Mockito.mock(Member.class);
        Mockito.when(member.getId()).thenReturn(id);
        return member;
    }

    @Test
    @DisplayName("create() success test")
    void create_success() {
        //given
        Member member = memberWithId(1L);

        //when
        ClosingReport closingReport = ClosingReport.create(DATE, 2, 1, member);

        //then
        assertThat(closingReport.getReportDate()).isEqualTo(DATE);
        assertThat(closingReport.getCompletedCount()).isEqualTo(2);
        assertThat(closingReport.getIncompleteCount()).isEqualTo(1);
        assertThat(closingReport.totalCount()).isEqualTo(2 + 1);
        assertThat(closingReport.getGeneratedPdfUrl()).isNull();
        assertThat(closingReport.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("createWithPdf() success test")
    void createWithPdf_success() {
        //given
        Member member = memberWithId(1L);
        String pdfUrl = " https://cdn.example.com/a.pdf   ";

        //when
        ClosingReport closingReport = ClosingReport.createWithPdf(DATE, 2, 1, pdfUrl, member);

        //then
        assertThat(closingReport.getReportDate()).isEqualTo(DATE);
        assertThat(closingReport.getCompletedCount()).isEqualTo(2);
        assertThat(closingReport.getIncompleteCount()).isEqualTo(1);
        assertThat(closingReport.totalCount()).isEqualTo(2 + 1);
        assertThat(closingReport.getGeneratedPdfUrl()).isEqualTo(pdfUrl.trim());
        assertThat(closingReport.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("create() fail test : reportDate is null")
    void create_fail_null_reportDate() {
        //given
        Member member = memberWithId(1L);

        //then
        assertThatThrownBy(() -> ClosingReport.create(null, 2, 1, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reportDate is null");
    }

    @Test
    @DisplayName("create() fail test : completedCount and incompleteCount is negative")
    void create_fail_negative_count() {
        //given
        Member member = memberWithId(1L);

        //then
        assertThatThrownBy(() -> ClosingReport.create(DATE, -1, 0, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("completedCount must be >= 0");

        assertThatThrownBy(() -> ClosingReport.create(DATE, 0, -1, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incompleteCount must be >= 0");
    }


    @Test
    @DisplayName("create() fail test : member is null or transient")
    void create_fail_transient_member() throws Exception {
        //given
        Member transientMember = memberWithId(null);

        //then
        assertThatThrownBy(() -> ClosingReport.create(DATE, 2, 1, transientMember))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");

        assertThatThrownBy(() -> ClosingReport.create(DATE, 2, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");
    }

    @Test
    @DisplayName("attachPdfUrl() success test")
    void attachPdfUrl_success() {
        //given
        Member member = memberWithId(1L);
        ClosingReport closingReport = ClosingReport.create(DATE, 2, 1, member);
        assertThat(closingReport.getGeneratedPdfUrl()).isNull();

        //when
        String pdfUrl = "   https://ex.com/a.pdf  ";
        closingReport.attachPdfUrl(pdfUrl);

        //then
        assertThat(closingReport.getGeneratedPdfUrl()).isEqualTo(pdfUrl.trim());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "nullValue",
            "'   '",
            "''",
    }, nullValues = "nullValue")
    @DisplayName("attachPdfUrl() fail test : generatedPdfUrl is null or blank")
    void attachPdfUrl_fail_null_or_blank_pdf(String param) {
        // given
        Member member = memberWithId(1L);
        ClosingReport closingReport = ClosingReport.create(DATE, 2, 1, member);
        assertThat(closingReport.getGeneratedPdfUrl()).isNull();

        // then
        assertThatThrownBy(() -> closingReport.attachPdfUrl(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("generatedPdfUrl is null");
    }

    @Test
    @DisplayName("removePdfUrl() success test")
    void removePdfUrl_success() {
        // given
        String pdfUrl = "   https://ex.com/a.pdf  ";
        Member member = memberWithId(1L);
        ClosingReport closingReport = ClosingReport.createWithPdf(DATE, 1, 1, pdfUrl, member);
        assertThat(closingReport.getGeneratedPdfUrl()).isNotNull();

        // when
        closingReport.removePdfUrl();

        // then
        assertThat(closingReport.getGeneratedPdfUrl()).isNull();
    }

    @Test
    @DisplayName("totalCount() success test")
    void totalCount_success() {
        //given
        Member member = memberWithId(1L);

        //when
        ClosingReport closingReport = ClosingReport.create(DATE, 7, 5, member);

        //then
        assertThat(closingReport.totalCount()).isEqualTo(12);
    }
}