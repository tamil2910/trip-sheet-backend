package com.example.trip_sheet_backend.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupCheck implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppStartupCheck.class);

    private final DataSource dataSource;

    public AppStartupCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (var conn = dataSource.getConnection()) {
            log.info("Database connected: {}", conn.getMetaData().getURL());
        } catch (Exception e) {
            log.warn("Application started without an active database connection. Database-backed requests may fail until connectivity returns.");
            log.debug("Initial database connectivity check failed", e);
        }

        log.info("Trip-Sheet Backend started.");
    }
}
