/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena.feature;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public final class EnemyTrackerCompass implements Runnable {

    private static final String ITEM_DATA = "ENEMY_TRACKER_COMPASS";
    private static final int HOTBAR_SLOT = 8;
    private static final double IDLE_TARGET_RADIUS = 16.0;
    private static final double IDLE_SPIN_STEP = Math.PI / 5.0;

    private double idleAngle;

    public static void giveTo(Player player) {
        if (player == null) return;

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isTrackingCompass(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }

        ItemStack displaced = inventory.getItem(HOTBAR_SLOT);
        if (displaced != null && displaced.getType() != Material.AIR) {
            int emptySlot = inventory.firstEmpty();
            if (emptySlot == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), displaced);
            } else {
                inventory.setItem(emptySlot, displaced);
            }
        }

        ItemStack compass = BedWars.nms.addCustomData(new ItemStack(Material.COMPASS), ITEM_DATA);
        inventory.setItem(HOTBAR_SLOT, compass);
        player.updateInventory();
    }

    public static boolean isTrackingCompass(ItemStack item) {
        return item != null
                && item.getType() == Material.COMPASS
                && BedWars.nms != null
                && BedWars.nms.isCustomBedWarsItem(item)
                && ITEM_DATA.equals(BedWars.nms.getCustomData(item));
    }

    @Override
    public void run() {
        idleAngle += IDLE_SPIN_STEP;
        if (idleAngle >= Math.PI * 2.0) {
            idleAngle -= Math.PI * 2.0;
        }

        for (IArena arena : new ArrayList<>(Arena.getArenas())) {
            updateArena(arena);
        }
    }

    private void updateArena(IArena arena) {
        if (arena == null || arena.getStatus() != GameState.playing) return;

        List<Player> arenaPlayers = arena.getPlayers();
        if (arenaPlayers == null || arenaPlayers.isEmpty()) return;
        List<Player> players = new ArrayList<>(arenaPlayers);

        for (Player player : players) {
            if (!isActivePlayer(arena, player) || !hasTrackingCompass(player)) continue;

            ITeam playerTeam = arena.getTeam(player);
            if (playerTeam == null) continue;

            Player nearestEnemy = findNearestEnemy(arena, players, player, playerTeam);
            if (nearestEnemy == null) {
                player.setCompassTarget(createIdleTarget(player));
            } else {
                player.setCompassTarget(nearestEnemy.getLocation());
            }
        }
    }

    private Player findNearestEnemy(IArena arena, List<Player> players, Player player, ITeam playerTeam) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player candidate : players) {
            if (!isActivePlayer(arena, candidate) || candidate.getUniqueId().equals(player.getUniqueId())) continue;
            if (isInvisible(arena, candidate)) continue;

            ITeam candidateTeam = arena.getTeam(candidate);
            if (candidateTeam == null || candidateTeam == playerTeam) continue;

            double distance = player.getLocation().distanceSquared(candidate.getLocation());
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean isActivePlayer(IArena arena, Player player) {
        return player != null
                && player.isOnline()
                && !player.isDead()
                && !arena.isSpectator(player)
                && !arena.isReSpawning(player)
                && arena.getWorld() != null
                && arena.getWorld().equals(player.getWorld());
    }

    private boolean isInvisible(IArena arena, Player player) {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                || (arena.getShowTime() != null && arena.getShowTime().containsKey(player));
    }

    private boolean hasTrackingCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTrackingCompass(item)) return true;
        }
        return false;
    }

    private Location createIdleTarget(Player player) {
        return player.getLocation().clone().add(
                Math.cos(idleAngle) * IDLE_TARGET_RADIUS,
                0,
                Math.sin(idleAngle) * IDLE_TARGET_RADIUS
        );
    }
}
