package org.qpeek.qpeek.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.qpeek.qpeek.domain.member.enums.MemberStatus;
import org.qpeek.qpeek.domain.member.value.LoginId;
import org.qpeek.qpeek.domain.member.value.PasswordHash;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

class MemberTest {

    private final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("create() success test")
    public void create_success() {
        //given
        String loginId = "member";
        String passwordHash = "hashed_pw_123";
        String nickname = "nickname";

        //when
        Member member1 = Member.create(loginId, passwordHash, nickname, KST);
        Member member2 = Member.create(LoginId.of(loginId), PasswordHash.of(passwordHash), nickname, KST);

        //then
        assertThat(member1.getLoginId().getValue()).isEqualTo(loginId);
        assertThat(member1.getPasswordHash().getValue()).isEqualTo(passwordHash);
        assertThat(member1.getNickname()).isEqualTo(nickname);
        assertThat(member1.getTimeZone()).isEqualTo(KST);
        assertThat(member1.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);

        assertThat(member1.getLoginId()).isEqualTo(member2.getLoginId());
        assertThat(member1.getPasswordHash()).isEqualTo(member2.getPasswordHash());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "nullValue, nickname must not be null",
            "'', nickname must not be empty",
            "'   ', nickname must not be blank",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, nickname length must be <= 50",
            "' aa', nickname must not start or end with whitespace",
            "'aa ', nickname must not start or end with whitespace",
    }, nullValues = "nullValue")
    @DisplayName("create() fail test : nickname valid fail")
    void create_fail_invalid_nickname(String param, String expectedMessage) {
        // then
        assertThatThrownBy(() -> Member.create("abcde", "hash", param, KST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("create() fail test : timeZone is null")
    void create_fail_null_timezone() {
        assertThatThrownBy(() -> Member.create("abcde", "hash", "nickname", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeZone is null");
    }


    @Test
    @DisplayName("changePassword() success test")
    void changePassword_success() {
        // given
        Member member = Member.create("abcde", "hash", "nickname", KST);

        // when
        String newPassword = "NEW_HASHED";
        member.changePassword(newPassword);

        // then
        assertThat(member.getPasswordHash()).
                isEqualTo(PasswordHash.of(newPassword));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "nullValue, passwordHash must not be null",
            "'', passwordHash must not be empty",
            "'   ', passwordHash must not be blank",
            "BAD HASH, passwordHash must not contain whitespace",
    }, nullValues = "nullValue")
    @DisplayName("changePassword() fail test : invalid arguments")
    void changePassword_fail_invalid_arguments(String param, String expectedMessage) {
        // given
        Member member = Member.create("abcde", "hash", "nickname", KST);

        // empty
        assertThatThrownBy(() -> member.changePassword(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("changeNickname() success test")
    void changeNickname_success() {
        // given
        Member member = Member.create("abcde", "hash", "nickname", KST);

        // when
        String newNickname = "newNickname";
        member.changeNickname(newNickname);

        // then
        assertThat(member.getNickname()).isEqualTo(newNickname);
    }


    @ParameterizedTest
    @CsvSource(value = {
            "nullValue, nickname must not be null",
            "'', nickname must not be empty",
            "'   ', nickname must not be blank",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa, nickname length must be <= 50",
            "' aa', nickname must not start or end with whitespace",
            "'aa ', nickname must not start or end with whitespace",
    }, nullValues = "nullValue")
    @DisplayName("changeNickname() fail test : invalid arguments")
    void changeNickname_fail_invalid_arguments(String param, String expectedMessage) {
        // given
        Member member = Member.create("abcde", "hash", "nickname", KST);

        // null
        assertThatThrownBy(() -> member.changeNickname(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }


    @Test
    @DisplayName("changeMemberStatus() success test")
    void changeMemberStatus_success() {
        // given
        Member member = Member.create("abcde", "hash", "nickname", KST);

        member.changeMemberStatus(MemberStatus.DISABLED); // when
        assertThat(member.getMemberStatus()).isEqualTo(MemberStatus.DISABLED); // then

        member.changeMemberStatus(MemberStatus.ACTIVE); // when
        assertThat(member.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE); // then

        member.changeMemberStatus(MemberStatus.DELETED); // when
        assertThat(member.getMemberStatus()).isEqualTo(MemberStatus.DELETED); // then
    }

    @Test
    @DisplayName("changeMemberStatus() fail test : memberStatus is null")
    void changeMemberStatus_fail_null() {
        // given
        Member member = Member.create("abcde", "hash", "nickname", KST);

        // then
        assertThatThrownBy(() -> member.changeMemberStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberStatus is null");
    }
}