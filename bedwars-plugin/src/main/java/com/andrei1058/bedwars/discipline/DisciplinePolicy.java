/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.discipline;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure policy used to turn a player's accumulated discipline strikes into a
 * cooldown decision.  The class has no Bukkit or database dependency, which
 * keeps the escalation rules deterministic and easy to test.
 */
public final class DisciplinePolicy {

    private final EnumMap<Category, Rule> rules;

    /** Creates a policy with the default AFK, abandonment, and violation rules. */
    public DisciplinePolicy() {
        this(defaultAfkRule(), defaultAbandonmentRule(), defaultViolationRule());
    }

    /**
     * Creates a policy with independently configurable rules for each category.
     * The supplied rules are copied, so later changes to the caller's objects do
     * not alter this policy.
     */
    public DisciplinePolicy(Rule afk, Rule abandonment, Rule violation) {
        this(Map.of(
                Category.AFK, Objects.requireNonNull(afk, "afk"),
                Category.ABANDONMENT, Objects.requireNonNull(abandonment, "abandonment"),
                Category.VIOLATION, Objects.requireNonNull(violation, "violation")));
    }

    /** Creates a policy from a complete set of category rules. */
    public DisciplinePolicy(Map<Category, Rule> rules) {
        Objects.requireNonNull(rules, "rules");
        EnumMap<Category, Rule> copy = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            Rule rule = rules.get(category);
            if (rule == null) {
                throw new IllegalArgumentException("Missing rule for " + category);
            }
            copy.put(category, rule);
        }
        this.rules = copy;
    }

    public static Rule defaultAfkRule() {
        return new Rule(new int[]{1, 2, 3, 5}, new long[]{0L, 600L, 3_600L, 86_400L});
    }

    public static Rule defaultAbandonmentRule() {
        return new Rule(new int[]{1, 2, 3, 5}, new long[]{300L, 900L, 3_600L, 86_400L});
    }

    public static Rule defaultViolationRule() {
        return Rule.fixed(1, 1_800L);
    }

    /** Returns a defensive copy of the configured rule for a category. */
    public Rule rule(Category category) {
        return rules.get(Objects.requireNonNull(category, "category"));
    }

    /**
     * Evaluates one strike count.  Counts below one intentionally produce a
     * no-punishment decision; this makes a missing/new player record safe.
     */
    public Decision evaluate(Category category, int occurrence) {
        Objects.requireNonNull(category, "category");
        return rules.get(category).evaluate(category, occurrence);
    }

    public long cooldownSeconds(Category category, int occurrence) {
        return evaluate(category, occurrence).cooldownSeconds();
    }

    public boolean shouldPunish(Category category, int occurrence) {
        return evaluate(category, occurrence).shouldPunish();
    }

    public enum Category {
        AFK,
        ABANDONMENT,
        VIOLATION
    }

    /**
     * Immutable threshold table.  Each occurrence at or above a threshold uses
     * that threshold's cooldown; occurrences above the final threshold keep the
     * final escalation level.
     */
    public static final class Rule {
        private final int[] thresholds;
        private final long[] cooldownSeconds;

        public Rule(int[] thresholds, long[] cooldownSeconds) {
            Objects.requireNonNull(thresholds, "thresholds");
            Objects.requireNonNull(cooldownSeconds, "cooldownSeconds");
            if (thresholds.length == 0 || thresholds.length != cooldownSeconds.length) {
                throw new IllegalArgumentException("thresholds and cooldownSeconds must have the same non-zero length");
            }

            this.thresholds = thresholds.clone();
            this.cooldownSeconds = cooldownSeconds.clone();
            int previous = 0;
            for (int index = 0; index < this.thresholds.length; index++) {
                int threshold = this.thresholds[index];
                if (threshold <= previous) {
                    throw new IllegalArgumentException("thresholds must be strictly increasing and positive");
                }
                long cooldown = this.cooldownSeconds[index];
                if (cooldown < 0L) {
                    throw new IllegalArgumentException("cooldownSeconds cannot contain negative values");
                }
                previous = threshold;
            }
        }

        /** Creates a rule with one cooldown level applying from {@code threshold}. */
        public static Rule fixed(int threshold, long cooldownSeconds) {
            return new Rule(new int[]{threshold}, new long[]{cooldownSeconds});
        }

        public int[] thresholds() {
            return thresholds.clone();
        }

        public long[] cooldownSeconds() {
            return cooldownSeconds.clone();
        }

        private Decision evaluate(Category category, int occurrence) {
            if (occurrence < thresholds[0]) {
                return new Decision(category, occurrence, 0, 0L, false);
            }

            int selectedIndex = 0;
            for (int index = 1; index < thresholds.length; index++) {
                if (occurrence < thresholds[index]) {
                    break;
                }
                selectedIndex = index;
            }
            return new Decision(category, occurrence, thresholds[selectedIndex],
                    cooldownSeconds[selectedIndex], cooldownSeconds[selectedIndex] > 0L);
        }
    }

    /** Immutable result of evaluating a category and occurrence. */
    public record Decision(Category category, int occurrence, int matchedThreshold,
                           long cooldownSeconds, boolean shouldPunish) {
        public Decision {
            Objects.requireNonNull(category, "category");
            if (matchedThreshold < 0) {
                throw new IllegalArgumentException("matchedThreshold cannot be negative");
            }
            if (cooldownSeconds < 0L) {
                throw new IllegalArgumentException("cooldownSeconds cannot be negative");
            }
            if (!shouldPunish && cooldownSeconds > 0L) {
                throw new IllegalArgumentException("a positive cooldown must require punishment");
            }
        }

        /** Returns the expiration instant for a cooldown issued at {@code issuedAt}. */
        public Instant expiresAt(Instant issuedAt) {
            Objects.requireNonNull(issuedAt, "issuedAt");
            return cooldownSeconds == 0L ? issuedAt : issuedAt.plusSeconds(cooldownSeconds);
        }

        /** Returns whether this decision still blocks a player at {@code now}. */
        public boolean activeAt(Instant issuedAt, Instant now) {
            Objects.requireNonNull(issuedAt, "issuedAt");
            Objects.requireNonNull(now, "now");
            return shouldPunish && now.isBefore(expiresAt(issuedAt));
        }

        /** Returns whether this decision has no active cooldown at {@code now}. */
        public boolean expiredAt(Instant issuedAt, Instant now) {
            return !activeAt(issuedAt, now);
        }
    }
}
