package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
