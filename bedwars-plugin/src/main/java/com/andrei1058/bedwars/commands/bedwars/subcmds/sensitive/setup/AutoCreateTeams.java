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
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.server.SetupType;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AutoCreateTeams extends SubCommand {

    public AutoCreateTeams(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    private static final HashMap<Player, Long> timeOut = new HashMap<>();
    private static final HashMap<Player, List<String>> teamsFound = new HashMap<>();

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null) {
            s.sendMessage("§c ▪ §7你当前不在竞技场设置会话中！");
            return true;
        }
        if (ss.getSetupType() == SetupType.ASSISTED) {
                if (timeOut.containsKey(p) && timeOut.get(p) >= System.currentTimeMillis() && teamsFound.containsKey(p)) {
                    for (String tf : teamsFound.get(p)) {
                        Bukkit.dispatchCommand(s, BedWars.mainCmd + " createTeam " + TeamColor.enName(tf) + " " + TeamColor.enName(tf));
                    }
                    if (ss.getConfig().getYml().get("waiting.Pos1") == null) {
                        s.sendMessage("");
                        s.sendMessage("§6§l移除等待大厅：");
                        s.sendMessage("§f如果希望游戏开始时移除等待大厅，");
                        s.sendMessage("§f请像 WorldEdit 选区一样设置下面两个位置。");
                        p.spigot().sendMessage(Misc.msgHoverClick("§c ▪ §7/" + BedWars.mainCmd + " waitingPos 1", "§d设置位置 1", "/" + getParent().getName() + " waitingPos 1", ClickEvent.Action.RUN_COMMAND));
                        p.spigot().sendMessage(Misc.msgHoverClick("§c ▪ §7/" + BedWars.mainCmd + " waitingPos 2", "§d设置位置 2", "/" + getParent().getName() + " waitingPos 2", ClickEvent.Action.RUN_COMMAND));
                        s.sendMessage("");
                        s.sendMessage("§7此步骤可选，如需跳过请输入 §6/" + BedWars.mainCmd);
                    }
                    return true;
                }
                List<String> found = new ArrayList<>();
                World w = p.getWorld();
                if (ss.getConfig().getYml().get("Team") == null) {
                    p.sendMessage("§6 ▪ §7正在搜索队伍，期间可能出现短暂卡顿。");
                    for (int x = -200; x < 200; x++) {
                        for (int y = 50; y < 130; y++) {
                            for (int z = -200; z < 200; z++) {
                                Block b = new Location(w, x, y, z).getBlock();
                                if (b.getType().toString().contains("_WOOL")) {
                                    if (!found.contains(b.getType().toString())) {
                                        int count = 0;
                                        for (int x1 = -2; x1 < 2; x1++) {
                                            for (int y1 = -2; y1 < 2; y1++) {
                                                for (int z1 = -2; z1 < 2; z1++) {
                                                    Block b2 = new Location(w, x, y, z).getBlock();
                                                    if (b2.getType() == b.getType()) {
                                                        count++;
                                                    }
                                                }
                                            }
                                        }
                                        if (count >= 5) {
                                            if (!TeamColor.enName(b.getType().toString()).isEmpty()) {
                                                if (ss.getConfig().getYml().get("Team." + TeamColor.enName(b.getType().toString())) == null) {
                                                    found.add(b.getType().toString());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (found.isEmpty()) {
                    p.sendMessage("§6 ▪ §7没有找到新队伍。\n§6 ▪ §7请手动创建队伍：§6/" + BedWars.mainCmd + " createTeam");
                } else {
                    if (timeOut.containsKey(p)) {
                        p.sendMessage("§c ▪ §7搜索超时，请再次输入命令重试。");
                        timeOut.remove(p);
                        return true;
                    } else {
                        timeOut.put(p, System.currentTimeMillis() + 16000);
                    }
                    if (teamsFound.containsKey(p)) {
                        teamsFound.replace(p, found);
                    } else {
                        teamsFound.put(p, found);
                    }
                    p.sendMessage("§6§l发现新队伍：");
                    for (String tf : found) {
                        String name = TeamColor.enName(tf);
                        p.sendMessage("§f ▪ " + TeamColor.getChatColor(name) + name.replace("_", " "));
                    }
                    p.spigot().sendMessage(Misc.msgHoverClick("§6 ▪ §7§l点击创建已发现的队伍。", "§f点击创建这些队伍", "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
                }
        } else return false;
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
