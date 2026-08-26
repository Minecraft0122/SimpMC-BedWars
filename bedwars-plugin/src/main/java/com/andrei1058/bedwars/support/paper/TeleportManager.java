package com.andrei1058.bedwars.support.paper;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class TeleportManager {

    public static void teleport(Entity entity, Location location) {
        teleportC(entity, location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public static void teleportC(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        entity.teleport(location, cause);
    }

}
