package com.andrei1058.bedwars.listeners;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Fireball;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/** Removes one BedWars fireball after it has travelled its sampled flight distance. */
final class FireballFlightTracker {

    private final Fireball fireball;
    private final World world;
    private final Location locationBuffer;
    private final double maximumDistance;
    private double previousX;
    private double previousY;
    private double previousZ;
    private double travelledDistance;

    private FireballFlightTracker(Fireball fireball, double maximumDistance) {
        this.fireball = fireball;
        this.maximumDistance = maximumDistance;
        this.locationBuffer = fireball.getLocation();
        this.world = locationBuffer.getWorld();
        this.previousX = locationBuffer.getX();
        this.previousY = locationBuffer.getY();
        this.previousZ = locationBuffer.getZ();
    }

    static void start(Plugin plugin, Fireball fireball, double maximumDistance) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(fireball, "fireball");
        if (!Double.isFinite(maximumDistance) || maximumDistance <= 0D) {
            throw new IllegalArgumentException("Fireball flight distance must be positive and finite");
        }

        FireballFlightTracker tracker = new FireballFlightTracker(fireball, maximumDistance);
        fireball.getScheduler().runAtFixedRate(plugin, tracker::tick, () -> { }, 1L, 1L);
    }

    private void tick(ScheduledTask task) {
        if (!fireball.isValid() || fireball.isDead()) {
            task.cancel();
            return;
        }

        Location current = fireball.getLocation(locationBuffer);
        if (current == null || current.getWorld() != world) {
            task.cancel();
            fireball.remove();
            return;
        }

        travelledDistance = FireballLaunchPhysics.accumulateTravelledDistance(
                travelledDistance,
                previousX, previousY, previousZ,
                current.getX(), current.getY(), current.getZ());
        previousX = current.getX();
        previousY = current.getY();
        previousZ = current.getZ();

        if (travelledDistance >= maximumDistance) {
            task.cancel();
            fireball.remove();
        }
    }
}
