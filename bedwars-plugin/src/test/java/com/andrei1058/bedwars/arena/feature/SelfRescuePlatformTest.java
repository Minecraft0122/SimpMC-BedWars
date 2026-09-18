package com.andrei1058.bedwars.arena.feature;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import com.andrei1058.bedwars.api.server.VersionSupport;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.MainConfig;
import com.andrei1058.bedwars.shop.ShopItemIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfRescuePlatformTest {
    private final Map<String, Block> blocks = new HashMap<>();
    private final List<Runnable> nextTick = new ArrayList<>();
    private final List<Runnable> expiry = new ArrayList<>();
    private final AtomicInteger itemAmount = new AtomicInteger(2);
    private final AtomicReference<Float> fallDistance = new AtomicReference<>(60.0F);
    private VersionSupport previousSupport;
    private MainConfig previousConfig;
    private MockedStatic<Bukkit> bukkit;
    private SelfRescuePlatform listener;
    private World world;
    private Player player;
    private IArena arena;
    private ItemStack item;
    private Location position;

    @BeforeEach
    void setUp() {
        previousSupport = BedWars.nms;
        previousConfig = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        BedWars.nms = mock(VersionSupport.class);
        world = mock(World.class);
        player = mock(Player.class);
        arena = mock(IArena.class);
        item = mock(ItemStack.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        UUID worldId = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("rescue-test");
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                block(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        when(player.getWorld()).thenReturn(world);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
        position = new Location(world, 0.5, 64, 0.5);
        when(player.getLocation()).thenAnswer(invocation -> position.clone());
        when(player.getBoundingBox()).thenAnswer(invocation -> new BoundingBox(
                position.getX() - 0.3, position.getY(), position.getZ() - 0.3,
                position.getX() + 0.3, position.getY() + 1.8, position.getZ() + 0.3));
        when(player.getFallDistance()).thenAnswer(invocation -> fallDistance.get());
        doAnswer(invocation -> {
            fallDistance.set(invocation.getArgument(0));
            return null;
        }).when(player).setFallDistance(any(Float.class));
        when(arena.getWorld()).thenReturn(world);
        when(arena.getWorldName()).thenReturn("rescue-test");
        when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.isPlayer(player)).thenReturn(true);
        when(arena.getPlayers()).thenReturn(List.of(player));
        Arena.setArenaByPlayer(player, arena);
        when(item.getType()).thenReturn(Material.BLAZE_ROD);
        when(item.getAmount()).thenAnswer(invocation -> itemAmount.get());
        doAnswer(invocation -> {
            itemAmount.set(invocation.getArgument(0));
            return null;
        }).when(item).setAmount(anyInt());
        when(inventory.getContents()).thenAnswer(invocation -> new ItemStack[]{itemAmount.get() > 0 ? item : null});
        doAnswer(invocation -> {
            itemAmount.set(0);
            return null;
        }).when(inventory).setItem(anyInt(), isNull());
        when(BedWars.nms.getTag(item, "shop-item-id")).thenReturn(ShopItemIdentifier.SELF_RESCUE_PLATFORM);
        when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(invocation -> {
            nextTick.add(invocation.getArgument(1));
            return mock(BukkitTask.class);
        });
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            assertEquals(320L, (long) invocation.getArgument(2));
            expiry.add(invocation.getArgument(1));
            return mock(BukkitTask.class);
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
        bukkit.when(() -> Bukkit.getWorld("rescue-test")).thenReturn(world);
        listener = new SelfRescuePlatform();
    }

    @AfterEach
    void tearDown() {
        Arena.getArenaByPlayer().remove(player);
        BedWars.nms = previousSupport;
        BedWars.config = previousConfig;
        if (bukkit != null) bukkit.close();
    }

    @Test
    void acceptsLeftAndRightClickActivation() {
        assertTrue(SelfRescuePlatform.isActivationAction(Action.LEFT_CLICK_AIR));
        assertTrue(SelfRescuePlatform.isActivationAction(Action.LEFT_CLICK_BLOCK));
        assertTrue(SelfRescuePlatform.isActivationAction(Action.RIGHT_CLICK_AIR));
        assertTrue(SelfRescuePlatform.isActivationAction(Action.RIGHT_CLICK_BLOCK));
        assertFalse(SelfRescuePlatform.isActivationAction(Action.PHYSICAL));
    }

    @ParameterizedTest
    @EnumSource(value = Action.class, names = {
            "LEFT_CLICK_AIR", "RIGHT_CLICK_AIR", "LEFT_CLICK_BLOCK", "RIGHT_CLICK_BLOCK"})
    void dispatchesManualUseEvenWhenPaperHasCancelledTheInteraction(Action action) throws Exception {
        PlayerInteractEvent event = interact(action, EquipmentSlot.HAND);
        event.setCancelled(true);

        dispatchUse(event);

        assertEquals(Material.SLIME_BLOCK, block(0, 61, 0).getType());
        assertEquals(17, blocks.values().stream().filter(block -> block.getType() == Material.SLIME_BLOCK).count());
        assertEquals(1, itemAmount.get());
        assertEquals(1, expiry.size());
        assertTrue(event.isCancelled());
    }

    @Test
    void onlyConsumesOnePlatformForBothHandsInTheSameTick() throws Exception {
        dispatchUse(interact(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        dispatchUse(interact(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND));

        assertEquals(1, itemAmount.get());
        assertEquals(1, expiry.size());
    }

    @Test
    void anotherTickAllowsAnOverlappingPlatformWithItsOwnLifetime() throws Exception {
        deploy();
        nextTick.getFirst().run();
        dispatchUse(interact(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND));

        assertEquals(0, itemAmount.get());
        assertEquals(2, expiry.size());
        expiry.getFirst().run();
        assertEquals(Material.SLIME_BLOCK, block(0, 61, 0).getType());
        expiry.getLast().run();
        assertEquals(Material.AIR, block(0, 61, 0).getType());
    }

    @Test
    void unrelatedBlazeRodsCannotDeployPlatforms() throws Exception {
        when(BedWars.nms.getTag(item, "shop-item-id")).thenReturn(null);

        dispatchUse(interact(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));

        assertEquals(2, itemAmount.get());
        assertTrue(expiry.isEmpty());
    }

    @Test
    void obstructedDeploymentKeepsTheItemAndExistingBlocks() throws Exception {
        block(0, 61, 0).setType(Material.STONE, false);

        dispatchUse(interact(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND));

        assertEquals(2, itemAmount.get());
        assertEquals(Material.STONE, block(0, 61, 0).getType());
        assertTrue(expiry.isEmpty());
    }

    @Test
    void spectatorsAndRespawningPlayersCannotManuallyDeploy() throws Exception {
        when(arena.isSpectator(player)).thenReturn(true);
        dispatchUse(interact(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        when(arena.isSpectator(player)).thenReturn(false);
        when(arena.isReSpawning(player)).thenReturn(true);
        dispatchUse(interact(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));

        assertEquals(2, itemAmount.get());
        assertTrue(expiry.isEmpty());
    }

    @Test
    void automaticDeploymentStillWorksWhileFalling() {
        position.setY(-60.1);

        listener.onMove(new PlayerMoveEvent(player, new Location(world, 0.5, -59.8, 0.5), position.clone()));

        assertEquals(Material.SLIME_BLOCK, block(0, -64, 0).getType());
        assertEquals(1, itemAmount.get());
    }

    @Test
    void landingWithinTheSameBlockHeightClearsThePreviousFallWithoutAddingBounce() throws Exception {
        deploy();
        position.setY(62.2);

        listener.onMove(new PlayerMoveEvent(player, position.clone(), new Location(world, 0.5, 62, 0.5)));

        assertEquals(0.0F, fallDistance.get());
        verify(player, never()).setVelocity(any(Vector.class));
    }

    @Test
    void toweringOffThePlatformClearsStaleFallDistanceBeforeLandingOnWool() throws Exception {
        deploy();
        position.setY(62);
        block(0, 62, 0).setType(Material.WHITE_WOOL, false);

        listener.onMove(new PlayerMoveEvent(player, position.clone(), new Location(world, 0.5, 63.1, 0.5)));

        assertEquals(0.0F, fallDistance.get());
        verify(player, never()).setVelocity(any(Vector.class));
    }

    @Test
    void landingOnTheEdgeUsesThePlayersFeetInsteadOfOnlyTheCenterBlock() throws Exception {
        deploy();
        position = new Location(world, 2.1, 62.2, 0.5);

        listener.onMove(new PlayerMoveEvent(player, position.clone(), new Location(world, 2.1, 62, 0.5)));

        assertEquals(0.0F, fallDistance.get());
    }

    @Test
    void merelyPassingAboveOrBelowThePlatformDoesNotClearFallDistance() throws Exception {
        deploy();
        listener.onMove(new PlayerMoveEvent(player, new Location(world, 0.5, 64.2, 0.5), position.clone()));
        position.setY(61);
        listener.onMove(new PlayerMoveEvent(player, new Location(world, 0.5, 61.2, 0.5), position.clone()));

        assertEquals(60.0F, fallDistance.get());
    }

    @Test
    void platformLandingCancelsFallDamageButDoesNotCancelAttacks() throws Exception {
        deploy();
        position.setY(62);
        EntityDamageEvent fall = damage(EntityDamageEvent.DamageCause.FALL);
        EntityDamageEvent attack = damage(EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        listener.onFallDamage(fall);
        listener.onFallDamage(attack);

        assertTrue(fall.isCancelled());
        assertFalse(attack.isCancelled());
        assertEquals(0.0F, fallDistance.get());
    }

    @Test
    void ordinaryFallsOntoWoolAboveThePlatformStillDealDamage() throws Exception {
        deploy();
        block(0, 62, 0).setType(Material.WHITE_WOOL, false);
        position.setY(63);
        EntityDamageEvent fall = damage(EntityDamageEvent.DamageCause.FALL);

        listener.onFallDamage(fall);

        assertFalse(fall.isCancelled());
        assertEquals(60.0F, fallDistance.get());
    }

    @Test
    void expiredPlatformDoesNotGrantFallProtectionAndKeepsPlayerPlacedWool() throws Exception {
        deploy();
        block(0, 62, 0).setType(Material.WHITE_WOOL, false);
        expiry.getFirst().run();
        position.setY(62);
        EntityDamageEvent fall = damage(EntityDamageEvent.DamageCause.FALL);

        listener.onFallDamage(fall);

        assertFalse(fall.isCancelled());
        assertEquals(Material.AIR, block(0, 61, 0).getType());
        assertEquals(Material.WHITE_WOOL, block(0, 62, 0).getType());
    }

    @Test
    void gameEndRemovesThePlatformAndFallProtection() throws Exception {
        deploy();
        GameEndEvent end = mock(GameEndEvent.class);
        when(end.getArena()).thenReturn(arena);
        listener.onGameEnd(end);
        position.setY(62);
        EntityDamageEvent fall = damage(EntityDamageEvent.DamageCause.FALL);

        listener.onFallDamage(fall);

        assertFalse(fall.isCancelled());
        assertEquals(Material.AIR, block(0, 61, 0).getType());
    }

    private void deploy() throws Exception {
        dispatchUse(interact(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        assertEquals(Material.SLIME_BLOCK, block(0, 61, 0).getType());
    }

    private PlayerInteractEvent interact(Action action, EquipmentSlot hand) {
        Block clicked = action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK
                ? block(0, 63, 1) : null;
        return new PlayerInteractEvent(player, action, item, clicked, BlockFace.UP, hand);
    }

    private void dispatchUse(PlayerInteractEvent event) throws Exception {
        EventHandler handler = SelfRescuePlatform.class.getMethod("onUse", PlayerInteractEvent.class)
                .getAnnotation(EventHandler.class);
        RegisteredListener registered = new RegisteredListener(listener,
                (target, dispatched) -> listener.onUse((PlayerInteractEvent) dispatched),
                handler.priority(), mock(Plugin.class), handler.ignoreCancelled());
        registered.callEvent(event);
    }

    private EntityDamageEvent damage(EntityDamageEvent.DamageCause cause) {
        return new EntityDamageEvent(player, cause, mock(DamageSource.class), 10);
    }

    private Block block(int blockX, int blockY, int blockZ) {
        return blocks.computeIfAbsent(blockX + ":" + blockY + ":" + blockZ, key -> {
            Block block = mock(Block.class);
            AtomicReference<Material> material = new AtomicReference<>(Material.AIR);
            when(block.getWorld()).thenReturn(world);
            when(block.getX()).thenReturn(blockX);
            when(block.getY()).thenReturn(blockY);
            when(block.getZ()).thenReturn(blockZ);
            when(block.getType()).thenAnswer(invocation -> material.get());
            when(block.isEmpty()).thenAnswer(invocation -> material.get() == Material.AIR);
            doAnswer(invocation -> {
                material.set(invocation.getArgument(0));
                return null;
            }).when(block).setType(any(Material.class), eq(false));
            return block;
        });
    }
}
