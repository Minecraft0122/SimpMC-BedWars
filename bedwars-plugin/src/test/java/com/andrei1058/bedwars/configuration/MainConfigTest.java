package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void migratesLobbyTabToOneVisiblePingSource() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY, false);
        configuration.set(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB, true);

        MainConfig.migrateTabDisplayDefaults(configuration);

        assertTrue(configuration.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY));
        assertFalse(configuration.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB));
    }
}
