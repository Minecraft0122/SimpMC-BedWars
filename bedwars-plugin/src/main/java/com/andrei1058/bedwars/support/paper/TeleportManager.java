package com.andrei1058.bedwars.support.paper;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.CompletableFuture;

import static com.andrei1058.bedwars.BedWars.config;

public final class TeleportManager {

    public static CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        return teleportC(entity, location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public static CompletableFuture<Boolean> teleportC(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_PAPER_FEATURES)) {
            return entity.teleportAsync(location, cause);
        }
        return CompletableFuture.completedFuture(entity.teleport(location, cause));
    }

}
