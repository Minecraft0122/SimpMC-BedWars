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

package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.SetupType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.ArenaGroupPolicy;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.getList;

public class CmdList extends SubCommand {

    public CmdList(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(11);
        showInList(true);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + com.andrei1058.bedwars.commands.bedwars.MainCommand.getInstance().getName() + " " + getSubCommandName() + "         §8 - §e查看玩家命令", "§f查看玩家可用命令。", "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        if (SetupSession.isInSetupSession(p.getUniqueId())) {
            SetupSession ss = SetupSession.getSession(p.getUniqueId());
            Objects.requireNonNull(ss).getConfig().reload();

            boolean waitingSpawn = ss.getConfig().getYml().get("waiting.Loc") != null,
                    pos1 = ss.getConfig().getYml().get("waiting.Pos1") != null,
                    pos2 = ss.getConfig().getYml().get("waiting.Pos2") != null,
                    pos = pos1 && pos2;
            StringBuilder spawnNotSetNames = new StringBuilder();
            StringBuilder bedNotSet = new StringBuilder();
            StringBuilder shopNotSet = new StringBuilder();
            StringBuilder killDropsNotSet = new StringBuilder();
            StringBuilder upgradeNotSet = new StringBuilder();
            StringBuilder spawnNotSet = new StringBuilder();
            StringBuilder generatorNotSet = new StringBuilder();
            int teams = 0;

            if (ss.getConfig().getYml().get("Team") != null) {
                for (String team : ss.getConfig().getYml().getConfigurationSection("Team").getKeys(true)) {
                    if (ss.getConfig().getYml().get("Team." + team + ".Color") == null) continue;
                    ChatColor color = TeamColor.getChatColor(ss.getConfig().getYml().getString("Team." + team + ".Color"));
                    if (ss.getConfig().getYml().get("Team." + team + ".Spawn") == null) {
                        spawnNotSet.append(color).append("▋");
                        spawnNotSetNames.append(color).append(team).append(" ");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + ".Bed") == null) {
                        bedNotSet.append(color).append("▋");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + ".Shop") == null) {
                        shopNotSet.append(color).append("▋");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + "." + ConfigPath.ARENA_TEAM_KILL_DROPS_LOC) == null) {
                        killDropsNotSet.append(color).append("▋");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + ".Upgrade") == null) {
                        upgradeNotSet.append(color).append("▋");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + ".Iron") == null || ss.getConfig().getYml().get("Team." + team + ".Gold") == null) {
                        generatorNotSet.append(color).append("▋");
                    }
                    teams++;
                }
            }
            int emGen = 0, dmGen = 0;
            if (ss.getConfig().getYml().get("generator.Emerald") != null) {
                emGen = ss.getConfig().getYml().getStringList("generator.Emerald").size();
            }
            if (ss.getConfig().getYml().get("generator.Diamond") != null) {
                dmGen = ss.getConfig().getYml().getStringList("generator.Diamond").size();
            }

            String posMsg, group = ChatColor.RED + "（未设置）";
            if (pos1 && !pos2) {
                posMsg = ChatColor.RED + "（位置 2 未设置）";
            } else if (!pos1 && pos2) {
                posMsg = ChatColor.RED + "（位置 1 未设置）";
            } else if (pos1) {
                posMsg = ChatColor.GREEN + "（已设置）";
            } else {
                posMsg = ChatColor.GRAY + "（未设置）" + ChatColor.ITALIC + "可选";
            }

            String arenaGroup = ArenaGroupPolicy.read(ss.getConfig().getYml());
            if (!arenaGroup.equalsIgnoreCase(ArenaGroupPolicy.DEFAULT_GROUP)) {
                group = ChatColor.GREEN + "(" + arenaGroup + ")";
            }

            int maxInTeam = ss.getConfig().getInt("maxInTeam");
            int minInTeam = ss.getConfig().getInt("minInTeam");

            String setWaitingSpawn = ss.dot() + (waitingSpawn ? ChatColor.STRIKETHROUGH : "") + "setWaitingSpawn" + ChatColor.RESET + " " + (waitingSpawn ? ChatColor.GREEN + "（已设置）" : ChatColor.RED + "（未设置）");
            String waitingPos = ss.dot() + (pos ? ChatColor.STRIKETHROUGH : "") + "waitingPos 1/2" + ChatColor.RESET + " " + posMsg;
            String setSpawn = ss.dot() + ((spawnNotSet.length() == 0) ? ChatColor.STRIKETHROUGH : "") + "setSpawn <teamName>" + ChatColor.RESET + " " + ((spawnNotSet.length() == 0) ? ChatColor.GREEN + "（全部已设置）" : ChatColor.RED + "（剩余：" + spawnNotSet + ChatColor.RED + "）");
            String setBed = ss.dot() + ((bedNotSet.toString().length() == 0) ? ChatColor.STRIKETHROUGH : "") + "setBed" + ChatColor.RESET + " " + ((bedNotSet.length() == 0) ? ChatColor.GREEN + "（全部已设置）" : ChatColor.RED + "（剩余：" + bedNotSet + ChatColor.RED + "）");
            String setShop = ss.dot() + ((shopNotSet.toString().length() == 0) ? ChatColor.STRIKETHROUGH : "") + "setShop" + ChatColor.RESET + " " + ((shopNotSet.length() == 0) ? ChatColor.GREEN + "（全部已设置）" : ChatColor.RED + "（剩余：" + shopNotSet + ChatColor.RED + "）");
            String setKillDrops = ss.dot() + ((killDropsNotSet.toString().length() == 0) ? ChatColor.STRIKETHROUGH : "") + "setKillDrops" + ChatColor.RESET + " " + ((killDropsNotSet.length() == 0) ? ChatColor.GREEN + "（全部已设置）" : ChatColor.RED + "（剩余：" + killDropsNotSet + ChatColor.RED + "）");
            String setUpgrade = ss.dot() + ((upgradeNotSet.toString().length() == 0) ? ChatColor.STRIKETHROUGH : "") + "setUpgrade" + ChatColor.RESET + " " + ((upgradeNotSet.length() == 0) ? ChatColor.GREEN + "（全部已设置）" : ChatColor.RED + "（剩余：" + upgradeNotSet + ChatColor.RED + "）");
            String addGenerator = ss.dot() + "addGenerator " + ((generatorNotSet.toString().length() == 0) ? "" : ChatColor.RED + "（剩余：" + generatorNotSet + ChatColor.RED + "） ") + ChatColor.YELLOW + "(" + ChatColor.DARK_GREEN + "E" + emGen + " " + ChatColor.AQUA + "D" + dmGen + ChatColor.YELLOW + ")";
            String setSpectatorSpawn = ss.dot() + (ss.getConfig().getYml().get(ConfigPath.ARENA_SPEC_LOC) == null ? "" : ChatColor.STRIKETHROUGH) + "setSpectSpawn" + ChatColor.RESET + " " + (ss.getConfig().getYml().get(ConfigPath.ARENA_SPEC_LOC) == null ? ChatColor.RED + "（未设置）" : ChatColor.GRAY + "（已设置）");

            s.sendMessage("");
            s.sendMessage(ChatColor.GRAY + "" + ChatColor.BOLD + com.andrei1058.bedwars.commands.bedwars.MainCommand.getDot() + ChatColor.GOLD + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion() + ChatColor.GRAY + '-' + " " + ChatColor.GREEN + ss.getWorldName() + " 设置命令");
            p.spigot().sendMessage(Misc.msgHoverClick(setWaitingSpawn, ChatColor.WHITE + "设置玩家在游戏开始前\n" + ChatColor.WHITE + "等待的位置。", "/" + getParent().getName() + " setWaitingSpawn", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(waitingPos, ChatColor.WHITE + "设置游戏开始时消失的等待大厅区域。\n" + ChatColor.WHITE + "请按 WorldEdit 选区方式选择。", "/" + getParent().getName() + " waitingPos ", ClickEvent.Action.SUGGEST_COMMAND));
            if (ss.getSetupType() == SetupType.ADVANCED) {
                p.spigot().sendMessage(Misc.msgHoverClick(setSpectatorSpawn, ChatColor.WHITE + "设置观战者出生点。", "/" + getParent().getName() + " setSpectSpawn", ClickEvent.Action.RUN_COMMAND));
            }
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "autoCreateTeams " + ChatColor.YELLOW + "（自动检测）", ChatColor.WHITE + "根据岛屿颜色自动创建队伍。", "/" + getParent().getName() + " autoCreateTeams", ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "createTeam <name> <color> " + ChatColor.YELLOW + "（已创建 " + teams + " 个）", ChatColor.WHITE + "创建一个队伍。", "/" + getParent().getName() + " createTeam ", ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "listTeams", ChatColor.WHITE + "列出当前地图的所有队伍。", "/" + mainCmd + " listTeams", ClickEvent.Action.RUN_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "removeTeam <name>", ChatColor.WHITE + "按名称删除队伍。", "/" + mainCmd + " removeTeam ", ClickEvent.Action.SUGGEST_COMMAND));


            p.spigot().sendMessage(Misc.msgHoverClick(setSpawn, ChatColor.WHITE + "设置队伍出生点。\n" + ChatColor.WHITE + "尚未设置出生点的队伍：\n" + spawnNotSetNames.toString(), "/" + getParent().getName() + " setSpawn ", ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(setBed, ChatColor.WHITE + "设置队伍床的位置。\n" + ChatColor.WHITE + "无需指定队伍名称。", "/" + getParent().getName() + " setBed", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(setShop, ChatColor.WHITE + "设置队伍商店 NPC。\n" + ChatColor.WHITE + "无需指定队伍名称。\n" + ChatColor.WHITE + "NPC 只会在游戏开始时生成。", "/" + getParent().getName() + " setShop", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(setUpgrade, ChatColor.WHITE + "设置队伍升级 NPC。\n" + ChatColor.WHITE + "无需指定队伍名称。\n" + ChatColor.WHITE + "NPC 只会在游戏开始时生成。", "/" + getParent().getName() + " setUpgrade", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            if (ss.getSetupType() == SetupType.ADVANCED) {
                p.spigot().sendMessage(Misc.msgHoverClick(setKillDrops, ChatColor.WHITE + "设置击杀敌人后\n" + ChatColor.WHITE + "掉落其物品的位置。", "/" + getParent().getName() + " setKillDrops ", ClickEvent.Action.SUGGEST_COMMAND));
            }
            String genHover = (ss.getSetupType() == SetupType.ADVANCED ? ChatColor.WHITE + "添加资源生成点。\n" + ChatColor.YELLOW + "/" + getParent().getName() + " addGenerator <Iron/Gold/Emerald/Diamond>" :
                    ChatColor.WHITE + "添加资源生成点。\n" + ChatColor.YELLOW + "站在队伍岛屿上设置队伍生成点") + "\n" + ChatColor.WHITE + "站在钻石块上设置钻石生成点。\n" + ChatColor.WHITE + "站在绿宝石块上设置绿宝石生成点。";

            p.spigot().sendMessage(Misc.msgHoverClick(addGenerator, genHover, "/" + getParent().getName() + " addGenerator ", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "removeGenerator", genHover, "/" + getParent().getName() + " removeGenerator", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));

            if (ss.getSetupType() == SetupType.ADVANCED) {
                p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "arenaGroup " + group, ChatColor.WHITE + "设置竞技场分组。", "/" + mainCmd + " arenaGroup ", ClickEvent.Action.SUGGEST_COMMAND));
            } else {
                p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "setType <type> " + group, ChatColor.WHITE + "设置竞技场分组。", "/" + getParent().getName() + " setType", ClickEvent.Action.RUN_COMMAND));
            }
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "setMaxInTeam <int>（当前每队 " + maxInTeam + " 人）",
                    ChatColor.WHITE + "设置每队容量；设置时会把开局下限同步为容量。",
                    "/" + mainCmd + " setMaxInTeam ", ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "setMinInTeam <int>（当前最少 " + minInTeam + " 人）",
                    ChatColor.WHITE + "单独设置每支启用队伍的开局下限；至少两支队伍达标即可开始。",
                    "/" + mainCmd + " setMinInTeam ", ClickEvent.Action.SUGGEST_COMMAND));

            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "save", ChatColor.WHITE + "保存竞技场并返回大厅", "/" + getParent().getName() + " save", ClickEvent.Action.SUGGEST_COMMAND));
        } else {
            TextComponent credits = new TextComponent(ChatColor.BLUE + "" + ChatColor.BOLD + com.andrei1058.bedwars.commands.bedwars.MainCommand.getDot() + " " + ChatColor.GOLD + plugin.getName() + " " + ChatColor.GRAY + "v" + plugin.getDescription().getVersion() + "，作者 andrei1058");
            credits.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
            credits.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GRAY + "竞技场数量：" + (Arena.getArenas().size() == 0 ? ChatColor.RED + "0" : ChatColor.GREEN + "" + Arena.getArenas().size())).create()));
            ((Player) s).spigot().sendMessage(credits);
            for (String string : getList((Player) s, Messages.COMMAND_MAIN)) {
                s.sendMessage(string);
            }
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }

    @Override
    public boolean canSee(CommandSender s, BedWars api) {

        if (s instanceof Player) {
            Player p = (Player) s;
            if (Arena.isInArena(p)) return false;

            if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        }

        return hasPermission(s);
    }
}
