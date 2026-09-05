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

import java.util.concurrent.atomic.AtomicBoolean;

/** Ensures one reconnect lifecycle can create at most one abandonment strike. */
final class AbandonmentPenaltyState {

    private final AtomicBoolean recorded = new AtomicBoolean();

    boolean tryRecord() {
        return recorded.compareAndSet(false, true);
    }
}
