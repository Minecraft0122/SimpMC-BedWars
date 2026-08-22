package com.andrei1058.bedwars.sidebar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a coloured fallback name in the server-owned PlayerInfo entry.
 *
 * <p>Paper clients can rebuild a PlayerInfo row after an ADD_PLAYER/show
 * update, before the viewer-specific packet replay runs. The explicit packet
 * remains the source of prefixes and suffixes; this state only guarantees the
 * team colour survives that vanilla rebuild.</p>
 */
final class TabColorFallback {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private TabColorFallback() {
    }

    static synchronized void claim(@NotNull UUID ownerId, @NotNull Player player,
                                    @Nullable ChatColor color) {
        Entry entry = ACTIVE.get(player.getUniqueId());
        if (entry == null) {
            entry = new Entry(player, player.playerListName());
            ACTIVE.put(player.getUniqueId(), entry);
        } else if (entry.player != player) {
            // A reconnect can produce a new Player wrapper for the same UUID
            // while an old viewer ownership is still being released.
            if (entry.applied != null && entry.applied.equals(entry.player.playerListName())) {
                entry.player.playerListName(entry.original);
            }
            entry.player = player;
            entry.original = player.playerListName();
            entry.applied = null;
        }
        entry.owners.add(ownerId);
        if (color == null) return;

        Component desired = LEGACY.deserialize(color + player.getName());
        if (!desired.equals(player.playerListName())) {
            player.playerListName(desired);
        }
        entry.applied = desired;
    }

    static synchronized void release(@NotNull UUID ownerId, @NotNull Player player) {
        Entry entry = ACTIVE.get(player.getUniqueId());
        if (entry == null || !entry.owners.remove(ownerId) || !entry.owners.isEmpty()) return;

        ACTIVE.remove(player.getUniqueId());
        if (entry.applied != null && entry.applied.equals(player.playerListName())) {
            player.playerListName(entry.original);
        }
    }

    static synchronized void releaseOwner(@NotNull UUID ownerId) {
        for (Entry entry : new HashSet<>(ACTIVE.values())) {
            if (!entry.owners.remove(ownerId) || !entry.owners.isEmpty()) continue;
            ACTIVE.remove(entry.player.getUniqueId());
            if (entry.applied != null && entry.applied.equals(entry.player.playerListName())) {
                entry.player.playerListName(entry.original);
            }
        }
    }

    static synchronized void clear() {
        for (Entry entry : new HashSet<>(ACTIVE.values())) {
            if (entry.applied != null && entry.applied.equals(entry.player.playerListName())) {
                entry.player.playerListName(entry.original);
            }
        }
        ACTIVE.clear();
    }

    private static final class Entry {
        private Player player;
        private Component original;
        private final Set<UUID> owners = new HashSet<>();
        private Component applied;

        private Entry(@NotNull Player player, @Nullable Component original) {
            this.player = player;
            this.original = original;
        }
    }
}
