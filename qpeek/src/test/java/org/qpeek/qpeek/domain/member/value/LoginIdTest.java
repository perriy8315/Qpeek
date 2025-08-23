package org.qpeek.qpeek.domain.member.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class LoginIdTest {

    /**
     * <정책>
     * - 허용 문자: 소문자 a~z, 숫자 0~9
     * - 길이: 5 ~ 20자
     * - 금지: 대문자, 특수문자, 모든 공백(스페이스/탭/개행 등), null/empty/blank
     * - 입력 가공 금지: trim, toLowerCase 등 정규화 없이 그대로 검증/거절
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "abcde", // 최소 길이 5
            "a1b2c2",
            "a1234",
            "aaaaaaaaaaaaaaaaaaaa" // 20자
    })
    @DisplayName("of() success test")
    void of_success(String param) {
        //when
        LoginId loginId = LoginId.of(param);

        //then
        assertThat(loginId.getValue()).isEqualTo(param);
        assertThat(loginId).isEqualTo(LoginId.of(param));
        assertThat(loginId.hashCode()).isEqualTo(LoginId.of(param).hashCode());
    }

    @Test
    @DisplayName("of() fail test : value is null")
    void of_fail_null() {
        //then
        assertThatThrownBy(() -> LoginId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loginId must not be null");
    }

    @Test
    @DisplayName("of() fail test : value is empty")
    void of_fail_empty() {
        //then
        assertThatThrownBy(() -> LoginId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loginId must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n", " \t\n"})
    @DisplayName("of() fail test : value is only whitespace")
    void of_fail_whitespace_only(String param) {
        //then
        assertThatThrownBy(() -> LoginId.of(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loginId must not be blank (whitespace only is not allowed)");
    }

    @ParameterizedTest
    @ValueSource(strings = {" abcd1", "abcd1 ", "ab cd1", "ab\tcd1", "ab\ncd1"})
    @DisplayName("of() fail : value contains whitespace")
    void of_fail_contains_whitespace(String param) {
        //then
        assertThatThrownBy(() -> LoginId.of(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loginId must not contain whitespace");
    }

    @ParameterizedTest
    @CsvSource({
            "abcd, loginId length must be between 5 and 20",
            "aaaaaaaaaaaaaaaaaaaaa, loginId length must be between 5 and 20"}
    )
    @DisplayName("of() fail : value is not allowed length")
    void of_fail_length(String param, String expectedMessage) {
        //then
        assertThatThrownBy(() -> LoginId.of(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abcde", "abc_de", "abc-def", "abc.def", ",acd,ef", "abc@de", "가나다라1"})
    @DisplayName("of() fail : value contains not allowed chars")
    void of_fail_not_allowed_chars(String param) {
        //then
        assertThatThrownBy(() -> LoginId.of(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loginId must contain only lowercase letters and digits");
    }
}