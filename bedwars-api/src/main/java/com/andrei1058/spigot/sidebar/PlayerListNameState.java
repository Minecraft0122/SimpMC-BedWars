package com.andrei1058.spigot.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns an explicitly rendered player-list name while one or more sidebars use it. */
final class PlayerListNameState {

    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private PlayerListNameState() {
    }

    static synchronized void acquire(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current != null) {
            current.references++;
            return;
        }

        Component original = player.playerListName();
        ACTIVE.put(player.getUniqueId(), new Entry(player, original));
    }

    static synchronized void apply(@NotNull Player player, @NotNull Component renderedName) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current == null) {
            return;
        }
        if (!renderedName.equals(player.playerListName())) {
            player.playerListName(renderedName);
        }
        current.applied = renderedName;
    }

    static synchronized void release(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current == null || --current.references > 0) {
            return;
        }

        ACTIVE.remove(player.getUniqueId());
        // Do not overwrite a list name installed by another plugin while the
        // BedWars sidebar was active.
        if (current.applied != null && current.applied.equals(current.player.playerListName())) {
            current.player.playerListName(current.original);
        }
    }

    private static final class Entry {
        private final Player player;
        private final Component original;
        private Component applied;
        private int references = 1;

        private Entry(Player player, Component original) {
            this.player = player;
            this.original = original;
        }
    }
}
