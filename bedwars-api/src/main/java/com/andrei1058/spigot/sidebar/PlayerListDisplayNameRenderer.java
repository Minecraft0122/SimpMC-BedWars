package com.andrei1058.spigot.sidebar;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Writes the display-name field of one player-list entry for one viewer.
 */
interface PlayerListDisplayNameRenderer {

    boolean render(@NotNull Player viewer, @NotNull Collection<RenderedName> names);

    boolean restore(@NotNull Player viewer, @NotNull Collection<Player> targets);

    record RenderedName(@NotNull Player target, @NotNull String legacyDisplayName) {
    }
}
