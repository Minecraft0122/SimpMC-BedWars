package com.andrei1058.bedwars.shop.main;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuyItemTest {

    @Test
    void shopAlwaysRejectsUpperBodyArmor() {
        assertFalse(BuyItem.shouldSellArmorPiece(Material.CHAINMAIL_HELMET));
        assertFalse(BuyItem.shouldSellArmorPiece(Material.IRON_CHESTPLATE));
        assertFalse(BuyItem.shouldSellArmorPiece(Material.ELYTRA));
    }

    @Test
    void shopKeepsLeggingsAndBootsPurchasable() {
        assertTrue(BuyItem.shouldSellArmorPiece(Material.IRON_LEGGINGS));
        assertTrue(BuyItem.shouldSellArmorPiece(Material.DIAMOND_BOOTS));
        assertTrue(BuyItem.shouldSellArmorPiece(Material.FIRE_CHARGE));
    }
}
