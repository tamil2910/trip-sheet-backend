package com.example.trip_sheet_backend.common.services;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

@Service
public class UniqueCodeGeneratorService {

  private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int RANDOM_PART_LENGTH = 6;
  private static final int MAX_ATTEMPTS = 50;

  private final SecureRandom secureRandom = new SecureRandom();

  public String generateUniqueCode(String prefix, Predicate<String> existsChecker) {
    String normalizedPrefix = normalizePrefix(prefix);

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      String code = normalizedPrefix + generateRandomPart();
      if (!existsChecker.test(code)) {
        return code;
      }
    }

    throw new RuntimeException("Unable to generate a unique code. Please try again.");
  }

  private String normalizePrefix(String prefix) {
    String normalized = prefix == null ? "" : prefix.trim()
        .replaceAll("[^a-zA-Z0-9]", "")
        .toUpperCase(Locale.ROOT);

    if (normalized.isBlank()) {
      throw new RuntimeException("Code prefix is required");
    }

    return normalized + "-";
  }

  private String generateRandomPart() {
    StringBuilder builder = new StringBuilder(RANDOM_PART_LENGTH);

    for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
      int index = secureRandom.nextInt(CHAR_POOL.length());
      builder.append(CHAR_POOL.charAt(index));
    }

    return builder.toString();
  }
}
