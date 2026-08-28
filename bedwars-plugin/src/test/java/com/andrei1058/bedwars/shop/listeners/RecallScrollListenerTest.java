package com.andrei1058.bedwars.shop.listeners;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecallScrollListenerTest {

    @Test
    void waitsFiveSecondsBeforeConsumingTheScrollAndStartingTeleport() {
        RecallScrollCountdown countdown = new RecallScrollCountdown(5);
        AtomicInteger consumptions = new AtomicInteger();

        for (int second = 1; second < 5; second++) {
            assertEquals(RecallScrollListener.ChannelAdvance.WAITING,
                    RecallScrollListener.advanceChannel(countdown, () -> {
                        consumptions.incrementAndGet();
                        return true;
                    }));
        }

        assertEquals(0, consumptions.get());
        assertEquals(RecallScrollListener.ChannelAdvance.READY_TO_TELEPORT,
                RecallScrollListener.advanceChannel(countdown, () -> {
                    consumptions.incrementAndGet();
                    return true;
                }));
        assertEquals(1, consumptions.get());
    }

    @Test
    void doesNotStartTeleportWhenTheScrollIsGoneAtCompletion() {
        RecallScrollCountdown countdown = new RecallScrollCountdown(1);

        assertEquals(RecallScrollListener.ChannelAdvance.MISSING_SCROLL,
                RecallScrollListener.advanceChannel(countdown, () -> false));
    }

    @Test
    void consumesOneScrollFromItsCurrentStorageSlot() {
        TestItemStack unrelated = new TestItemStack(3);
        TestItemStack scroll = new TestItemStack(2);
        ItemStack[] storage = {unrelated, scroll};
        AtomicReference<ItemStack> offHand = new AtomicReference<>();

        assertTrue(RecallScrollListener.consumeRecallScroll(
                inventory(storage, offHand), item -> item == scroll));

        assertSame(unrelated, storage[0]);
        assertSame(scroll, storage[1]);
        assertEquals(1, scroll.getAmount());
        assertNull(offHand.get());
    }

    @Test
    void consumesTheOffHandScrollAfterItWasMovedDuringChanneling() {
        TestItemStack unrelated = new TestItemStack(1);
        TestItemStack scroll = new TestItemStack(1);
        ItemStack[] storage = {unrelated};
        AtomicReference<ItemStack> offHand = new AtomicReference<>(scroll);

        assertTrue(RecallScrollListener.consumeRecallScroll(
                inventory(storage, offHand), item -> item == scroll));

        assertSame(unrelated, storage[0]);
        assertNull(offHand.get());
    }

    @Test
    void writesBackAReducedStackWhenInventoryReturnsItemCopies() {
        TestItemStack scroll = new TestItemStack(2);
        ItemStack[] stored = {scroll};
        AtomicReference<ItemStack> offHand = new AtomicReference<>();

        assertTrue(RecallScrollListener.consumeRecallScroll(
                copyingInventory(stored, offHand), item -> item.getAmount() > 0));

        assertEquals(1, stored[0].getAmount());
    }

    @Test
    void refusesAFreeTeleportWhenNoScrollRemains() {
        ItemStack[] storage = {new TestItemStack(1)};
        AtomicReference<ItemStack> offHand = new AtomicReference<>();

        assertFalse(RecallScrollListener.consumeRecallScroll(
                inventory(storage, offHand), item -> false));
    }

    private static PlayerInventory inventory(ItemStack[] storage, AtomicReference<ItemStack> offHand) {
        return (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getStorageContents" -> storage;
                    case "setItem" -> {
                        storage[(int) args[0]] = (ItemStack) args[1];
                        yield null;
                    }
                    case "getItemInOffHand" -> offHand.get();
                    case "setItemInOffHand" -> {
                        offHand.set((ItemStack) args[0]);
                        yield null;
                    }
                    default -> null;
                });
    }

    private static PlayerInventory copyingInventory(ItemStack[] stored, AtomicReference<ItemStack> offHand) {
        return (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getStorageContents" -> new ItemStack[]{new TestItemStack(stored[0].getAmount())};
                    case "setItem" -> {
                        stored[0] = (ItemStack) args[1];
                        yield null;
                    }
                    case "getItemInOffHand" -> offHand.get() == null
                            ? null : new TestItemStack(offHand.get().getAmount());
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
}
