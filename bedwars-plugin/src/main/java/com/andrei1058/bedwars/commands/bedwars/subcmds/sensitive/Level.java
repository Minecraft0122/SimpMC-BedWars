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
import com.andrei1058.bedwars.api.events.player.PlayerXpGainEvent;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.LevelsConfig;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class Level extends SubCommand {

    public Level(ParentCommand parent, String name) {
        super(parent, name);
        setPermission(Permissions.PERMISSION_LEVEL);
        setPriority(10);
        showInList(true);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " §8      - §e点击查看详情", "§f管理玩家等级。",
                "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (args.length == 0) {
            sendSubCommands(s, BedWars.getAPI());
            return true;
        }
        if (args[0].equalsIgnoreCase("setlevel")) {
            if (args.length != 3) {
                s.sendMessage(ChatColor.GOLD + " ▪ " + ChatColor.GRAY + "用法：/bw level setLevel §o<玩家> <等级>");
                return true;
            }
            Player pl = Bukkit.getPlayer(args[1]);
            if (pl == null) {
                s.sendMessage(ChatColor.RED + " ▪ " + ChatColor.GRAY + "找不到玩家！");
                return true;
            }

            int level;

            try {
                level = Integer.parseInt(args[2]);
            } catch (Exception e) {
                s.sendMessage(ChatColor.RED + "等级必须是整数！");
                return true;
            }

            BedWars.getAPI().getLevelsUtil().setLevel(pl, level);

            int nextLevelCost =  LevelsConfig.levels.getYml().get("levels." + level + ".rankup-cost") == null ?
                    LevelsConfig.levels.getInt("levels.others.rankup-cost") : LevelsConfig.levels.getInt("levels." + level + ".rankup-cost");

            String levelName = LevelsConfig.levels.getYml().get("levels." + level + ".name") == null ?
                    LevelsConfig.levels.getYml().getString("levels.others.name") : LevelsConfig.levels.getYml().getString("levels." + level + ".name");


            UUID playerId = pl.getUniqueId();
            BedWars.plugin.getServer().getScheduler().runTaskAsynchronously(BedWars.plugin, () -> {
                BedWars.getRemoteDatabase().setLevelData(playerId, level, 0, levelName, nextLevelCost);
                Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                s.sendMessage(ChatColor.GOLD + " ▪ " + ChatColor.GRAY + "已将 " + pl.getName() + " 的等级设为：" + args[2]);
                s.sendMessage(ChatColor.GOLD + " ▪ " + ChatColor.GRAY + "玩家可能需要重新加入才能看到更新。");
                });
            });
        } else if (args[0].equalsIgnoreCase("givexp")) {
            if (args.length != 3) {
                s.sendMessage(ChatColor.GOLD + " ▪ " + ChatColor.GRAY + "用法：/bw level giveXp §o<玩家> <数量>");
                return true;
            }
            Player pl = Bukkit.getPlayer(args[1]);
            if (pl == null) {
                s.sendMessage(ChatColor.RED + " ▪ " + ChatColor.GRAY + "找不到玩家！");
                return true;
            }

            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (Exception e) {
                s.sendMessage(ChatColor.RED + "数量必须是整数！");
                return true;
            }

            BedWars.getAPI().getLevelsUtil().addXp(pl, amount, PlayerXpGainEvent.XpSource.OTHER);

            UUID playerId = pl.getUniqueId();
            BedWars.plugin.getServer().getScheduler().runTaskAsynchronously(BedWars.plugin, () -> {
                Object[] data = BedWars.getRemoteDatabase().getLevelData(playerId);
                BedWars.getRemoteDatabase().setLevelData(playerId, (Integer) data[0], ((Integer)data[1]) + amount, (String) data[2], (Integer)data[3]);
                Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                s.sendMessage(ChatColor.GOLD + " ▪ " + ChatColor.GRAY + "已给予 " + pl.getName() + " " + args[2] + " 点经验。");
                s.sendMessage(ChatColor.GOLD + " ▪ " + ChatColor.GRAY + "玩家可能需要重新加入才能看到更新。");
                });
            });
        } else {
            sendSubCommands(s, BedWars.getAPI());
        }
        return true;
    }

    private void sendSubCommands(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof Player) {
            Player p = (Player) s;
            p.spigot().sendMessage(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " setLevel §o<player> <level>",
                    "设置玩家等级。", "/" + getParent().getName() + " " + getSubCommandName() + " setLevel",
                    ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " giveXp §o<player> <amount>",
                    "给予玩家经验值。", "/" + getParent().getName() + " " + getSubCommandName() + " giveXp",
                    ClickEvent.Action.SUGGEST_COMMAND));
        } else {
            s.sendMessage(ChatColor.GOLD + "bw level setLevel <玩家> <等级>");
            s.sendMessage(ChatColor.GOLD + "bw level giveXp <玩家> <数量>");
        }
    }

    @Override
    public List<String> getTabComplete() {
        return null;
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
