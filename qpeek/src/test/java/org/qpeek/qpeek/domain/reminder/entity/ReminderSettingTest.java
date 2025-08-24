package org.qpeek.qpeek.domain.reminder.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.member.entity.Member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderSettingTest {

    private Member memberWithId(Long id) {
        Member mock = Mockito.mock(Member.class);
        Mockito.when(mock.getId()).thenReturn(id);
        return mock;
    }

    @Test
    @DisplayName("create() success test")
    void create_success() {
        // given
        Member member = memberWithId(1L);

        // when
        ReminderSetting setting = ReminderSetting
                .create(3, true, true, 24, member);

        // then
        assertThat(setting.getImminentHours()).isEqualTo(3);
        assertThat(setting.getOverdueIntervalHours()).isEqualTo(24);
        assertThat(setting.isNotifyDayBefore()).isTrue();
        assertThat(setting.isNotifyOnDueDay()).isTrue();
        assertThat(setting.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("createDefaultSetting() success test")
    void createDefault_success() {
        // given
        Member member = memberWithId(1L);

        // when
        ReminderSetting setting = ReminderSetting.createDefaultSetting(member);

        // then
        assertThat(setting.getImminentHours()).isEqualTo(3);
        assertThat(setting.getOverdueIntervalHours()).isEqualTo(24);
        assertThat(setting.isNotifyDayBefore()).isTrue();
        assertThat(setting.isNotifyOnDueDay()).isTrue();
        assertThat(setting.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("create() fail test : member is transient")
    void create_fail_transient_member() {
        // given
        Member transientMember = memberWithId(null);

        // then
        assertThatThrownBy(() -> ReminderSetting.create(3, true, true, 24, transientMember))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");
    }

    @Test
    @DisplayName("create() fail test : member is null")
    void create_fail_null_member() {
        // then
        assertThatThrownBy(() -> ReminderSetting.create(3, true, true, 24, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");
    }

    @Test
    @DisplayName("create() fail test : invalid imminentHours")
    void create_fail_invalid_imminent() {
        // given
        Member member = memberWithId(1L);

        // then
        assertThatThrownBy(() -> ReminderSetting.create(-1, true, true, 24, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("imminentHours must be between 0 and 168");

        assertThatThrownBy(() -> ReminderSetting.create(169, true, true, 24, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("imminentHours must be between 0 and 168");
    }

    @Test
    @DisplayName("create() fail test : invalid overdueIntervalHours")
    void create_fail_invalid_overdueInterval() {
        // given
        Member member = memberWithId(1L);

        // then
        assertThatThrownBy(() -> ReminderSetting.create(3, true, true, -1, member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("overdueIntervalHours must be >= 0");
    }

    @Test
    @DisplayName("updateAll() success test")
    void updateAll_success() {
        // given
        Member member = memberWithId(1L);
        ReminderSetting setting = ReminderSetting.createDefaultSetting(member);

        // when
        setting.updateAll(10, 12, false, false);

        // then
        assertThat(setting.getImminentHours()).isEqualTo(10);
        assertThat(setting.getOverdueIntervalHours()).isEqualTo(12);
        assertThat(setting.isNotifyDayBefore()).isFalse();
        assertThat(setting.isNotifyOnDueDay()).isFalse();
    }

    @Test
    @DisplayName("updateAll() fail test : invalid arguments")
    void updateAll_fail_invalid_arguments() {
        // given
        Member member = memberWithId(1L);
        ReminderSetting setting = ReminderSetting.createDefaultSetting(member);

        // then
        assertThatThrownBy(() -> setting.updateAll(200, 12, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("imminentHours must be between 0 and 168");

        assertThatThrownBy(() -> setting.updateAll(10, -1, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("overdueIntervalHours must be >= 0");
    }


    @Test
    @DisplayName("changeImminentHours() success test")
    void changeImminent_success() {
        // given
        Member member = memberWithId(1L);
        ReminderSetting setting = ReminderSetting.createDefaultSetting(member);

        setting.changeImminentHours(0); // when
        assertThat(setting.getImminentHours()).isEqualTo(0); // then

        setting.changeImminentHours(168); // when
        assertThat(setting.getImminentHours()).isEqualTo(168); // then
    }

    @Test
    @DisplayName("changeImminentHours() fail test : invalid imminentHours")
    void changeImminent_fail_invalid_imminentHours() {
        // given
        Member member = memberWithId(1L);
        ReminderSetting setting = ReminderSetting.createDefaultSetting(member);

        // then
        assertThatThrownBy(() -> setting.changeImminentHours(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("imminentHours must be between 0 and 168");

        assertThatThrownBy(() -> setting.changeImminentHours(169))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("imminentHours must be between 0 and 168");
    }

    @Test
    @DisplayName("changeOverdueIntervalHours() success test")
    void changeOverdueInterval_success() {
        // given
        Member member = memberWithId(1L);
        ReminderSetting setting = ReminderSetting.createDefaultSetting(member);

        setting.changeOverdueIntervalHours(0); // when
        assertThat(setting.getOverdueIntervalHours()).isEqualTo(0); // then

        setting.changeOverdueIntervalHours(5); // when
        assertThat(setting.getOverdueIntervalHours()).isEqualTo(5); // then
    }

    @Test
    @DisplayName("changeOverdueIntervalHours() fail test : negative OverdueIntervalHours ")
    void changeOverdueInterval_fail_negative_OverdueIntervalHours() {
        // given
        Member member = memberWithId(1L);
        ReminderSetting setting = ReminderSetting.createDefaultSetting(member);

        // then
        assertThatThrownBy(() -> setting.changeOverdueIntervalHours(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("overdueIntervalHours must be >= 0");
    }

    @Test
    @DisplayName("enableDayBefore(), enableOnDueDay() success test")
    void enable_flags_success() {
        // given
        Member member = memberWithId(1L);
        ReminderSetting setting = ReminderSetting.createDefaultSetting(member);

        // when
        setting.enableDayBefore(false);
        setting.enableOnDueDay(false);

        // then
        assertThat(setting.isNotifyDayBefore()).isFalse();
        assertThat(setting.isNotifyOnDueDay()).isFalse();

        // when
        setting.enableDayBefore(true);
        setting.enableOnDueDay(true);

        // then
        assertThat(setting.isNotifyDayBefore()).isTrue();
        assertThat(setting.isNotifyOnDueDay()).isTrue();
    }
}