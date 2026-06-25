package com.fini.todo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class TaskRules {

    private static final Set<String> VALID_REPEAT_TYPES = Set.of("NONE", "DAILY", "WEEKLY");
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private static final Map<String, String> WEEKDAY_ALIASES = Map.ofEntries(
            Map.entry("1", "MON"),
            Map.entry("MON", "MON"),
            Map.entry("MONDAY", "MON"),
            Map.entry("2", "TUE"),
            Map.entry("TUE", "TUE"),
            Map.entry("TUESDAY", "TUE"),
            Map.entry("3", "WED"),
            Map.entry("WED", "WED"),
            Map.entry("WEDNESDAY", "WED"),
            Map.entry("4", "THU"),
            Map.entry("THU", "THU"),
            Map.entry("THURSDAY", "THU"),
            Map.entry("5", "FRI"),
            Map.entry("FRI", "FRI"),
            Map.entry("FRIDAY", "FRI"),
            Map.entry("6", "SAT"),
            Map.entry("SAT", "SAT"),
            Map.entry("SATURDAY", "SAT"),
            Map.entry("7", "SUN"),
            Map.entry("SUN", "SUN"),
            Map.entry("SUNDAY", "SUN")
    );

    private TaskRules() {
    }

    static String normalizeRepeatType(String repeatType) {
        if (repeatType == null || repeatType.isBlank()) {
            return "NONE";
        }

        String normalized = repeatType.trim().toUpperCase();
        if (!VALID_REPEAT_TYPES.contains(normalized)) {
            throw new RuntimeException("Invalid repeatType. Use NONE, DAILY, or WEEKLY");
        }

        return normalized;
    }

    static String normalizeRepeatDays(String repeatType, String repeatDays) {
        if (!"WEEKLY".equals(repeatType)) {
            return null;
        }

        if (repeatDays == null || repeatDays.isBlank()) {
            throw new RuntimeException("repeatDays is required when repeatType is WEEKLY");
        }

        Set<String> days = new LinkedHashSet<>();
        for (String token : repeatDays.split("[,\\s]+")) {
            if (token.isBlank()) {
                continue;
            }

            String normalized = WEEKDAY_ALIASES.get(token.trim().toUpperCase());
            if (normalized == null) {
                throw new RuntimeException(
                        "repeatDays must contain weekdays as 1-7, MON-SUN, or MONDAY-SUNDAY"
                );
            }

            days.add(normalized);
        }

        if (days.isEmpty()) {
            throw new RuntimeException("repeatDays is required when repeatType is WEEKLY");
        }

        return String.join(",", days);
    }


    static void validateNotification(
            String repeatType,
            LocalDateTime notifyAt,
            LocalTime notifyTime
    ) {
        validateNotification(repeatType, notifyAt, notifyTime, null);
    }

    static void validateNotification(
            String repeatType,
            LocalDateTime notifyAt,
            LocalTime notifyTime,
            LocalDateTime existingNotifyAt
    ) {
        if ("NONE".equals(repeatType)) {
            if (notifyAt != null) {
                boolean isChanging = existingNotifyAt == null ||
                        !existingNotifyAt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
                                         .isEqual(notifyAt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
                if (isChanging && notifyAt.isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("notifyAt must be now or in the future");
                }
            }
            return;
        }

        if (notifyTime == null) {
            throw new RuntimeException("notifyTime is required when repeatType is DAILY or WEEKLY");
        }
    }

    static void validateLocation(Boolean hasLocation, BigDecimal latitude, BigDecimal longitude) {
        if (!Boolean.TRUE.equals(hasLocation)) {
            return;
        }

        if (latitude == null || longitude == null) {
            throw new RuntimeException("Latitude and longitude are required when task has location");
        }

        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new RuntimeException("Latitude must be between -90 and 90");
        }

        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new RuntimeException("Longitude must be between -180 and 180");
        }
    }

    static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    static String trimRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }

        return value.trim();
    }
}
