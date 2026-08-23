package com.andrei1058.bedwars.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreakPlaceTest {

    @Test
    void npcProtectionIsSymmetricAroundCenteredCoordinates() {
        World arena = world("arena");
        Location npc = new Location(arena, 10.5, 64, 20.5);

        assertTrue(BreakPlace.isWithinNpcProtection(new Location(arena, 9, 64, 19), npc, 1));
        assertTrue(BreakPlace.isWithinNpcProtection(new Location(arena, 11, 64, 21), npc, 1));
        assertTrue(BreakPlace.isWithinNpcProtection(new Location(arena, 10, 63, 20), npc, 1));
        assertTrue(BreakPlace.isWithinNpcProtection(new Location(arena, 10, 66, 20), npc, 1));
    }

    @Test
    void blocksOutsideNpcAndOtherWorldsAreNotProtected() {
        World arena = world("arena");
        Location npc = new Location(arena, 10.5, 64, 20.5);

        assertFalse(BreakPlace.isWithinNpcProtection(new Location(arena, 8, 64, 20), npc, 1));
        assertFalse(BreakPlace.isWithinNpcProtection(new Location(arena, 10, 67, 20), npc, 1));
        assertFalse(BreakPlace.isWithinNpcProtection(new Location(world("lobby"), 10, 64, 20), npc, 1));
    }

    @Test
    void preventsSpreadWithoutBlockingFireballIgnition() {
        assertTrue(BreakPlace.isFireSpread(BlockIgniteEvent.IgniteCause.SPREAD));
        assertFalse(BreakPlace.isFireSpread(BlockIgniteEvent.IgniteCause.FIREBALL));
        assertFalse(BreakPlace.isFireSpread(BlockIgniteEvent.IgniteCause.EXPLOSION));
    }

    @Test
    void playerBlocksAlwaysTakePrecedenceOverMapProtection() {
        assertFalse(BreakPlace.isProtectedMapBlock(true, true, false));
        assertFalse(BreakPlace.isProtectedMapBlock(true, false, false));
        assertFalse(BreakPlace.isProtectedMapBlock(true, true, true));
    }

    @Test
    void originalMapBlocksStillRespectRegionsAndMapBreakRule() {
        assertTrue(BreakPlace.isProtectedMapBlock(false, true, true));
        assertTrue(BreakPlace.isProtectedMapBlock(false, false, false));
        assertFalse(BreakPlace.isProtectedMapBlock(false, false, true));
    }

    @Test
    void fireballsNeverDestroyOriginalMapBlocksOrTeamBeds() {
        assertTrue(BreakPlace.shouldProtectExplosionBlock(true, false, false));
        assertTrue(BreakPlace.shouldProtectExplosionBlock(true, true, true));
        assertFalse(BreakPlace.shouldProtectExplosionBlock(true, true, false));
        assertFalse(BreakPlace.shouldProtectExplosionBlock(false, false, false));
    }

    @Test
    void consumesTheTowerFromTheOffHandThatPlacedIt() {
        AtomicReference<ItemStack> mainHand = new AtomicReference<>(new TestItemStack(5));
        AtomicReference<ItemStack> offHand = new AtomicReference<>(new TestItemStack(2));

        BreakPlace.consumeTowerItem(inventory(mainHand, offHand), EquipmentSlot.OFF_HAND);

        assertEquals(5, mainHand.get().getAmount());
        assertEquals(1, offHand.get().getAmount());
    }

    @Test
    void clearsOnlyTheSingleTowerInThePlacementHand() {
        AtomicReference<ItemStack> mainHand = new AtomicReference<>(new TestItemStack(3));
        AtomicReference<ItemStack> offHand = new AtomicReference<>(new TestItemStack(1));

        BreakPlace.consumeTowerItem(inventory(mainHand, offHand), EquipmentSlot.OFF_HAND);

        assertEquals(3, mainHand.get().getAmount());
        assertTrue(offHand.get() == null);
    }

    @Test
    void consumesFromTheMainHandWhenThePlacementEventUsesTheMainHand() {
        AtomicReference<ItemStack> mainHand = new AtomicReference<>(new TestItemStack(1));
        AtomicReference<ItemStack> offHand = new AtomicReference<>(new TestItemStack(4));

        BreakPlace.consumeTowerItem(inventory(mainHand, offHand), EquipmentSlot.HAND);

        assertTrue(mainHand.get() == null);
        assertEquals(4, offHand.get().getAmount());
    }

    private static PlayerInventory inventory(AtomicReference<ItemStack> mainHand,
                                             AtomicReference<ItemStack> offHand) {
        return (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getItemInMainHand" -> mainHand.get();
                    case "getItemInOffHand" -> offHand.get();
                    case "setItemInMainHand" -> {
                        mainHand.set((ItemStack) args[0]);
                        yield null;
                    }
                    case "setItemInOffHand" -> {
                        offHand.set((ItemStack) args[0]);
                        yield null;
                    }
                    default -> null;
                });
    }

    private static final class TestItemStack extends ItemStack {
        private int amount;

        private TestItemStack(int amount) {
            this.amount = amount;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public void setAmount(int amount) {
            this.amount = amount;
        }
    }

    private static World world(String name) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "toString" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                });
    }
}
