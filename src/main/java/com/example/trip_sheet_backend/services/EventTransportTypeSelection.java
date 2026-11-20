package com.example.trip_sheet_backend.services;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.example.trip_sheet_backend.models.TransportEvent;

final class EventTransportTypeSelection {
  private EventTransportTypeSelection() { }

  static List<String> validateAndNormalize(TransportEvent event, List<String> selectedTypes) {
    if (selectedTypes != null && selectedTypes.stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException("Transport types cannot contain blank values");
    }
    List<String> selected = selectedTypes == null ? List.of()
        : selectedTypes.stream().map(value -> value.trim().toLowerCase(Locale.ROOT)).toList();
    Set<String> unique = new HashSet<>(selected);
    if (unique.size() != selected.size()) {
      throw new IllegalArgumentException("Duplicate transport types are not allowed");
    }
    Set<String> allowed = new HashSet<>(event.getTransportTypes());
    if (selected.stream().anyMatch(value -> !allowed.contains(value))) {
      throw new IllegalArgumentException("Select only transport types configured for this event");
    }
    if (!allowed.isEmpty() && selected.isEmpty()) {
      throw new IllegalArgumentException("Select at least one transport type for this event");
    }
    return selected;
  }
}
