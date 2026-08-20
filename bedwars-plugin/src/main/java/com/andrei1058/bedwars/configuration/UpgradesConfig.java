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

package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.andrei1058.bedwars.BedWars.plugin;

public class UpgradesConfig extends ConfigManager {

    private static final int CONFIG_VERSION = 11;
    private static final int PREVIOUS_SWORD_PRICE_SCHEMA = 10;
    private static final int PREVIOUS_ARMOR_PRICE_SCHEMA = 9;
    private static final int MAX_SWORD_TIER = 4;
    private static final int MAX_ARMOR_TIER = 4;
    private static final List<Integer> SWORD_TIER_COSTS = List.of(4, 8, 16, 32);
    private static final List<Integer> SCHEMA_TEN_SWORD_TIER_COSTS = List.of(2, 4, 8, 16);
    private static final List<Integer> SCHEMA_NINE_SWORD_TIER_COSTS = List.of(2, 4, 8, 14);
    private static final List<Integer> SCHEMA_EIGHT_SWORD_TIER_COSTS = List.of(4, 6, 9, 14);
    private static final List<Integer> LEGACY_SWORD_TIER_COSTS = List.of(4, 8, 16, 32);
    private static final List<Integer> ARMOR_TIER_COSTS = List.of(2, 4, 10, 24);
    private static final List<Integer> PREVIOUS_ARMOR_TIER_COSTS = List.of(2, 4, 8, 16);

