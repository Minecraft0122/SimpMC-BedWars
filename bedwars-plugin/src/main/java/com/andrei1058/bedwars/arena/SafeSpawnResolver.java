/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.support.paper.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SafeSpawnResolver {

    private static final int SEARCH_RADIUS = 5;
    private static final int[] Y_OFFSETS = {0, 1, -1, 2, -2};
    private static final Set<Material> UNSAFE_BLOCKS = EnumSet.of(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CACTUS,
            Material.MAGMA_BLOCK,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.POWDER_SNOW,
            Material.POINTED_DRIPSTONE
    );

    private SafeSpawnResolver() {
    }

    public record Result(@NotNull Location location, boolean crawling) {
    }

    /**
     * Resolve a safe location near the configured team spawn. A one-block-high
     * space is valid and causes the player to enter the swimming/crawling pose.
     */
    public static Result resolve(@NotNull Location configured) {
        World world = configured.getWorld();
        if (world == null) {
            return new Result(configured.clone(), false);
        }

        int configuredX = configured.getBlockX();
        int configuredY = configured.getBlockY();
        int configuredZ = configured.getBlockZ();
        Set<Long> loadedChunks = captureLoadedSearchChunks(world, configuredX, configuredZ);
        if (loadedChunks.isEmpty()) {
            return new Result(configured.clone(), false);
        }

        Result exact = inspect(world, configuredX, configuredY, configuredZ, configured, loadedChunks);
        if (exact != null) return exact;

        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    if (Math.max(Math.abs(xOffset), Math.abs(zOffset)) != radius) continue;
                    for (int yOffset : Y_OFFSETS) {
                        Result candidate = inspect(
                                world,
                                configuredX + xOffset,
                                configuredY + yOffset,
                                configuredZ + zOffset,
                                configured,
                                loadedChunks
                        );
                        if (candidate != null) return candidate;
                    }
                }
            }
        }
        return new Result(configured.clone(), false);
    }

    public static void teleport(@NotNull Player player, @NotNull Location configured) {
        teleportResult(player, configured);
    }

    /**
     * Teleport to a resolved spawn and expose the completion handle to callers
     * that must synchronize entity state (for example collision) with Paper's
     * asynchronous teleport path.
     */
    public static CompletableFuture<Boolean> teleportResult(@NotNull Player player,
                                                             @NotNull Location configured) {
        Result result = resolve(configured);
        applyPose(player, result.crawling());
        return TeleportManager.teleportC(player, result.location(), PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public static void applyPose(@NotNull Player player, boolean crawling) {
        player.setSwimming(crawling);
        Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
            if (player.isOnline()) {
                player.setSwimming(crawling);
            }
        });
    }

    /**
     * Snapshot the chunks which are already available to the current tick. Calling
     * {@link World#getBlockAt(int, int, int)} for an unloaded chunk can synchronously
     * start a region read, which is especially expensive from a respawn event.
     */
    private static Set<Long> captureLoadedSearchChunks(World world, int centerX, int centerZ) {
        int minChunkX = Math.floorDiv(centerX - SEARCH_RADIUS, 16);
        int maxChunkX = Math.floorDiv(centerX + SEARCH_RADIUS, 16);
        int minChunkZ = Math.floorDiv(centerZ - SEARCH_RADIUS, 16);
        int maxChunkZ = Math.floorDiv(centerZ + SEARCH_RADIUS, 16);
        int chunkCount = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        Set<Long> loadedChunks = new HashSet<>(chunkCount);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    loadedChunks.add(chunkKey(chunkX, chunkZ));
                }
            }
        }
        return loadedChunks;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static Result inspect(World world, int x, int y, int z, Location configured,
                                  Set<Long> loadedChunks) {
        if (!loadedChunks.contains(chunkKey(Math.floorDiv(x, 16), Math.floorDiv(z, 16)))) {
            return null;
        }
        Block feet = world.getBlockAt(x, y, z);
        Block floor = world.getBlockAt(x, y - 1, z);
        if (!feet.isPassable() || feet.isLiquid() || UNSAFE_BLOCKS.contains(feet.getType())
                || floor.isPassable() || floor.isLiquid() || UNSAFE_BLOCKS.contains(floor.getType())) {
            return null;
        }

        Block head = world.getBlockAt(x, y + 1, z);
        if (head.isLiquid() || UNSAFE_BLOCKS.contains(head.getType())) return null;
        boolean crawling = !head.isPassable();
        Location result = new Location(world, x + 0.5, y, z + 0.5, configured.getYaw(), configured.getPitch());
        if (!world.getWorldBorder().isInside(result)) return null;
        return new Result(result, crawling);
    }
}
