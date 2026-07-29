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
    void slightlyReducesOnlyBuiltInFireballDefaults() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE, 3.5);
        configuration.set(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, 1.25);
        configuration.set(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, 0.8);
        configuration.set(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, 4.0);
        configuration.set(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF, 1.75);

        MainConfig.migrateFireballDefaults(configuration, 16);

        assertEquals(3.25, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE));
        assertEquals(1.15, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL));
        assertEquals(0.75, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL));
        assertEquals(3.5, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY));
        assertEquals(1.75, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF));
    }

    @Test
    void preservesEarlyLookingValuesWhenTheyWereCustomizedInANewerConfig() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE, 3.0);
        configuration.set(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, 1.0);
        configuration.set(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, 0.65);
        configuration.set(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, 2.0);

        MainConfig.migrateFireballDefaults(configuration, 16);

        assertEquals(3.0, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE));
        assertEquals(1.0, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL));
        assertEquals(0.65, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL));
        assertEquals(2.0, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY));
    }

    @Test
    void extendsOnlyTheUnchangedFireballFlightSpeed() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 10.0);
        YamlConfiguration customized = new YamlConfiguration();
        customized.set(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 9.5);

        MainConfig.migrateFireballDefaults(defaults, 22);
        MainConfig.migrateFireballDefaults(customized, 22);

        assertEquals(11.0, defaults.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
        assertEquals(9.5, customized.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
    }

    @Test
    void doesNotRepeatFireballSpeedMigration() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 10.0);

        MainConfig.migrateFireballDefaults(configuration, 23);

        assertEquals(10.0, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
    }

    @Test
    void migratesPreEnhancementDefaultsForOldConfigs() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE, 3.0);
        configuration.set(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, 1.0);
        configuration.set(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, 0.65);
        configuration.set(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, 2.0);

        MainConfig.migrateFireballDefaults(configuration, 9);

        assertEquals(3.25, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE));
        assertEquals(1.15, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL));
        assertEquals(0.75, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL));
        assertEquals(3.5, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY));
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
    void restoresBackToLobbyItemWithoutOverwritingCustomValues() {
        YamlConfiguration configuration = new YamlConfiguration();
        String path = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
        configuration.set(path + ".material", "BLUE_BED");

        MainConfig.ensureLobbyItem(configuration, "leave", "bw leave", false, "RED_BED", 0, 8);

        assertEquals("BLUE_BED", configuration.getString(path + ".material"));
        assertEquals("bw leave", configuration.getString(path + ".command"));
        assertEquals(0, configuration.getInt(path + ".data"));
        assertEquals(8, configuration.getInt(path + ".slot"));
        assertFalse(configuration.getBoolean(path + ".enchanted"));
    }

    @Test
    void removesRetiredFullArmorSaleOption() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_SHOP_SELL_FULL_ARMOR, true);

        MainConfig.removeRetiredFullArmorSetting(configuration);

        assertFalse(configuration.isSet(ConfigPath.GENERAL_CONFIGURATION_SHOP_SELL_FULL_ARMOR));
    }

    @Test
    void normalizesStoredJoinNpcDirectionWithoutChangingMetadata() {
        assertEquals("1.5,64.0,2.5,-90.0,0.0,lobby,skin,名称,Default,42",
                MainConfig.normalizeNpcLocationEntry(
                        "1.5,64.0,2.5,-88.328,35.0,lobby,skin,名称,Default,42", "world"));
    }
}
