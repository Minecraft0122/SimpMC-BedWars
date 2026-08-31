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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BungeeNodeRoleTest {

    @Test
    void parsesRoleWithoutDependingOnCase() {
        assertEquals(BungeeNodeRole.LOBBY, BungeeNodeRole.parse("lObBy"));
        assertEquals(BungeeNodeRole.ARENA, BungeeNodeRole.parse(" arena "));
    }

    @Test
    void invalidOrMissingRoleFallsBackToArenaForCompatibility() {
        assertEquals(BungeeNodeRole.ARENA, BungeeNodeRole.parse(null));
        assertEquals(BungeeNodeRole.ARENA, BungeeNodeRole.parse(""));
        assertEquals(BungeeNodeRole.ARENA, BungeeNodeRole.parse("unknown-role"));
    }
}
