package com.andrei1058.bedwars.arena.spectator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeleporterGUITest {

    @Test
    void emptyTargetListDoesNotCreateAnInventory() {
        assertEquals(0, TeleporterGUI.inventorySizeFor(0));
        assertEquals(0, TeleporterGUI.inventorySizeFor(-1));
    }

    @Test
    void inventorySizeUsesValidNineSlotPagesAndCapsAtFiftyFour() {
        assertEquals(9, TeleporterGUI.inventorySizeFor(1));
        assertEquals(9, TeleporterGUI.inventorySizeFor(9));
        assertEquals(18, TeleporterGUI.inventorySizeFor(10));
        assertEquals(54, TeleporterGUI.inventorySizeFor(60));
    }
}
