package com.timedoesntwait.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated status endpoint. Sign-in (/auth/google, /auth/kakao) and sync
 * (/sync/push, /sync/pull) arrive in milestone M3.
 */
@RestController
@RequestMapping("/api/v1")
public class StatusController {

    private final JdbcTemplate jdbc;
    private final String version;

    public StatusController(JdbcTemplate jdbc, @Value("${app.version:dev}") String version) {
        this.jdbc = jdbc;
        this.version = version;
    }

    public record Status(String service, String version, String schemaVersion) {}

    @GetMapping("/status")
    public Status status() {
        String schema = jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class);
        return new Status("time-doesnt-wait-api", version, schema);
    }
}
