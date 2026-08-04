package com.andrei1058.spigot.sidebar;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Controls the nullable PlayerInfo display name for one viewer. Managed rows
 * keep that field clear so Minecraft renders the scoreboard team's prefix,
 * suffix and color, exactly like BedWars1058/SidebarLib.
 */
interface PlayerListDisplayNameRenderer {

    boolean clear(@NotNull Player viewer, @NotNull Collection<Player> targets);

    /** Advertise spectator mode for these rows without changing Bukkit game mode. */
    boolean setSpectatorMode(@NotNull Player viewer, @NotNull Collection<Player> targets);

    /** Restore the targets' current server-side game mode for this viewer. */
    boolean restoreGameMode(@NotNull Player viewer, @NotNull Collection<Player> targets);

    /** Restore both nullable display names and real game modes. */
    boolean restore(@NotNull Player viewer, @NotNull Collection<Player> targets);
}
