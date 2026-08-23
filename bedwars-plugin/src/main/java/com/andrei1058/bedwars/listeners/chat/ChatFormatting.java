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
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.shout.ShoutFormattingContext;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.support.papi.SupportPAPI;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.getMsg;
import static com.andrei1058.bedwars.api.language.Language.getPlayerLanguage;

public class ChatFormatting implements Listener {

    private static final Set<Character> SHOUT_PREFIXES = Set.of('@', '!', '！', '#', '$', '%', '&', '*');

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (e == null) return;
        Player p = e.getPlayer();
        // Paper's AsyncChatEvent viewers contain players only. Keep the
        // server console in every routed chat so private team/arena messages
        // remain available for moderation and diagnostics.
        e.viewers().add(Bukkit.getConsoleSender());

        // in shared mode we don't want messages from outside the arena to be seen in game
        if (getServerType() == ServerType.SHARED && Arena.getArenaByPlayer(p) == null) {
            e.viewers().removeIf(viewer -> viewer instanceof Player pl && Arena.getArenaByPlayer(pl) != null);
            return;
        }

        // handle chat color. we would need to work on permission inheritance
        boolean canUseLegacyColors = Permissions.hasPermission(p, Permissions.PERMISSION_CHAT_COLOR,
                Permissions.PERMISSION_VIP, Permissions.PERMISSION_ALL);
        if (canUseLegacyColors) {
            e.message(deserializeLegacy(AdventureText.section(e.message())));
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
                setRecipients(e, a.getSpectators());
                setRenderer(e, parsePHolders(language.m(Messages.FORMATTING_CHAT_SPECTATOR), p, null));
                return;
            }

            // arena lobby chat
            if (a.getStatus() == GameState.waiting || a.getStatus() == GameState.starting) {
                setRecipients(e, a.getPlayers());
                setRenderer(e, parsePHolders(language.m(Messages.FORMATTING_CHAT_WAITING), p, null));
                return;
            }

            ITeam team = a.getTeam(p);
            // Keep a legacy representation while checking the shout prefix so any
            // formatting in the message body survives the prefix removal.
            String msg = AdventureText.section(e.message());

            // shout format
            if (isShouting(msg, language)) {
                if (!hasShoutPermission(p)) {
                    e.setCancelled(true);
                    AdventureText.send(p, language.m(Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
                    return;
                }
                setRecipients(e, a.getPlayers(), a.getSpectators());
                msg = clearShout(msg, language);
                if (msg.isEmpty()) {
                    e.setCancelled(true);
                    return;
                }
                e.message(canUseLegacyColors ? deserializeLegacy(msg) : AdventureText.section(msg));
                setRenderer(e, ShoutFormattingContext.format(p,
                        () -> parsePHolders(language.m(Messages.FORMATTING_CHAT_SHOUT), p, team)));
                return;
            }

            // A team that started alone has nobody to receive private chat.
            // Use public arena chat without requiring /shout.
            if (usesPublicChannel(a.getTeamSizeAtGameStart(team))) {
                setRecipients(e, a.getPlayers(), a.getSpectators());
                // A one-player team has no private audience, but this is not a
                // shout. Use the normal public format so it does not display
                // the [公屏] marker or a synthetic team label.
                setRenderer(e, parsePHolders(language.m(Messages.FORMATTING_CHAT_LOBBY), p, null));
            } else {
                setRecipients(e, team.getMembers());
                setRenderer(e, parsePHolders(language.m(Messages.FORMATTING_CHAT_TEAM), p, team));
            }
            return;
        }

        // multi arena lobby chat
        setRenderer(e, parsePHolders(language.m(Messages.FORMATTING_CHAT_LOBBY), p, null));
    }

