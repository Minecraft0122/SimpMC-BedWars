package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OreGeneratorDropPolicyTest {

    @Test
    void stackedDropsUseTheMinimumNumberOfItemEntities() {
        assertEquals(0, OreGenerator.dropEntityCount(0, 64, true));
        assertEquals(1, OreGenerator.dropEntityCount(2, 64, true));
        assertEquals(1, OreGenerator.dropEntityCount(64, 64, true));
        assertEquals(2, OreGenerator.dropEntityCount(65, 64, true));
    }

    @Test
    void unstackedDropsKeepOneEntityPerConfiguredItem() {
        assertEquals(65, OreGenerator.dropEntityCount(65, 64, false));
    }
}
