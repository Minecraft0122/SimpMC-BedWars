package com.andrei1058.bedwars.arena;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/** Formats elapsed match time consistently for TAB and PlaceholderAPI. */
public final class ElapsedTimeFormatter {

    private ElapsedTimeFormatter() {
    }

    public static @NotNull String format(@Nullable Instant startTime) {
        return format(startTime, Instant.now());
    }

    static @NotNull String format(@Nullable Instant startTime, @NotNull Instant now) {
        if (startTime == null) return "";

        long totalSeconds = Math.max(0L, Duration.between(startTime, now).getSeconds());
        long hours = totalSeconds / 3_600L;
        long minutes = totalSeconds % 3_600L / 60L;
        long seconds = totalSeconds % 60L;
        if (hours == 0L) {
            return twoDigits(minutes) + ':' + twoDigits(seconds);
        }
        return twoDigits(hours) + ':' + twoDigits(minutes) + ':' + twoDigits(seconds);
    }

    private static String twoDigits(long value) {
        return value < 10L ? "0" + value : Long.toString(value);
    }
}
