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
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
import org.bukkit.event.block.BlockFromToEvent;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        if (arena == null || arena.getStatus() != GameState.playing || !event.canBuild()) return;

        List<PlacementSnapshot> snapshots = capturePlacement(arena, event);
        for (PlacementSnapshot snapshot : snapshots) {
            trackCurrentBlock(arena, snapshot.block());
            queueClientResync(event.getPlayer(), snapshot.block());
        }
        Bukkit.getScheduler().runTask(plugin, () -> reconcilePlacement(arena, event, snapshots));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        resyncBuffer.discard(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        scheduleDestructionReconciliation(arenaAt(event.getBlock()), List.of(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        IArena arena = Arena.getArenaByIdentifier(event.getLocation().getWorld().getName());
        scheduleDestructionReconciliation(arena, event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        scheduleDestructionReconciliation(arenaAt(event.getBlock()), event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(@NotNull BlockBurnEvent event) {
        scheduleDestructionReconciliation(arenaAt(event.getBlock()), List.of(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(@NotNull BlockFadeEvent event) {
        scheduleDestructionReconciliation(arenaAt(event.getBlock()), List.of(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDestroy(@NotNull BlockDestroyEvent event) {
        scheduleDestructionReconciliation(arenaAt(event.getBlock()), List.of(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreakBlock(@NotNull BlockBreakBlockEvent event) {
        scheduleDestructionReconciliation(arenaAt(event.getBlock()), List.of(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        protectMapFromPiston(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        protectMapFromPiston(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        IArena arena = arenaAt(event.getBlock());
        if (arena == null || arena.getStatus() != GameState.playing) return;
        // Player-placed fluids may still flow normally. Only an original map
        // fluid source or a non-air original block may be changed by a flow;
        // this prevents erosion without disabling water/lava gameplay.
        Block destination = event.getToBlock();
        if (shouldCancelFluidFlow(arena.isBlockPlaced(event.getBlock()), destination.getType().isAir(),
                arena.isBlockPlaced(destination), arena.isAllowMapBreak())) {
            event.setCancelled(true);
            return;
        }
        if (arena.isBlockPlaced(event.getBlock())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!event.isCancelled() && arena.getStatus() == GameState.playing
                        && !destination.getType().isAir()) {
                    arena.addPlacedBlock(destination);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectOriginalFallingBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)
                || event.getTo() == Material.AIR
                || fallingBlock.hasMetadata(PLAYER_PLACED_FALLING_BLOCK)) return;
        IArena arena = Arena.getArenaByIdentifier(event.getBlock().getWorld().getName());
        if (arena != null && arena.getStatus() == GameState.playing && !arena.isAllowMapBreak()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectOriginalEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock) return;
        IArena arena = Arena.getArenaByIdentifier(event.getBlock().getWorld().getName());
        if (arena != null && shouldCancelEntityChange(
                !arena.isBlockPlaced(event.getBlock()), arena.isAllowMapBreak(),
                arena.getStatus() == GameState.playing)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlockChange(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
        IArena arena = Arena.getArenaByIdentifier(event.getBlock().getWorld().getName());
        if (arena == null) return;

        if (event.getTo() == Material.AIR) {
            if (arena.isBlockPlaced(event.getBlock())) {
                fallingBlock.setMetadata(PLAYER_PLACED_FALLING_BLOCK, new FixedMetadataValue(plugin, true));
                TrackedBlockSnapshot source = new TrackedBlockSnapshot(
                        BlockPosition.of(event.getBlock()), event.getBlock().getBlockData());
                Bukkit.getScheduler().runTask(plugin, () -> reconcileFallingSource(arena, fallingBlock, source));
            }
            return;
        }

        if (fallingBlock.hasMetadata(PLAYER_PLACED_FALLING_BLOCK)) {
            Block block = event.getBlock();
            PlacementSnapshot landing = new PlacementSnapshot(block, block.getBlockData(), arena.isBlockPlaced(block));
            arena.addPlacedBlock(block);
            Bukkit.getScheduler().runTask(plugin,
                    () -> reconcileFallingLanding(arena, fallingBlock, event, landing));
        }
    }

    private static List<PlacementSnapshot> capturePlacement(IArena arena, BlockPlaceEvent event) {
        List<BlockState> replacedStates = event instanceof BlockMultiPlaceEvent multiPlaceEvent
                ? multiPlaceEvent.getReplacedBlockStates()
                : List.of(event.getBlockReplacedState());
        List<PlacementSnapshot> snapshots = new ArrayList<>(replacedStates.size());
        for (BlockState replacedState : replacedStates) {
            Block block = replacedState.getBlock();
            snapshots.add(new PlacementSnapshot(block, replacedState.getBlockData(), arena.isBlockPlaced(block)));
        }
        return snapshots;
    }

    private static void reconcilePlacement(IArena arena, BlockPlaceEvent event,
                                           List<PlacementSnapshot> snapshots) {
        if (arena.getStatus() != GameState.playing) return;
        boolean accepted = !event.isCancelled() && event.canBuild();
        for (PlacementSnapshot snapshot : snapshots) {
            if (!isCurrentArena(arena, snapshot.block().getWorld())) return;
            if (!accepted) {
                setTracked(arena, snapshot.block(), snapshot.wasTracked());
                continue;
            }

            Block current = currentBlockIfLoaded(snapshot.block());
            if (current == null) continue;
            boolean track = shouldTrackAfterPlacement(snapshot.wasTracked(), false, true,
                    current.getType().isAir(), current.getBlockData().equals(snapshot.replacedData()));
            setTracked(arena, current, track);
        }
    }

    private static void reconcileFallingSource(IArena arena, FallingBlock fallingBlock,
                                               TrackedBlockSnapshot source) {
        if (arena.getStatus() != GameState.playing) return;
        if (!isCurrentArena(arena, source.position().world())) return;
        Block current = source.position().currentBlockIfLoaded();
        if (current == null) return;
        if (current.getBlockData().equals(source.originalData())) {
            arena.addPlacedBlock(current);
            fallingBlock.removeMetadata(PLAYER_PLACED_FALLING_BLOCK, plugin);
        } else {
            arena.removePlacedBlock(current);
        }
    }

    private static void reconcileFallingLanding(IArena arena, FallingBlock fallingBlock,
                                                EntityChangeBlockEvent event, PlacementSnapshot landing) {
        if (arena.getStatus() != GameState.playing) return;
        if (!isCurrentArena(arena, landing.block().getWorld())) return;
        Block current = currentBlockIfLoaded(landing.block());
        if (current == null) return;
        boolean track = shouldTrackAfterPlacement(landing.wasTracked(), event.isCancelled(), true,
                current.getType().isAir(), current.getBlockData().equals(landing.replacedData()));
        setTracked(arena, current, track);
        if (track && !event.isCancelled() && !current.getBlockData().equals(landing.replacedData())) {
            fallingBlock.removeMetadata(PLAYER_PLACED_FALLING_BLOCK, plugin);
        }
    }

    static boolean shouldTrackAfterPlacement(boolean wasTracked, boolean cancelled, boolean canBuild,
                                             boolean finalStateIsAir, boolean finalStateMatchesReplaced) {
        if (cancelled || !canBuild || finalStateMatchesReplaced) return wasTracked;
        return !finalStateIsAir;
    }

    static boolean shouldCancelFluidFlow(boolean sourcePlaced, boolean destinationAir,
                                         boolean destinationPlaced, boolean allowMapBreak) {
        return !allowMapBreak && (!sourcePlaced || (!destinationAir && !destinationPlaced));
    }

    static boolean shouldCancelPiston(boolean pistonPlaced, boolean movedOriginalBlock,
                                      boolean allowMapBreak, boolean playing) {
        return playing && (!allowMapBreak || !pistonPlaced || movedOriginalBlock);
    }

    static boolean shouldCancelEntityChange(boolean originalBlock, boolean allowMapBreak,
                                             boolean playing) {
        return playing && originalBlock && !allowMapBreak;
    }

    private static void scheduleDestructionReconciliation(IArena arena, Collection<Block> blocks) {
        if (arena == null) return;
        Set<TrackedBlockSnapshot> snapshots = captureTrackedBlocks(arena, blocks);
        if (snapshots.isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin, () -> reconcileDestroyedBlocks(arena, snapshots));
    }

    private static Set<TrackedBlockSnapshot> captureTrackedBlocks(IArena arena, Collection<Block> blocks) {
        Set<TrackedBlockSnapshot> snapshots = new LinkedHashSet<>();
        for (Block block : blocks) {
            if (arena.isBlockPlaced(block)) {
                snapshots.add(new TrackedBlockSnapshot(BlockPosition.of(block), block.getBlockData()));
            }
        }
        return snapshots;
    }

    private static void reconcileDestroyedBlocks(IArena arena, Collection<TrackedBlockSnapshot> snapshots) {
        if (arena.getStatus() != GameState.playing) return;
        for (TrackedBlockSnapshot snapshot : snapshots) {
            BlockPosition position = snapshot.position();
            if (!isCurrentArena(arena, position.world())) return;
            Block current = position.currentBlockIfLoaded();
            if (current == null) continue;
            setTracked(arena, current,
                    shouldRetainAfterDestruction(current.getBlockData().equals(snapshot.originalData())));
        }
    }

    static boolean shouldRetainAfterDestruction(boolean finalStateMatchesOriginal) {
        return finalStateMatchesOriginal;
    }

    private static void setTracked(IArena arena, Block block, boolean tracked) {
        if (tracked) arena.addPlacedBlock(block);
        else arena.removePlacedBlock(block);
    }

    private static IArena arenaAt(Block block) {
        return Arena.getArenaByIdentifier(block.getWorld().getName());
    }

    private static boolean isCurrentArena(IArena arena, World world) {
        return arena != null && world != null
                && Bukkit.getWorld(world.getUID()) == world
                && Arena.getArenaByIdentifier(world.getName()) == arena;
    }

    private static Block currentBlockIfLoaded(Block reference) {
        return BlockPosition.of(reference).currentBlockIfLoaded();
    }

    private static void trackCurrentBlock(IArena arena, Block block) {
        if (!block.getType().isAir()) arena.addPlacedBlock(block);
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

    private static void protectMapFromPiston(BlockPistonEvent event) {
        IArena arena = Arena.getArenaByIdentifier(event.getBlock().getWorld().getName());
        if (arena == null || arena.getStatus() != GameState.playing) return;
        boolean movedOriginal = event instanceof BlockPistonExtendEvent extend
                && extend.getBlocks().stream().anyMatch(block -> !arena.isBlockPlaced(block));
        if (event instanceof BlockPistonRetractEvent retract) {
            movedOriginal = retract.getBlocks().stream().anyMatch(block -> !arena.isBlockPlaced(block));
        }
        if (shouldCancelPiston(arena.isBlockPlaced(event.getBlock()), movedOriginal,
                arena.isAllowMapBreak(), true)) event.setCancelled(true);
    }

    private record PlacementSnapshot(Block block, BlockData replacedData, boolean wasTracked) {
    }

    private record TrackedBlockSnapshot(BlockPosition position, BlockData originalData) {
    }

    private record BlockPosition(World world, int x, int y, int z) {

        private static BlockPosition of(Block block) {
            return new BlockPosition(block.getWorld(), block.getX(), block.getY(), block.getZ());
        }

        private Block currentBlockIfLoaded() {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
            return world.getBlockAt(x, y, z);
        }
    }
}
