package com.andrei1058.spigot.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Temporarily clears custom player-list names while scoreboard teams format a
 * player. Minecraft does not apply scoreboard team colors to a non-null custom
 * player-list component, so leaving that component in place makes the tab name
 * stay white even though the same scoreboard team colors the overhead name.
 */
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
        if (original != null) {
            player.playerListName(null);
        }
    }

    static synchronized void release(@NotNull Player player) {
        Entry current = ACTIVE.get(player.getUniqueId());
        if (current == null || --current.references > 0) {
            return;
        }

        ACTIVE.remove(player.getUniqueId());
        // Do not overwrite a list name installed by another plugin while the
        // BedWars sidebar was active.
        if (current.player.playerListName() == null && current.original != null) {
            current.player.playerListName(current.original);
        }
    }

    private static final class Entry {
        private final Player player;
        private final Component original;
        private int references = 1;

        private Entry(Player player, Component original) {
            this.player = player;
            this.original = original;
        }
    }
}
