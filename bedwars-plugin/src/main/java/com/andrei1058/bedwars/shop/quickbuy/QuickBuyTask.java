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

package com.andrei1058.bedwars.shop.quickbuy;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.shop.ShopManager;
import com.andrei1058.bedwars.shop.main.CategoryContent;
import com.andrei1058.bedwars.shop.main.ShopCategory;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class QuickBuyTask extends BukkitRunnable {

    private UUID uuid;


    public QuickBuyTask(UUID uuid){
        this.uuid = uuid;
        this.runTaskLaterAsynchronously(BedWars.plugin, 20*7);
    }

    @Override
    public void run() {
        PlayerQuickBuyCache cache = PlayerQuickBuyCache.getQuickBuyCache(uuid);
        if (cache == null) {
            return;
        }

        boolean hasQuickBuy = BedWars.getRemoteDatabase().hasQuickBuy(uuid);
        HashMap<Integer, String> items = hasQuickBuy
                ? BedWars.getRemoteDatabase().getQuickBuySlots(uuid, PlayerQuickBuyCache.quickSlots)
                : new HashMap<>();

        Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
            if (PlayerQuickBuyCache.getQuickBuyCache(uuid) != cache) {
                return;
            }
            if (hasQuickBuy) {
                applyStoredItems(cache, items);
            } else {
                applyDefaults(cache);
            }
        });
    }

    private void applyStoredItems(PlayerQuickBuyCache cache, HashMap<Integer, String> items) {
        for (Map.Entry<Integer, String> entry : items.entrySet()) {
            if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                continue;
            }
            QuickBuyElement element = new QuickBuyElement(entry.getValue(), entry.getKey());
            if (element.isLoaded()) {
                cache.addQuickElement(element);
            }
        }
    }

    private void applyDefaults(PlayerQuickBuyCache cache) {
        if (BedWars.shop.getYml().getConfigurationSection(ConfigPath.SHOP_QUICK_DEFAULTS_PATH) == null) {
            return;
        }

        for (String key : BedWars.shop.getYml().getConfigurationSection(ConfigPath.SHOP_QUICK_DEFAULTS_PATH).getKeys(false)) {
            String basePath = ConfigPath.SHOP_QUICK_DEFAULTS_PATH + "." + key;
            String identifier = BedWars.shop.getYml().getString(basePath + ".path");
            String configuredSlot = BedWars.shop.getYml().getString(basePath + ".slot");
            if (identifier == null || configuredSlot == null) {
                continue;
            }

            final int slot;
            try {
                slot = Integer.parseInt(configuredSlot);
            } catch (NumberFormatException exception) {
                BedWars.debug(configuredSlot + " must be an integer!");
                continue;
            }

            for (ShopCategory category : ShopManager.getShop().getCategoryList()) {
                for (CategoryContent content : category.getCategoryContentList()) {
                    if (content.getIdentifier().equals(identifier)) {
                        cache.setElement(slot, content);
                    }
                }
            }
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
    }
}
