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

package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class Permissions {
    private static final String PREFIX = "bw";

    public static final String PERMISSION_FORCESTART = PREFIX + ".forcestart";
    public static final String PERMISSION_DEBUG_START = PREFIX + ".command.start.debug";
    public static final String PERMISSION_ALL = PREFIX + ".*";
    public static final String PERMISSION_COMMAND_BYPASS = PREFIX + ".cmd.bypass";
    public static final String PERMISSION_SHOUT_COMMAND = PREFIX + ".shout";

    public static final String PERMISSION_SETUP_ARENA = PREFIX + ".setup";
    public static final String PERMISSION_ARENA_GROUP = PREFIX + ".groups";
    public static final String PERMISSION_BUILD = PREFIX + ".build";
    public static final String PERMISSION_CLONE = PREFIX + ".clone";
    public static final String PERMISSION_DEL_ARENA = PREFIX + ".delete";
    public static final String PERMISSION_ARENA_ENABLE = PREFIX + ".enableRotation";
    public static final String PERMISSION_ARENA_DISABLE = PREFIX + ".disable";
    public static final String PERMISSION_NPC = PREFIX + ".npc";
    public static final String PERMISSION_RELOAD = PREFIX + ".reload";
    public static final String PERMISSION_REJOIN = PREFIX + ".rejoin";
    public static final String PERMISSION_LEVEL = PREFIX + ".level";
    public static final String PERMISSION_CHAT_COLOR = PREFIX + ".chatcolor";
    public static final String PERMISSION_VIP = PREFIX + ".vip";
    public static final String PERMISSION_PLAYER = "bw.player";
    public static final String PERMISSION_COMMAND_ALL = "bw.command.*";

    public static String command(String name) {
        return "bw.command." + name.toLowerCase(Locale.ROOT);
    }

    public static boolean hasCommandPermission(CommandSender sender, String name, String... legacyPermissions) {
        if (sender.hasPermission(PERMISSION_ALL) || sender.hasPermission(PERMISSION_COMMAND_ALL)
                || sender.hasPermission(command(name))
                || (SubCommand.isPlayerCommand(name) && sender.hasPermission(PERMISSION_PLAYER))) {
            return true;
        }
        for (String permission : legacyPermissions) {
            if (sender.hasPermission(permission)) return true;
        }
        return false;
    }

    /**
     * Check if player has one of the given permissions.
     */
    public static boolean hasPermission(Player player, String... permissions){
        for (String permission : permissions){
            if (player.hasPermission(permission)){
                return true;
            }
        }
        return false;
    }

    /**
     * Check if player has all given permissions.
     */
    public static boolean hasPermissions(Player player, String... permissions){
        for (String permission : permissions){
            if (!player.hasPermission(permission)){
                return false;
            }
        }
        return true;
    }
}
