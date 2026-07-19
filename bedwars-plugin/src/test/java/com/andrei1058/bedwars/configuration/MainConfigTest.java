package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainConfigTest {

    @Test
    void upgradesOnlyAnUnchangedLegacyFireballDefault() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("fireball.legacy", 1.0);
        configuration.set("fireball.custom", 1.1);

        MainConfig.upgradeLegacyNumber(configuration, "fireball.legacy", 1.0, 1.25);
        MainConfig.upgradeLegacyNumber(configuration, "fireball.custom", 1.0, 1.25);

        assertEquals(1.25, configuration.getDouble("fireball.legacy"));
        assertEquals(1.1, configuration.getDouble("fireball.custom"));
    }
}
