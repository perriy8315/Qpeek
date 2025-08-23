package org.qpeek.qpeek.domain.member.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

    /**
     * <정책>
     * - 이 클래스는 이미 해싱된 비밀번호 문자열만 캡슐화
     * - 공백 문자가 전혀 없는 문자열. (대/소문자, 숫자, 특수문자, 비-ASCII 포함은 제한하지 않음)
     * - 금지 정책 : null, empty(""), blank(전부 공백), 공백 포함, 길이(MAX_LENGTH) 초과.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "a",
            "ABCdef123!@#$%^&*()",
            "123456789",
            "739440071",
            "비밀번호해시",
    })
    @DisplayName("of() success test")
    void of_success(String param) {
        //when
        PasswordHash passwordHash = PasswordHash.of(param);

        //then
        assertThat(passwordHash.getValue()).isEqualTo(param);
        assertThat(passwordHash).isEqualTo(PasswordHash.of(param));
        assertThat(passwordHash.hashCode()).isEqualTo(PasswordHash.of(param).hashCode());
    }

    @Test
    @DisplayName("of() success test : 255 chars")
    void of_success_length_255() {
        //given
        String chars = "a".repeat(255);

        //when
        PasswordHash passwordHash = PasswordHash.of(chars);

        //then
        assertThat(passwordHash.getValue()).isEqualTo(chars);
    }

    @Test
    @DisplayName("of() fail test : value is null")
    void of_fail_null() {
        //then
        assertThatThrownBy(() -> PasswordHash.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must not be null");
    }

    @Test
    @DisplayName("of() fail test : value is empty")
    void of_fail_empty() {
        //then
        assertThatThrownBy(() -> PasswordHash.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n", " \t\n"})
    @DisplayName("of() fail test : value is only whitespace")
    void of_fail_whitespace_only(String param) {
        //then
        assertThatThrownBy(() -> PasswordHash.of(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {" abcd1", "abcd1 ", "ab cd1", "ab\tcd1", "ab\ncd1"})
    @DisplayName("of() fail : value contains whitespace")
    void of_fail_contains_whitespace(String param) {
        //then
        assertThatThrownBy(() -> PasswordHash.of(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must not contain whitespace");
    }

    @Test
    @DisplayName("of() fail : value is not allowed length")
    void of_fail_length() {
        //given
        String chars = "a".repeat(256);

        //then
        assertThatThrownBy(() -> PasswordHash.of(chars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash length must be <= 255");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "비밀번호", "hash"})
    @DisplayName("toString() : toString must return PasswordHash(****)")
    void toString_limited(String param) {
        //when
        PasswordHash passwordHash = PasswordHash.of(param);

        //then
        assertThat(passwordHash.toString()).isEqualTo("PasswordHash(****)");
        assertThat(passwordHash.toString()).doesNotContain(param);
    }

    @Test
    @DisplayName("equals and hashCode : same value equals same hash")
    void equals_and_hashCode() {
        //given
        String value1 = "abcd1234";
        String value2 = "abcd1234";
        String value3 = "xyz5678";

        //when
        PasswordHash a = PasswordHash.of(value1);
        PasswordHash b = PasswordHash.of(value2);
        PasswordHash c = PasswordHash.of(value3);

        //then
        assertThat(a).isEqualTo(b).isNotEqualTo(c);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}