package org.qpeek.qpeek.domain.database.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.member.entity.Member;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

class DatabaseTest {

    private final Clock baseClock = Clock.fixed(Instant.parse("2025-08-08T00:00:00Z"), ZoneOffset.UTC);

    private Member memberWithId(long id) {
        Member mock = Mockito.mock(Member.class);
        Mockito.when(mock.getId()).thenReturn(id);
        return mock;
    }

    // ------------------------------------------------------------------
    // create()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create() success test")
    void create_success() {
        //given
        String dbName = "Project DB";
        String dbDesc = "Project 개인 DB";
        Member member = memberWithId(1L);

        //when
        Database database = Database.create(dbName, dbDesc, member);

        //then
        assertThat(database.getMember().getId()).isEqualTo(member.getId());
        assertThat(database.getName()).isEqualTo(dbName);
        assertThat(database.getDescription()).isEqualTo(dbDesc);
        assertThat(database.getDeletedAt()).isNull();
        assertThat(database.getQueues()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("create() fail test : transient member")
    void create_fail_transient_member() {
        //given
        String dbName = "Project DB";
        String dbDesc = "Project 개인 DB";

        //when
        Member transientMember = Mockito.mock(Member.class);
        Mockito.when(transientMember.getId()).thenReturn(null);

        //then
        assertThatThrownBy(() -> Database.create(dbName, dbDesc, transientMember))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member is null or transient");
    }

    @Test
    @DisplayName("create() fail test : invalid name")
    void create_fail_invalid_name() {
        //given
        String dbName = "a";
        String dbDesc = "a";
        Member member = memberWithId(1L);


        //then
        assertThatThrownBy(() -> Database.create(
                dbName.repeat(101),
                dbDesc,
                member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name length > 100");

        assertThatThrownBy(() -> Database.create(
                " ",
                dbDesc,
                member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is blank");

        assertThatThrownBy(() -> Database.create(
                null,
                dbDesc,
                member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @DisplayName("create() fail test : invalid description")
    void create_fail_invalid_desc() {
        //given
        String dbName = "a";
        String dbDesc = "a";
        Member member = memberWithId(1L);


        //then
        assertThatThrownBy(() -> Database.create(
                dbName,
                dbDesc.repeat(501),
                member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description length > 500");


        Database blankDescDatabase = Database.create(dbName, "   ", member);
        assertThat(blankDescDatabase.getDescription()).isNull();

        Database nullDescDatabase = Database.create(dbName, null, member);
        assertThat(nullDescDatabase.getDescription()).isNull();
    }


    // ------------------------------------------------------------------
    // rename()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("rename() success test")
    void rename_success() {
        //given
        String dbName = "Project DB";
        String dbDesc = "Project 개인 DB";
        Member member = memberWithId(1L);

        //when
        Database database = Database.create(dbName, dbDesc, member);

        String newName = "Project 팀 DB";
        String newDesc = "Project 팀 협업 DB";
        database.rename(newName, newDesc, member);

        //then
        assertThat(database.getName()).isEqualTo(newName);
        assertThat(database.getDescription()).isEqualTo(newDesc);
    }


    @Test
    @DisplayName("rename() fail test : actor is not owner")
    void rename_fail_not_owner() {
        //given
        String dbName = "Project DB";
        String dbDesc = "Project 개인 DB";
        Member owner = memberWithId(1L);
        Member notOwner = memberWithId(2L);

        //when
        Database database = Database.create(dbName, dbDesc, owner);

        String newName = "Project 팀 DB";
        String newDesc = "Project 팀 협업 DB";

        //then
        assertThatThrownBy(() -> database.rename(newName, newDesc, notOwner))
                .isInstanceOf(SecurityException.class)
                .hasMessage("actor is not this database owner");
    }

    @Test
    @DisplayName("rename() fail test : actor is null or transient")
    void rename_fail_actor_transient() {
        //given
        String dbName = "Project DB";
        String dbDesc = "Project 개인 DB";
        Member owner = memberWithId(1L);

        //when
        Database database = Database.create(dbName, dbDesc, owner);

        String newName = "Project 팀 DB";
        String newDesc = "Project 팀 협업 DB";
        Member transientMember = Mockito.mock(Member.class);
        Mockito.when(transientMember.getId()).thenReturn(null);

        //then
        assertThatThrownBy(() -> database.rename(newName, newDesc, transientMember))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actor is null or transient");

        assertThatThrownBy(() -> database.rename(newName, newDesc, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actor is null or transient");
    }


    // ------------------------------------------------------------------
    // deleteByOwner()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleteByOwner() success test")
    void deleteByOwner_success() {
        //given
        String dbName = "Project DB";
        String dbDesc = "Project 개인 DB";
        Member owner = memberWithId(1L);

        Clock t0 = baseClock;
        Clock t1 = Clock.offset(t0, Duration.ofHours(1));

        //when
        Database database = Database.create(dbName, dbDesc, owner);
        database.deleteByOwner(t0, owner);

        //then
        assertThat(database.getDeletedAt()).isEqualTo(OffsetDateTime.now(t0));

        database.deleteByOwner(t1, owner);
        assertThat(database.getDeletedAt()).isEqualTo(OffsetDateTime.now(t0));
    }

    @Test
    @DisplayName("deleteByOwner() fail test : actor is not owner")
    void deleteByOwner_fail_not_owner() {
        //given
        String dbName = "Project DB";
        String dbDesc = "Project 개인 DB";
        Member owner = memberWithId(1L);
        Member notOwner = memberWithId(2L);

        //when
        Member transientMember = Mockito.mock(Member.class);
        Mockito.when(transientMember.getId()).thenReturn(null);

        Database database = Database.create(dbName, dbDesc, owner);

        //then
        assertThatThrownBy(() -> database.deleteByOwner(baseClock, notOwner))
                .isInstanceOf(SecurityException.class)
                .hasMessage("actor is not this database owner");

        assertThatThrownBy(() -> database.deleteByOwner(baseClock, transientMember))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actor is null or transient");

        assertThatThrownBy(() -> database.deleteByOwner(baseClock, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actor is null or transient");
    }
}