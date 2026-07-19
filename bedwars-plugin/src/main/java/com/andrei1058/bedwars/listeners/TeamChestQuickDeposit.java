/*
 * BedWars1058 - a Bed Wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.listeners;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Calculates the result of Bukkit's inventory insertion without coupling the
 * player interaction listener to double-chest and partial-stack details.
 */
final class TeamChestQuickDeposit {

    private TeamChestQuickDeposit() {
    }

    static int transferredAmount(int offeredAmount, Map<Integer, ItemStack> leftovers) {
        if (offeredAmount <= 0) return 0;

        long remaining = 0;
        if (leftovers != null) {
            for (ItemStack item : leftovers.values()) {
                if (item != null && item.getAmount() > 0) remaining += item.getAmount();
            }
        }
        return transferredAmount(offeredAmount, remaining);
    }

    static int transferredAmount(int offeredAmount, long leftoverAmount) {
        if (offeredAmount <= 0) return 0;
        return (int) Math.max(0L, Math.min((long) offeredAmount, offeredAmount - leftoverAmount));
    }
}
