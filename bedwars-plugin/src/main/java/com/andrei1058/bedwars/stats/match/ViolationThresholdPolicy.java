/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.stats.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure threshold crossing policy shared by the database writer and tests. */
final class ViolationThresholdPolicy {

    private ViolationThresholdPolicy() {
    }

    static Evaluation evaluate(int previousTotal, int newTotal, int previousMask,
                               List<Integer> thresholds) {
        if (newTotal <= previousTotal || thresholds == null || thresholds.isEmpty()) {
            return new Evaluation(List.of(), previousMask);
        }
        List<Integer> crossed = new ArrayList<>();
        int mask = previousMask;
        for (int index = 0; index < thresholds.size() && index < Byte.SIZE; index++) {
            Integer thresholdValue = thresholds.get(index);
            if (thresholdValue == null || thresholdValue <= 0) continue;
            int threshold = thresholdValue;
            int bit = 1 << index;
            if ((mask & bit) != 0) continue;
            /* User wording is strict: 10 -> 10 is not a crossing; 10 -> 11 is. */
            if (previousTotal <= threshold && newTotal > threshold) crossed.add(threshold);
            if (newTotal > threshold) mask |= bit;
        }
        return new Evaluation(Collections.unmodifiableList(crossed), mask);
    }

    record Evaluation(List<Integer> crossedThresholds, int warningMask) {
    }
}
