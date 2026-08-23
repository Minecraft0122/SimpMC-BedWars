/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.api.util.AdventureText;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GUI used by {@code /bw team} to choose a configured game team. */
public final class PreGameTeamSelectionGUI implements Listener {

    private static final PreGameTeamSelectionGUI INSTANCE = new PreGameTeamSelectionGUI();
    private static final String PREFIX = ChatColor.GOLD + "[选队] " + ChatColor.RESET;
    private static final int INVENTORY_SIZE = 54;
    private static final int CLEAR_SLOT = 49;
    private final PreGameTeamSelectionManager selections = PreGameTeamSelectionManager.getInstance();

    private PreGameTeamSelectionGUI() {
    }

    public static PreGameTeamSelectionGUI getInstance() {
        return INSTANCE;
    }

    public void open(@NotNull Player player) {
        IArena arena = preGameArena(player);
        if (arena == null) {
            AdventureText.send(player, PREFIX + ChatColor.RED + "只能在开局前选择队伍。");
            return;
        }

        TeamSelectionHolder holder = new TeamSelectionHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, AdventureText.section(ChatColor.DARK_GRAY + "选择游戏队伍"));
        holder.attach(inventory);

        ITeam current = selections.getSelection(arena, player);
        int slot = 0;
        for (ITeam team : arena.getTeams()) {
            if (slot >= 45) break;
            inventory.setItem(slot, teamItem(team, team.equals(current)));
            holder.teams.put(slot, team.getIdentity());
            slot++;
        }
        inventory.setItem(CLEAR_SLOT, namedItem(Material.BARRIER, ChatColor.RED + "取消选择",
                List.of(ChatColor.GRAY + "开局时重新参与自动均衡分队")));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof TeamSelectionHolder holder)) return;
        event.setCancelled(true);
        if (!holder.viewer.equals(player.getUniqueId())) return;

        IArena arena = preGameArena(player);
        if (arena == null) {
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() == CLEAR_SLOT) {
            selections.clear(player);
            AdventureText.send(player, PREFIX + ChatColor.YELLOW + "已取消队伍选择，开局时将自动均衡分队。");
            open(player);
            return;
        }

        UUID teamIdentity = holder.teams.get(event.getRawSlot());
        if (teamIdentity == null) return;
        ITeam team = arena.getTeams().stream()
                .filter(candidate -> candidate.getIdentity().equals(teamIdentity))
                .findFirst()
                .orElse(null);
        if (team == null) return;

        PreGameTeamSelectionManager.Result result = selections.select(player, team);
        if (result == PreGameTeamSelectionManager.Result.TEAM_FULL) {
            AdventureText.send(player, PREFIX + ChatColor.RED + "该队伍的预选人数已达到每队上限。");
            return;
        }
        if (result != PreGameTeamSelectionManager.Result.SELECTED) {
            AdventureText.send(player, PREFIX + ChatColor.RED + "当前无法选择该队伍。");
            return;
        }

        AdventureText.send(player, PREFIX + team.getColor().chat() + "已选择 " + team.getName()
                + ChatColor.GRAY + "（固定队友会一起选择；若组合无法合法分队，系统将自动均衡）");
        open(player);
    }

    private ItemStack teamItem(ITeam team, boolean selected) {
        ItemStack item = new ItemStack(team.getColor().woolMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        AdventureText.displayName(meta, team.getColor().chat() + team.getName());
        AdventureText.lore(meta, List.of(
                ChatColor.GRAY + "已预选：" + ChatColor.WHITE + selections.selectedCount(team)
                        + ChatColor.GRAY + "/" + ChatColor.WHITE + team.getArena().getMaxInTeam(),
                selected ? ChatColor.GREEN + "已选择" : ChatColor.YELLOW + "点击选择"
        ));
        if (selected) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        AdventureText.displayName(meta, name);
        AdventureText.lore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    private static IArena preGameArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.isPlayer(player)) return null;
        return arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting ? arena : null;
    }

    private static final class TeamSelectionHolder implements InventoryHolder {
        private final UUID viewer;
        private final Map<Integer, UUID> teams = new HashMap<>();
        private Inventory inventory;

        private TeamSelectionHolder(UUID viewer) {
            this.viewer = viewer;
        }

        private void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
