package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.GameRules;
import com.andrei1058.bedwars.configuration.MainConfig;
import io.papermc.paper.event.world.WorldDifficultyChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LobbyWorldProtectionTest {
    private final World lobby = mock(World.class);
    private final World arenaWorld = mock(World.class);
    private final Block block = mock(Block.class);
    private final Player player = mock(Player.class);
    private final LobbyWorldProtection protection = new LobbyWorldProtection();
    private MockedStatic<BedWarsWorldEnvironment> environment;

    @BeforeEach
    void setUp() {
        when(block.getWorld()).thenReturn(lobby);
        when(player.getWorld()).thenReturn(lobby);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(lobby.getUID()).thenReturn(UUID.randomUUID());
        environment = mockStatic(BedWarsWorldEnvironment.class);
        environment.when(() -> BedWarsWorldEnvironment.isLobbyManagedWorld(lobby)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        BreakPlace.removeBuildSession(player);
        environment.close();
    }

    @Test
    void entityAndBlockExplosionsAreStoppedEvenWithAnEmptyBlockList() {
        EntityExplodeEvent entity = mock(EntityExplodeEvent.class);
        when(entity.getLocation()).thenReturn(new Location(lobby, 0, 0, 0));
        List<Block> damaged = new ArrayList<>(List.of(block));
        when(entity.blockList()).thenReturn(damaged);
        protection.onEntityExplode(entity);
        verify(entity).setCancelled(true);
        assertTrue(damaged.isEmpty());

        BlockExplodeEvent explosion = mock(BlockExplodeEvent.class);
        when(explosion.getBlock()).thenReturn(block);
        when(explosion.blockList()).thenReturn(new ArrayList<>());
        protection.onBlockExplode(explosion);
        verify(explosion).setCancelled(true);
    }

    @Test
    void arenaExplosionsAndWaterAreUnaffected() {
        when(block.getWorld()).thenReturn(arenaWorld);
        BlockExplodeEvent explosion = mock(BlockExplodeEvent.class);
        when(explosion.getBlock()).thenReturn(block);
        protection.onBlockExplode(explosion);
        verify(explosion, never()).setCancelled(anyBoolean());
        BlockFromToEvent flow = mock(BlockFromToEvent.class);
        when(flow.getBlock()).thenReturn(block);
        protection.onFlow(flow);
        verify(flow, never()).setCancelled(anyBoolean());
    }

    @Test
    void naturalDamageHungerAndSpawningAreBlockedButCustomNpcsRemain() {
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        when(damage.getEntity()).thenReturn(player);
        protection.onDamage(damage);
        verify(damage).setCancelled(true);
        FoodLevelChangeEvent food = mock(FoodLevelChangeEvent.class);
        when(food.getEntity()).thenReturn(player);
        protection.onFoodChange(food);
        verify(food).setCancelled(true);
        verify(player).setFoodLevel(20);
        verify(player).setSaturation(20);
        clearInvocations(player, food);
        when(food.isCancelled()).thenReturn(true);
        protection.onFoodChange(food);
        verify(food).setCancelled(true);
        verify(player).setFoodLevel(20);
        verify(player).setSaturation(20);

        CreatureSpawnEvent spawn = mock(CreatureSpawnEvent.class);
        when(spawn.getLocation()).thenReturn(new Location(lobby, 0, 0, 0));
        when(spawn.getSpawnReason()).thenReturn(CreatureSpawnEvent.SpawnReason.NATURAL);
        protection.onCreatureSpawn(spawn);
        verify(spawn).setCancelled(true);
        clearInvocations(spawn);
        when(spawn.getSpawnReason()).thenReturn(CreatureSpawnEvent.SpawnReason.CUSTOM);
        protection.onCreatureSpawn(spawn);
        verify(spawn, never()).setCancelled(anyBoolean());
    }

    @Test
    void environmentCannotBurnFloodOrPushLobbyBlocks() {
        BlockBurnEvent burn = mock(BlockBurnEvent.class);
        when(burn.getBlock()).thenReturn(block);
        protection.onBurn(burn);
        verify(burn).setCancelled(true);
        BlockFromToEvent flow = mock(BlockFromToEvent.class);
        when(flow.getBlock()).thenReturn(block);
        protection.onFlow(flow);
        verify(flow).setCancelled(true);
        BlockPistonExtendEvent piston = mock(BlockPistonExtendEvent.class);
        when(piston.getBlock()).thenReturn(block);
        protection.onPistonExtend(piston);
        verify(piston).setCancelled(true);
        EntityChangeBlockEvent change = mock(EntityChangeBlockEvent.class);
        when(change.getBlock()).thenReturn(block);
        protection.onEntityChangeBlock(change);
        verify(change).setCancelled(true);
    }

    @Test
    void explicitBuildSessionCanEditBlocksAndDisplays() {
        BlockBreakEvent breaking = new BlockBreakEvent(block, player);
        protection.onBreak(breaking);
        assertTrue(breaking.isCancelled());
        BreakPlace.addBuildSession(player);
        breaking.setCancelled(false);
        protection.onBreak(breaking);
        assertFalse(breaking.isCancelled());
        PlayerBucketEmptyEvent bucket = mock(PlayerBucketEmptyEvent.class);
        when(bucket.getPlayer()).thenReturn(player);
        when(bucket.getBlock()).thenReturn(block);
        protection.onBucketEmpty(bucket);
        verify(bucket, never()).setCancelled(anyBoolean());

        ArmorStand display = mock(ArmorStand.class);
        when(display.getWorld()).thenReturn(lobby);
        PlayerInteractEntityEvent interact = new PlayerInteractEntityEvent(player, display);
        protection.onDisplayInteract(interact);
        assertFalse(interact.isCancelled());
        BreakPlace.removeBuildSession(player);
        protection.onDisplayInteract(interact);
        assertTrue(interact.isCancelled());
    }

    @Test
    void startupAndWorldLoadApplyLobbyEnvironment() {
        MainConfig previous = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Arena> arenas = mockStatic(Arena.class);
             MockedStatic<GameRules> rules = mockStatic(GameRules.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(lobby));
            arenas.when(Arena::getEnableQueue).thenReturn(new java.util.LinkedList<>());
            WorldLoadListener listener = new WorldLoadListener();
            listener.enforceLoadedWorlds();
            listener.onLoad(new WorldLoadEvent(lobby));
            rules.verify(() -> GameRules.enforceLobbyEnvironment(lobby), times(2));
        } finally {
            BedWars.config = previous;
        }
    }

    @Test
    void difficultyIsRestoredOnceAfterTheNotificationWithoutRecursion() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<GameRules> rules = mockStatic(GameRules.class)) {
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            List<Runnable> tasks = new ArrayList<>();
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getWorld(lobby.getUID())).thenReturn(lobby);
            when(scheduler.runTask(eq(BedWars.plugin), any(Runnable.class))).thenAnswer(call -> {
                tasks.add(call.getArgument(1));
                return null;
            });
            WorldDifficultyChangeEvent change = mock(WorldDifficultyChangeEvent.class);
            when(change.getWorld()).thenReturn(lobby);
            when(change.getDifficulty()).thenReturn(Difficulty.HARD);
            protection.onDifficultyChange(change);
            assertEquals(1, tasks.size());
            tasks.removeFirst().run();
            rules.verify(() -> GameRules.enforceLobbyEnvironment(lobby));
            when(change.getDifficulty()).thenReturn(Difficulty.PEACEFUL);
            protection.onDifficultyChange(change);
            assertTrue(tasks.isEmpty());
        }
    }
}
