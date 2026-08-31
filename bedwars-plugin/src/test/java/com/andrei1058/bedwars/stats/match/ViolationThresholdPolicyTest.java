/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViolationThresholdPolicyTest {

    private static final List<Integer> THRESHOLDS = List.of(10, 20, 50, 100);

    @Test
    void usesStrictCrossingAndRecordsAllThresholdsInOneUpdate() {
        ViolationThresholdPolicy.Evaluation exact = ViolationThresholdPolicy.evaluate(
                0, 10, 0, THRESHOLDS);
        assertEquals(List.of(), exact.crossedThresholds());
        assertEquals(0, exact.warningMask());

        ViolationThresholdPolicy.Evaluation jump = ViolationThresholdPolicy.evaluate(
                0, 101, 0, THRESHOLDS);
        assertEquals(THRESHOLDS, jump.crossedThresholds());
        assertEquals(15, jump.warningMask());
    }

    @Test
    void doesNotRepeatWarnedThresholdAndResetAllowsItAgain() {
        ViolationThresholdPolicy.Evaluation first = ViolationThresholdPolicy.evaluate(
                10, 11, 0, THRESHOLDS);
        assertEquals(List.of(10), first.crossedThresholds());

        ViolationThresholdPolicy.Evaluation repeated = ViolationThresholdPolicy.evaluate(
                11, 12, first.warningMask(), THRESHOLDS);
        assertEquals(List.of(), repeated.crossedThresholds());

        ViolationThresholdPolicy.Evaluation afterReset = ViolationThresholdPolicy.evaluate(
                0, 11, 0, THRESHOLDS);
        assertEquals(List.of(10), afterReset.crossedThresholds());
    }
}
