/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena.team;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated The player-selectable team-color GUI was removed. Calling
 * {@link #open(Player)} now opens the supported teammate-invite GUI so older
 * integrations fail closed without restoring color selection.
 */
@Deprecated
public final class PreGameTeamSelectionGUI implements Listener {

    private static final PreGameTeamSelectionGUI INSTANCE = new PreGameTeamSelectionGUI();

    private PreGameTeamSelectionGUI() {
    }

    public static PreGameTeamSelectionGUI getInstance() {
        return INSTANCE;
    }

    public void open(@NotNull Player player) {
        PreGameSquadGUI.getInstance().open(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // The old inventory is never created or registered.
    }
}
