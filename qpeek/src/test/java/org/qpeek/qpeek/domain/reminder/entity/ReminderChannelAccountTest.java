package org.qpeek.qpeek.domain.reminder.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.member.entity.Member;
import org.qpeek.qpeek.domain.reminder.enums.ReminderChannelType;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

class ReminderChannelAccountTest {

    private static final Clock baseClock = Clock.fixed(Instant.parse("2025-08-08T00:00:00Z"), ZoneOffset.UTC);
    private static final String EMAIL_ADDRESS = "user@example.com";
    private static final String KAKAO_TOKEN = "KAKAOTOKEN"; // 길이 10+
    private static final String SLACK_WEBHOOK = "https://hooks.slack.com/services/test";
    private static final String WEBPUSH_ENDPOINT = "https://push.example.com/endpoint/test";

    private Member memberWithId(Long id) {
        Member member = Mockito.mock(Member.class);
        Mockito.when(member.getId()).thenReturn(id);
        return member;
    }


    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create() success test")
    void create_success() {
        //given
        Member member = memberWithId(1L);

        //when
        ReminderChannelAccount mailChannelAccount = ReminderChannelAccount.create(
                ReminderChannelType.EMAIL, EMAIL_ADDRESS, member);

        ReminderChannelAccount kakaoChannelAccount = ReminderChannelAccount.create(
                ReminderChannelType.KAKAO, KAKAO_TOKEN, member);

        ReminderChannelAccount slackChannelAccount = ReminderChannelAccount.create(
                ReminderChannelType.SLACK, SLACK_WEBHOOK, member);

        ReminderChannelAccount webPushChannelAccount = ReminderChannelAccount.create(
                ReminderChannelType.WEBPUSH, WEBPUSH_ENDPOINT, member);

        //then
        basicTestByChannel(mailChannelAccount, ReminderChannelType.EMAIL, EMAIL_ADDRESS, member);
        basicTestByChannel(kakaoChannelAccount, ReminderChannelType.KAKAO, KAKAO_TOKEN, member);
        basicTestByChannel(slackChannelAccount, ReminderChannelType.SLACK, SLACK_WEBHOOK, member);
        basicTestByChannel(webPushChannelAccount, ReminderChannelType.WEBPUSH, WEBPUSH_ENDPOINT, member);
    }

