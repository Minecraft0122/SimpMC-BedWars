package com.andrei1058.bedwars.shop.listeners;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryListenerTest {

    @Test
    void detectsDragIntoTopContainer() {
        assertTrue(InventoryListener.touchesTopInventory(Set.of(2, 30), 27));
    }

    @Test
    void permitsDragWithinPlayerInventory() {
        assertFalse(InventoryListener.touchesTopInventory(Set.of(27, 35, 44), 27));
        assertFalse(InventoryListener.touchesTopInventory(Set.of(-1), 27));
    }

    @Test
    void reservesOnlyShiftRightForBulkPurchase() {
        assertTrue(InventoryListener.isBulkPurchaseClick(ClickType.SHIFT_RIGHT));
        assertFalse(InventoryListener.isBulkPurchaseClick(ClickType.SHIFT_LEFT));
        assertFalse(InventoryListener.isBulkPurchaseClick(ClickType.RIGHT));
    }

    @Test
    void allowsOrdinarySortingInsidePlayerInventory() {
        assertFalse(InventoryListener.shouldCancelPlayerInventoryAction(InventoryAction.PICKUP_ALL));
        assertFalse(InventoryListener.shouldCancelPlayerInventoryAction(InventoryAction.PLACE_ALL));
        assertFalse(InventoryListener.shouldCancelPlayerInventoryAction(InventoryAction.HOTBAR_SWAP));
        assertFalse(InventoryListener.shouldCancelPlayerInventoryAction(InventoryAction.DROP_ONE_SLOT));
    }

    @Test
    void blocksActionsThatCanCrossIntoTheShopInventory() {
        assertTrue(InventoryListener.shouldCancelPlayerInventoryAction(
                InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertTrue(InventoryListener.shouldCancelPlayerInventoryAction(
                InventoryAction.COLLECT_TO_CURSOR));
    }
}
