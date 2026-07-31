package com.andrei1058.spigot.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Temporarily clears a custom player-list component while scoreboard teams
 * render a player. A non-null component bypasses the viewer's scoreboard team
 * formatting in the client, so it must stay null for TAB and name-tag colors
 * to use the same team metadata.
 */
final class PlayerListNameState {

    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private PlayerListNameState() {
    }

    static synchronized void acquire(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current != null) {
            current.references++;
            enforceScoreboardFormatting(player);
            return;
        }

        Entry entry = new Entry(player, player.playerListName());
        ACTIVE.put(player.getUniqueId(), entry);
        clearCustomName(entry);
    }

    static synchronized void enforceScoreboardFormatting(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current == null) {
            return;
        }
        clearCustomName(current);
    }

    static synchronized void release(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current == null || --current.references > 0) {
            return;
        }

        ACTIVE.remove(player.getUniqueId());
        // Do not overwrite a list name installed by another plugin while the
        // BedWars sidebar was active.
        if (current.player.playerListName() == null && current.restoreName != null) {
            current.player.playerListName(current.restoreName);
        }
    }

    private static void clearCustomName(@NotNull Entry entry) {
        Component customName = entry.player.playerListName();
        if (customName == null) {
            return;
        }

        // Remember the latest external value, not only the value present when
        // the player entered BedWars, so removal never restores stale data.
        entry.restoreName = customName;
        entry.player.playerListName(null);
    }

    private static final class Entry {
        private final Player player;
        private Component restoreName;
        private int references = 1;

        private Entry(Player player, Component restoreName) {
            this.player = player;
            this.restoreName = restoreName;
        }
    }
}
