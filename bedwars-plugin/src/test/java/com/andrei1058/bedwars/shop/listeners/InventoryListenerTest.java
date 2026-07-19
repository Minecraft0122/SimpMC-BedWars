package com.andrei1058.bedwars.shop.listeners;

import org.bukkit.event.inventory.ClickType;
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
}
