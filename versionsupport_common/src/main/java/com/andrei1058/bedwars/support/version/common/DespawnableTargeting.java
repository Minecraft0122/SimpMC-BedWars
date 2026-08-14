package com.andrei1058.bedwars.support.version.common;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Internal target policy shared by the Bukkit event and version-support fallback. */
public final class DespawnableTargeting {

    private DespawnableTargeting() {
    }

    public static Player resolveTarget(IArena arena, ITeam ownerTeam, LivingEntity proposedTarget,
                                       Location source, Collection<Player> candidates) {
        if (proposedTarget instanceof Player player && isEligibleTarget(arena, ownerTeam, player, source)) {
            return player;
        }
        return findNearestTarget(arena, ownerTeam, source, candidates);
    }

    public static Player findNearestTarget(IArena arena, ITeam ownerTeam, Location source,
                                           Collection<Player> candidates) {
        if (arena == null || ownerTeam == null || source == null || candidates == null) return null;

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player candidate : candidates) {
            if (!isEligibleTarget(arena, ownerTeam, candidate, source)) continue;

            double distance = candidate.getLocation().distanceSquared(source);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public static boolean isEligibleTarget(IArena arena, ITeam ownerTeam, Player candidate, Location source) {
        if (arena == null || ownerTeam == null || candidate == null || source == null) return false;
        if (arena.getStatus() != GameState.playing || candidate.isDead() || !arena.isPlayer(candidate)) return false;

        World sourceWorld = source.getWorld();
        if (sourceWorld == null || !sourceWorld.equals(candidate.getWorld())) return false;

        if (arena.isReSpawning(candidate.getUniqueId()) || arena.isSpectator(candidate.getUniqueId())) return false;
        ITeam candidateTeam = arena.getTeam(candidate);
        return candidateTeam != null && candidateTeam != ownerTeam;
    }
}