    private static String parsePHolders(String content, Player player, @Nullable ITeam team) {
        content = withMessageSeparator(content)
                .replace("{vPrefix}", getChatSupport().getPrefix(player))
                .replace("{vSuffix}", getChatSupport().getSuffix(player))
                .replace("{playername}", player.getName())
                .replace("{level}", getLevelSupport().getLevel(player))
                .replace("{player}", AdventureText.displayName(player));
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
            if (before.length() >= 2 && before.charAt(before.length() - 2) == '\u00a7') {
                before = before.substring(0, before.length() - 2);
            } else if (before.endsWith(">>")) {
                before = before.substring(0, before.length() - 2);
            } else if (before.endsWith(">") || before.endsWith(":") || before.endsWith("：")) {
                before = before.substring(0, before.length() - 1);
            } else {
                trimming = false;
            }
        }
        return before.stripTrailing() + " \u00a7f> \u00a77{message}"
                + content.substring(messageIndex + "{message}".length());
    }

    private static boolean isShouting(String msg, Language lang) {
        return isShouting(msg) || msg.startsWith("shout") ||
                msg.startsWith("SHOUT") || msg.startsWith(lang.m(Messages.MEANING_SHOUT));
    }

    /** Compatibility helper used by command/tests for the fixed public prefixes. */
    static boolean isShouting(String msg) {
        return msg != null && !msg.isEmpty() && SHOUT_PREFIXES.contains(msg.charAt(0));
    }

    static boolean hasShoutPermission(CommandSender sender) {
        return Permissions.hasShoutPermission(sender);
    }

    static boolean usesPublicChannel(int teamSizeAtGameStart) {
        return teamSizeAtGameStart <= 1;
    }

    private static String clearShout(String msg, Language lang) {
        if (isShouting(msg)) return clearShout(msg);
        if (msg.startsWith("!")) msg = msg.replaceFirst("!", "");
        if (msg.startsWith("SHOUT")) msg = msg.replaceFirst("SHOUT", "");
        if (msg.startsWith("shout")) msg = msg.replaceFirst("shout", "");
        if (msg.startsWith(lang.m(Messages.MEANING_SHOUT))) {
            msg = msg.replaceFirst(lang.m(Messages.MEANING_SHOUT), "");
        }
        return msg.trim();
    }

    /** Remove a fixed public shout prefix while preserving the existing trim semantics. */
    static String clearShout(String msg) {
        return isShouting(msg) ? msg.substring(1).trim() : msg.trim();
    }

    @SafeVarargs
    public static void setRecipients(AsyncChatEvent event, List<Player>... target) {
        if (!config.getBoolean(ConfigPath.GENERAL_CHAT_GLOBAL)) {
            event.viewers().clear();
            for (List<Player> list : target) {
                event.viewers().addAll(list);
            }
        }
        event.viewers().add(Bukkit.getConsoleSender());
    }

    private static void setRenderer(AsyncChatEvent event, String format) {
        // The renderer runs once per viewer, preserving the existing recipient split.
        event.renderer((source, sourceDisplayName, message, viewer) -> render(format, sourceDisplayName, message));
    }

    private static Component render(String format, Component sourceDisplayName, Component message) {
        if (format == null || format.isEmpty()) {
            return message;
        }
        Component result = Component.empty();
        int cursor = 0;
        while (cursor < format.length()) {
            int messageMarker = format.indexOf("%2$s", cursor);
            int nameMarker = format.indexOf("%1$s", cursor);
            int marker;
            Component replacement;
            int markerLength;
            if (messageMarker < 0 && nameMarker < 0) {
                break;
            } else if (nameMarker >= 0 && (messageMarker < 0 || nameMarker < messageMarker)) {
                marker = nameMarker;
                markerLength = 4;
                replacement = sourceDisplayName;
            } else {
                marker = messageMarker;
                markerLength = 4;
                replacement = message;
            }
            result = result.append(deserializeLegacy(format.substring(cursor, marker))).append(replacement);
            cursor = marker + markerLength;
        }
        return result.append(deserializeLegacy(format.substring(cursor)));
    }

    /** Parse both section and ampersand legacy codes at the chat boundary. */
    private static Component deserializeLegacy(String text) {
        return AdventureText.ampersand((text == null ? "" : text).replace('\u00a7', '&'));
    }
}
