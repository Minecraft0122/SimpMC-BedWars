/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.discipline;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisciplinePolicyTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void defaultAfkEscalatesAtConfiguredOccurrences() {
        DisciplinePolicy policy = new DisciplinePolicy();

        assertDecision(policy, DisciplinePolicy.Category.AFK, 0, 0L, false);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 1, 0L, false);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 2, 600L, true);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 3, 3_600L, true);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 5, 86_400L, true);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 99, 86_400L, true);
    }

    @Test
    void defaultAbandonmentEscalatesAtConfiguredOccurrences() {
        DisciplinePolicy policy = new DisciplinePolicy();

        assertDecision(policy, DisciplinePolicy.Category.ABANDONMENT, 1, 300L, true);
        assertDecision(policy, DisciplinePolicy.Category.ABANDONMENT, 2, 900L, true);
        assertDecision(policy, DisciplinePolicy.Category.ABANDONMENT, 3, 3_600L, true);
        assertDecision(policy, DisciplinePolicy.Category.ABANDONMENT, 5, 86_400L, true);
        assertDecision(policy, DisciplinePolicy.Category.ABANDONMENT, 6, 86_400L, true);
    }

    @Test
    void violationUsesOneEjectionCooldownForEveryPositiveOccurrence() {
        DisciplinePolicy policy = new DisciplinePolicy();

        assertDecision(policy, DisciplinePolicy.Category.VIOLATION, 0, 0L, false);
        assertDecision(policy, DisciplinePolicy.Category.VIOLATION, 1, 1_800L, true);
        assertDecision(policy, DisciplinePolicy.Category.VIOLATION, 50, 1_800L, true);
    }

    @Test
    void cooldownBecomesExpiredAtItsEndAndZeroCooldownIsNeverActive() {
        DisciplinePolicy policy = new DisciplinePolicy();
        DisciplinePolicy.Decision active = policy.evaluate(DisciplinePolicy.Category.ABANDONMENT, 1);
        DisciplinePolicy.Decision none = policy.evaluate(DisciplinePolicy.Category.AFK, 1);

        assertEquals(ISSUED_AT.plusSeconds(300L), active.expiresAt(ISSUED_AT));
        assertTrue(active.activeAt(ISSUED_AT, ISSUED_AT.plusSeconds(299L)));
        assertFalse(active.activeAt(ISSUED_AT, ISSUED_AT.plusSeconds(300L)));
        assertTrue(active.expiredAt(ISSUED_AT, ISSUED_AT.plusSeconds(300L)));
        assertFalse(none.activeAt(ISSUED_AT, ISSUED_AT));
        assertTrue(none.expiredAt(ISSUED_AT, ISSUED_AT));
    }

    @Test
    void acceptsCustomRulesAndCopiesTheirArrays() {
        int[] thresholds = {1, 4};
        long[] durations = {7L, 11L};
        DisciplinePolicy.Rule custom = new DisciplinePolicy.Rule(thresholds, durations);
        thresholds[0] = 99;
        durations[0] = 99L;

        DisciplinePolicy policy = new DisciplinePolicy(custom, custom, custom);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 1, 7L, true);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 4, 11L, true);
    }

    @Test
    void occurrencesBelowAConfiguredFirstThresholdAreNotPunished() {
        DisciplinePolicy.Rule custom = new DisciplinePolicy.Rule(new int[]{3}, new long[]{12L});
        DisciplinePolicy policy = new DisciplinePolicy(custom, custom, custom);

        assertDecision(policy, DisciplinePolicy.Category.AFK, 1, 0L, false);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 2, 0L, false);
        assertDecision(policy, DisciplinePolicy.Category.AFK, 3, 12L, true);
    }

    @Test
    void rejectsIncompleteOrNegativeRules() {
        assertThrows(IllegalArgumentException.class,
                () -> new DisciplinePolicy.Rule(new int[]{1, 1}, new long[]{0L, 1L}));
        assertThrows(IllegalArgumentException.class,
                () -> new DisciplinePolicy.Rule(new int[]{1}, new long[]{-1L}));
        assertThrows(IllegalArgumentException.class,
                () -> new DisciplinePolicy.Rule(new int[]{}, new long[]{}));
        assertThrows(IllegalArgumentException.class,
                () -> new DisciplinePolicy(Map.of(DisciplinePolicy.Category.AFK,
                        DisciplinePolicy.defaultAfkRule())));
    }

    private static void assertDecision(DisciplinePolicy policy, DisciplinePolicy.Category category,
                                       int occurrence, long expectedCooldown, boolean expectedPunishment) {
        DisciplinePolicy.Decision decision = policy.evaluate(category, occurrence);
        assertEquals(expectedCooldown, decision.cooldownSeconds());
        assertEquals(expectedPunishment, decision.shouldPunish());
    }
}
