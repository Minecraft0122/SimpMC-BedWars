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

import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.andrei1058.bedwars.BedWars.mainCmd;
import static com.andrei1058.bedwars.commands.Misc.removeArmorStand;

public class SetSpawn extends SubCommand {

    public SetSpawn(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null) {
            return false;
        }
        if (args.length < 1) {
            p.sendMessage(ss.getPrefix() + ChatColor.RED + "用法：/" + mainCmd + " setSpawn <队伍>");
            if (ss.getConfig().getYml().get("Team") != null) {
                for (String team : Objects.requireNonNull(ss.getConfig().getYml().getConfigurationSection("Team")).getKeys(false)) {
                    if (ss.getConfig().getYml().get("Team." + team + ".Spawn") == null) {
                        p.spigot().sendMessage(Misc.msgHoverClick(ss.getPrefix() + "为队伍设置出生点：" + ss.getTeamColor(team) + team + " " + ChatColor.getLastColors(ss.getPrefix()) + "（点击设置）", ChatColor.WHITE + "设置 " + ss.getTeamColor(team) + team + " 的出生点", "/" + mainCmd + " setSpawn " + team, ClickEvent.Action.RUN_COMMAND));
                    }
                }
            }
        } else {
            if (ss.getConfig().getYml().get("Team." + args[0]) == null) {
                p.sendMessage(ss.getPrefix() + ChatColor.RED + "找不到目标队伍：" + args[0]);
                if (ss.getConfig().getYml().get("Team") != null) {
                    p.sendMessage(ss.getPrefix() + "队伍列表：");
                    for (String team : Objects.requireNonNull(ss.getConfig().getYml().getConfigurationSection("Team")).getKeys(false)) {
                        p.spigot().sendMessage(Misc.msgHoverClick(ChatColor.GOLD + " " + '▪' + " " + ss.getTeamColor(team) + team + " " + ChatColor.getLastColors(ss.getPrefix()) + "（点击设置）", ChatColor.WHITE + "设置 " + ss.getTeamColor(team) + team + " 的出生点", "/" + mainCmd + " setSpawn " + team, ClickEvent.Action.RUN_COMMAND));
                    }
                }
            } else {
                if (ss.getConfig().getYml().get("Team." + args[0] + ".Spawn") != null) {
                    removeArmorStand("出生点", ss.getConfig().getArenaLoc("Team." + args[0] + ".Spawn"), ss.getConfig().getString("Team." + args[0] + ".Spawn"));
                }
                String teamRoot = "Team." + args[0] + ".";
                ss.getConfig().savePlayerArenaLocation(teamRoot + "Spawn",
                        teamRoot + ConfigPath.ARENA_TEAM_SPAWN_FACING, p.getLocation());
                String teamm = ss.getTeamColor(args[0]) + args[0];
                p.sendMessage(ChatColor.GOLD + " " + '▪' + " " + "已设置出生点：" + teamm);
                com.andrei1058.bedwars.commands.Misc.createArmorStand(teamm + " " + ChatColor.GOLD + "出生点已设置", p.getLocation(), ss.getConfig().stringLocationArenaFormat(p.getLocation()));
                Location bed = ss.autoDetectBed(args[0], true);
                if (bed != null) {
                    p.sendMessage(ss.getPrefix() + ChatColor.GREEN + "已自动识别床位：" + teamm);
                } else {
                    p.sendMessage(ss.getPrefix() + ChatColor.YELLOW + "在队伍岛屿半径内没有找到床。");
                }
                if (ss.getConfig().getYml().get("Team") != null) {
                    StringBuilder remainging = new StringBuilder();
                    for (String team : Objects.requireNonNull(ss.getConfig().getYml().getConfigurationSection("Team")).getKeys(false)) {
                        if (ss.getConfig().getYml().get("Team." + team + ".Spawn") == null) {
                            remainging.append(ss.getTeamColor(team)).append(team).append(" ");
                        }
                    }
                    if (remainging.toString().length() > 0) {
                        p.sendMessage(ss.getPrefix() + "尚未设置：" + remainging);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return new ArrayList<>();
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}
