/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.andrei1058.bedwars.BedWars.plugin;

/**
 * Keeps the arena's player-placed block index synchronized with Bukkit block
 * lifecycle events. Placement permission and explosion protection remain in
 * {@link BreakPlace}; this listener owns only tracking and movement semantics.
 */
public final class PlacedBlockListener implements Listener {

    private static final String PLAYER_PLACED_FALLING_BLOCK = "bw-player-placed-falling-block";
    private final BlockPlacementResyncBuffer resyncBuffer = new BlockPlacementResyncBuffer();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        IArena arena = Arena.getArenaByPlayer(event.getPlayer());
        if (arena == null || arena.getStatus() != GameState.playing) return;

        if (event instanceof BlockMultiPlaceEvent multiPlaceEvent) {
            multiPlaceEvent.getReplacedBlockStates().forEach(state -> {
                trackCurrentBlock(arena, state.getBlock());
                queueClientResync(event.getPlayer(), state.getBlock());
            });
            return;
        }
        trackCurrentBlock(arena, event.getBlockPlaced());
        queueClientResync(event.getPlayer(), event.getBlockPlaced());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        resyncBuffer.discard(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        IArena arena = Arena.getArenaByPlayer(event.getPlayer());
        if (arena != null) arena.removePlacedBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        IArena arena = Arena.getArenaByIdentifier(event.getLocation().getWorld().getName());
        if (arena != null) event.blockList().forEach(arena::removePlacedBlock);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        IArena arena = Arena.getArenaByIdentifier(event.getBlock().getWorld().getName());
        if (arena != null) event.blockList().forEach(arena::removePlacedBlock);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(@NotNull BlockBurnEvent event) {
        removeTrackedBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(@NotNull BlockFadeEvent event) {
        removeTrackedBlock(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        protectMapFromPiston(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        protectMapFromPiston(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlockChange(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
        IArena arena = Arena.getArenaByIdentifier(event.getBlock().getWorld().getName());
        if (arena == null) return;

        if (event.getTo() == Material.AIR) {
            if (arena.isBlockPlaced(event.getBlock())) {
                arena.removePlacedBlock(event.getBlock());
                fallingBlock.setMetadata(PLAYER_PLACED_FALLING_BLOCK, new FixedMetadataValue(plugin, true));
            }
            return;
        }

        if (fallingBlock.hasMetadata(PLAYER_PLACED_FALLING_BLOCK)) {
            arena.addPlacedBlock(event.getBlock());
            fallingBlock.removeMetadata(PLAYER_PLACED_FALLING_BLOCK, plugin);
        }
    }

    private static void trackCurrentBlock(IArena arena, Block block) {
        if (block.getType() != Material.AIR) arena.addPlacedBlock(block);
    }

    private void queueClientResync(Player player, Block block) {
        UUID playerId = player.getUniqueId();
        boolean shouldSchedule = resyncBuffer.queue(playerId, block.getWorld().getUID(),
                block.getX(), block.getY(), block.getZ());
        if (shouldSchedule) {
            Bukkit.getScheduler().runTask(plugin, () -> flushClientResync(playerId));
        }
    }

    private void flushClientResync(UUID playerId) {
        List<BlockPlacementResyncBuffer.BlockPosition> positions = resyncBuffer.drain(playerId);
        if (positions.isEmpty()) return;

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        World world = player.getWorld();
        UUID worldId = world.getUID();
        Map<Position, BlockData> currentStates = HashMap.newHashMap(positions.size());
        for (BlockPlacementResyncBuffer.BlockPosition position : positions) {
            if (!worldId.equals(position.worldId())) continue;
            Block block = world.getBlockAt(position.x(), position.y(), position.z());
            currentStates.put(Position.block(position.x(), position.y(), position.z()), block.getBlockData());
        }
        if (!currentStates.isEmpty()) {
            player.sendMultiBlockChange(currentStates);
        }
    }

    private static void removeTrackedBlock(Block block) {
        IArena arena = Arena.getArenaByIdentifier(block.getWorld().getName());
        if (arena != null) arena.removePlacedBlock(block);
    }

    private static void protectMapFromPiston(BlockPistonEvent event) {
        IArena arena = Arena.getArenaByIdentifier(event.getBlock().getWorld().getName());
        if (arena != null && !arena.isAllowMapBreak()) event.setCancelled(true);
    }
}
