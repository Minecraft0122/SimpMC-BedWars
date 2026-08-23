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
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static com.andrei1058.bedwars.BedWars.mainCmd;

public class WaitingPos extends SubCommand {

    public WaitingPos(ParentCommand parent, String name) {
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
            AdventureText.send(s, "§c ▪ §7你当前不在竞技场设置会话中！");
            return true;
        }
        if (args.length == 0) {
            AdventureText.send(p, "§c▪ §7用法：/" + mainCmd + " "+getSubCommandName()+" 1 或 2");
        } else {
            if (args[0].equalsIgnoreCase("1") || args[0].equalsIgnoreCase("2")) {
                AdventureText.send(p, "§6 ▪ §7已设置位置 " + args[0] + "！");
                ss.getConfig().saveArenaLoc("waiting.Pos" + args[0], p.getLocation());
                ss.getConfig().reload();
                if (ss.getConfig().getYml().get("waiting.Pos1") == null){
                    AdventureText.send(p, "§c ▪ §7请设置剩余位置：");
                    AdventureText.send(p, Misc.msgHoverClick("§c ▪ §7/"+ BedWars.mainCmd+" waitingPos 1", "§d设置位置 1", "/"+getParent().getName()+" waitingPos 1", ClickEvent.Action.RUN_COMMAND));
                } else if (ss.getConfig().getYml().get("waiting.Pos2") == null){
                    AdventureText.send(p, "§c ▪ §7请设置剩余位置：");
                    AdventureText.send(p, Misc.msgHoverClick("§c ▪ §7/"+ BedWars.mainCmd+" waitingPos 2", "§d设置位置 2", "/"+getParent().getName()+" waitingPos 2", ClickEvent.Action.RUN_COMMAND));
                }
            } else {
                AdventureText.send(p, "§c▪ §7用法：/" + mainCmd + " "+getSubCommandName()+" 1 或 2");
            }
        }
        if (!((ss.getConfig().getYml().get("waiting.Pos1") == null || ss.getConfig().getYml().get("waiting.Pos2") == null))){
            Bukkit.dispatchCommand(p, BedWars.mainCmd+" cmds");
            AdventureText.send(s, "§6 ▪ §7如果尚未设置，请继续设置各队伍出生点！");
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("1", "2");
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}
