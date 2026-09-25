package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.server.VersionSupport;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
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

class ShearsCooldownTest {
    private VersionSupport previousSupport;
    private BreakPlace listener;
    private Player player;
    private IArena arena;
    private Block block;
    private ItemStack heldItem;

    @BeforeEach
    void setUp() {
        previousSupport = BedWars.nms;
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
        Arena.getArenaByPlayer().put(player, arena);
    }

    @AfterEach
    void tearDown() {
        Arena.getArenaByPlayer().remove(player);
        BedWars.nms = previousSupport;
    }

    @Test
    void successfulWoolBreakAppliesQuarterSecondShearsCooldown() throws Exception {
        dispatch(new BlockBreakEvent(block, player));

        verify(player).setCooldown(Material.SHEARS, 5);
    }

    @Test
    void cancelledWoolBreakDoesNotConsumeTheShearsCooldown() throws Exception {
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        event.setCancelled(true);

        dispatch(event);

        verifyNoCooldown();
    }

    @Test
    void otherToolsDoNotReceiveTheShearsCooldown() throws Exception {
        when(heldItem.getType()).thenReturn(Material.WOODEN_AXE);

        dispatch(new BlockBreakEvent(block, player));

        verifyNoCooldown();
    }

    @Test
    void otherBlocksDoNotReceiveTheShearsCooldown() throws Exception {
        when(block.getType()).thenReturn(Material.WHITE_CARPET);

        dispatch(new BlockBreakEvent(block, player));

        verifyNoCooldown();
    }

    @ParameterizedTest
    @EnumSource(value = GameState.class, names = "playing", mode = EnumSource.Mode.EXCLUDE)
    void inactiveArenaDoesNotApplyTheShearsCooldown(GameState state) throws Exception {
        when(arena.getStatus()).thenReturn(state);

        dispatch(new BlockBreakEvent(block, player));

        verifyNoCooldown();
    }

    @Test
    void spectatorsDoNotReceiveTheShearsCooldown() throws Exception {
        when(arena.isSpectator(player)).thenReturn(true);

        dispatch(new BlockBreakEvent(block, player));

        verifyNoCooldown();
    }

    @Test
    void respawningPlayersDoNotReceiveTheShearsCooldown() throws Exception {
        arena.getRespawnSessions().put(player, 3);

        dispatch(new BlockBreakEvent(block, player));

        verifyNoCooldown();
    }

    @Test
    void playersOutsideAnArenaDoNotReceiveTheShearsCooldown() throws Exception {
        Arena.getArenaByPlayer().remove(player);

        dispatch(new BlockBreakEvent(block, player));

        verifyNoCooldown();
    }

    private void verifyNoCooldown() {
        verify(player, never()).setCooldown(any(Material.class), anyInt());
    }

    private void dispatch(BlockBreakEvent event) throws Exception {
        EventHandler handler = BreakPlace.class.getMethod("onShearsBlockBreak", BlockBreakEvent.class)
                .getAnnotation(EventHandler.class);
        RegisteredListener registered = new RegisteredListener(listener,
                (target, dispatched) -> listener.onShearsBlockBreak((BlockBreakEvent) dispatched),
                handler.priority(), mock(Plugin.class), handler.ignoreCancelled());
        registered.callEvent(event);
    }
}
