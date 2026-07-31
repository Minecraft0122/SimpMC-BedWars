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
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class Save extends SubCommand {

    public Save(ParentCommand parent, String name) {
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

        boolean assisted = SetupSession.usesAutomaticAssistance(ss.getSetupType());
        List<String> missingBeds = assisted ? ss.autoDetectAllBeds() : ss.findTeamsWithoutValidBeds();
        Map<String, List<String>> missingGenerators = ss.findTeamsWithoutRequiredGenerators();
        if (!missingBeds.isEmpty()) {
            StringJoiner teams = new StringJoiner(", ");
            missingBeds.forEach(teams::add);
            p.sendMessage(ss.getPrefix() + ChatColor.RED + "在以下队伍出生点附近找不到床：" + teams);
            if (assisted) {
                p.sendMessage(ss.getPrefix() + ChatColor.YELLOW + "请将队伍出生点设得更靠近床，或使用 /"
                        + getParent().getName() + " setBed <队伍> 手动设置。");
            } else {
                p.sendMessage(ss.getPrefix() + ChatColor.YELLOW + "高级模式不会自动设置床位，请使用 /"
                        + getParent().getName() + " setBed <队伍> 手动设置。");
            }
        }

        if (!missingGenerators.isEmpty()) {
            p.sendMessage(ss.getPrefix() + ChatColor.RED + "以下队伍缺少必要的铁或金生成器：");
            missingGenerators.forEach((team, types) -> p.sendMessage(
                    ChatColor.GRAY + " - " + ss.getTeamColor(team) + team + ChatColor.GRAY
                            + "：缺少" + String.join("、", types) + "生成器"));
            p.sendMessage(ss.getPrefix() + ChatColor.YELLOW + "请站在资源点使用 /" + getParent().getName()
                    + " addGenerator；高级模式可使用 addGenerator <iron/gold> <队伍>。");
        }
        if (!missingBeds.isEmpty() || !missingGenerators.isEmpty()) return true;

        //Clear setup armor-stands
        for (Entity e : p.getWorld().getEntities()) {
            if (e.hasMetadata("bw1058-setup") || e.getType() == EntityType.ARMOR_STAND) {
                e.remove();
            }
        }

        ss.done();
        p.sendMessage(ss.getPrefix() + "竞技场修改已保存！");
        p.sendMessage(ss.getPrefix() + "现在可以使用以下命令启用：");
        p.spigot().sendMessage(Misc.msgHoverClick(ChatColor.GOLD + "/" + getParent().getName() + " enableArena " + ss.getWorldName() + ChatColor.GRAY +"（点击启用）", ChatColor.GREEN + "启用此竞技场", "/" + getParent().getName() + " enableArena " + ss.getWorldName(), ClickEvent.Action.RUN_COMMAND));
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}
