/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.lobbysocket;

/** 协议常量。1.8 竞技场节点与 1.21.11 大厅共用这一版本。 */
public final class BungeeProtocol {

    public static final int CURRENT_VERSION = 2;
    public static final String ARENA_ROLE = "ARENA";

    private BungeeProtocol() {
    }
}
