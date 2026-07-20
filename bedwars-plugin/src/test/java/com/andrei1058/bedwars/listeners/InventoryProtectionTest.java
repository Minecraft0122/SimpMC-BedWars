package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryProtectionTest {

    @Test
    void commandItemsAreEditableOnlyByLobbyOperators() {
        assertFalse(Inventory.shouldProtectCommandItems(true, true));
        assertTrue(Inventory.shouldProtectCommandItems(false, true));
        assertTrue(Inventory.shouldProtectCommandItems(true, false));
        assertTrue(Inventory.shouldProtectCommandItems(false, false));
    }
}