    public UpgradesConfig(String name, String dir) {
        super(plugin, name, dir);
        YamlConfiguration yml = getYml();
        yml.options().header("SimpMC-BedWars 队伍升级与陷阱配置。\n动作格式和完整示例请参阅 docs/zh_CN/configuration.md。");
        List<String> elements = Arrays.asList("upgrade-swords,10", "upgrade-armor,11", "upgrade-miner,12", "upgrade-forge,13",
                "upgrade-heal-pool,14", "upgrade-dragon,15", "category-traps,16", "separator-glass,18,19,20,21,22,23,24,25,26",
                "trap-slot-first,30", "trap-slot-second,31", "trap-slot-third,32");
        yml.addDefault("default-upgrades-settings.menu-content", elements);
        yml.addDefault("default-upgrades-settings.trap-start-price", 1);
        yml.addDefault("default-upgrades-settings.trap-increment-price", 1);
        yml.addDefault("default-upgrades-settings.trap-currency", "diamond");
        yml.addDefault("default-upgrades-settings.trap-queue-limit", 3);

        addDefaultSwordTiers(yml);
        addDefaultArmorTiers(yml);

        if (isFirstTime()) {
            yml.addDefault("upgrade-miner.tier-1.currency", "diamond");
            yml.addDefault("upgrade-miner.tier-1.cost", 2);
            addDefaultDisplayItem("upgrade-miner.tier-1", "GOLDEN_PICKAXE", 0, 1, false);
            yml.addDefault("upgrade-miner.tier-1.receive", Collections.singletonList("player-effect: FAST_DIGGING,0,0,team"));

            yml.addDefault("upgrade-miner.tier-2.currency", "diamond");
            yml.addDefault("upgrade-miner.tier-2.cost", 4);
            addDefaultDisplayItem("upgrade-miner.tier-2", "GOLDEN_PICKAXE", 0, 2, false);
            yml.addDefault("upgrade-miner.tier-2.receive", Collections.singletonList("player-effect: FAST_DIGGING,1,0,team"));

            yml.addDefault("upgrade-forge.tier-1.currency", "diamond");
            yml.addDefault("upgrade-forge.tier-1.cost", 2);
            addDefaultDisplayItem("upgrade-forge.tier-1", "FURNACE", 0, 1, false);
            yml.addDefault("upgrade-forge.tier-1.receive", Arrays.asList("generator-edit: iron,1,3,41", "generator-edit: gold,4,3,14"));

            yml.addDefault("upgrade-forge.tier-2.currency", "diamond");
            yml.addDefault("upgrade-forge.tier-2.cost", 4);
            addDefaultDisplayItem("upgrade-forge.tier-2", "FURNACE", 0, 2, false);
            yml.addDefault("upgrade-forge.tier-2.receive", Arrays.asList("generator-edit: iron,1,4,48", "generator-edit: gold,2,2,21"));

            yml.addDefault("upgrade-forge.tier-3.currency", "diamond");
            yml.addDefault("upgrade-forge.tier-3.cost", 6);
            addDefaultDisplayItem("upgrade-forge.tier-3", "FURNACE", 0, 3, false);
            yml.addDefault("upgrade-forge.tier-3.receive", Arrays.asList("generator-edit: iron,1,5,64", "generator-edit: gold,2,3,29",
                    "generator-edit: emerald,10,1,10"));

            yml.addDefault("upgrade-forge.tier-4.currency", "diamond");
            yml.addDefault("upgrade-forge.tier-4.cost", 8);
            addDefaultDisplayItem("upgrade-forge.tier-4", "FURNACE", 0, 4, false);
            yml.addDefault("upgrade-forge.tier-4.receive", Arrays.asList("generator-edit: iron,1,8,120", "generator-edit: gold,1,4,80",
                    "generator-edit: emerald,10,2,20"));

            yml.addDefault("upgrade-heal-pool.tier-1.currency", "diamond");
            yml.addDefault("upgrade-heal-pool.tier-1.cost", 1);
            addDefaultDisplayItem("upgrade-heal-pool.tier-1", "BEACON", 0, 1, false);
            yml.addDefault("upgrade-heal-pool.tier-1.receive", Collections.singletonList("player-effect: REGENERATION,1,0,base"));

            yml.addDefault("upgrade-dragon.tier-1.currency", "diamond");
            yml.addDefault("upgrade-dragon.tier-1.cost", 5);
            addDefaultDisplayItem("upgrade-dragon.tier-1", "DRAGON_EGG", 0, 1, false);
            yml.addDefault("upgrade-dragon.tier-1.receive", Collections.singletonList("dragon: 1"));

            addDefaultDisplayItem("category-traps", "LEATHER", 0, 1, false);
            yml.addDefault("category-traps.category-content", Arrays.asList("base-trap-1,10", "base-trap-2,11",
                    "base-trap-3,12", "base-trap-4,13", "separator-back,31"));

            yml.addDefault("separator-glass.on-click", "");
            addDefaultDisplayItem("separator-glass", "GRAY_STAINED_GLASS_PANE", 7, 1, false);

            yml.addDefault("trap-slot-first.trap", 1);
            addDefaultDisplayItem("trap-slot-first", "GRAY_STAINED_GLASS", 8, 1, false);
            yml.addDefault("trap-slot-second.trap", 2);
            addDefaultDisplayItem("trap-slot-second", "GRAY_STAINED_GLASS", 8, 2, false);
            yml.addDefault("trap-slot-third.trap", 3);
            addDefaultDisplayItem("trap-slot-third", "GRAY_STAINED_GLASS", 8, 3, false);

            addDefaultDisplayItem("base-trap-1", "TRIPWIRE_HOOK", 0, 1, false);
            yml.addDefault("base-trap-1.receive", Arrays.asList("player-effect: BLINDNESS,1,5,enemy", "player-effect: SLOW,1,5,enemy"));

            addDefaultDisplayItem("base-trap-2", "FEATHER", 0, 1, false);
            yml.addDefault("base-trap-2.receive", Collections.singletonList("player-effect: SPEED,1,15,base"));

            addDefaultDisplayItem("base-trap-3", "REDSTONE_TORCH", 0, 1, false);
            yml.addDefault("base-trap-3.custom-announce", true);
            yml.addDefault("base-trap-3.receive", Collections.singletonList("remove-effect: INVISIBILITY,enemy"));

            addDefaultDisplayItem("base-trap-4", "IRON_PICKAXE", 0, 1, false);
            yml.addDefault("base-trap-4.receive", Collections.singletonList("player-effect: SLOW_DIGGING,1,15,enemy"));

            yml.addDefault("separator-back.on-click.player", Collections.singletonList("bw upgradesmenu"));
            yml.addDefault("separator-back.on-click.console", Collections.singletonList(""));
            addDefaultDisplayItem("separator-back", "ARROW", 0, 1, false);
        }
        yml.options().copyDefaults(true);
        setComments("default-upgrades-settings", "队伍升级菜单布局、陷阱价格和队列上限。");
        setComments("upgrade-swords", "锋利 I–IV 逐级购买；默认价格为 4、8、16、32 钻石。", "每个 tier 包含价格、货币、显示物品和 receive 动作。");
        setComments("upgrade-armor", "保护 I–IV 逐级购买；默认价格为 2、4、10、24 钻石。");
        setComments("category-traps", "陷阱分类菜单内容及槽位。");
        ChineseConfigDocumentation.upgrades(this);
        int storedVersion = yml.getInt(CONFIG_VERSION_PATH, 0);
        updateToLatestVersion(CONFIG_VERSION, config -> migrateDefaults(config, storedVersion));
    }

