/*
 * BedWars1058 - a Bed Wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.shop.main;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculations used by the shop's buy-to-limit path.
 */
final class PurchaseBatch {

    private PurchaseBatch() {
    }

    static int affordablePurchases(int availableCurrency, int unitPrice) {
        if (unitPrice <= 0) return availableCurrency >= unitPrice ? 1 : 0;
        if (availableCurrency < unitPrice) return 0;
        return availableCurrency / unitPrice;
    }

    static ItemStack[] createStacks(ItemStack template, int purchases) {
        if (template == null || template.getType() == Material.AIR || template.getAmount() <= 0 || purchases <= 0) {
            return new ItemStack[0];
        }

        int maxStackSize = Math.max(1, template.getType().getMaxStackSize());
        int[] amounts = stackAmounts(template.getAmount(), purchases, maxStackSize);
        List<ItemStack> stacks = new ArrayList<>(amounts.length);
        for (int amount : amounts) {
            ItemStack stack = template.clone();
            stack.setAmount(amount);
            stacks.add(stack);
        }
        return stacks.toArray(new ItemStack[0]);
    }

    static int[] stackAmounts(int itemAmount, int purchases, int maxStackSize) {
        if (itemAmount <= 0 || purchases <= 0 || maxStackSize <= 0) return new int[0];

        long total = (long) itemAmount * purchases;
        long stackCount = (total + maxStackSize - 1) / maxStackSize;
        if (stackCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Purchase result contains too many item stacks");
        }

        int[] amounts = new int[(int) stackCount];
        long remaining = total;
        for (int index = 0; index < amounts.length; index++) {
            amounts[index] = (int) Math.min(maxStackSize, remaining);
            remaining -= amounts[index];
        }
        return amounts;
    }
}
