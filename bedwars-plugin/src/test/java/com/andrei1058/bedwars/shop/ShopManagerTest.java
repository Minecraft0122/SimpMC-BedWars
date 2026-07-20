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
        yml.set(ConfigManager.CONFIG_VERSION_PATH, 3);
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
    void oldBuiltInArmorProductsAreMigratedToFullSets() {
        YamlConfiguration yml = new YamlConfiguration();
        seedStandardLowerSet(yml, "chainmail", "CHAINMAIL");
        seedStandardLowerSet(yml, "iron-armor", "IRON");
        seedStandardLowerSet(yml, "diamond-armor", "DIAMOND");

        ShopManager.migrateFullArmorSets(yml);

        assertArmorPiece(yml, "chainmail", "helmet", "CHAINMAIL_HELMET");
        assertArmorPiece(yml, "chainmail", "chestplate", "CHAINMAIL_CHESTPLATE");
        assertArmorPiece(yml, "iron-armor", "helmet", "IRON_HELMET");
        assertArmorPiece(yml, "iron-armor", "chestplate", "IRON_CHESTPLATE");
        assertArmorPiece(yml, "diamond-armor", "helmet", "DIAMOND_HELMET");
        assertArmorPiece(yml, "diamond-armor", "chestplate", "DIAMOND_CHESTPLATE");
    }

    @Test
    void fullArmorMigrationPreservesCustomPiecesAndRemovedCategories() {
        YamlConfiguration yml = new YamlConfiguration();
        seedStandardLowerSet(yml, "iron-armor", "IRON");
        String customHelmet = armorItemPath("iron-armor", "helmet") + ".material";
        yml.set(customHelmet, "NETHERITE_HELMET");

        ShopManager.migrateFullArmorSets(yml);

        assertEquals("NETHERITE_HELMET", yml.getString(customHelmet));
        assertFalse(yml.isConfigurationSection(armorTierPath("chainmail")));
        assertFalse(yml.isConfigurationSection(armorTierPath("diamond-armor")));
    }

    @Test
    void fullArmorMigrationDoesNotRewriteCustomizedLowerSets() {
        YamlConfiguration yml = new YamlConfiguration();
        seedStandardLowerSet(yml, "iron-armor", "IRON");
        yml.set(armorItemPath("iron-armor", "boots") + ".material", "DIAMOND_BOOTS");

        ShopManager.migrateFullArmorSets(yml);

        assertFalse(yml.isConfigurationSection(armorItemPath("iron-armor", "helmet")));
        assertFalse(yml.isConfigurationSection(armorItemPath("iron-armor", "chestplate")));
    }

    private static void assertArmorPiece(YamlConfiguration yml, String content, String piece, String material) {
        String path = armorItemPath(content, piece);
        assertEquals(material, yml.getString(path + ".material"));
        assertEquals(1, yml.getInt(path + ".amount"));
        assertTrue(yml.getBoolean(path + ".auto-equip"));
    }

    private static String armorTierPath(String content) {
        return ConfigPath.SHOP_PATH_CATEGORY_ARMOR + ConfigPath.SHOP_CATEGORY_CONTENT_PATH + '.' + content + '.'
                + ConfigPath.SHOP_CATEGORY_CONTENT_CONTENT_TIERS + ".tier1";
    }

    private static String armorItemPath(String content, String piece) {
        return armorTierPath(content) + '.' + ConfigPath.SHOP_CONTENT_BUY_ITEMS_PATH + '.' + piece;
    }

    private static void seedStandardLowerSet(YamlConfiguration yml, String content, String materialPrefix) {
        yml.set(armorItemPath(content, "boots") + ".material", materialPrefix + "_BOOTS");
        yml.set(armorItemPath(content, "leggings") + ".material", materialPrefix + "_LEGGINGS");
    }
}
