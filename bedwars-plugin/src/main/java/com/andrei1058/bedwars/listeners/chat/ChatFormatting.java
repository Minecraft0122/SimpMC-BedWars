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

package com.andrei1058.bedwars.listeners.chat;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.shout.ShoutFormattingContext;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.support.papi.SupportPAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.getMsg;
import static com.andrei1058.bedwars.api.language.Language.getPlayerLanguage;

public class ChatFormatting implements Listener {

    private static final Set<Character> SHOUT_PREFIXES = Set.of('@', '!', '！', '#', '%', '&');

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (e == null) return;
        Player p = e.getPlayer();
        String incomingMessage = e.getMessage();
        boolean shoutPrefix = isShouting(incomingMessage);

        // in shared mode we don't want messages from outside the arena to be seen in game
        if (getServerType() == ServerType.SHARED && Arena.getArenaByPlayer(p) == null) {
            e.getRecipients().removeIf(pl -> Arena.getArenaByPlayer(pl) != null);
            return;
        }

        // handle chat color. we would need to work on permission inheritance
        if (!shoutPrefix && Permissions.hasPermission(p, Permissions.PERMISSION_CHAT_COLOR, Permissions.PERMISSION_VIP, Permissions.PERMISSION_ALL)) {
            e.setMessage(ChatColor.translateAlternateColorCodes('&', e.getMessage()));
        }

        Language language = getPlayerLanguage(p);

        // handle lobby world for multi arena
        if (getServerType() == ServerType.MULTIARENA && p.getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
            setRecipients(e, p.getWorld().getPlayers());
        }

        // handle arena chat
        if (Arena.getArenaByPlayer(p) != null) {
            IArena a = Arena.getArenaByPlayer(p);

            // spectator chat
            if (a.isSpectator(p)) {
                // Keep the existing spectator channel, but never let it fall
                // through to active players when global chat is enabled.
                restrictRecipients(e, a.getSpectators());
                e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_SPECTATOR), p, null));
                return;
            }

            // Prefix-based shout and /hh,/h all use the same public route,
            // including the waiting/starting phase.
            if (shoutPrefix) {
                if (!hasShoutPermission(p)) {
                    e.setCancelled(true);
                    p.sendMessage(language.m(Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
                    return;
                }
                ITeam team = a.getTeam(p);
                setRecipients(e, a.getPlayers());
                excludeArenaSpectators(e);
                String msg = clearShout(incomingMessage);
                if (msg.isEmpty()) {
                    e.setCancelled(true);
                    return;
                }
                if (Permissions.hasPermission(p, Permissions.PERMISSION_CHAT_COLOR, Permissions.PERMISSION_VIP, Permissions.PERMISSION_ALL)) {
                    msg = ChatColor.translateAlternateColorCodes('&', msg);
                }
                e.setMessage(msg);
                e.setFormat(ShoutFormattingContext.format(p,
                        () -> parsePHolders(language.m(Messages.FORMATTING_CHAT_SHOUT), p, team)));
                return;
            }

            // arena lobby chat
            if (a.getStatus() == GameState.waiting || a.getStatus() == GameState.starting) {
                setRecipients(e, a.getPlayers());
                excludeArenaSpectators(e);
                e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_WAITING), p, null));
                return;
            }

            ITeam team = a.getTeam(p);
            String msg = incomingMessage;

            // A team that started alone has nobody to receive private chat.
            // Use public arena chat without requiring /shout.
            if (usesPublicChannel(a.getTeamSizeAtGameStart(team))) {
                setRecipients(e, a.getPlayers());
                excludeArenaSpectators(e);
                e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_SHOUT), p, team));
            } else {
                setRecipients(e, team.getMembers());
                excludeArenaSpectators(e);
                e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_TEAM), p, team));
            }
            return;
        }

        // multi arena lobby chat
        e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_LOBBY), p, null));
    }

    private static String parsePHolders(String content, Player player, @Nullable ITeam team) {
        content = withMessageSeparator(content)
                .replace("{vPrefix}", getChatSupport().getPrefix(player))
                .replace("{vSuffix}", getChatSupport().getSuffix(player))
                .replace("{playername}", player.getName())
                .replace("{level}", getLevelSupport().getLevel(player))
                .replace("{player}", player.getDisplayName());
        if (team != null) {
            String teamFormat = getMsg(player, Messages.FORMAT_PAPI_PLAYER_TEAM_TEAM)
                    .replace("{TeamColor}", team.getColor().chat() + "")
                    .replace("{TeamName}", team.getDisplayName(Language.getPlayerLanguage(player)));
            content = content.replace("{team}", teamFormat);
        }
        return SupportPAPI.getSupportPAPI().replace(player, content).replace("{message}", "%2$s");
    }

    static String withMessageSeparator(String content) {
        int messageIndex = content.indexOf("{message}");
        if (messageIndex < 0) return content;

        String before = content.substring(0, messageIndex).stripTrailing();
        boolean trimming = true;
        while (trimming) {
            before = before.stripTrailing();
            if (before.length() >= 2 && before.charAt(before.length() - 2) == ChatColor.COLOR_CHAR) {
                before = before.substring(0, before.length() - 2);
            } else if (before.endsWith(">>")) {
                before = before.substring(0, before.length() - 2);
            } else if (before.endsWith(">") || before.endsWith(":") || before.endsWith("：")) {
                before = before.substring(0, before.length() - 1);
            } else {
                trimming = false;
            }
        }
        return before.stripTrailing() + ' ' + ChatColor.WHITE + "> " + ChatColor.GRAY + "{message}"
                + content.substring(messageIndex + "{message}".length());
    }

    static boolean isShouting(String msg) {
        return msg != null && !msg.isEmpty() && SHOUT_PREFIXES.contains(msg.charAt(0));
    }

    static boolean hasShoutPermission(CommandSender sender) {
        return Permissions.hasShoutPermission(sender);
    }

    static boolean usesPublicChannel(int teamSizeAtGameStart) {
        return teamSizeAtGameStart <= 1;
    }

    static String clearShout(String msg) {
        return isShouting(msg) ? msg.substring(1).trim() : msg.trim();
    }

    @SafeVarargs
    public static void setRecipients(AsyncPlayerChatEvent event, List<Player>... target) {
        if (!config.getBoolean(ConfigPath.GENERAL_CHAT_GLOBAL)) {
            event.getRecipients().clear();
            for (List<Player> list : target) {
                event.getRecipients().addAll(list);
            }
        }
    }

    private static void excludeArenaSpectators(AsyncPlayerChatEvent event) {
        event.getRecipients().removeIf(recipient -> {
            IArena arena = Arena.getArenaByPlayer(recipient);
            return arena != null && arena.isSpectator(recipient);
        });
    }

    private static void restrictRecipients(AsyncPlayerChatEvent event, List<Player> allowed) {
        event.getRecipients().clear();
        event.getRecipients().addAll(allowed);
    }
}
