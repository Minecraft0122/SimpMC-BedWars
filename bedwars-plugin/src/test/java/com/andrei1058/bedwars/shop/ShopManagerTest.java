package com.andrei1058.bedwars.shop;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopManagerTest {

    @Test
    void configVersionAndReservedSectionsAreNotShopCategories() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set(ConfigManager.CONFIG_VERSION_PATH, 4);
        yml.createSection(ConfigPath.SHOP_SETTINGS_PATH);
        yml.createSection(ConfigPath.SHOP_QUICK_DEFAULTS_PATH);
        yml.createSection(ConfigPath.SHOP_SPECIALS_PATH);
        yml.createSection("blocks-category");

        assertFalse(ShopManager.isCategoryRoot(yml, ConfigManager.CONFIG_VERSION_PATH));
        assertFalse(ShopManager.isCategoryRoot(yml, ConfigPath.SHOP_SETTINGS_PATH));
        assertFalse(ShopManager.isCategoryRoot(yml, ConfigPath.SHOP_QUICK_DEFAULTS_PATH));
        assertFalse(ShopManager.isCategoryRoot(yml, ConfigPath.SHOP_SPECIALS_PATH));
        assertTrue(ShopManager.isCategoryRoot(yml, "blocks-category"));
    }

    @Test
    void migrationRemovesUpperBodyArmorAndPreservesLowerBodyPieces() {
        YamlConfiguration yml = new YamlConfiguration();
        seedArmorPiece(yml, "iron-armor", "boots", "IRON_BOOTS");
        seedArmorPiece(yml, "iron-armor", "leggings", "IRON_LEGGINGS");
        seedArmorPiece(yml, "iron-armor", "chestplate", "IRON_CHESTPLATE");
        seedArmorPiece(yml, "iron-armor", "helmet", "IRON_HELMET");

        ShopManager.migrateLowerBodyArmorOnly(yml);

        assertEquals("IRON_BOOTS", yml.getString(armorItemPath("iron-armor", "boots") + ".material"));
        assertEquals("IRON_LEGGINGS", yml.getString(armorItemPath("iron-armor", "leggings") + ".material"));
        assertFalse(yml.isConfigurationSection(armorItemPath("iron-armor", "chestplate")));
        assertFalse(yml.isConfigurationSection(armorItemPath("iron-armor", "helmet")));
    }

    @Test
    void migrationRemovesCustomizedUpperArmorAcrossAllTiers() {
        YamlConfiguration yml = new YamlConfiguration();
        String itemPath = armorTierPath("custom", "tier2") + '.'
                + ConfigPath.SHOP_CONTENT_BUY_ITEMS_PATH + ".special.material";
        yml.set(itemPath, "NETHERITE_HELMET");

        ShopManager.migrateLowerBodyArmorOnly(yml);

        assertFalse(yml.isSet(itemPath));
    }

    @Test
    void migrationPreservesNonArmorCustomItems() {
        YamlConfiguration yml = new YamlConfiguration();
        String itemPath = armorTierPath("custom", "tier1") + '.'
                + ConfigPath.SHOP_CONTENT_BUY_ITEMS_PATH + ".token.material";
        yml.set(itemPath, "NETHER_STAR");

        ShopManager.migrateLowerBodyArmorOnly(yml);

        assertEquals("NETHER_STAR", yml.getString(itemPath));
    }

    private static String armorTierPath(String content, String tier) {
        return ConfigPath.SHOP_PATH_CATEGORY_ARMOR + ConfigPath.SHOP_CATEGORY_CONTENT_PATH + '.' + content + '.'
                + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_TIERS + '.' + tier;
    }

    private static String armorItemPath(String content, String piece) {
        return armorTierPath(content, "tier1") + '.' + ConfigPath.SHOP_CONTENT_BUY_ITEMS_PATH + '.' + piece;
    }

    private static void seedArmorPiece(YamlConfiguration yml, String content, String piece, String material) {
        yml.set(armorItemPath(content, piece) + ".material", material);
        yml.set(armorItemPath(content, piece) + ".auto-equip", true);
    }
}
