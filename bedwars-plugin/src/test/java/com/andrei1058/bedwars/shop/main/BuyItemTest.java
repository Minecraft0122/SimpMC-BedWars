package com.andrei1058.bedwars.shop.main;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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

    @Test
    void overflowingPurchaseIsDroppedInsteadOfLost() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        World world = mock(World.class);
        Location location = mock(Location.class);
        ItemStack potion = mock(ItemStack.class);
        when(potion.getType()).thenReturn(Material.POTION);
        when(potion.getAmount()).thenReturn(1);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        HashMap<Integer, ItemStack> overflow = new HashMap<>();
        overflow.put(0, potion);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(overflow);

        BuyItem.dropOverflow(player, inventory.addItem(new ItemStack[]{potion}));

        verify(world).dropItemNaturally(location, potion);
    }
}
