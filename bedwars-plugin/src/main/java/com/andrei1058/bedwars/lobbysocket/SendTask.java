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

package com.andrei1058.bedwars.lobbysocket;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class SendTask {

    /**
     * Send an occasional full snapshot so a lobby that restarted, or a socket
     * that was reconnected without an arena event, can rebuild its directory.
     * Normal state changes are sent by {@link ArenaListeners} immediately.
     */
    public SendTask() {
        long heartbeatSeconds = Math.max(5L, BedWars.config.getYml().getLong(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_STATUS_HEARTBEAT_SECONDS, 15L));
        long period = Math.max(100L, heartbeatSeconds * 20L);
        Bukkit.getScheduler().runTaskTimer(BedWars.plugin, () -> {
            List<String> messages = new ArrayList<>();
            for (IArena arena : Arena.getArenas()) {
                messages.add(ArenaSocket.formatUpdateMessage(arena));
            }
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin,
                    () -> messages.forEach(ArenaSocket::sendMessage));
        }, 100L, period);
    }
}
