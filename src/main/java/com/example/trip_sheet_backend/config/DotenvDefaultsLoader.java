package com.example.trip_sheet_backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DotenvDefaultsLoader {

    private static final List<Path> CANDIDATES = List.of(
        Path.of(".env"),
        Path.of("..", ".env"),
        Path.of("config", ".env")
    );

    private DotenvDefaultsLoader() {
    }

    public static Map<String, Object> load() {
        Map<String, Object> properties = new LinkedHashMap<>();

        for (Path candidate : CANDIDATES) {
            Path path = candidate.toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                continue;
            }

            try {
                for (String line : Files.readAllLines(path)) {
                    addProperty(properties, line);
                }
                break;
            } catch (IOException ignored) {
                // Fall back to the next candidate.
            }
        }

        return properties;
    }

    private static void addProperty(Map<String, Object> properties, String rawLine) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        int separatorIndex = line.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = line.substring(0, separatorIndex).trim();
        String value = line.substring(separatorIndex + 1).trim();

        if ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        properties.putIfAbsent(key, value);
    }
}
