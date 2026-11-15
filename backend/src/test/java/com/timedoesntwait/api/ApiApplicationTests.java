package com.timedoesntwait.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class ApiApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    StatusController status;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migratesAndReportsStatus() {
        var s = status.status();
        assertThat(s.schemaVersion()).isEqualTo("1");
        assertThat(s.service()).isEqualTo("time-doesnt-wait-api");
    }

    @Test
    void oneLiveEntryPerUserHour() {
        jdbc.update("INSERT INTO app_user (id) VALUES ('00000000-0000-7000-8000-000000000001')");
        String insert = "INSERT INTO entry (id, user_id, local_date, hour, source, logged_at, updated_at) "
                + "VALUES (?::uuid, '00000000-0000-7000-8000-000000000001', DATE '2026-11-16', 14, 'APP', 0, 0)";
        jdbc.update(insert, "00000000-0000-7000-8000-0000000000a1");
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> jdbc.update(insert, "00000000-0000-7000-8000-0000000000a2"));
        // A soft-deleted entry frees the hour.
        jdbc.update("UPDATE entry SET deleted_at = 1 WHERE id = '00000000-0000-7000-8000-0000000000a1'");
        jdbc.update(insert, "00000000-0000-7000-8000-0000000000a2");
    }
}
