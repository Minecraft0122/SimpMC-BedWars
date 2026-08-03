package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElapsedTimeFormatterTest {

    @Test
    void formatsElapsedSecondsWithClockComponents() {
        Instant start = Instant.parse("2026-08-03T00:00:00Z");

        assertEquals("00:00", ElapsedTimeFormatter.format(start, start));
        assertEquals("00:59", ElapsedTimeFormatter.format(start, start.plusSeconds(59)));
        assertEquals("01:00", ElapsedTimeFormatter.format(start, start.plusSeconds(60)));
        assertEquals("59:59", ElapsedTimeFormatter.format(start, start.plusSeconds(3_599)));
        assertEquals("01:00:00", ElapsedTimeFormatter.format(start, start.plusSeconds(3_600)));
        assertEquals("01:01:01", ElapsedTimeFormatter.format(start, start.plusSeconds(3_661)));
    }

    @Test
    void missingAndFutureStartTimesAreSafe() {
        Instant now = Instant.parse("2026-08-03T00:00:00Z");

        assertEquals("", ElapsedTimeFormatter.format(null, now));
        assertEquals("00:00", ElapsedTimeFormatter.format(now.plusSeconds(5), now));
    }
}
