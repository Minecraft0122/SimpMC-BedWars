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

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static com.andrei1058.bedwars.BedWars.mainCmd;

public class SetMaxInTeam extends SubCommand {

    public SetMaxInTeam(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null){
            s.sendMessage("§c ▪ §7你当前不在竞技场设置会话中！");
            return true;
        }
        int maximum;
        try {
            maximum = args.length == 0 ? 0 : Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            maximum = 0;
        }
        if (maximum < 1) {
            p.sendMessage("§c▪ §7每队人数必须是大于 0 的整数。");
            p.sendMessage("§c▪ §7用法：/" + mainCmd + " setMaxInTeam <整数>");
            return true;
        }
        ss.getConfig().getYml().set("maxInTeam", maximum);
        ss.getConfig().getYml().set("minInTeam", maximum);
        ss.getConfig().save();
        p.sendMessage("§6 ▪ §7已设置每队人数为 §e" + maximum
                + "§7，并将每队开局下限同步为相同值；可用 /" + mainCmd
                + " setMinInTeam <整数> 单独降低下限。");
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("1", "2", "3", "4", "8", "16");
    }

    @Override
    public boolean canSee(CommandSender s, BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}
