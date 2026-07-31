package com.andrei1058.spigot.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Temporarily clears a custom player-list component while scoreboard teams
 * render a player. Paper returns the effective profile-name component when the
 * internal custom value is {@code null}, so this class must never use a null
 * getter result to determine whether the custom value is clear.
 */
final class PlayerListNameState {

    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private PlayerListNameState() {
    }

    static synchronized void acquire(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current != null && current.player == player) {
            current.references++;
            enforceScoreboardFormatting(player);
            return;
        }

        // CraftPlayer instances are connection-scoped. Stale viewer tabs from
        // a previous connection may still hold the same UUID, but their
        // reference count must not own the new connection's list-name state.
        Component effectiveName = player.playerListName();
        Component defaultName = defaultName(player);
        Entry entry = new Entry(player, effectiveName.equals(defaultName) ? null : effectiveName);
        ACTIVE.put(player.getUniqueId(), entry);
        // The getter cannot reveal Paper's nullable internal value. Clear once
        // unconditionally when taking ownership, then avoid duplicate global
        // UPDATE_DISPLAY_NAME packets while the default effective name remains.
        player.playerListName(null);
    }

    static synchronized void enforceScoreboardFormatting(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current == null || current.player != player) {
            return;
        }

        Component effectiveName = player.playerListName();
        if (effectiveName.equals(defaultName(player))) {
            return;
        }

        // Preserve the latest value supplied by another plugin, but keep the
        // list-name field clear while BedWars' per-viewer teams are active.
        current.restoreName = effectiveName;
        player.playerListName(null);
    }

    static synchronized void release(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current == null || current.player != player || --current.references > 0) {
            return;
        }

        ACTIVE.remove(player.getUniqueId());
        // Do not overwrite a list name installed by another plugin while the
        // BedWars sidebar was active.
        if (current.restoreName != null
                && current.player.playerListName().equals(defaultName(current.player))) {
            current.player.playerListName(current.restoreName);
        }
    }

    private static @NotNull Component defaultName(@NotNull Player player) {
        return Component.text(player.getName());
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
