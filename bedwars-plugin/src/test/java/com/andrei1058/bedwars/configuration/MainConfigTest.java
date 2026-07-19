package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void migratesTemporaryTabDefaultsWithoutRemovingLobbyOverride() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY, false);
        configuration.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING, true);
        configuration.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING, true);
        configuration.set(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB, true);
        configuration.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_TEAMMATE_COLOR, "GREEN");
        configuration.set(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP, "yourServer.Com");
        configuration.set(ConfigPath.SB_CONFIG_TAB_LOBBY_HEADER, List.of("&b自定义大厅"));
        configuration.set(ConfigPath.SB_CONFIG_TAB_HEADER, List.of("temporary"));
        configuration.set(ConfigPath.SB_CONFIG_TAB_FOOTER, List.of("temporary"));

        MainConfig.migrateTabDisplayDefaults(configuration);

        assertTrue(configuration.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY));
        assertFalse(configuration.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING));
        assertFalse(configuration.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING));
        assertFalse(configuration.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB));
        assertFalse(configuration.isSet(ConfigPath.SB_CONFIG_SIDEBAR_LIST_TEAMMATE_COLOR));
        assertFalse(configuration.isSet(ConfigPath.SB_CONFIG_TAB_HEADER));
        assertFalse(configuration.isSet(ConfigPath.SB_CONFIG_TAB_FOOTER));
        assertEquals(List.of("&b自定义大厅"),
                configuration.getStringList(ConfigPath.SB_CONFIG_TAB_LOBBY_HEADER));
        assertEquals("simpmc.org", configuration.getString(
                ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP));
    }

    @Test
    void migratesOnlyTheOldDefaultRestartCountdown() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("restart.default", 45);
        configuration.set("restart.custom", 75);

        MainConfig.upgradeLegacyNumber(configuration, "restart.default", 45, 60);
        MainConfig.upgradeLegacyNumber(configuration, "restart.custom", 45, 60);

        assertEquals(60, configuration.getInt("restart.default"));
        assertEquals(75, configuration.getInt("restart.custom"));
    }

    @Test
    void removesObsoleteBackToLobbyItemFromMainLobby() {
        YamlConfiguration configuration = new YamlConfiguration();
        String path = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
        configuration.set(path + ".command", "bw leave");
        configuration.set(path + ".material", "RED_BED");

        MainConfig.removeObsoleteLobbyItems(configuration);

        assertFalse(configuration.isSet(path));
    }
}
