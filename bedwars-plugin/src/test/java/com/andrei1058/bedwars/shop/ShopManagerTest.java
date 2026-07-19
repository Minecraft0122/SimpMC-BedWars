package com.andrei1058.bedwars.shop;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

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
}