    @Test
    @DisplayName("create() fail : ChannelType is null")
    void create_fail_null_channel_type() {
        // given
        Member member = memberWithId(1L);

        // then
        assertThatThrownBy(() -> ReminderChannelAccount.create(null, "test", member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("channel type is null");
    }

    @Test
    @DisplayName("create() fail : member is null or transient")
    void create_fail_transient_member() {
        // given
        Member transientMember = memberWithId(null);

        // then
        assertThatThrownBy(() -> ReminderChannelAccount.create(ReminderChannelType.EMAIL, EMAIL_ADDRESS, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");

        assertThatThrownBy(() -> ReminderChannelAccount.create(ReminderChannelType.EMAIL, EMAIL_ADDRESS, transientMember))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");
    }

    @Test
    @DisplayName("create() fail : addressOrToken is null or blank")
    void create_fail_null_or_blank_addressToken() {
        // given
        Member member = memberWithId(1L);

        //then
        assertThatThrownBy(() -> ReminderChannelAccount.create(ReminderChannelType.EMAIL, null, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("addressOrToken is null");

        assertThatThrownBy(() -> ReminderChannelAccount.email("   ", member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("addressOrToken is blank");
    }


    // ------------------------------------------------------------------
    // email(), kakao(), slack(), webPush()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("email() success test")
    void email_success() {
        // given
        Member member = memberWithId(1L);

        // when
        ReminderChannelAccount account = ReminderChannelAccount.email(EMAIL_ADDRESS, member);

        // then
        basicTestByChannel(account, ReminderChannelType.EMAIL, EMAIL_ADDRESS, member);
    }

    @Test
    @DisplayName("email() fail : invalid email pattern ")
    void email_fail_invalid_email_pattern() {
        // given
        Member member = memberWithId(1L);

        //then
        assertThatThrownBy(() -> ReminderChannelAccount.email("bad-email", member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid email");
    }

    @Test
    @DisplayName("kakao() success test")
    void kakao_success() {
        // given
        Member member = memberWithId(1L);

        // when
        ReminderChannelAccount account = ReminderChannelAccount.kakao(KAKAO_TOKEN, member);

        // then
        basicTestByChannel(account, ReminderChannelType.KAKAO, KAKAO_TOKEN, member);
    }

    @Test
    @DisplayName("kakao() fail : invalid kakao token")
    void kakao_fail_invalid_kakao_token() {
        // given
        Member member = memberWithId(1L);

        //then
        assertThatThrownBy(() -> ReminderChannelAccount.kakao("short", member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("kakao token too short");
    }

    @Test
    @DisplayName("slack() success test")
    void slack_success() {
        // given
        Member member = memberWithId(1L);

        // when
        ReminderChannelAccount account = ReminderChannelAccount.slack(SLACK_WEBHOOK, member);

        // then
        basicTestByChannel(account, ReminderChannelType.SLACK, SLACK_WEBHOOK, member);
    }

    @Test
    @DisplayName("slack() fail : invalid slack url pattern")
    void slack_fail_slack_url() {
        // given
        Member member = memberWithId(1L);

        //then
        assertThatThrownBy(() -> ReminderChannelAccount.slack("https://example.com/not-slack-hook", member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid slack webhook url");
    }

    @Test
    @DisplayName("webPush() success test")
    void webPush_success() {
        // given
        Member member = memberWithId(1L);

        // when
        ReminderChannelAccount account = ReminderChannelAccount.webPush(WEBPUSH_ENDPOINT, member);

        // then
        basicTestByChannel(account, ReminderChannelType.WEBPUSH, WEBPUSH_ENDPOINT, member);
    }

    @Test
    @DisplayName("webPush() fail : invalid webPush url")
    void web_push_fail_invalid_web_push_url() {
        // given
        Member member = memberWithId(1L);

        //then
        assertThatThrownBy(() -> ReminderChannelAccount.webPush("XXX://example.com", member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid webPush endpoint");
    }

    private static void basicTestByChannel(ReminderChannelAccount account, ReminderChannelType type, String addressOrToken, Member member) {
        assertThat(account.getChannelType()).isEqualTo(type);
        assertThat(account.getAddressOrToken()).isEqualTo(addressOrToken);
        assertThat(account.isChannelEnabled()).isTrue();
        assertThat(account.getVerifiedAt()).isNull();
        assertThat(account.getMember()).isEqualTo(member);
        assertThat(account.canSend()).isFalse(); // verifiedAt == null 이므로 false
    }


    // ------------------------------------------------------------------
    //  enable(), disable()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("enable() disable() success test")
    void enable_disable() {
        //given
        Member member = memberWithId(1L);
        ReminderChannelAccount account = ReminderChannelAccount.email(EMAIL_ADDRESS, member);
        assertThat(account.isChannelEnabled()).isTrue();

        account.disable(); //when
        assertThat(account.isChannelEnabled()).isFalse(); //then

        account.enable(); //when
        assertThat(account.isChannelEnabled()).isTrue(); //then
    }


    // ------------------------------------------------------------------
    //  setVerifiedAt(), canSend()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("setVerifiedAt() success test")
    void setVerifiedAt_success() {
        // given
        Member member = memberWithId(1L);
        ReminderChannelAccount account = ReminderChannelAccount.email(EMAIL_ADDRESS, member);

        // when
        account.setVerifiedAt(baseClock);

        // then
        assertThat(account.getVerifiedAt()).isEqualTo(
                OffsetDateTime.ofInstant(Instant.parse("2025-08-08T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("setVerifiedAt() fail test : clock is null")
    void setVerifiedAt_fail_null_clock() {
        // given
        Member member = memberWithId(1L);
        ReminderChannelAccount account = ReminderChannelAccount.email(EMAIL_ADDRESS, member);

        // then
        assertThatThrownBy(() -> account.setVerifiedAt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clock is null");
    }

    @Test
    @DisplayName("canSend() test : ( enabled=true && verifiedAt!=null ) is true")
    void canSend_test() {
        // given
        Member member = memberWithId(1L);
        ReminderChannelAccount account = ReminderChannelAccount.email(EMAIL_ADDRESS, member);

        assertThat(account.isChannelEnabled()).isTrue();
        assertThat(account.getVerifiedAt()).isNull();
        assertThat(account.canSend()).isFalse();


        account.setVerifiedAt(baseClock); // when
        assertThat(account.canSend()).isTrue(); // then

        account.disable(); // when
        assertThat(account.isChannelEnabled()).isFalse(); // then
        assertThat(account.canSend()).isFalse(); // then

        account.enable(); // when
        assertThat(account.isChannelEnabled()).isTrue(); // then
        assertThat(account.canSend()).isTrue(); //then
    }


    // ------------------------------------------------------------------
    //  updateAddressOrToken()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateAddressOrToken() success test")
    void updateAddressOrToken_success() {
        // given
        Member member = memberWithId(1L);
        ReminderChannelAccount account = ReminderChannelAccount.email(EMAIL_ADDRESS, member);
        account.setVerifiedAt(baseClock);
        assertThat(account.getVerifiedAt()).isNotNull();

        // when
        account.updateAddressOrToken("NewAddress@example.com");

        // then
        assertThat(account.getAddressOrToken()).isEqualTo("newaddress@example.com"); // lowercased
        assertThat(account.getVerifiedAt()).isNull(); // 주소 갱신 시 검증상태 초기화(verifiedAt=null), 형식 재검증
        assertThat(account.getChannelType()).isEqualTo(ReminderChannelType.EMAIL); // 불변
        assertThat(account.getMember()).isEqualTo(member);
        assertThat(account.canSend()).isFalse();
    }

    @Test
    @DisplayName("updateAddressOrToken() fail")
    void updateAddressOrToken_fail_rollback() {
        // given
        Member member = memberWithId(1L);
        ReminderChannelAccount account = ReminderChannelAccount.email(EMAIL_ADDRESS, member);
        account.setVerifiedAt(baseClock);

        OffsetDateTime before = account.getVerifiedAt();

        //then
        assertThatThrownBy(() -> account.updateAddressOrToken("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("addressOrToken is blank");


        assertThat(account.getVerifiedAt()).isEqualTo(before); // 검증 실패 시 상태 롤백(verifiedAt 유지)
        assertThat(account.getAddressOrToken()).isEqualTo(EMAIL_ADDRESS);
    }
}