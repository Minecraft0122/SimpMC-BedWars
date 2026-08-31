/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.util.AdventureText;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.configuration.Sounds;
import com.andrei1058.bedwars.listeners.arenaselector.ArenaSelectorListener;
import com.andrei1058.bedwars.lobbysocket.ArenaNodeSnapshot;
import com.andrei1058.bedwars.lobbysocket.LobbyArenaDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ArenaGUI {

    //Object[0] = inventory, Object[1] = group
    //private static HashMap<Player, Object[]> refresh = new HashMap<>();
    private static YamlConfiguration yml = BedWars.config.getYml();

    public static final String PAGE_GUI_IDENTIFIER = "arena-page=";

    private static final Map<UUID, Long> antiCalledTwice = new HashMap<>();

    //Object[0] = inventory, Object[1] = group
    public static void refreshInv(Player p, IArena arena, int players) {
        if (p == null) return;
        if (p.getOpenInventory() == null) return;
        if (!(p.getOpenInventory().getTopInventory().getHolder() instanceof ArenaSelectorHolder)) return;
        ArenaSelectorHolder ash = ((ArenaSelectorHolder) p.getOpenInventory().getTopInventory().getHolder());

        if (BedWars.isBungeeLobby()) {
            refreshRemoteInv(p, ash);
            return;
        }

        List<IArena> arenas = availableArenas(ash.getGroup());

        Inventory inventory = p.getOpenInventory().getTopInventory();
        List<Integer> usedSlots = ArenaSelectorPagination.contentSlots(
                BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS),
                inventory.getSize());
        int page = ArenaSelectorPagination.clampPage(ash.getPage(), arenas.size(), usedSlots.size());
        ash.setPage(page);
        int arenaKey = page * usedSlots.size();
        for (Integer slot : usedSlots) {
            ItemStack i;
            inventory.setItem(slot, new ItemStack(Material.AIR));
            if (arenaKey >= arenas.size()) {
                continue;
            }

            String status;
            switch (arenas.get(arenaKey).getStatus()) {
                case waiting:
                    status = "waiting";
                    break;
                case playing:
                    status = "playing";
                    break;
                case starting:
                    status = "starting";
                    break;
                default:
                    continue;
            }

            i = BedWars.nms.createItemStack(yml.getString(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_MATERIAL.replace("%path%", status)),
                    1, (short) yml.getInt(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_DATA.replace("%path%", status)));
            if (yml.getBoolean(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_ENCHANTED.replace("%path%", status))) {
                ItemMeta im = i.getItemMeta();
                im.addEnchant(Enchantment.LURE, 1, true);
                im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                i.setItemMeta(im);
            }


            ItemMeta im = i.getItemMeta();
            AdventureText.displayName(im, Language.getMsg(p, Messages.ARENA_GUI_ARENA_CONTENT_NAME).replace("{name}", arenas.get(arenaKey).getDisplayName()).replace("{map_name}", arenas.get(arenaKey).getArenaName()));
            List<String> lore = new ArrayList<>();
            for (String s : Language.getList(p, Messages.ARENA_GUI_ARENA_CONTENT_LORE)) {
                if (!(s.contains("{group}") && arenas.get(arenaKey).getGroup().equalsIgnoreCase("default"))) {
                    lore.add(s.replace("{on}", String.valueOf(arena != null ? arena == arenas.get(arenaKey) ? players : arenas.get(arenaKey).getPlayers().size() : arenas.get(arenaKey).getPlayers().size())).replace("{max}",
                                    String.valueOf(arenas.get(arenaKey).getMaxPlayers())).replace("{status}", arenas.get(arenaKey).getDisplayStatus(Language.getPlayerLanguage(p)))
                            .replace("{group}", arenas.get(arenaKey).getDisplayGroup(p)));
                }
            }
            AdventureText.lore(im, lore);
            i.setItemMeta(im);
            i = BedWars.nms.addCustomData(i, ArenaSelectorListener.ARENA_SELECTOR_GUI_IDENTIFIER + arenas.get(arenaKey).getArenaName());
            inventory.setItem(slot, i);
            arenaKey++;
        }
        renderPagination(ash, inventory, page,
                ArenaSelectorPagination.pageCount(arenas.size(), usedSlots.size()));
        p.updateInventory();
    }

    public static void openGui(Player p, String group) {
        openGui(p, group, 0, true);
    }

    /** Open another selector page without triggering the command double-call guard. */
    public static void openPage(Player player, String group, int page) {
        openGui(player, group, page, false);
    }

    private static void openGui(Player p, String group, int page, boolean guardDoubleCall) {
        if (guardDoubleCall && preventCalledTwice(p)) return;
        if (guardDoubleCall) updateCalledTwice(p);
        String configuredSlots = BedWars.config.getString(
                ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS);
        int size = ArenaSelectorPagination.effectiveSize(BedWars.config.getYml()
                        .getInt(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE),
                configuredSlots, selectorEntryCount(group));
        ArenaSelectorHolder ash = new ArenaSelectorHolder(group, Math.max(0, page));
        Inventory inv = Bukkit.createInventory(ash, size, AdventureText.section(Language.getMsg(p, Messages.ARENA_GUI_INV_NAME)));
        ash.attach(inv);

        String skippedSlotMaterial = BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_MATERIAL.replace("%path%", "skipped-slot"));
        if (!skippedSlotMaterial.equalsIgnoreCase("none") && !skippedSlotMaterial.equalsIgnoreCase("air")) {
            ItemStack i = BedWars.nms.createItemStack(skippedSlotMaterial,
                    1, (byte) BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_DATA.replace("%path%", "skipped-slot")));
            i = BedWars.nms.addCustomData(i, "RUNCOMMAND_bw join random");
            ItemMeta im = i.getItemMeta();
            assert im != null;
            im.displayName(AdventureText.ampersand(
                    Language.getMsg(p, Messages.ARENA_GUI_SKIPPED_ITEM_NAME)
                            .replaceAll(
                                    "\\{serverIp}",
                                    BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP)
                            )
                            .replaceAll(
                                    "\\{poweredBy}",
                                    BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_POWERED_BY)
                            )
                    ));
            List<String> lore = new ArrayList<>();
            for (String line : Language.getList(p, Messages.ARENA_GUI_SKIPPED_ITEM_LORE)) {
                line = line
                        .replaceAll(
                                "\\{serverIp}",
                                BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP)
                        )
                        .replaceAll(
                                "\\{poweredBy}",
                                BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_POWERED_BY)
                        );
                lore.add(line);
            }
            if (lore.size() > 0) {
                AdventureText.lore(im, lore);
            }
            im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            i.setItemMeta(im);

            List<Integer> used = ArenaSelectorPagination.contentSlots(
                    BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS),
                    inv.getSize());
            for (int x = 0; x < inv.getSize(); x++) {
                if (used.contains(x)) continue;
                inv.setItem(x, i);
            }
        }

        p.openInventory(inv);
        refreshInv(p, null, 0);
        //refresh.put(p, new Object[]{inv, group});
        Sounds.playSound("arena-selector-open", p);
    }

    /** Render the same selector layout from the authenticated lobby directory. */
    private static void refreshRemoteInv(Player player, ArenaSelectorHolder holder) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        List<Integer> usedSlots = ArenaSelectorPagination.contentSlots(
                BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS),
                inventory.getSize());
        List<ArenaNodeSnapshot> arenas = availableRemoteArenas(holder.getGroup());
        int page = ArenaSelectorPagination.clampPage(holder.getPage(), arenas.size(), usedSlots.size());
        holder.setPage(page);
        holder.clearRemoteArenas();

        int arenaKey = page * usedSlots.size();
        for (Integer slot : usedSlots) {
            inventory.setItem(slot, new ItemStack(Material.AIR));
            if (arenaKey >= arenas.size()) continue;

            ArenaNodeSnapshot snapshot = arenas.get(arenaKey++);
            ItemStack item = remoteStatusItem(snapshot);
            if (item == null) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            String displayName = remoteDisplayName(snapshot.arenaName());
            AdventureText.displayName(meta, Language.getMsg(player, Messages.ARENA_GUI_ARENA_CONTENT_NAME)
                    .replace("{name}", displayName)
                    .replace("{map_name}", snapshot.arenaName()));
            List<String> lore = new ArrayList<>();
            String status = remoteDisplayStatus(player, snapshot);
            String groups = remoteDisplayGroups(snapshot);
            boolean defaultOnly = snapshot.groups().size() == 1 && snapshot.groups().contains("DEFAULT");
            for (String line : Language.getList(player, Messages.ARENA_GUI_ARENA_CONTENT_LORE)) {
                if (line.contains("{group}") && defaultOnly) continue;
                lore.add(line.replace("{on}", String.valueOf(snapshot.currentPlayers()))
                        .replace("{max}", String.valueOf(snapshot.maxPlayers()))
                        .replace("{status}", status)
                        .replace("{group}", groups));
            }
            AdventureText.lore(meta, lore);
            item.setItemMeta(meta);
            item = BedWars.nms.addCustomData(item,
                    ArenaSelectorListener.REMOTE_ARENA_SELECTOR_IDENTIFIER + slot);
            inventory.setItem(slot, item);
            holder.setRemoteArena(slot, snapshot);
        }

        // A disconnected/starting network should never present a completely
        // empty menu. Keep one explicit command fallback in the content area.
        if (arenas.isEmpty() && !usedSlots.isEmpty()) {
            int slot = usedSlots.getFirst();
            ItemStack fallback = new ItemStack(Material.PAPER);
            ItemMeta meta = fallback.getItemMeta();
            if (meta != null) {
                AdventureText.displayName(meta, "§e随机加入竞技场");
                AdventureText.lore(meta, List.of("§7当前没有可用的竞技场节点。", "§7点击后将再次尝试随机加入。"));
                fallback.setItemMeta(meta);
            }
            if (BedWars.plugin.getLobbyArenaDispatcher() != null) {
                fallback = BedWars.nms.addCustomData(fallback, "RUNCOMMAND_bw join random");
            }
            inventory.setItem(slot, fallback);
        }

        renderPagination(holder, inventory, page,
                ArenaSelectorPagination.pageCount(arenas.size(), usedSlots.size()));
        player.updateInventory();
    }

    private static int selectorEntryCount(String group) {
        return BedWars.isBungeeLobby() ? availableRemoteArenas(group).size() : availableArenas(group).size();
    }

    private static List<ArenaNodeSnapshot> availableRemoteArenas(String group) {
        LobbyArenaDispatcher dispatcher = BedWars.plugin.getLobbyArenaDispatcher();
        if (dispatcher == null) return List.of();
        boolean showPlaying = BedWars.config.getBoolean(
                ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SHOW_PLAYING);
        return dispatcher.visibleSnapshots(group, showPlaying);
    }

    private static ItemStack remoteStatusItem(ArenaNodeSnapshot snapshot) {
        String status = snapshot.status().toLowerCase(Locale.ROOT);
        if (!(status.equals("waiting") || status.equals("starting") || status.equals("playing"))) return null;
        String material = yml.getString(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_MATERIAL
                .replace("%path%", status));
        if (material == null || material.isBlank()) return null;
        ItemStack item = BedWars.nms.createItemStack(material, 1,
                (short) yml.getInt(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_DATA
                        .replace("%path%", status)));
        if (item == null) return null;
        if (yml.getBoolean(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_ENCHANTED
                .replace("%path%", status))) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private static String remoteDisplayStatus(Player player, ArenaNodeSnapshot snapshot) {
        String key = switch (snapshot.status()) {
            case "WAITING" -> Messages.ARENA_STATUS_WAITING_NAME;
            case "STARTING" -> Messages.ARENA_STATUS_STARTING_NAME;
            case "PLAYING" -> Messages.ARENA_STATUS_PLAYING_NAME;
            default -> Messages.ARENA_STATUS_RESTARTING_NAME;
        };
        String full = snapshot.currentPlayers() >= snapshot.maxPlayers()
                ? Language.getMsg(player, Messages.MEANING_FULL) : "";
        return Language.getMsg(player, key).replace("{full}", full);
    }

    private static String remoteDisplayGroups(ArenaNodeSnapshot snapshot) {
        return snapshot.groups().stream().sorted(String.CASE_INSENSITIVE_ORDER).reduce((left, right) -> left + "," + right)
                .orElse("DEFAULT");
    }

    private static String remoteDisplayName(String name) {
        if (name == null || name.isBlank()) return "未知地图";
        String normalized = name.replace('_', ' ').replace('-', ' ').trim();
        return normalized.isEmpty() ? name : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private static void renderPagination(ArenaSelectorHolder holder, Inventory inventory,
                                         int page, int pageCount) {
        int previousSlot = ArenaSelectorPagination.previousSlot(inventory.getSize());
        int indicatorSlot = ArenaSelectorPagination.indicatorSlot(inventory.getSize());
        int nextSlot = ArenaSelectorPagination.nextSlot(inventory.getSize());
        holder.pageBySlot.clear();
        inventory.setItem(previousSlot, page > 0
                ? pageItem(Material.ARROW, "§e上一页", page - 1) : new ItemStack(Material.AIR));
        if (page > 0) holder.pageBySlot.put(previousSlot, page - 1);
        inventory.setItem(nextSlot, page + 1 < pageCount
                ? pageItem(Material.ARROW, "§e下一页", page + 1) : new ItemStack(Material.AIR));
        if (page + 1 < pageCount) holder.pageBySlot.put(nextSlot, page + 1);

        ItemStack indicator = new ItemStack(Material.PAPER);
        ItemMeta meta = indicator.getItemMeta();
        if (meta != null) {
            AdventureText.displayName(meta, "§f第 §e" + (page + 1) + "§f/§e" + pageCount + " §f页");
            indicator.setItemMeta(meta);
        }
        inventory.setItem(indicatorSlot, indicator);
    }

    private static ItemStack pageItem(Material material, String name, int page) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            AdventureText.displayName(meta, name);
            item.setItemMeta(meta);
        }
        return BedWars.nms.addCustomData(item, PAGE_GUI_IDENTIFIER + page);
    }

    private static List<IArena> availableArenas(String group) {
        List<IArena> arenas = new ArrayList<>();
        for (IArena arena : Arena.getArenas()) {
            if (group.equalsIgnoreCase("default")
                    || ArenaGroupPolicy.matches(arena.getGroup(), group)) {
                arenas.add(arena);
            }
        }
        boolean showPlaying = BedWars.config.getBoolean(
                ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SHOW_PLAYING);
        arenas.removeIf(candidate -> candidate.getStatus() != GameState.waiting
                && candidate.getStatus() != GameState.starting
                && (!showPlaying || candidate.getStatus() != GameState.playing));
        return Arena.getSorted(arenas);
    }

    public static class ArenaSelectorHolder implements InventoryHolder {

        private final String group;
        private final Map<Integer, Integer> pageBySlot = new HashMap<>();
        private final Map<Integer, ArenaNodeSnapshot> remoteBySlot = new HashMap<>();
        private int page;
        private Inventory inventory;

        public ArenaSelectorHolder(String group, int page) {
            this.group = group;
            this.page = page;
        }

        public String getGroup() {
            return group;
        }

        public int getPage() {
            return page;
        }

        private void setPage(int page) {
            this.page = page;
        }

        private void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        public Integer getPageAt(int slot) {
            return pageBySlot.get(slot);
        }

        public ArenaNodeSnapshot getRemoteArenaAt(int slot) {
            return remoteBySlot.get(slot);
        }

        private void setRemoteArena(int slot, ArenaNodeSnapshot snapshot) {
            remoteBySlot.put(slot, snapshot);
        }

        private void clearRemoteArenas() {
            remoteBySlot.clear();
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

    }

    private static boolean preventCalledTwice(@NotNull Player player) {
        return antiCalledTwice.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private static void updateCalledTwice(@NotNull Player player) {
        if (antiCalledTwice.containsKey(player.getUniqueId())) {
            antiCalledTwice.replace(player.getUniqueId(), System.currentTimeMillis() + 2000);
        } else {
            antiCalledTwice.put(player.getUniqueId(), System.currentTimeMillis() + 2000);
        }
    }
}
