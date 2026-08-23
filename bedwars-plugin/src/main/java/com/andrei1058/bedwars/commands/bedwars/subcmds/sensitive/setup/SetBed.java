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
import com.andrei1058.bedwars.api.server.SetupType;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.BedLocator;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.configuration.Sounds;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.andrei1058.bedwars.BedWars.mainCmd;
import static com.andrei1058.bedwars.commands.Misc.createArmorStand;
import static com.andrei1058.bedwars.commands.Misc.removeArmorStand;

public class SetBed extends SubCommand {

    public SetBed(ParentCommand parent, String name) {
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
        if (args.length == 0) {
            String foundTeam = ss.getNearestTeam();
            if (foundTeam.isEmpty()) {
                AdventureText.send(p, "");
                AdventureText.send(p, ss.getPrefix() + ChatColor.RED + "附近没有找到队伍。");
                AdventureText.send(p, Misc.msgHoverClick(ss.getPrefix() + "请先设置队伍出生点！", ChatColor.WHITE + "设置队伍床位", "/" + getParent().getName() + " " + getSubCommandName() + " ", ClickEvent.Action.SUGGEST_COMMAND));
                AdventureText.send(p, Misc.msgHoverClick(ss.getPrefix() + "如果已设置出生点但未自动找到，请使用：/bw " + getSubCommandName() + " <队伍>", "添加队伍床位", "/" + getParent().getName() + " " + getSubCommandName() + " ", ClickEvent.Action.SUGGEST_COMMAND));
                com.andrei1058.bedwars.BedWars.nms.sendTitle(p, AdventureText.section(" "), AdventureText.section(ChatColor.RED + "附近没有找到队伍。"), 5, 60, 5);
                Sounds.playSound(ConfigPath.SOUNDS_INSUFF_MONEY, p);
                ss.displayAvailableTeams();

            } else {
                Bukkit.dispatchCommand(s, getParent().getName() + " " + getSubCommandName() + " " + foundTeam);
            }
        } else {
            Location bedLocation = BedLocator.findNearestBed(p.getLocation(), 2);
            if (bedLocation == null) {
                AdventureText.send(p, ss.getPrefix() + ChatColor.RED + "使用此命令时必须站在床旁边！");
                com.andrei1058.bedwars.BedWars.nms.sendTitle(p, AdventureText.section(" "), AdventureText.section(ChatColor.RED + "请站在床旁边。"), 5, 40, 5);
                Sounds.playSound(ConfigPath.SOUNDS_INSUFF_MONEY, p);
                return true;
            }
            if (ss.getConfig().getYml().get("Team." + args[0]) == null) {
                AdventureText.send(p, ss.getPrefix() + ChatColor.RED + "该队伍不存在！");
                if (ss.getConfig().getYml().get("Team") != null) {
                    AdventureText.send(p, ss.getPrefix() + "可用队伍：");
                    for (String team : Objects.requireNonNull(ss.getConfig().getYml().getConfigurationSection("Team")).getKeys(false)) {
                        AdventureText.send(p, Misc.msgHoverClick(ChatColor.GOLD + " " + '▪' + " " + ss.getTeamColor(team) + team, ChatColor.WHITE + "设置 " + ss.getTeamColor(team) + team + " 的床位", "/" + mainCmd + " setBed " + team, ClickEvent.Action.RUN_COMMAND));
                    }
                }
            } else {
                String team = ss.getTeamColor(args[0]) + args[0];
                if (ss.getConfig().getYml().get("Team." + args[0] + ".Bed") != null) {
                    removeArmorStand("床位", ss.getConfig().getArenaLoc("Team." + args[0] + ".Bed"), null);
                }
                createArmorStand(team + " " + ChatColor.GOLD + "床位已设置", bedLocation, null);
                ss.getConfig().saveArenaLoc("Team." + args[0] + ".Bed", bedLocation);
                AdventureText.send(p, ss.getPrefix() + "已设置床位：" + team);

                com.andrei1058.bedwars.BedWars.nms.sendTitle(p, AdventureText.section(" "), AdventureText.section(ChatColor.GREEN + "已设置床位：" + team), 5, 40, 5);
                Sounds.playSound(ConfigPath.SOUNDS_BOUGHT, p);

                if (ss.getSetupType() == SetupType.ASSISTED) {
                    Bukkit.dispatchCommand(p, getParent().getName());
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
