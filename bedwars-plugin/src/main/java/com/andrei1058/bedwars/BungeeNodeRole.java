/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars;

import java.util.Locale;

/** Runtime role of a BUNGEE-mode Paper instance. */
public enum BungeeNodeRole {
    ARENA,
    LOBBY;

    public static BungeeNodeRole parse(String value) {
        if (value == null || value.isBlank()) return ARENA;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ARENA;
        }
    }
}
