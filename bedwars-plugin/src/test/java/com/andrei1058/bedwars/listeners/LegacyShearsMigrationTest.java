package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyShearsMigrationTest {
    @Test
    void ignoresEmptyItemsWithoutTouchingTheirComponents() {
        assertFalse(LegacyShearsMigration.restore(null));
    }
}
