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

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static com.andrei1058.bedwars.BedWars.mainCmd;

public class CreateTeam extends SubCommand {

    public CreateTeam(ParentCommand parent, String name) {
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
            AdventureText.send(s, "§c ▪ §7你当前不在竞技场设置会话中！");
            return true;
        }
        if (args.length < 2) {
            AdventureText.send(p, "§c▪ §7用法：/" + mainCmd + " createTeam §o<名称> §o<颜色>");
            StringBuilder colors = new StringBuilder("§7");
            for (TeamColor t : TeamColor.selectableValues()) {
                colors.append(t.chat()).append(t).append(ChatColor.GRAY).append(", ");
            }
            colors = new StringBuilder(colors.substring(0, colors.toString().length() - 2) + ChatColor.GRAY + ".");
            AdventureText.send(p, "§6 ▪ §7可用颜色：" + colors);
        } else {
            TeamColor selectedColor;
            try {
                selectedColor = TeamColor.fromName(args[1]);
            } catch (IllegalArgumentException exception) {
                AdventureText.send(p, "§c▪ §7无效的颜色！");
                StringBuilder colors = new StringBuilder("§7");
                for (TeamColor t : TeamColor.selectableValues()) {
                    colors.append(t.chat()).append(t).append(ChatColor.GRAY).append(", ");
                }
                colors = new StringBuilder(colors.substring(0, colors.toString().length() - 2) + ChatColor.GRAY + ".");
                AdventureText.send(p, "§6 ▪ §7可用颜色：" + colors);
                return true;
            }
            if (ss.getConfig().getYml().get("Team." + args[0] + ".Color") != null) {
                AdventureText.send(p, "§c▪ §7队伍 " + args[0] + " 已存在！");
                return true;
            }
            ss.getConfig().set("Team." + args[0] + ".Color", selectedColor.name());
            AdventureText.send(p, "§6 ▪ §7已创建队伍 " + selectedColor.chat() + args[0] + "§7！");
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }

    @Override
    public boolean canSee(CommandSender s, BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}
