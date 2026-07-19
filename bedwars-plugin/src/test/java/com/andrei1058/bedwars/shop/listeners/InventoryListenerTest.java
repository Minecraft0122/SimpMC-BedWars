package com.andrei1058.bedwars.shop.listeners;

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
}
