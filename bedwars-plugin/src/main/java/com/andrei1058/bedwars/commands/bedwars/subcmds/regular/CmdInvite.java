package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.arena.matchmaking.ArenaInviteManager;
import com.andrei1058.bedwars.arena.matchmaking.ArenaInvitePolicy;
import com.andrei1058.bedwars.listeners.LobbyAnnouncements;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Invites a lobby player or another pre-game arena player into the sender's arena. */
public final class CmdInvite extends SubCommand {

    private static final String PREFIX = ChatColor.GOLD + "[BW] " + ChatColor.RESET;
    private final ArenaInviteManager invitations = ArenaInviteManager.getInstance();

    public CmdInvite(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(13);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (!(sender instanceof Player player)) return false;
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "accept" -> accept(player, args);
            case "decline", "reject" -> decline(player, args);
            case "list" -> list(player);
            default -> invite(player, args[0]);
        }
        return true;
    }

    private void invite(Player inviter, String targetName) {
        IArena arena = preGameArena(inviter);
        if (arena == null) {
            fail(inviter, "你必须先加入一个尚未开始的竞技场。");
            return;
        }
        if (!ArenaInvitePolicy.canAcceptPlayer(arena)) {
            fail(inviter, "竞技场即将开始，暂时无法邀请玩家。");
            return;
        }
        if (!ArenaInvitePolicy.hasRoom(arena)) {
            fail(inviter, "当前竞技场已满，无法继续邀请。");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            fail(inviter, "找不到在线玩家：" + targetName);
            return;
        }
        if (target.equals(inviter)) {
            fail(inviter, "不能邀请自己。");
            return;
        }
        IArena targetArena = Arena.getArenaByPlayer(target);
        boolean targetIsLobby = LobbyAnnouncements.isLobbyPlayer(target);
        if (targetArena != null && !targetArena.isPlayer(target)) {
            targetArena = null;
        }
        if (!ArenaInvitePolicy.canInviteTarget(arena, targetArena, targetIsLobby)) {
            fail(inviter, "只能邀请大厅玩家或其他尚未开始竞技场中的玩家。");
            return;
        }

        invitations.create(inviter.getUniqueId(), target.getUniqueId(), arena.getArenaName(),
                System.currentTimeMillis());
        inviter.sendMessage(PREFIX + ChatColor.GREEN + "已邀请 " + target.getName() + " 加入竞技场 "
                + arena.getDisplayName() + "，邀请 30 秒内有效。");

        TextComponent message = new TextComponent(PREFIX + ChatColor.AQUA + inviter.getName()
                + ChatColor.YELLOW + " 邀请你加入竞技场 " + ChatColor.GREEN + arena.getDisplayName() + " ");
        TextComponent accept = action("[接受]", ChatColor.GREEN,
                "/bw invite accept " + inviter.getName(), "点击加入该竞技场");
        TextComponent decline = action(" [拒绝]", ChatColor.RED,
                "/bw invite decline " + inviter.getName(), "点击拒绝邀请");
        message.addExtra(accept);
        message.addExtra(decline);
        target.spigot().sendMessage(message);
    }

    private void accept(Player target, String[] args) {
        if (args.length < 2) {
            fail(target, "用法：/bw invite accept <邀请者>");
            return;
        }
        Player inviter = Bukkit.getPlayerExact(args[1]);
        if (inviter == null) {
            fail(target, "邀请者已离线。");
            return;
        }
        ArenaInviteManager.Invitation invitation = invitations.find(target.getUniqueId(), inviter.getUniqueId(),
                System.currentTimeMillis()).orElse(null);
        if (invitation == null) {
            fail(target, "没有找到有效邀请，邀请可能已经过期。");
            return;
        }
        IArena arena = Arena.getArenaByName(invitation.arenaName());
        IArena targetArena = Arena.getArenaByPlayer(target);
        boolean targetIsLobby = LobbyAnnouncements.isLobbyPlayer(target);
        if (targetArena != null && !targetArena.isPlayer(target)) {
            targetArena = null;
        }
        if (arena == null || Arena.getArenaByPlayer(inviter) != arena || !arena.isPlayer(inviter)
                || !ArenaInvitePolicy.hasRoom(arena)
                || !ArenaInvitePolicy.canAcceptFrom(arena, targetArena, targetIsLobby)) {
            invitations.remove(target.getUniqueId(), inviter.getUniqueId());
            fail(target, "该邀请对应的竞技场已不可加入，可能已经开始或已满员。");
            return;
        }

        boolean joined;
        if (targetArena == null) {
            joined = arena.addPlayer(target, true);
        } else if (targetArena instanceof Arena oldArena && arena instanceof Arena newArena) {
            joined = oldArena.transferPreGamePlayer(target, newArena);
        } else {
            joined = false;
        }
        if (!joined) {
            invitations.remove(target.getUniqueId(), inviter.getUniqueId());
            fail(target, "加入失败，竞技场可能已满、即将开始或不支持跨竞技场转移。");
            return;
        }
        invitations.clearPlayer(target.getUniqueId());
        target.sendMessage(PREFIX + ChatColor.GREEN + "你已接受 " + inviter.getName() + " 的邀请。");
        inviter.sendMessage(PREFIX + ChatColor.GREEN + target.getName() + " 已接受邀请并加入竞技场。");
    }

    private void decline(Player target, String[] args) {
        if (args.length < 2) {
            fail(target, "用法：/bw invite decline <邀请者>");
            return;
        }
        Player inviter = Bukkit.getPlayerExact(args[1]);
        if (inviter == null || invitations.find(target.getUniqueId(), inviter.getUniqueId(),
                System.currentTimeMillis()).isEmpty()) {
            fail(target, "没有找到有效邀请。");
            return;
        }
        invitations.remove(target.getUniqueId(), inviter.getUniqueId());
        target.sendMessage(PREFIX + ChatColor.YELLOW + "已拒绝 " + inviter.getName() + " 的竞技场邀请。");
        inviter.sendMessage(PREFIX + ChatColor.YELLOW + target.getName() + " 拒绝了你的竞技场邀请。");
    }

    private void list(Player target) {
        List<ArenaInviteManager.Invitation> active = invitations.findAll(target.getUniqueId(),
                System.currentTimeMillis());
        if (active.isEmpty()) {
            target.sendMessage(PREFIX + ChatColor.GRAY + "当前没有有效的竞技场邀请。");
            return;
        }
        target.sendMessage(PREFIX + ChatColor.YELLOW + "当前竞技场邀请：");
        for (ArenaInviteManager.Invitation invitation : active) {
            Player inviter = Bukkit.getPlayer(invitation.inviter());
            String inviterName = inviter == null ? invitation.inviter().toString() : inviter.getName();
            target.sendMessage(ChatColor.GRAY + " - " + ChatColor.AQUA + inviterName + ChatColor.GRAY
                    + " → " + ChatColor.GREEN + invitation.arenaName());
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(PREFIX + ChatColor.YELLOW + "/bw invite <玩家> " + ChatColor.WHITE
                + "邀请大厅或其他未开局竞技场玩家加入当前竞技场");
        player.sendMessage(PREFIX + ChatColor.YELLOW + "/bw invite list " + ChatColor.WHITE + "查看收到的邀请");
    }

    private static IArena preGameArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        return arena != null && arena.isPlayer(player) && ArenaInvitePolicy.isPreGame(arena) ? arena : null;
    }

    private static TextComponent action(String label, ChatColor color, String command, String hover) {
        TextComponent component = new TextComponent(color + label);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));
        return component;
    }

    private static void fail(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.RED + message);
    }

    @Override
    public List<String> getTabComplete() {
        return getTabComplete(null);
    }

    @Override
    public List<String> getTabComplete(CommandSender sender) {
        List<String> values = new ArrayList<>(List.of("accept", "decline", "list"));
        IArena inviterArena = sender instanceof Player player ? preGameArena(player) : null;
        if (inviterArena == null) {
            Bukkit.getOnlinePlayers().stream()
                    .filter(player -> LobbyAnnouncements.isLobbyPlayer(player) || preGameArena(player) != null)
                    .map(Player::getName).forEach(values::add);
            return values;
        }
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> inviterArena != null && !player.equals(sender))
                .filter(player -> {
                    IArena targetArena = Arena.getArenaByPlayer(player);
                    if (targetArena != null && !targetArena.isPlayer(player)) targetArena = null;
                    return ArenaInvitePolicy.canInviteTarget(inviterArena, targetArena,
                            LobbyAnnouncements.isLobbyPlayer(player));
                })
                .map(Player::getName).forEach(values::add);
        return values;
    }

    @Override
    public boolean canSee(CommandSender sender, BedWars api) {
        if (sender instanceof ConsoleCommandSender || !(sender instanceof Player player)) return false;
        if (SetupSession.isInSetupSession(player.getUniqueId())) return false;
        return (LobbyAnnouncements.isLobbyPlayer(player) || preGameArena(player) != null) && hasPermission(sender);
    }
}
