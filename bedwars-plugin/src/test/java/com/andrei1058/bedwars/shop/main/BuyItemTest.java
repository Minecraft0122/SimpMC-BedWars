package com.andrei1058.bedwars.shop.main;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuyItemTest {

    @Test
    void fullArmorModeAllowsEveryArmorSlot() {
        assertTrue(BuyItem.shouldSellArmorPiece(Material.DIAMOND_HELMET, true));
        assertTrue(BuyItem.shouldSellArmorPiece(Material.DIAMOND_CHESTPLATE, true));
        assertTrue(BuyItem.shouldSellArmorPiece(Material.DIAMOND_LEGGINGS, true));
        assertTrue(BuyItem.shouldSellArmorPiece(Material.DIAMOND_BOOTS, true));
    }

    @Test
    void lowerBodyModeSkipsOnlyUpperBodyArmor() {
        assertFalse(BuyItem.shouldSellArmorPiece(Material.CHAINMAIL_HELMET, false));
        assertFalse(BuyItem.shouldSellArmorPiece(Material.IRON_CHESTPLATE, false));
        assertFalse(BuyItem.shouldSellArmorPiece(Material.ELYTRA, false));
        assertTrue(BuyItem.shouldSellArmorPiece(Material.IRON_LEGGINGS, false));
        assertTrue(BuyItem.shouldSellArmorPiece(Material.IRON_BOOTS, false));
    }
}
