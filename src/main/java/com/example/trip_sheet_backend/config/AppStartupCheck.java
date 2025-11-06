package com.example.trip_sheet_backend.config;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupCheck implements CommandLineRunner {
 private final DataSource dataSource;
  
  public AppStartupCheck(DataSource dataSource) {
      this.dataSource = dataSource;
  }

  @Override
  public void run(String... args) throws Exception {
    try (var conn = dataSource.getConnection()) {
      System.out.println("✅ DATABASE CONNECTED: " + conn.getMetaData().getURL());
    } catch (Exception e) {
      System.err.println("❌ DATABASE CONNECTION FAILED!");
      throw e;
    }
      System.out.println("🚀 Trip-Sheet Backend Started Successfully!");
  }
}
