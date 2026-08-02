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

    boolean restore(@NotNull Player viewer, @NotNull Collection<Player> targets);
}
