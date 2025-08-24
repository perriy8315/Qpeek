package org.qpeek.qpeek.domain.queue.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qpeek.qpeek.domain.database.entity.Database;

import static org.assertj.core.api.Assertions.*;

class TaskQueueTest {
    private Database databaseWithId(Long id) {
        Database mock = Mockito.mock(Database.class);
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
        Database database = databaseWithId(1L);
        String queueName = " Queue / 2025 ";
        String description = "   ";

        //when
        TaskQueue taskQueue = TaskQueue.create(queueName, description, database);

        //then
        assertThat(taskQueue.getId()).isNull();
        assertThat(taskQueue.getVersion()).isNull();
        assertThat(taskQueue.getName()).isEqualTo(queueName); // name 은 trim() 하지 않고 원문 보존
        assertThat(taskQueue.getDescription()).isNull(); // desc 는 blank -> null 처리
        assertThat(taskQueue.getMaxTasks()).isEqualTo(50); // default queue size 50
        assertThat(taskQueue.getDatabase()).isEqualTo(database);
        assertThat(taskQueue.getTasks()).isNotNull();
        assertThat(taskQueue.getTasks()).isEmpty();
    }

    @Test
    @DisplayName("create() fail test : name is null")
    void create_fail_name_null() {
        //given
        Database database = databaseWithId(1L);

        //then
        assertThatThrownBy(() -> TaskQueue.create(null, "description", database))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @DisplayName("create() fail test : name is blank")
    void create_fail_name_blank() {
        //given
        Database database = databaseWithId(1L);

        //then
        assertThatThrownBy(() -> TaskQueue.create("   ", "description", database))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is blank");
    }

    @Test
    @DisplayName("create() fail test : name.length > 100")
    void create_fail_invalid_name_length() {
        //given
        Database database = databaseWithId(1L);
        String queueName = "a".repeat(101);

        //then
        assertThatThrownBy(() -> TaskQueue.create(queueName, "description", database))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name length > 100");
    }

    @Test
    @DisplayName("description normalization test")
    void description_normalization() {
        //given
        Database database = databaseWithId(1L);

        // null → null
        TaskQueue queue1 = TaskQueue.create("Queue Name", null, database); // when
        assertThat(queue1.getDescription()).isNull(); // then

        // 공백-only → null
        TaskQueue queue2 = TaskQueue.create("Queue Name", "   \t  ", database); // when
        assertThat(queue2.getDescription()).isNull(); // then

        // 500자 이하 허용
        String descriptionUnder500 = "a".repeat(500);
        TaskQueue queue3 = TaskQueue.create("Queue Name", descriptionUnder500, database); // when
        assertThat(queue3.getDescription()).isEqualTo(descriptionUnder500); // then

        // 500자 초과 예외
        String descriptionOver500 = "a".repeat(501);
        assertThatThrownBy(() -> TaskQueue.create("Queue Name", descriptionOver500, database))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description length > 500");
    }

    @Test
    @DisplayName("create() fail test : database is null or transient")
    void create_fail_database_null_or_transient() {
        //given
        Database transientDb = databaseWithId(null);

        //then
        assertThatThrownBy(() -> TaskQueue.create("Queue Name", "description", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("database is null or transient");

        assertThatThrownBy(() -> TaskQueue.create("Queue Name", "description", transientDb))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("database is null or transient");
    }


    // ------------------------------------------------------------------
    // createWithLimit()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createWithLimit() success test")
    void createWithLimit_success() {
        //given
        Database database = databaseWithId(1L);
        String queueName = " Queue / 2025 ";
        String description = "   ";

        //when
        TaskQueue taskQueue = TaskQueue.createWithLimit(queueName, description, 200, database);

        //then
        assertThat(taskQueue.getName()).isEqualTo(queueName);
        assertThat(taskQueue.getDescription()).isNull();
        assertThat(taskQueue.getMaxTasks()).isEqualTo(200);
        assertThat(taskQueue.getDatabase()).isEqualTo(database);
    }

    @Test
    @DisplayName("createWithLimit() fail test : maxTasks <= 0")
    void createWithLimit_fail_invalid_max_tasks() {
        Database database = databaseWithId(1L);
        assertThatThrownBy(() -> TaskQueue.createWithLimit("Queue Name", "description", 0, database))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxTasks must be > 0");

        assertThatThrownBy(() -> TaskQueue.createWithLimit("Queue Name", "description", -1, database))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxTasks must be > 0");
    }
}