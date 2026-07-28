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

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.server.SetupType;
import com.andrei1058.bedwars.arena.ArenaGroupMembership;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static com.andrei1058.bedwars.BedWars.config;

public class SetType extends SubCommand {

    public SetType(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    private static final List<String> available = Arrays.asList("Solo", "Doubles", "3v3v3v3", "4v4v4v4");

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null) {
            s.sendMessage("§c ▪ §7你当前不在竞技场设置会话中！");
            return true;
        }
        if (args.length == 0) {
            sendUsage(p);
        } else {
            if (!available.contains(args[0])) {
                sendUsage(p);
                return true;
            }
            List<String> groups = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
            String input = args[0].substring(0, 1).toUpperCase() + args[0].substring(1).toLowerCase();
            if (!groups.contains(input)) {
                groups.add(input);
                BedWars.config.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS, groups);
            }
            if (input.equals("Solo")) {
                ss.getConfig().getYml().set("maxInTeam", 1);
            } else if (input.equalsIgnoreCase("Doubles")) {
                ss.getConfig().getYml().set("maxInTeam", 2);
            } else if (input.equalsIgnoreCase("3v3v3v3")) {
                ss.getConfig().getYml().set("maxInTeam", 3);
            } else if (input.equalsIgnoreCase("4v4v4v4")) {
                ss.getConfig().getYml().set("maxInTeam", 4);
            }
            int maximum = ss.getConfig().getYml().getInt("maxInTeam", 1);
            ss.getConfig().getYml().set("minInTeam", maximum);
            List<String> arenaGroups = ArenaGroupMembership.withPrimary(
                    ArenaGroupMembership.read(ss.getConfig().getYml()), input);
            ss.getConfig().getYml().set(ArenaGroupMembership.GROUPS_PATH, arenaGroups);
            ss.getConfig().getYml().set(ArenaGroupMembership.LEGACY_GROUP_PATH, null);
            ss.getConfig().save();
            p.sendMessage("§6 ▪ §7竞技场主分组已改为：§d" + input);
            if (ss.getSetupType() == SetupType.ASSISTED) {
                Bukkit.dispatchCommand(p, getParent().getName());
            }
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        List<String> groups = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
        available.forEach(available -> {
            if (!groups.contains(available)) {
                groups.add(available);
            }
        });
        return config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
    }

    private void sendUsage(Player p) {
        p.sendMessage("§9 ▪ §7用法：" + getParent().getName() + " " + getSubCommandName() + " <类型>");
        p.sendMessage("§9可用类型：");
        for (String st : available) {
            p.spigot().sendMessage(Misc.msgHoverClick("§1 ▪ §e" + st + " §7（点击设置）", "§d点击将竞技场设为 " + st, "/" + getParent().getName() + " " + getSubCommandName() + " " + st, ClickEvent.Action.RUN_COMMAND));
        }
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}
