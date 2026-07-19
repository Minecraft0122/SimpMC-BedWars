/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.upgrades.UpgradesIndex;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.upgrades.UpgradesManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CmdUpgrades extends SubCommand {

    public CmdUpgrades(ParentCommand parent, String name) {
        super(parent, name);
        showInList(false);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (!(s instanceof Player player)) return false;
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) return true;
        ITeam team = arena.getTeam(player);
        if (canOpenMenu(arena.getStatus(), arena.isPlayer(player), team, player.getLocation())) {
            UpgradesIndex menu = UpgradesManager.getMenuForArena(arena);
            if (menu != null) menu.open(player);
        }
        return true;
    }

    static boolean canOpenMenu(GameState state, boolean arenaPlayer, ITeam team, Location playerLocation) {
        return state == GameState.playing && arenaPlayer && team != null
                && ConfigManager.isSameWorldWithin(playerLocation, team.getTeamUpgrades(), 4D);
    }

    @Override
    public List<String> getTabComplete() {
        return new ArrayList<>();
    }
}
