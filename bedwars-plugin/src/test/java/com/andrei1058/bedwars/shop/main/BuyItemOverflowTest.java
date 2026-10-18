package com.andrei1058.bedwars.shop.main;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BuyItemOverflowTest {

    @Test
    public void dropsEveryValidOverflowItem() {
        Map<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, new ItemStack(Material.POTION, 1));
        leftovers.put(1, new ItemStack(Material.IRON_INGOT, 3));
        List<ItemStack> dropped = new java.util.ArrayList<>();

        BuyItem.dropOverflow(leftovers, dropped::add);

        assertEquals(2, dropped.size());
        assertEquals(Material.POTION, dropped.get(0).getType());
        assertEquals(3, dropped.get(1).getAmount());
    }

    @Test
    public void ignoresInvalidOverflowEntries() {
        Map<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, null);
        leftovers.put(1, new ItemStack(Material.AIR));
        leftovers.put(2, new ItemStack(Material.IRON_INGOT, 0));
        List<ItemStack> dropped = new java.util.ArrayList<>();

        BuyItem.dropOverflow(leftovers, dropped::add);

        assertEquals(0, dropped.size());
    }

    @Test
    public void handlesNullOverflowMap() {
        BuyItem.dropOverflow(null, item -> {
            throw new AssertionError("No item should be dropped");
        });
    }
}
