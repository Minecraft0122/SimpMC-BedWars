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

package com.andrei1058.bedwars.commands.shout;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShoutCommand extends BukkitCommand {

    private static final ConcurrentHashMap<UUID, Long> shoutCooldown = new ConcurrentHashMap<>();

    public ShoutCommand(String name) {
        super(name);
    }

    @Override
    public boolean execute(CommandSender s, String st, String[] args) {
        if (s instanceof ConsoleCommandSender) return true;
        Player p = (Player) s;
        if (!Permissions.hasCommandPermission(p, "shout", Permissions.PERMISSION_SHOUT_COMMAND)) {
            p.sendMessage(Language.getMsg(p, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
            return true;
        }
        IArena a = Arena.getArenaByPlayer(p);
        if (a == null || a.isSpectator(p)) {
            p.sendMessage(Language.getMsg(p, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (String ar : args) {
            sb.append(ar).append(" ");
        }

        p.chat("!" + sb);
        return true;
    }

    public static void updateShout(Player player) {
        if (player.hasPermission("bw.shout.bypass")) return;
        shoutCooldown.put(player.getUniqueId(), System.currentTimeMillis()
                + (BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN) * 1000L));
    }

    public static boolean isShoutCooldown(Player player) {
        if (player.hasPermission("bw.shout.bypass")) return false;
        Long expiresAt = shoutCooldown.get(player.getUniqueId());
        if (expiresAt == null) return false;
        if (expiresAt <= System.currentTimeMillis()) {
            shoutCooldown.remove(player.getUniqueId(), expiresAt);
            return false;
        }
        return true;
    }

    public static double getShoutCooldown(Player p) {
        return Math.max(0, (shoutCooldown.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis()) / 1000f);
    }

    public static boolean isShout(Player p) {
        if (!shoutCooldown.containsKey(p.getUniqueId())) return false;
        return shoutCooldown.get(p.getUniqueId()) + 1000 > System.currentTimeMillis();
    }
}