    static void addDefaultSwordTiers(YamlConfiguration yml) {
        for (int tier = 1; tier <= MAX_SWORD_TIER; tier++) {
            String path = "upgrade-swords.tier-" + tier;
            yml.addDefault(path + ".cost", defaultSwordTierCost(tier));
            yml.addDefault(path + ".currency", "diamond");
            yml.addDefault(path + ".display-item.material", "IRON_SWORD");
            yml.addDefault(path + ".display-item.data", 0);
            yml.addDefault(path + ".display-item.amount", tier);
            yml.addDefault(path + ".display-item.enchanted", false);
            yml.addDefault(path + ".receive",
                    Collections.singletonList("enchant-item: DAMAGE_ALL," + tier + ",sword"));
        }
    }

    static void addDefaultArmorTiers(YamlConfiguration yml) {
        for (int tier = 1; tier <= MAX_ARMOR_TIER; tier++) {
            String path = "upgrade-armor.tier-" + tier;
            yml.addDefault(path + ".cost", defaultArmorTierCost(tier));
            yml.addDefault(path + ".currency", "diamond");
            yml.addDefault(path + ".display-item.material", "IRON_CHESTPLATE");
            yml.addDefault(path + ".display-item.data", 0);
            yml.addDefault(path + ".display-item.amount", tier);
            yml.addDefault(path + ".display-item.enchanted", false);
            yml.addDefault(path + ".receive",
                    Collections.singletonList("enchant-item: PROTECTION_ENVIRONMENTAL," + tier + ",armor"));
        }
    }

    static int defaultSwordTierCost(int tier) {
        if (tier < 1 || tier > MAX_SWORD_TIER) {
            throw new IllegalArgumentException("Sword tier must be between 1 and " + MAX_SWORD_TIER);
        }
        return SWORD_TIER_COSTS.get(tier - 1);
    }

    static int defaultArmorTierCost(int tier) {
        if (tier < 1 || tier > MAX_ARMOR_TIER) {
            throw new IllegalArgumentException("Armor tier must be between 1 and " + MAX_ARMOR_TIER);
        }
        return ARMOR_TIER_COSTS.get(tier - 1);
    }

    static void migrateLegacyForgeDefaults(YamlConfiguration yml) {
        replaceLegacyList(yml, "upgrade-forge.tier-1.receive",
                Arrays.asList("generator-edit: iron,2,2,41", "generator-edit: gold,3,1,14"),
                Arrays.asList("generator-edit: iron,1,3,41", "generator-edit: gold,4,3,14"));
        replaceLegacyList(yml, "upgrade-forge.tier-2.receive",
                Arrays.asList("generator-edit: iron,1,2,48", "generator-edit: gold,3,2,21"),
                Arrays.asList("generator-edit: iron,1,4,48", "generator-edit: gold,2,2,21"));
        replaceLegacyList(yml, "upgrade-forge.tier-3.receive",
                Arrays.asList("generator-edit: iron,1,2,64", "generator-edit: gold,3,2,29", "generator-edit: emerald,10,1,10"),
                Arrays.asList("generator-edit: iron,1,5,64", "generator-edit: gold,2,3,29", "generator-edit: emerald,10,1,10"));
        replaceLegacyList(yml, "upgrade-forge.tier-4.receive",
                Arrays.asList("generator-edit: iron,1,4,120", "generator-edit: gold,2,4,80", "generator-edit: emerald,10,2,20"),
                Arrays.asList("generator-edit: iron,1,8,120", "generator-edit: gold,1,4,80", "generator-edit: emerald,10,2,20"));
    }

