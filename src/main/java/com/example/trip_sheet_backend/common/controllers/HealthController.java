package com.example.trip_sheet_backend.common.controllers;

import javax.sql.DataSource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.response_setups.ApiResponse;

import java.sql.Connection;

import org.springframework.core.env.Environment;

@RestController
public class HealthController {
    private final DataSource dataSource;
    private final Environment env;

    public HealthController(DataSource dataSource, Environment env) {
        this.dataSource = dataSource;
        this.env = env;
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<String>> health() {
        // Try DB connection
        try (Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            String port = env.getProperty("local.server.port"); // available when request is handled by web server
            String message = "Trip Sheet Backend is Running ✅; DB: " + dbUrl + "; Port: " + (port != null ? port : "unknown");
            return ResponseEntity.ok(new ApiResponse<>(true, message, null));
        } catch (Exception ex) {
            String port = env.getProperty("local.server.port");
            String message = "Trip Sheet Backend STARTED but DB connection FAILED; Port: " + (port != null ? port : "unknown");
            return ResponseEntity.status(503).body(new ApiResponse<>(false, message, ex.getMessage()));
        }
    }
}
