package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.server.VersionSupport;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class InteractCommandItemTest {
    private final List<Runnable> nextTick = new ArrayList<>();
    private MainConfig previousConfig;
    private BedWars previousPlugin;
    private VersionSupport previousSupport;
    private MockedStatic<Bukkit> bukkit;
    private Interact listener;
    private Player player;
    private ItemStack bed;

    @BeforeEach
    void setUp() {
        previousConfig = BedWars.config;
        previousPlugin = BedWars.plugin;
        previousSupport = BedWars.nms;
        BedWars.config = mock(MainConfig.class);
        BedWars.plugin = mock(BedWars.class);
        BedWars.nms = mock(VersionSupport.class);
        when(BedWars.config.getYml()).thenReturn(new YamlConfiguration());
        player = mock(Player.class);
        bed = mock(ItemStack.class);
        when(bed.getType()).thenReturn(Material.RED_BED);
        when(BedWars.nms.getItemInHand(player)).thenReturn(bed);
        when(BedWars.nms.isCustomBedWarsItem(bed)).thenReturn(true);
        when(BedWars.nms.getCustomData(bed)).thenReturn("RUNCOMMAND_ bw leave ");
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(BedWars.plugin), any(Runnable.class))).thenAnswer(invocation -> {
            nextTick.add(invocation.getArgument(1));
            return mock(BukkitTask.class);
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        listener = new Interact();
    }

    @AfterEach
    void tearDown() {
        if (bukkit != null) bukkit.close();
        BedWars.config = previousConfig;
        BedWars.plugin = previousPlugin;
        BedWars.nms = previousSupport;
    }

    @ParameterizedTest
    @EnumSource(value = Action.class, names = {"RIGHT_CLICK_AIR", "RIGHT_CLICK_BLOCK"})
    void cancelledRedBedInteractionStillDispatchesLeaveOnTheNextTick(Action action) throws Exception {
        PlayerInteractEvent event = interact(action, EquipmentSlot.HAND);
        event.setCancelled(true);

        dispatch(event);

        assertTrue(event.isCancelled());
        assertEquals(1, nextTick.size());
        bukkit.verify(() -> Bukkit.dispatchCommand(player, "bw leave"), never());

        nextTick.getFirst().run();

        bukkit.verify(() -> Bukkit.dispatchCommand(player, "bw leave"));
    }

    @Test
    void offHandMirrorDoesNotDispatchTheReturnCommandTwice() throws Exception {
        dispatch(interact(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        dispatch(interact(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND));

        assertEquals(1, nextTick.size());
        nextTick.getFirst().run();
        bukkit.verify(() -> Bukkit.dispatchCommand(player, "bw leave"));
    }

    private PlayerInteractEvent interact(Action action, EquipmentSlot hand) {
        Block clicked = action == Action.RIGHT_CLICK_BLOCK ? mock(Block.class) : null;
        return new PlayerInteractEvent(player, action, bed, clicked, BlockFace.UP, hand);
    }

    private void dispatch(PlayerInteractEvent event) throws Exception {
        EventHandler handler = Interact.class.getMethod("onItemCommand", PlayerInteractEvent.class)
                .getAnnotation(EventHandler.class);
        RegisteredListener registered = new RegisteredListener(listener,
                (target, dispatched) -> listener.onItemCommand((PlayerInteractEvent) dispatched),
                handler.priority(), BedWars.plugin, handler.ignoreCancelled());
        registered.callEvent(event);
    }
}
