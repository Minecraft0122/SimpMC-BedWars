package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BucketFlowTest {
    private final PlacedBlockListener listener = new PlacedBlockListener();
    private final IArena arena = mock(IArena.class);
    private final World world = mock(World.class);
    private final Player player = mock(Player.class);
    private final Block source = mock(Block.class);
    private final Block clicked = mock(Block.class);
    private final Set<Block> placed = new HashSet<>();
    private final List<Runnable> tasks = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<Arena> arenas;
    private MainConfig originalConfig;

    @BeforeEach
    void setUp() {
        originalConfig = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        when(world.getName()).thenReturn("arena");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(source.getWorld()).thenReturn(world);
        when(source.getType()).thenReturn(Material.AIR);
        when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.isBlockPlaced(any())).thenAnswer(call -> placed.contains(call.getArgument(0)));
        doAnswer(call -> placed.add(call.getArgument(0))).when(arena).addPlacedBlock(any());
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld(world.getUID())).thenReturn(world);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTask(eq(BedWars.plugin), any(Runnable.class))).thenAnswer(call -> {
            tasks.add(call.getArgument(1));
            return null;
        });
        arenas = mockStatic(Arena.class);
        arenas.when(() -> Arena.getArenaByPlayer(player)).thenReturn(arena);
        arenas.when(() -> Arena.getArenaByIdentifier("arena")).thenReturn(arena);
    }

    @AfterEach
    void tearDown() {
        if (arenas != null) arenas.close();
        if (bukkit != null) bukkit.close();
        BedWars.config = originalConfig;
    }

    @Test
    void waterBucketCreatesATrackedSourceAndCanFlowMoreThanOneBlock() {
        listener.onBucketEmpty(bucket(Material.WATER_BUCKET));
        assertFalse(placed.contains(source));
        when(source.getType()).thenReturn(Material.WATER);
        tick();
        assertTrue(placed.contains(source));
        assertFalse(placed.contains(clicked));

        Block first = flowIntoAir(source);
        Block second = flowIntoAir(first);
        assertTrue(placed.contains(second));

        Block mapFlower = mock(Block.class);
        when(mapFlower.getType()).thenReturn(Material.DANDELION);
        BlockFromToEvent erosion = new BlockFromToEvent(second, mapFlower);
        listener.onFluidFlow(erosion);
        assertTrue(erosion.isCancelled());
    }

    @Test
    void lavaAndFishBucketsUseTheirResultingLiquid() {
        for (Material bucket : List.of(Material.LAVA_BUCKET, Material.COD_BUCKET)) {
            placed.clear();
            listener.onBucketEmpty(bucket(bucket));
            when(source.getType()).thenReturn(bucket == Material.LAVA_BUCKET ? Material.LAVA : Material.WATER);
            tick();
            assertTrue(placed.contains(source));
        }
    }

    @Test
    void cancellationAtEitherStageDoesNotClaimTheSource() {
        PlayerBucketEmptyEvent event = bucket(Material.WATER_BUCKET);
        event.setCancelled(true);
        listener.onBucketEmpty(event);
        assertTrue(tasks.isEmpty());
        event.setCancelled(false);
        listener.onBucketEmpty(event);
        event.setCancelled(true);
        when(source.getType()).thenReturn(Material.WATER);
        tick();
        assertTrue(placed.isEmpty());
    }

    @Test
    void failedPlacementWaterloggedMapBlocksAndResetWorldsAreNotClaimed() {
        for (Material unchanged : List.of(Material.AIR, Material.OAK_SLAB)) {
            listener.onBucketEmpty(bucket(Material.WATER_BUCKET));
            when(source.getType()).thenReturn(unchanged);
            tick();
            assertTrue(placed.isEmpty());
        }
        listener.onBucketEmpty(bucket(Material.WATER_BUCKET));
        when(source.getType()).thenReturn(Material.WATER);
        bukkit.when(() -> Bukkit.getWorld(world.getUID())).thenReturn(mock(World.class));
        tick();
        assertTrue(placed.isEmpty());
    }

    private PlayerBucketEmptyEvent bucket(Material bucket) {
        return new PlayerBucketEmptyEvent(player, source, clicked, BlockFace.UP,
                bucket, null, EquipmentSlot.HAND);
    }

    private Block flowIntoAir(Block from) {
        Block destination = mock(Block.class);
        when(destination.getType()).thenReturn(Material.AIR);
        when(destination.isEmpty()).thenReturn(true);
        when(destination.getWorld()).thenReturn(world);
        BlockFromToEvent flow = new BlockFromToEvent(from, destination);
        listener.onFluidFlow(flow);
        assertFalse(flow.isCancelled());
        when(destination.getType()).thenReturn(Material.WATER);
        when(destination.isEmpty()).thenReturn(false);
        tick();
        return destination;
    }

    private void tick() {
        List<Runnable> pending = List.copyOf(tasks);
        tasks.clear();
        pending.forEach(Runnable::run);
    }
}
