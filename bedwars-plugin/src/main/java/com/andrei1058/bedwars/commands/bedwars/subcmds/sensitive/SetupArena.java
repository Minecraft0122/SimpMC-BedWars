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

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static com.andrei1058.bedwars.arena.Arena.getArenaByName;

public class SetupArena extends SubCommand {

    public SetupArena(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(2);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + com.andrei1058.bedwars.commands.bedwars.MainCommand.getInstance().getName() + " setupArena §6<世界名>", "§f创建或编辑竞技场。\n世界名中的 '_' 和 '-' 不会显示在竞技场名称中。",
                "/" + com.andrei1058.bedwars.commands.bedwars.MainCommand.getInstance().getName() + " setupArena ", ClickEvent.Action.SUGGEST_COMMAND));
        showInList(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        if (!MainCommand.isLobbySet(p)) return true;
        if (args.length != 1) {
            p.sendMessage("§c▪ §7用法：§o/" + getParent().getName() + " " + getSubCommandName() + " <地图名>");
            return true;
        }
        if (!args[0].equals(args[0].toLowerCase())) {
            p.sendMessage("§c▪ §c" + args[0] + ChatColor.GRAY + " 不能包含大写字母，请将文件夹重命名为：" + ChatColor.GREEN + args[0].toLowerCase());
            return true;
        }
        if (args[0].contains("+")) {
            p.sendMessage("§c▪ §7" + args[0] + " 不能包含符号：" + ChatColor.RED + "+");
            return true;
        }
        //if (!BedWars.getAPI().getRestoreAdapter().isWorld(args[0])) {
        //    return true;
        //}
        if (getArenaByName(args[0]) != null && !BedWars.autoscale) {
            p.sendMessage("§c▪ §7请先禁用该竞技场！");
            return true;
        }
        if (SetupSession.isInSetupSession(p.getUniqueId())) {
            p.sendMessage("§c ▪ §7你已经处于竞技场设置会话中！");
            return true;
        }
        new SetupSession(p, args[0]);
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return BedWars.getAPI().getRestoreAdapter().getWorldsList();
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (Arena.isInArena(p)) return false;

        if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        return hasPermission(s);
    }
}
