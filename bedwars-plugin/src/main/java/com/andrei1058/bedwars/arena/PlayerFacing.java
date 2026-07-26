package com.andrei1058.bedwars.arena;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores cardinal, flat player direction separately from centered arena coordinates.
 */
public final class PlayerFacing {

    private PlayerFacing() {
    }

    public static @NotNull String serialize(@NotNull Location location) {
        return serialize(location.getYaw(), location.getPitch());
    }

    public static @NotNull String serialize(float yaw, float pitch) {
        return NpcFacing.normalize(yaw) + ",0.0";
    }

    public static @Nullable Location apply(@Nullable Location location, @Nullable String configuredFacing) {
        if (location == null) return null;
        location.setYaw(NpcFacing.normalize(location.getYaw()));
        location.setPitch(0.0F);
        if (configuredFacing == null || configuredFacing.isBlank()) return location;

        String[] parts = configuredFacing.replace("[", "").replace("]", "").split(",");
        if (parts.length == 0) return location;
        try {
            float yaw = Float.parseFloat(parts[0].trim());
            if (!Float.isFinite(yaw)) return location;
            location.setYaw(NpcFacing.normalize(yaw));
        } catch (NumberFormatException ignored) {
            // Keep the location's existing direction when an administrator
            // manually entered an invalid facing value.
        }
        return location;
    }
}
