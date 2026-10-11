package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.bedwars.subcmds.regular.PlayerListFormatter;
import com.andrei1058.bedwars.language.MatchmakingMessages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.List;

/** Replaces the single-line vanilla /list output with lobby and arena groups. */
public final class PlayerListCommandListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!isListCommand(event.getMessage())) return;
        event.setCancelled(true);
        sendList(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (!isListCommand(event.getCommand())) return;
        event.setCancelled(true);
        sendList(event.getSender());
    }

    static boolean isListCommand(String input) {
        if (input == null) return false;
        String command = input.trim();
        if (command.startsWith("/")) command = command.substring(1);
        int space = command.indexOf(' ');
        if (space >= 0) command = command.substring(0, space);
        return command.equalsIgnoreCase("list") || command.equalsIgnoreCase("minecraft:list");
    }

    private static void sendList(CommandSender sender) {
        List<Player> onlinePlayers = List.copyOf(Bukkit.getOnlinePlayers());
        List<PlayerListFormatter.Group> groups = PlayerListFormatter.groupPlayers(onlinePlayers,
                Player::getName,
                player -> {
                    IArena arena = Arena.getArenaByPlayer(player);
                    return arena == null ? null : arena.getArenaName();
                });
        MatchmakingMessages.Snapshot messages = MatchmakingMessages.forSender(sender);
        for (String line : PlayerListFormatter.format(groups, onlinePlayers.size(), Bukkit.getMaxPlayers(), messages)) {
            AdventureText.send(sender, line);
        }
    }
}