    static void migrateDefaults(YamlConfiguration yml, int storedVersion) {
        migrateLegacyForgeDefaults(yml);
        migrateLegacySwordTierCosts(yml, storedVersion);
        migrateLegacyArmorTierCosts(yml, storedVersion);
        materializeTiers(yml, "upgrade-swords", MAX_SWORD_TIER);
        materializeTiers(yml, "upgrade-armor", MAX_ARMOR_TIER);
    }

    private static void materializeTiers(YamlConfiguration yml, String section, int maximumTier) {
        for (int tier = 1; tier <= maximumTier; tier++) {
            String root = section + ".tier-" + tier;
            for (String child : List.of("cost", "currency", "display-item.material", "display-item.data",
                    "display-item.amount", "display-item.enchanted", "receive")) {
                String path = root + "." + child;
                if (!yml.contains(path, true) && yml.getDefaults() != null) {
                    yml.set(path, yml.getDefaults().get(path));
                }
            }
        }
    }

    static void migrateLegacySwordTierCosts(YamlConfiguration yml, int storedVersion) {
        if (storedVersion > PREVIOUS_SWORD_PRICE_SCHEMA) {
            return;
        }
        List<Integer> oldDefaults;
        if (storedVersion == PREVIOUS_SWORD_PRICE_SCHEMA) {
            oldDefaults = SCHEMA_TEN_SWORD_TIER_COSTS;
        } else if (storedVersion == 9) {
            oldDefaults = SCHEMA_NINE_SWORD_TIER_COSTS;
        } else if (storedVersion == 8) {
            oldDefaults = SCHEMA_EIGHT_SWORD_TIER_COSTS;
        } else {
            oldDefaults = LEGACY_SWORD_TIER_COSTS;
        }
        // A single value equal to an old default may still be an administrator's
        // deliberate choice. Only an unchanged built-in four-tier sequence is safe to migrate.
        if (!hasExactTierCosts(yml, "upgrade-swords", MAX_SWORD_TIER, oldDefaults)) {
            return;
        }
        for (int tier = 1; tier <= MAX_SWORD_TIER; tier++) {
            String path = "upgrade-swords.tier-" + tier + ".cost";
            yml.set(path, defaultSwordTierCost(tier));
        }
    }

    static void migrateLegacyArmorTierCosts(YamlConfiguration yml, int storedVersion) {
        if (storedVersion > PREVIOUS_ARMOR_PRICE_SCHEMA
                || !hasExactTierCosts(yml, "upgrade-armor", MAX_ARMOR_TIER, PREVIOUS_ARMOR_TIER_COSTS)) {
            return;
        }
        for (int tier = 1; tier <= MAX_ARMOR_TIER; tier++) {
            yml.set("upgrade-armor.tier-" + tier + ".cost", defaultArmorTierCost(tier));
        }
    }

    private static boolean hasExactTierCosts(YamlConfiguration yml, String section, int maximumTier,
                                             List<Integer> expectedCosts) {
        for (int tier = 1; tier <= maximumTier; tier++) {
            String path = section + ".tier-" + tier + ".cost";
            if (!yml.contains(path, true) || yml.getInt(path) != expectedCosts.get(tier - 1)) {
                return false;
            }
        }
        return true;
    }

    private static void replaceLegacyList(YamlConfiguration yml, String path, List<String> oldValue, List<String> newValue) {
        if (yml.getStringList(path).equals(oldValue)) {
            yml.set(path, newValue);
        }
    }

    private void addDefaultDisplayItem(String path, String material, int data, int amount, boolean enchanted) {
        getYml().addDefault(path + ".display-item.material", material);
        getYml().addDefault(path + ".display-item.data", data);
        getYml().addDefault(path + ".display-item.amount", amount);
        getYml().addDefault(path + ".display-item.enchanted", enchanted);
    }
}
