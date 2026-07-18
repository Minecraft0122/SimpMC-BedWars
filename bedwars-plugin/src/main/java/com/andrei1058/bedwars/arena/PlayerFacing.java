package com.andrei1058.bedwars.arena;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores player direction separately from centered arena coordinates.
 */
public final class PlayerFacing {

    private PlayerFacing() {
    }

    public static @NotNull String serialize(@NotNull Location location) {
        return serialize(location.getYaw(), location.getPitch());
    }

    public static @NotNull String serialize(float yaw, float pitch) {
        return NpcFacing.normalize(yaw) + "," + clampPitch(pitch);
    }

    public static @Nullable Location apply(@Nullable Location location, @Nullable String configuredFacing) {
        if (location == null) return null;
        if (configuredFacing == null || configuredFacing.isBlank()) return location;

        String[] parts = configuredFacing.replace("[", "").replace("]", "").split(",");
        if (parts.length != 2) return location;
        try {
            float yaw = Float.parseFloat(parts[0].trim());
            float pitch = Float.parseFloat(parts[1].trim());
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) return location;
            location.setYaw(NpcFacing.normalize(yaw));
            location.setPitch(clampPitch(pitch));
        } catch (NumberFormatException ignored) {
            // Keep the location's existing direction when an administrator
            // manually entered an invalid facing value.
        }
        return location;
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }
}
