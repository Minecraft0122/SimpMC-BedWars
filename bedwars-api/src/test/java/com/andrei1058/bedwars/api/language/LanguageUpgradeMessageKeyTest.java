package com.andrei1058.bedwars.api.language;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageUpgradeMessageKeyTest {

    @Test
    void migratesHistoricalUpgradeNameKeysToTheCurrentSchema() {
        YamlConfiguration language = new YamlConfiguration();
        language.set("upgrades-name-armor-tier-1", "&c护甲强化 I");
        language.set("upgrades-lore-armor", java.util.List.of("&7保护 I"));

        Language.migrateLegacyUpgradeMessageKeys(language);

        assertEquals("&c护甲强化 I", language.getString("upgrades-upgrade-name-armor-tier-1"));
        assertEquals(java.util.List.of("&7保护 I"),
                language.getStringList("upgrades-upgrade-lore-armor"));
        assertEquals("&c护甲强化 I", language.getString("upgrades-name-armor-tier-1"));
    }

    @Test
    void acceptsLegacyIsoValuesWithWhitespace() {
        org.junit.jupiter.api.Assertions.assertTrue(Language.isSimplifiedChineseIso(" zh_cn "));
    }
}
