package com.andrei1058.bedwars.arena.team;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedWarsTeamArmorTest {

    @Test
    void defaultLoadoutContainsTheFullLeatherSetInBukkitSlotOrder() {
        assertEquals(List.of(
                Material.LEATHER_BOOTS,
                Material.LEATHER_LEGGINGS,
                Material.LEATHER_CHESTPLATE,
                Material.LEATHER_HELMET
        ), BedWarsTeam.defaultArmorMaterials());
    }

    @Test
    void emptyAndAirArmorSlotsAreRefilled() {
        assertTrue(BedWarsTeam.isEmptyArmorMaterial(null));
        assertTrue(BedWarsTeam.isEmptyArmorMaterial(Material.AIR));
        assertFalse(BedWarsTeam.isEmptyArmorMaterial(Material.LEATHER_BOOTS));
    }
}
