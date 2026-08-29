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

package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class CmdProcess implements Listener {

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {

        Player p = e.getPlayer();

        if (e.getMessage().equals("/party sethome")){
            AdventureText.send(p, getMsg(p, Messages.COMMAND_NOT_ALLOWED_IN_GAME));
            e.setCancelled(true);
        }

        if (e.getMessage().equals("/party home")){
            AdventureText.send(p, getMsg(p, Messages.COMMAND_NOT_ALLOWED_IN_GAME));
            e.setCancelled(true);
        }

        if (p.hasPermission(Permissions.PERMISSION_COMMAND_BYPASS)) return;
        String[] cmd = e.getMessage().replaceFirst("/", "").split(" ");
        if (cmd.length == 0) return;
        if (Arena.isInArena(p)) {
            if (!isAllowedInArena(cmd[0], BedWars.config.getList(
                    ConfigPath.CENERAL_CONFIGURATION_ALLOWED_COMMANDS))) {
                AdventureText.send(p, getMsg(p, Messages.COMMAND_NOT_ALLOWED_IN_GAME));
                e.setCancelled(true);
            }
        }
    }

    static boolean isAllowedInArena(String command, List<?> configuredCommands) {
        if (command == null) return false;
        if (isBuiltInArenaCommand(command)) return true;
        if (configuredCommands == null) return false;
        return configuredCommands.stream()
                .map(String::valueOf)
                .anyMatch(command::equalsIgnoreCase);
    }

    /** Commands implemented by BedWars that must remain usable during a game. */
    private static boolean isBuiltInArenaCommand(String command) {
        return isShoutCommand(command) || command.equalsIgnoreCase("rejoin");
    }

    private static boolean isShoutCommand(String command) {
        return command.equalsIgnoreCase("shout")
                || command.equalsIgnoreCase("hh")
                || command.equalsIgnoreCase("h");
    }
}
