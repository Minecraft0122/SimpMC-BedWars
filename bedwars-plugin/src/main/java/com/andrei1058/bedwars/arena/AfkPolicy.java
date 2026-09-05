/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Pure state machine for the in-game AFK warnings and removal decision.
 *
 * <p>The Bukkit listener owns one {@link State} per player and calls
 * {@link #recordActivity(State, Instant)} for meaningful activity. The
 * periodic arena task calls {@link #evaluate(State, Instant, boolean)}. No
 * Bukkit state or wall clock is read here, so the transitions can be tested
 * without a server.</p>
 */
public final class AfkPolicy {

    public static final long DEFAULT_WARNING_SECONDS = 60L;
    public static final long DEFAULT_FINAL_WARNING_SECONDS = 120L;
    public static final long DEFAULT_REMOVAL_SECONDS = 180L;

    private final boolean enabled;
    private final Duration warningAfter;
    private final Duration finalWarningAfter;
    private final Duration removalAfter;

    public AfkPolicy(boolean enabled, long warningSeconds, long finalWarningSeconds,
                     long removalSeconds) {
        this(enabled, Duration.ofSeconds(warningSeconds),
                Duration.ofSeconds(finalWarningSeconds), Duration.ofSeconds(removalSeconds));
    }

    public AfkPolicy(long warningSeconds, long finalWarningSeconds, long removalSeconds) {
        this(true, warningSeconds, finalWarningSeconds, removalSeconds);
    }

    public AfkPolicy(boolean enabled, Duration warningAfter, Duration finalWarningAfter,
                     Duration removalAfter) {
        this.enabled = enabled;
        this.warningAfter = requireThreshold("warningAfter", warningAfter);
        this.finalWarningAfter = requireThreshold("finalWarningAfter", finalWarningAfter);
        this.removalAfter = requireThreshold("removalAfter", removalAfter);
        if (!strictlyPositive(this.finalWarningAfter.minus(this.warningAfter))) {
            throw new IllegalArgumentException("finalWarningAfter must be greater than warningAfter");
        }
        if (!strictlyPositive(this.removalAfter.minus(this.finalWarningAfter))) {
            throw new IllegalArgumentException("removalAfter must be greater than finalWarningAfter");
        }
    }

    public static AfkPolicy defaults() {
        return new AfkPolicy(true, DEFAULT_WARNING_SECONDS, DEFAULT_FINAL_WARNING_SECONDS,
                DEFAULT_REMOVAL_SECONDS);
    }

    public boolean enabled() {
        return enabled;
    }

    public long warningSeconds() {
        return warningAfter.getSeconds();
    }

    public long finalWarningSeconds() {
        return finalWarningAfter.getSeconds();
    }

    public long removalSeconds() {
        return removalAfter.getSeconds();
    }

    /** Creates an active state whose idle timer starts at {@code now}. */
    public State initialState(Instant now) {
        return new State(requireTime(now), Stage.ACTIVE, null);
    }

    /** Resets the timer and warning stage after meaningful player activity. */
    public State recordActivity(State state, Instant now) {
        Objects.requireNonNull(state, "state");
        return new State(requireTime(now), Stage.ACTIVE, null);
    }

    /**
     * Evaluates the next state. A paused player does not accumulate idle time.
     * The arena task should call this on its normal cadence while paused; each
     * pause checkpoint moves the effective activity timestamp forward by the
     * time spent since the previous checkpoint.
     *
     * @param state  current immutable state
     * @param now    current timestamp
     * @param paused true while the player is dead, spectating, respawning, or
     *               being transferred between servers
     */
    public Evaluation evaluate(State state, Instant now, boolean paused) {
        Objects.requireNonNull(state, "state");
        Instant currentTime = requireTime(now);
        State current = state;

        if (paused) {
            Instant pausedAt = state.pausedAt();
            Instant effectiveActivity = state.lastActivityAt();
            if (pausedAt != null && currentTime.isAfter(pausedAt)) {
                effectiveActivity = state.lastActivityAt().plus(Duration.between(pausedAt, currentTime));
            }
            current = new State(effectiveActivity, state.stage(), currentTime);
            return new Evaluation(current, Decision.NONE,
                    elapsedSeconds(effectiveActivity, currentTime));
        }

        if (state.pausedAt() != null) {
            // Pause checkpoints have already compensated the activity time.
            current = new State(state.lastActivityAt(), state.stage(), null);
        }

        long idleSeconds = elapsedSeconds(current.lastActivityAt(), currentTime);
        if (!enabled || current.stage() == Stage.REMOVAL_SENT) {
            return new Evaluation(current, Decision.NONE, idleSeconds);
        }

        Decision decision = decisionFor(current.stage(), idleSeconds);
        if (decision == Decision.NONE) {
            return new Evaluation(current, Decision.NONE, idleSeconds);
        }
        return new Evaluation(new State(current.lastActivityAt(), stageAfter(decision), null),
                decision, idleSeconds);
    }

    private Decision decisionFor(Stage stage, long idleSeconds) {
        if (stage.ordinal() < Stage.REMOVAL_SENT.ordinal()
                && idleAtLeast(idleSeconds, removalAfter)) {
            return Decision.REMOVE;
        }
        if (stage.ordinal() < Stage.FINAL_WARNING_SENT.ordinal()
                && idleAtLeast(idleSeconds, finalWarningAfter)) {
            return Decision.FINAL_WARNING;
        }
        if (stage.ordinal() < Stage.WARNING_SENT.ordinal()
                && idleAtLeast(idleSeconds, warningAfter)) {
            return Decision.WARNING;
        }
        return Decision.NONE;
    }

    private static Stage stageAfter(Decision decision) {
        return switch (decision) {
            case WARNING -> Stage.WARNING_SENT;
            case FINAL_WARNING -> Stage.FINAL_WARNING_SENT;
            case REMOVE -> Stage.REMOVAL_SENT;
            case NONE -> Stage.ACTIVE;
        };
    }

    private static boolean idleAtLeast(long idleSeconds, Duration threshold) {
        return idleSeconds >= threshold.getSeconds();
    }

    private static long elapsedSeconds(Instant start, Instant end) {
        if (!end.isAfter(start)) return 0L;
        try {
            return Duration.between(start, end).getSeconds();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private static Duration requireThreshold(String name, Duration threshold) {
        Objects.requireNonNull(threshold, name);
        if (threshold.isNegative()) throw new IllegalArgumentException(name + " cannot be negative");
        return threshold;
    }

    private static boolean strictlyPositive(Duration duration) {
        return !duration.isNegative() && !duration.isZero();
    }

    private static Instant requireTime(Instant time) {
        return Objects.requireNonNull(time, "time");
    }

    public enum Decision {
        NONE,
        WARNING,
        FINAL_WARNING,
        REMOVE
    }

    public enum Stage {
        ACTIVE,
        WARNING_SENT,
        FINAL_WARNING_SENT,
        REMOVAL_SENT
    }

    /** Immutable per-player state held by the arena task. */
    public record State(Instant lastActivityAt, Stage stage, Instant pausedAt) {
        public State {
            Objects.requireNonNull(lastActivityAt, "lastActivityAt");
            Objects.requireNonNull(stage, "stage");
        }
    }

    /** Result of evaluating a state at one point in time. */
    public record Evaluation(State state, Decision decision, long idleSeconds) {
        public Evaluation {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(decision, "decision");
            if (idleSeconds < 0L) throw new IllegalArgumentException("idleSeconds cannot be negative");
        }
    }
}
