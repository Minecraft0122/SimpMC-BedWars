/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AfkPolicyTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void emitsEachWarningOnceAndThenRemovesAtConfiguredThreshold() {
        AfkPolicy policy = new AfkPolicy(true, 60, 120, 180);
        AfkPolicy.State state = policy.initialState(START);

        AfkPolicy.Evaluation beforeWarning = policy.evaluate(state, START.plusSeconds(59), false);
        assertEquals(AfkPolicy.Decision.NONE, beforeWarning.decision());

        AfkPolicy.Evaluation warning = policy.evaluate(beforeWarning.state(), START.plusSeconds(60), false);
        assertEquals(AfkPolicy.Decision.WARNING, warning.decision());
        assertEquals(AfkPolicy.Stage.WARNING_SENT, warning.state().stage());

        AfkPolicy.Evaluation repeatedWarning = policy.evaluate(warning.state(), START.plusSeconds(90), false);
        assertEquals(AfkPolicy.Decision.NONE, repeatedWarning.decision());

        AfkPolicy.Evaluation finalWarning = policy.evaluate(repeatedWarning.state(), START.plusSeconds(120), false);
        assertEquals(AfkPolicy.Decision.FINAL_WARNING, finalWarning.decision());
        assertEquals(AfkPolicy.Stage.FINAL_WARNING_SENT, finalWarning.state().stage());

        AfkPolicy.Evaluation removed = policy.evaluate(finalWarning.state(), START.plusSeconds(180), false);
        assertEquals(AfkPolicy.Decision.REMOVE, removed.decision());
        assertEquals(AfkPolicy.Stage.REMOVAL_SENT, removed.state().stage());

        AfkPolicy.Evaluation repeatedRemoval = policy.evaluate(removed.state(), START.plusSeconds(181), false);
        assertEquals(AfkPolicy.Decision.NONE, repeatedRemoval.decision());
    }

    @Test
    void activityResetsIdleTimerAndWarningStage() {
        AfkPolicy policy = new AfkPolicy(true, 60, 120, 180);
        AfkPolicy.State state = policy.initialState(START);
        AfkPolicy.Evaluation warning = policy.evaluate(state, START.plusSeconds(60), false);
        assertEquals(AfkPolicy.Decision.WARNING, warning.decision());

        AfkPolicy.State active = policy.recordActivity(warning.state(), START.plusSeconds(61));
        assertEquals(AfkPolicy.Stage.ACTIVE, active.stage());
        AfkPolicy.Evaluation afterActivity = policy.evaluate(active, START.plusSeconds(120), false);
        assertEquals(AfkPolicy.Decision.NONE, afterActivity.decision());
        assertEquals(59L, afterActivity.idleSeconds());
        AfkPolicy.Evaluation warningAgain = policy.evaluate(afterActivity.state(), START.plusSeconds(121), false);
        assertEquals(AfkPolicy.Decision.WARNING, warningAgain.decision());
    }

    @Test
    void pausedTimeDoesNotCountTowardsRemoval() {
        AfkPolicy policy = new AfkPolicy(true, 60, 120, 180);
        AfkPolicy.State state = policy.initialState(START);

        AfkPolicy.Evaluation paused = policy.evaluate(state, START.plusSeconds(50), true);
        assertEquals(AfkPolicy.Decision.NONE, paused.decision());
        assertEquals(50L, paused.idleSeconds());

        AfkPolicy.Evaluation stillPaused = policy.evaluate(paused.state(), START.plusSeconds(110), true);
        assertEquals(AfkPolicy.Decision.NONE, stillPaused.decision());
        assertEquals(50L, stillPaused.idleSeconds());

        AfkPolicy.Evaluation resumed = policy.evaluate(stillPaused.state(), START.plusSeconds(120), false);
        assertEquals(AfkPolicy.Decision.WARNING, resumed.decision());
        assertEquals(60L, resumed.idleSeconds());
    }

    @Test
    void disabledPolicyNeverProducesAWarningOrRemoval() {
        AfkPolicy policy = new AfkPolicy(false, 60, 120, 180);
        AfkPolicy.State state = policy.initialState(START);

        AfkPolicy.Evaluation evaluation = policy.evaluate(state, START.plusSeconds(1_000), false);
        assertEquals(AfkPolicy.Decision.NONE, evaluation.decision());
        assertEquals(AfkPolicy.Stage.ACTIVE, evaluation.state().stage());
        assertEquals(1_000L, evaluation.idleSeconds());
    }

    @Test
    void rejectsNonIncreasingThresholds() {
        assertThrows(IllegalArgumentException.class, () -> new AfkPolicy(true, 60, 60, 180));
        assertThrows(IllegalArgumentException.class, () -> new AfkPolicy(true, 120, 60, 180));
        assertThrows(IllegalArgumentException.class, () -> new AfkPolicy(true, 60, 120, 120));
        assertThrows(IllegalArgumentException.class, () -> new AfkPolicy(true, -1, 120, 180));
    }
}
