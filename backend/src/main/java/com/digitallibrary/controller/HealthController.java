package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Full health check — verifies backend, database (RDS), and Redis connectivity.
     * Safe to expose publicly (no sensitive data returned).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("backend", "UP");

        // ── Database (RDS) check ──────────────────────────────────────────────
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "UP");
        } catch (Exception e) {
            status.put("database", "DOWN: " + e.getMessage());
        }

        // ── Redis check ───────────────────────────────────────────────────────
        if (redisTemplate != null) {
            try {
                String pong = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .ping();
                status.put("redis", "PONG".equalsIgnoreCase(pong) ? "UP" : "UNEXPECTED: " + pong);
            } catch (Exception e) {
                status.put("redis", "DOWN: " + e.getMessage());
            }
        } else {
            status.put("redis", "DISABLED");
        }

        boolean allUp = status.values().stream().allMatch(v -> "UP".equals(v) || "DISABLED".equals(v));
        if (!allUp) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("One or more services are degraded", status));
        }
        return ResponseEntity.ok(ApiResponse.success("All systems operational", status));
    }
}
