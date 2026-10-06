package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BlockPlacementLifecycleTest {
    @Test
    void oldDestructionCannotRemoveANewerPlacementEvenDuringDelayedReconciliation() {
        var originalConfig = BedWars.config;
        BedWars.config = mock(com.andrei1058.bedwars.configuration.MainConfig.class);
        PlacedBlockListener listener = new PlacedBlockListener();
        IArena arena = mock(IArena.class);
        when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.isBlockPlaced(any())).thenReturn(true);
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("arena");
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        Material solid = mock(Material.class);
        when(solid.isAir()).thenReturn(false);
        when(block.getType()).thenReturn(solid);
        when(world.getBlockAt(0, 0, 0)).thenReturn(block);
        AtomicReference<BlockData> data = new AtomicReference<>(mock(BlockData.class));
        when(block.getBlockData()).thenAnswer(ignored -> data.get());
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        List<Runnable> tasks = new ArrayList<>();
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(BedWars.plugin), any(Runnable.class))).thenAnswer(call -> {
            tasks.add(call.getArgument(1)); return null;
        });
        try (var bukkit = mockStatic(Bukkit.class); var arenas = mockStatic(Arena.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            arenas.when(() -> Arena.getArenaByIdentifier("arena")).thenReturn(arena);
            arenas.when(() -> Arena.getArenaByPlayer(player)).thenReturn(arena);
            listener.onBlockBreak(new BlockBreakEvent(block, player));
            data.set(mock(BlockData.class));
            BlockState replaced = mock(BlockState.class);
            when(replaced.getBlock()).thenReturn(block);
            when(replaced.getBlockData()).thenReturn(mock(BlockData.class));
            BlockPlaceEvent placement = mock(BlockPlaceEvent.class);
            when(placement.getPlayer()).thenReturn(player);
            when(placement.canBuild()).thenReturn(true);
            when(placement.getBlockReplacedState()).thenReturn(replaced);
            listener.onBlockPlace(placement);
            assertEquals(3, tasks.size());
            tasks.getFirst().run();
            verify(arena, never()).removePlacedBlock(block);
            tasks.subList(1, tasks.size()).forEach(Runnable::run);
            verify(arena, never()).removePlacedBlock(block);
            verify(arena, times(2)).addPlacedBlock(block);
        } finally {
            BedWars.config = originalConfig;
        }
    }
}
