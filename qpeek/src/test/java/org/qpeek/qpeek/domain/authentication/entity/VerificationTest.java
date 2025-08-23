package org.qpeek.qpeek.domain.authentication.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.member.entity.Member;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.qpeek.qpeek.domain.authentication.enums.VerificationChannelType;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

class VerificationTest {

    private final Clock baseClock = Clock.fixed(Instant.parse("2025-08-08T00:00:00Z"), ZoneOffset.UTC);

    private Member memberWithId(long id) {
        Member mock = Mockito.mock(Member.class);
        Mockito.when(mock.getId()).thenReturn(id);
        return mock;
    }

    @Test
    @DisplayName("issue() success test")
    void issue_success() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Duration duration = Duration.ofMinutes(5);
        Member member = memberWithId(1L);

        // when
        Verification verification = Verification.issue(
                channelType,
                hashCode,
                duration,
                baseClock,
                member
        );

        // then
        assertThat(verification.getChannelType()).isEqualTo(VerificationChannelType.MAIL);
        assertThat(verification.getCodeHash()).isEqualTo("hashCode");
        assertThat(verification.getExpiresAt()).isEqualTo(OffsetDateTime.ofInstant(
                Instant.parse("2025-08-08T00:05:00Z"), ZoneOffset.UTC));
        assertThat(verification.isVerified()).isFalse();
        assertThat(verification.isExpired(baseClock)).isFalse();
    }

    @Test
    @DisplayName("issue() fail test : transient member")
    void issue_fail_transient_member() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Duration duration = Duration.ofMinutes(5);

        Member transientMember = Mockito.mock(org.qpeek.qpeek.domain.member.entity.Member.class);
        Mockito.when(transientMember.getId()).thenReturn(null);

        // when
        assertThatThrownBy(() -> Verification.issue(
                channelType,
                hashCode,
                duration,
                baseClock,
                transientMember
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");
    }

    @Test
    @DisplayName("issue() fail test : invalid duration")
    void issue_fail_duration() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Member member = memberWithId(1L);

        // then
        assertThatThrownBy(() -> Verification.issue(
                channelType, hashCode, null, baseClock, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duration must be positive");

        assertThatThrownBy(() -> Verification.issue(
                channelType, hashCode, Duration.ZERO, baseClock, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duration must be positive");

        assertThatThrownBy(() -> Verification.issue(
                channelType, hashCode, Duration.ofSeconds(-1), baseClock, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duration must be positive");
    }

    @Test
    @DisplayName("verify() success test")
    void verify_success() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Duration duration = Duration.ofMinutes(5);
        Member member = memberWithId(1L);

        // when
        Verification verification = Verification.issue(
                channelType,
                hashCode,
                duration,
                baseClock,
                member
        );

        verification.verify(hashCode, baseClock);

        // then
        assertThat(verification.isVerified()).isTrue();
    }

    @Test
    @DisplayName("verify() fail test : already verify")
    void verify_fail_already_verify() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Duration duration = Duration.ofMinutes(5);
        Member member = memberWithId(1L);

        // when
        Verification verification = Verification.issue(
                channelType,
                hashCode,
                duration,
                baseClock,
                member
        );

        verification.verify(hashCode, baseClock);

        // then
        assertThat(verification.isVerified()).isTrue();
        assertThatThrownBy(() -> verification.verify(hashCode, baseClock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("already verified");
    }

    @Test
    @DisplayName("verify() fail test : hashCode mismatch")
    void verify_fail_code_mismatch() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Duration duration = Duration.ofMinutes(5);
        Member member = memberWithId(1L);

        // when
        Verification verification = Verification.issue(
                channelType,
                hashCode,
                duration,
                baseClock,
                member
        );

        // then
        assertThatThrownBy(() -> verification.verify("wrong", baseClock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code mismatch");
    }

    @Test
    @DisplayName("verify() fail test : after expiredAt")
    void verify_fail_after_expired() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Duration duration = Duration.ofMinutes(5);
        Member member = memberWithId(1L);

        // when
        Verification verification = Verification.issue(
                channelType,
                hashCode,
                duration,
                baseClock,
                member
        );

        Clock after5 = Clock.offset(baseClock, Duration.ofMinutes(5));
        Clock after6 = Clock.offset(baseClock, Duration.ofMinutes(6));

        // then
        // 정책: "현재 시간이 expiresAt과 같거나 이후면 만료"
        assertThatThrownBy(() -> verification.verify(hashCode, after5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("verification code expired");

        assertThatThrownBy(() -> verification.verify(hashCode, after6))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("verification code expired");
    }

    @Test
    @DisplayName("verify() success test")
    void isVerified_success() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Duration duration = Duration.ofMinutes(5);
        Member member = memberWithId(1L);

        // when
        Verification verification = Verification.issue(
                channelType,
                hashCode,
                duration,
                baseClock,
                member
        );

        verification.verify(hashCode, baseClock);

        // then
        assertThat(verification.isVerified()).isTrue();
    }

    @Test
    @DisplayName("isExpired() success test")
    void isExpired_success() {
        // given
        VerificationChannelType channelType = VerificationChannelType.MAIL;
        String hashCode = "hashCode";
        Duration duration = Duration.ofMinutes(5);
        Member member = memberWithId(1L);

        // when
        Verification verification = Verification.issue(
                channelType,
                hashCode,
                duration,
                baseClock,
                member
        );

        Clock afterClock = Clock.offset(baseClock, Duration.ofMinutes(6));

        // then
        assertThat(verification.isExpired(afterClock)).isTrue();
    }
}