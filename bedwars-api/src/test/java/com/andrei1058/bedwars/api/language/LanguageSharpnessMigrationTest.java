package com.andrei1058.bedwars.api.language;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageSharpnessMigrationTest {

    @Test
    void expandsUnchangedOneTierMessagesAndKeepsCustomLore() {
        String namePath = Messages.UPGRADES_UPGRADE_TIER_ITEM_NAME.replace("{name}", "swords");
        String lorePath = Messages.UPGRADES_UPGRADE_TIER_ITEM_LORE.replace("{name}", "swords");
        List<String> legacy = List.of("old tier one");
        List<String> expanded = List.of("tier one", "tier two", "tier three", "tier four");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(namePath.replace("{tier}", "tier-1"), "Sharpness");
        defaults.set(lorePath, legacy);
        Language.addSharpnessUpgradeMessages(defaults, "Sharpness", legacy, expanded);
        defaults.options().copyDefaults(true);

        assertEquals("Sharpness I", defaults.getString(namePath.replace("{tier}", "tier-1")));
        assertEquals("Sharpness IV", defaults.getString(namePath.replace("{tier}", "tier-4")));
        assertEquals(expanded, defaults.getStringList(lorePath));

        YamlConfiguration customized = new YamlConfiguration();
        List<String> customLore = List.of("server-specific wording");
        customized.set(lorePath, customLore);
        Language.addSharpnessUpgradeMessages(customized, "Sharpness", legacy, expanded);
        customized.options().copyDefaults(true);
        assertEquals(customLore, customized.getStringList(lorePath));
    }
}
