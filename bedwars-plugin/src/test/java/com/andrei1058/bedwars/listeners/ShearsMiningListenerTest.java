package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.configuration.MainConfig;
import com.andrei1058.bedwars.api.server.VersionSupport;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.support.version.common.ShearsMining;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.mockito.MockedStatic;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class ShearsMiningListenerTest {
    private VersionSupport previousSupport;
    private MainConfig previousConfig;
    private BreakPlace listener;
    private Player player;
    private IArena arena;
    private Block block;
    private ItemStack heldItem;
    private PlayerInventory inventory;
    private MockedStatic<ShearsMining> mining;

    @BeforeEach
    void setUp() {
        previousSupport = BedWars.nms;
        previousConfig = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        BedWars.nms = mock(VersionSupport.class);
        listener = mock(BreakPlace.class, CALLS_REAL_METHODS);
        player = mock(Player.class);
        arena = mock(IArena.class);
        block = mock(Block.class);
        heldItem = mock(ItemStack.class);
        when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.isPlayer(player)).thenReturn(true);
        when(arena.getRespawnSessions()).thenReturn(new ConcurrentHashMap<>());
        when(block.getType()).thenReturn(Material.WHITE_WOOL);
        when(heldItem.getType()).thenReturn(Material.SHEARS);
        when(BedWars.nms.getItemInHand(player)).thenReturn(heldItem);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        mining = mockStatic(ShearsMining.class);
        mining.when(() -> ShearsMining.apply(heldItem)).thenAnswer(call -> heldItem.getType() == Material.SHEARS);
        Arena.getArenaByPlayer().put(player, arena);
    }

    @AfterEach
    void tearDown() {
        Arena.getArenaByPlayer().remove(player);
        BedWars.nms = previousSupport;
        BedWars.config = previousConfig;
        mining.close();
    }

    @Test
    void startingWoolMiningUpdatesNativeToolWithoutPostBreakCooldown() throws Exception {
        dispatch(damageEvent());
        verify(inventory).setItemInMainHand(heldItem);
        verify(player, never()).setCooldown(any(Material.class), anyInt());
    }

    @Test
    void cancelledWoolBreakDoesNotConsumeTheShearsCooldown() throws Exception {
        BlockDamageEvent event = damageEvent();
        event.setCancelled(true);

        dispatch(event);

        verifyNoCooldown();
    }

    @Test
    void otherToolsDoNotReceiveTheShearsCooldown() throws Exception {
        when(heldItem.getType()).thenReturn(Material.WOODEN_AXE);

        dispatch(damageEvent());

        verifyNoCooldown();
    }

    @Test
    void otherBlocksDoNotReceiveTheShearsCooldown() throws Exception {
        when(block.getType()).thenReturn(Material.WHITE_CARPET);

        dispatch(damageEvent());

        verifyNoCooldown();
    }

    @ParameterizedTest
    @EnumSource(value = GameState.class, names = "playing", mode = EnumSource.Mode.EXCLUDE)
    void inactiveArenaDoesNotApplyTheShearsCooldown(GameState state) throws Exception {
        when(arena.getStatus()).thenReturn(state);

        dispatch(damageEvent());

        verifyNoCooldown();
    }

    @Test
    void spectatorsDoNotReceiveTheShearsCooldown() throws Exception {
        when(arena.isSpectator(player)).thenReturn(true);

        dispatch(damageEvent());

        verifyNoCooldown();
    }

    @Test
    void respawningPlayersDoNotReceiveTheShearsCooldown() throws Exception {
        arena.getRespawnSessions().put(player, 3);

        dispatch(damageEvent());

        verifyNoCooldown();
    }

    @Test
    void playersOutsideAnArenaDoNotReceiveTheShearsCooldown() throws Exception {
        Arena.getArenaByPlayer().remove(player);

        dispatch(damageEvent());

        verifyNoCooldown();
    }

    private void verifyNoCooldown() {
        verify(player, never()).setCooldown(any(Material.class), anyInt());
        verify(inventory, never()).setItemInMainHand(any());
    }

    private BlockDamageEvent damageEvent() {
        return new BlockDamageEvent(player, block, BlockFace.UP, heldItem, false);
    }

    private void dispatch(BlockDamageEvent event) throws Exception {
        EventHandler handler = BreakPlace.class.getMethod("onShearsBlockDamage", BlockDamageEvent.class)
                .getAnnotation(EventHandler.class);
        RegisteredListener registered = new RegisteredListener(listener,
                (target, dispatched) -> listener.onShearsBlockDamage((BlockDamageEvent) dispatched),
                handler.priority(), mock(Plugin.class), handler.ignoreCancelled());
        registered.callEvent(event);
    }
}
