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
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.server.SetupType;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AutoCreateTeams extends SubCommand {

    public AutoCreateTeams(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    private static final long CONFIRMATION_TIMEOUT_MILLIS = 16_000L;
    private static final Map<UUID, PendingDetection> pendingDetections = new HashMap<>();

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        UUID playerId = p.getUniqueId();
        SetupSession ss = SetupSession.getSession(playerId);
        if (ss == null) {
            AdventureText.send(s, "§c ▪ §7你当前不在竞技场设置会话中！");
            return true;
        }
        if (ss.getSetupType() != SetupType.ASSISTED) return false;

        PendingDetection pending = pendingDetections.remove(playerId);
        if (pending != null && pending.expiresAt() >= System.currentTimeMillis()) {
            for (TeamColor color : pending.colors()) {
                Bukkit.dispatchCommand(s, BedWars.mainCmd + " createTeam "
                        + color.setupName() + " " + color.name());
            }
            if (ss.getConfig().getYml().get("waiting.Pos1") == null) {
                AdventureText.send(s, "");
                AdventureText.send(s, "§6§l移除等待大厅：");
                AdventureText.send(s, "§f如果希望游戏开始时移除等待大厅，");
                AdventureText.send(s, "§f请像 WorldEdit 选区一样设置下面两个位置。");
                AdventureText.send(p, Misc.msgHoverClick("§c ▪ §7/" + BedWars.mainCmd + " waitingPos 1", "§d设置位置 1", "/" + getParent().getName() + " waitingPos 1", ClickEvent.Action.RUN_COMMAND));
                AdventureText.send(p, Misc.msgHoverClick("§c ▪ §7/" + BedWars.mainCmd + " waitingPos 2", "§d设置位置 2", "/" + getParent().getName() + " waitingPos 2", ClickEvent.Action.RUN_COMMAND));
                AdventureText.send(s, "");
                AdventureText.send(s, "§7此步骤可选，如需跳过请输入 §6/" + BedWars.mainCmd);
            }
            return true;
        }

        List<TeamColor> found = new ArrayList<>();
        World w = Bukkit.getWorld(ss.getWorldName());
        if (w == null) {
            AdventureText.send(p, ss.getPrefix() + "§c竞技场世界尚未加载，无法扫描队伍羊毛。");
            return true;
        }
        if (ss.getConfig().getYml().get("Team") == null) {
            AdventureText.send(p, "§6 ▪ §7正在搜索队伍，期间可能出现短暂卡顿。");
            for (int x = -200; x < 200; x++) {
                for (int y = 50; y < 130; y++) {
                    for (int z = -200; z < 200; z++) {
                        Block block = w.getBlockAt(x, y, z);
                        TeamColor color = TeamColor.fromWool(block.getType());
                        if (color != null && !found.contains(color)
                                && countNearbyWool(block, color.woolMaterial()) >= 5) {
                            found.add(color);
                        }
                    }
                }
            }
        }
        if (found.isEmpty()) {
            AdventureText.send(p, "§6 ▪ §7没有找到新队伍。\n§6 ▪ §7请手动创建队伍：§6/" + BedWars.mainCmd + " createTeam");
            return true;
        }

        PendingDetection detection = new PendingDetection(
                System.currentTimeMillis() + CONFIRMATION_TIMEOUT_MILLIS, List.copyOf(found));
        pendingDetections.put(playerId, detection);
        Bukkit.getScheduler().runTaskLater(BedWars.plugin,
                () -> pendingDetections.remove(playerId, detection),
                CONFIRMATION_TIMEOUT_MILLIS / 50L);
        AdventureText.send(p, "§6§l发现新队伍：");
        for (TeamColor color : found) {
            AdventureText.send(p, "§f ▪ " + color.chat() + color.setupName().replace("_", " "));
        }
        AdventureText.send(p, Misc.msgHoverClick("§6 ▪ §7§l点击创建已发现的队伍。", "§f点击创建这些队伍", "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
        return true;
    }

    static int countNearbyWool(Block center, Material wool) {
        int count = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (center.getRelative(x, y, z).getType() == wool) count++;
                }
            }
        }
        return count;
    }

    private record PendingDetection(long expiresAt, List<TeamColor> colors) {
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
