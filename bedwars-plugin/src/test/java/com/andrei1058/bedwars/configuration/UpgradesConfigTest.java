package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradesConfigTest {

    @Test
    void upgradesLegacyForgeActionsWithoutOverwritingCustomActions() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("upgrade-forge.tier-1.receive",
                Arrays.asList("generator-edit: iron,2,2,41", "generator-edit: gold,3,1,14"));
        List<String> customTier = List.of("generator-edit: iron,5,1,10");
        configuration.set("upgrade-forge.tier-2.receive", customTier);

        UpgradesConfig.migrateLegacyForgeDefaults(configuration);

        assertEquals(Arrays.asList("generator-edit: iron,1,3,41", "generator-edit: gold,4,3,14"),
                configuration.getStringList("upgrade-forge.tier-1.receive"));
        assertEquals(customTier, configuration.getStringList("upgrade-forge.tier-2.receive"));
    }

    @Test
    void suppliesFourSharpnessTiersWithoutOverwritingCustomValues() {
        YamlConfiguration configuration = new YamlConfiguration();
        UpgradesConfig.addDefaultSwordTiers(configuration);
        configuration.set("upgrade-swords.tier-2.cost", 99);
        configuration.options().copyDefaults(true);

        assertEquals(4, configuration.getInt("upgrade-swords.tier-1.cost"));
        assertEquals(99, configuration.getInt("upgrade-swords.tier-2.cost"));
        assertEquals(16, configuration.getInt("upgrade-swords.tier-3.cost"));
        assertEquals(32, configuration.getInt("upgrade-swords.tier-4.cost"));
        assertEquals(List.of("enchant-item: DAMAGE_ALL,4,sword"),
                configuration.getStringList("upgrade-swords.tier-4.receive"));
        assertEquals(4, configuration.getInt("upgrade-swords.tier-4.display-item.amount"));
        assertFalse(configuration.getBoolean("upgrade-swords.tier-4.display-item.enchanted"));
    }

    @Test
    void materializesSharpnessTiersDuringExistingConfigMigration() {
        YamlConfiguration configuration = new YamlConfiguration();
        UpgradesConfig.addDefaultSwordTiers(configuration);
        configuration.set("upgrade-swords.tier-1.cost", 7);

        UpgradesConfig.migrateDefaults(configuration);

        assertEquals(7, configuration.getInt("upgrade-swords.tier-1.cost"));
        assertEquals(16, configuration.getInt("upgrade-swords.tier-3.cost"));
        assertEquals(List.of("enchant-item: DAMAGE_ALL,4,sword"),
                configuration.getStringList("upgrade-swords.tier-4.receive"));
        assertTrue(configuration.contains("upgrade-swords.tier-4.receive", true),
                "tier IV must be stored in the migrated file, not only inherited in memory");
    }
}
