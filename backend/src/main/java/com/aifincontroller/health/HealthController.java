package com.aifincontroller.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FOUND-004: Health endpoint.
 * Returns service status and database connectivity.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    @Autowired
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("service", "ai-fin-controller");
        response.put("version", "0.1.0-SNAPSHOT");

        String dbStatus;
        try (Connection conn = dataSource.getConnection()) {
            dbStatus = conn.isValid(2) ? "UP" : "DEGRADED";
        } catch (Exception e) {
            dbStatus = "DOWN";
            response.put("databaseError", e.getMessage());
        }
        response.put("database", dbStatus);

        if ("DOWN".equals(dbStatus)) {
            return ResponseEntity.status(503).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
