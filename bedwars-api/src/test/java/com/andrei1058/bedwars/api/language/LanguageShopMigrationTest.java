package com.andrei1058.bedwars.api.language;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LanguageShopMigrationTest {

    private static final String LEGACY_NAME = namePath("Compact Pop-up Tower");
    private static final String LEGACY_LORE = lorePath("Compact Pop-up Tower");
    private static final String TOWER_NAME = namePath("tower");
    private static final String TOWER_LORE = lorePath("tower");

    @Test
    void movesLegacyTowerTranslationOverGeneratedPlaceholders() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(LEGACY_NAME, "{color}袖珍弹出塔");
        language.set(LEGACY_LORE, List.of("&7自动生成防御塔"));
        language.set(TOWER_NAME, "&cName not set");
        language.set(TOWER_LORE, "&8Lore not set");

        Language.migrateLegacyTowerShopItem(language);

        assertEquals("{color}袖珍弹出塔", language.getString(TOWER_NAME));
        assertEquals(List.of("&7自动生成防御塔"), language.getStringList(TOWER_LORE));
        assertFalse(language.isSet(LEGACY_NAME));
        assertFalse(language.isSet(LEGACY_LORE));
    }

    @Test
    void preservesExplicitTranslationAtTheStableTowerPath() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(LEGACY_NAME, "legacy");
        language.set(LEGACY_LORE, List.of("legacy lore"));
        language.set(TOWER_NAME, "custom tower");
        language.set(TOWER_LORE, List.of("custom lore"));

        Language.migrateLegacyTowerShopItem(language);

        assertEquals("custom tower", language.getString(TOWER_NAME));
        assertEquals(List.of("custom lore"), language.getStringList(TOWER_LORE));
        assertFalse(language.isSet(LEGACY_NAME));
        assertFalse(language.isSet(LEGACY_LORE));
    }

    @Test
    void clearsGeneratedPlaceholdersSoFallbackDefaultsCanApply() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(TOWER_NAME, "&cName not set");
        language.set(TOWER_LORE, List.of("&8Lore not set"));

        Language.migrateLegacyTowerShopItem(language);
        Language.addContentMessages(language, "tower", ConfigPath.SHOP_PATH_CATEGORY_UTILITY,
                "{color}Compact Pop-up Tower", List.of("&7Fallback lore"));
        language.options().copyDefaults(true);

        assertEquals("{color}Compact Pop-up Tower", language.getString(TOWER_NAME));
        assertEquals(List.of("&7Fallback lore"), language.getStringList(TOWER_LORE));
    }

    private static String namePath(String content) {
        return Messages.SHOP_CONTENT_TIER_ITEM_NAME
                .replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY)
                .replace("%content%", content);
    }

    private static String lorePath(String content) {
        return Messages.SHOP_CONTENT_TIER_ITEM_LORE
                .replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY)
                .replace("%content%", content);
    }
}
