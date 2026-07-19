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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.PreGameSquad;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.arena.team.PreGameSquadManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CmdTeam extends SubCommand {

    private static final String PREFIX = ChatColor.GOLD + "[组队] " + ChatColor.RESET;
    private final PreGameSquadManager squads = PreGameSquadManager.getInstance();

    public CmdTeam(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(12);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (!(sender instanceof Player player)) return false;
        IArena arena = preGameArena(player);
        if (arena == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "只能在开局前使用竞技场组队功能。");
            return true;
        }
        if (arena.getMaxInTeam() <= 1) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "当前地图为单人队模式，无需邀请队友。");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            showSquad(player, arena);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "invite" -> invite(player, args);
            case "accept" -> accept(player, args);
            case "decline", "reject" -> decline(player, args);
            case "leave" -> leave(player);
            case "help" -> showHelp(player);
            default -> showHelp(player);
        }
        return true;
    }

    private void invite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "用法：/bw team invite <玩家>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "找不到在线玩家：" + args[1]);
            return;
        }

        PreGameSquad.Result result = squads.invite(player, target);
        if (result != PreGameSquad.Result.SUCCESS) {
            sendFailure(player, result);
            return;
        }
        player.sendMessage(PREFIX + ChatColor.GREEN + "已邀请 " + target.getName() + "，邀请 30 秒内有效。");

        TextComponent message = new TextComponent(PREFIX + ChatColor.AQUA + player.getName()
                + ChatColor.YELLOW + " 邀请你在本局成为队友。 ");
        TextComponent accept = new TextComponent(ChatColor.GREEN + "[接受]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw team accept " + player.getName()));
        TextComponent decline = new TextComponent(ChatColor.RED + " [拒绝]");
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw team decline " + player.getName()));
        message.addExtra(accept);
        message.addExtra(decline);
        target.spigot().sendMessage(message);
    }

    private void accept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "用法：/bw team accept <邀请者>");
            return;
        }
        Player inviter = Bukkit.getPlayerExact(args[1]);
        if (inviter == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "邀请者已离线或离开竞技场。");
            return;
        }
        PreGameSquad.Result result = squads.accept(player, inviter);
        if (result != PreGameSquad.Result.SUCCESS) {
            sendFailure(player, result);
            return;
        }
        for (Player member : squads.getMembers(player)) {
            member.sendMessage(PREFIX + ChatColor.GREEN + player.getName() + " 已加入开局队伍。");
        }
    }

    private void decline(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "用法：/bw team decline <邀请者>");
            return;
        }
        Player inviter = Bukkit.getPlayerExact(args[1]);
        if (inviter == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "邀请者已离线或离开竞技场。");
            return;
        }
        PreGameSquad.Result result = squads.decline(player, inviter);
        if (result != PreGameSquad.Result.SUCCESS) {
            sendFailure(player, result);
            return;
        }
        player.sendMessage(PREFIX + ChatColor.YELLOW + "已拒绝 " + inviter.getName() + " 的邀请。");
        inviter.sendMessage(PREFIX + ChatColor.YELLOW + player.getName() + " 拒绝了你的邀请。");
    }

    private void leave(Player player) {
        List<Player> previousMembers = squads.getMembers(player);
        PreGameSquad.Result result = squads.leave(player);
        if (result != PreGameSquad.Result.SUCCESS) {
            sendFailure(player, result);
            return;
        }
        player.sendMessage(PREFIX + ChatColor.YELLOW + "你已退出开局队伍，将作为单人队参与分配。");
        previousMembers.stream().filter(member -> !member.equals(player)).forEach(member ->
                member.sendMessage(PREFIX + ChatColor.YELLOW + player.getName() + " 已退出开局队伍。"));
    }

    private void showSquad(Player player, IArena arena) {
        List<Player> members = squads.getMembers(player);
        Player leader = squads.getLeader(player);
        player.sendMessage(PREFIX + ChatColor.YELLOW + "当前开局队伍（" + members.size() + "/"
                + arena.getMaxInTeam() + "）：");
        for (Player member : members) {
            String leaderMark = leader != null && leader.equals(member) ? ChatColor.GOLD + " [队长]" : "";
            player.sendMessage(ChatColor.GRAY + " - " + ChatColor.AQUA + member.getName() + leaderMark);
        }

        if (!squads.isLeader(player) || members.size() >= arena.getMaxInTeam()) return;
        List<Player> available = squads.getAvailableTargets(player);
        if (available.isEmpty()) {
            player.sendMessage(PREFIX + ChatColor.GRAY + "当前没有可邀请的玩家。");
            return;
        }
        player.sendMessage(PREFIX + ChatColor.YELLOW + "点击玩家发送邀请：");
        for (Player target : available) {
            TextComponent invite = new TextComponent(ChatColor.GRAY + " - " + ChatColor.GREEN + target.getName());
            invite.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/bw team invite " + target.getName()));
            player.spigot().sendMessage(invite);
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(PREFIX + ChatColor.YELLOW + "开局组队命令：");
        player.sendMessage(ChatColor.GRAY + "/bw team list " + ChatColor.WHITE + "查看队伍和可邀请玩家");
        player.sendMessage(ChatColor.GRAY + "/bw team invite <玩家> " + ChatColor.WHITE + "邀请队友");
        player.sendMessage(ChatColor.GRAY + "/bw team accept <玩家> " + ChatColor.WHITE + "接受邀请");
        player.sendMessage(ChatColor.GRAY + "/bw team decline <玩家> " + ChatColor.WHITE + "拒绝邀请");
        player.sendMessage(ChatColor.GRAY + "/bw team leave " + ChatColor.WHITE + "退出队伍");
    }

    private void sendFailure(Player player, PreGameSquad.Result result) {
        String message = switch (result) {
            case NOT_IN_PRE_GAME -> "只能在开局前组队。";
            case DIFFERENT_ARENA -> "双方必须在同一个等待中的竞技场。";
            case CANNOT_INVITE_SELF -> "不能邀请自己。";
            case NOT_LEADER -> "只有队长可以邀请玩家。";
            case TARGET_ALREADY_GROUPED -> "该玩家已经加入其他开局队伍。";
            case SQUAD_FULL -> "你的队伍人数已达到本地图上限。";
            case ALREADY_INVITED -> "你已经邀请过该玩家，请等待其处理。";
            case NO_INVITE -> "没有找到对应的有效邀请。";
            case INVITE_EXPIRED -> "邀请已过期，请重新发送。";
            case NOT_IN_SQUAD -> "你当前是单人队，无需退出。";
            case SUCCESS -> "";
        };
        player.sendMessage(PREFIX + ChatColor.RED + message);
    }

    private IArena preGameArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.isPlayer(player)) return null;
        return arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting ? arena : null;
    }

    @Override
    public List<String> getTabComplete() {
        return List.of("list", "invite", "accept", "decline", "leave", "help");
    }

    @Override
    public boolean canSee(CommandSender sender, BedWars api) {
        if (sender instanceof ConsoleCommandSender || !(sender instanceof Player player)) return false;
        if (SetupSession.isInSetupSession(player.getUniqueId())) return false;
        return preGameArena(player) != null && hasPermission(sender);
    }
}
