package com.andrei1058.bedwars.support.version.v1_21_R3;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipmentVisibilityTest {

    @Test
    void mapsBothHandsAndAllFourArmorSlots() {
        assertEquals(List.of(
                EquipmentSlot.HAND,
                EquipmentSlot.OFF_HAND,
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        ), v1_21_R3.equipmentSlots());
    }
}
