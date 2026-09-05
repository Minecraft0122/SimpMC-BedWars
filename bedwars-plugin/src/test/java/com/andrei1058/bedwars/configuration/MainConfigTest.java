package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.ArenaSelectorPagination;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainConfigTest {

    @Test
    void removesTheObsoleteShoutCooldownRegardlessOfItsValue() {
        YamlConfiguration defaultValue = new YamlConfiguration();
        YamlConfiguration customValue = new YamlConfiguration();
        defaultValue.set(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN, 30);
        customValue.set(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN, 90);

        MainConfig.removeShoutCooldownSetting(defaultValue);
        MainConfig.removeShoutCooldownSetting(customValue);

        assertFalse(defaultValue.isSet(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN));
        assertFalse(customValue.isSet(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN));
    }

    @Test
    void removesTheRetiredFullArenaCountdownWithoutTouchingOtherCountdowns() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("countdowns.game-start-shortened", 12);
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_REGULAR, 40);

        MainConfig.removeRetiredFullArenaCountdownSetting(configuration);

        assertFalse(configuration.isSet("countdowns.game-start-shortened"));
        assertEquals(40, configuration.getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_REGULAR));
    }

    @Test
    void schemaTwentyEightPreservesCurrentAdministratorValues() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigManager.CONFIG_VERSION_PATH, 27);
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN, 90);
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, 300);
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_RESTART, 45);
        configuration.set("custom-administrator-value", "keep-me");

        assertTrue(MainConfig.migrateSchema28Only(configuration, 27));

        assertFalse(configuration.isSet(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN));
        assertEquals(300, configuration.getInt(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME));
        assertEquals(45, configuration.getInt(ConfigPath.GENERAL_CONFIGURATION_RESTART));
        assertEquals("keep-me", configuration.getString("custom-administrator-value"));
    }

    @Test
    void olderSchemasContinueThroughTheHistoricalMigrationPath() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN, 90);

        assertFalse(MainConfig.migrateSchema28Only(configuration, 26));
        assertEquals(90, configuration.getInt(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN));
    }

    @Test
    void disciplineMigrationPreservesCustomAfkWarningAndRepairsFollowingThresholds() {
        YamlConfiguration oldDefaults = new YamlConfiguration();
        oldDefaults.set(ConfigPath.DISCIPLINE_AFK_WARNING_SECONDS, 300);
        oldDefaults.set(ConfigPath.DISCIPLINE_AFK_FINAL_WARNING_SECONDS, 120);
        oldDefaults.set(ConfigPath.DISCIPLINE_AFK_REMOVAL_SECONDS, 180);

        MainConfig.migrateDisciplineDefaults(oldDefaults);

        assertEquals(300, oldDefaults.getInt(ConfigPath.DISCIPLINE_AFK_WARNING_SECONDS));
        assertEquals(301, oldDefaults.getInt(ConfigPath.DISCIPLINE_AFK_FINAL_WARNING_SECONDS));
        assertEquals(302, oldDefaults.getInt(ConfigPath.DISCIPLINE_AFK_REMOVAL_SECONDS));
    }

    @Test
    void disciplineMigrationPreservesEveryValidAfkThresholdBeforeTheBrokenOne() {
        YamlConfiguration custom = new YamlConfiguration();
        custom.set(ConfigPath.DISCIPLINE_AFK_WARNING_SECONDS, 300);
        custom.set(ConfigPath.DISCIPLINE_AFK_FINAL_WARNING_SECONDS, 450);
        custom.set(ConfigPath.DISCIPLINE_AFK_REMOVAL_SECONDS, 180);

        MainConfig.migrateDisciplineDefaults(custom);

        assertEquals(300, custom.getInt(ConfigPath.DISCIPLINE_AFK_WARNING_SECONDS));
        assertEquals(450, custom.getInt(ConfigPath.DISCIPLINE_AFK_FINAL_WARNING_SECONDS));
        assertEquals(451, custom.getInt(ConfigPath.DISCIPLINE_AFK_REMOVAL_SECONDS));
    }

    @Test
    void expandsOnlyTheUnchangedArenaSelectorLayout() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE, 27);
        defaults.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS,
                "10,11,12,13,14,15,16");
        YamlConfiguration customized = new YamlConfiguration();
        customized.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE, 36);
        customized.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS, "0,1,2");

        MainConfig.migrateArenaSelectorDefaults(defaults, 23);
        MainConfig.migrateArenaSelectorDefaults(customized, 23);

        assertEquals(ArenaSelectorPagination.DEFAULT_SIZE,
                defaults.getInt(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE));
        assertEquals(ArenaSelectorPagination.DEFAULT_CONTENT_SLOTS,
                defaults.getString(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS));
        assertEquals(36, customized.getInt(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE));
        assertEquals("0,1,2",
                customized.getString(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS));
    }

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
    void slightlyReducesOnlyBuiltInTntDamageDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES, 5.0D);
        defaults.set(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS, 10.0D);
        YamlConfiguration customized = new YamlConfiguration();
        customized.set(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES, 3.0D);
        customized.set(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS, -1.0D);

        MainConfig.migrateTntDefaults(defaults, 33);
        MainConfig.migrateTntDefaults(customized, 33);

        assertEquals(4.0D, defaults.getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES));
        assertEquals(8.0D, defaults.getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS));
        assertEquals(3.0D, customized.getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES));
        assertEquals(-1.0D, customized.getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS));
    }

    @Test
    void doesNotRepeatTntDamageMigrationForCurrentSchema() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES, 5.0D);
        configuration.set(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS, 10.0D);

        MainConfig.migrateTntDefaults(configuration, 34);

        assertEquals(5.0D, configuration.getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES));
        assertEquals(10.0D, configuration.getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS));
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

        assertEquals(15.0, defaults.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
        assertEquals(9.5, customized.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
    }

    @Test
    void extendsOnlyThePreviousDefaultFireballSpeeds() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 11.0);
        defaults.set(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER, 1.25);
        YamlConfiguration customized = new YamlConfiguration();
        customized.set(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 13.0);
        customized.set(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER, 1.75);

        MainConfig.migrateFireballDefaults(defaults, 24);
        MainConfig.migrateFireballDefaults(customized, 24);

        assertEquals(15.0, defaults.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
        assertEquals(1.6, defaults.getDouble(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER));
        assertEquals(13.0, customized.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
        assertEquals(1.75, customized.getDouble(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER));
    }

    @Test
    void lowersOnlyThePreviousDefaultStandingSpeedAndPreservesCustomizedFireballSpeeds() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 16.0);
        defaults.set(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER, 1.5);
        YamlConfiguration customized = new YamlConfiguration();
        customized.set(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 15.5);
        customized.set(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER, 1.7);

        MainConfig.migrateFireballDefaults(defaults, 29);
        MainConfig.migrateFireballDefaults(customized, 29);

        assertEquals(15.0, defaults.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
        assertEquals(1.6, defaults.getDouble(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER));
        assertEquals(15.5, customized.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
        assertEquals(1.7, customized.getDouble(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER));
    }

    @Test
    void doesNotRepeatFireballSpeedMigration() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 10.0);

        MainConfig.migrateFireballDefaults(configuration, 23);

        assertEquals(10.0, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER));
    }

    @Test
    void addsFireballFlightRangeDefaultsWithoutOverwritingCustomValues() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigManager.CONFIG_VERSION_PATH, 26);
        configuration.set(ConfigPath.GENERAL_FIREBALL_FLIGHT_RANGE_MAX, 275.0D);
        MainConfig.addFireballFlightRangeDefaults(configuration);

        assertTrue(ConfigManager.applyVersionedMigration(configuration, 27, ignored -> { }));

        assertEquals(200.0D, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_FLIGHT_RANGE_MIN));
        assertEquals(275.0D, configuration.getDouble(ConfigPath.GENERAL_FIREBALL_FLIGHT_RANGE_MAX));
        assertEquals(27, configuration.getInt(ConfigManager.CONFIG_VERSION_PATH));
    }

    @Test
    void increasesOnlyThePreviousDefaultSneakRecoilAndFireRate() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(ConfigPath.GENERAL_FIREBALL_SNEAK_RECOIL, 0.05D);
        defaults.set(ConfigPath.GENERAL_FIREBALL_COOLDOWN, 0.5D);
        YamlConfiguration customized = new YamlConfiguration();
        customized.set(ConfigPath.GENERAL_FIREBALL_SNEAK_RECOIL, 0.07D);
        customized.set(ConfigPath.GENERAL_FIREBALL_COOLDOWN, 0.6D);

        MainConfig.migrateFireballDefaults(defaults, 28);
        MainConfig.migrateFireballDefaults(customized, 28);

        assertEquals(0.1D, defaults.getDouble(ConfigPath.GENERAL_FIREBALL_SNEAK_RECOIL));
        assertEquals(0.4D, defaults.getDouble(ConfigPath.GENERAL_FIREBALL_COOLDOWN));
        assertEquals(0.07D, customized.getDouble(ConfigPath.GENERAL_FIREBALL_SNEAK_RECOIL));
        assertEquals(0.6D, customized.getDouble(ConfigPath.GENERAL_FIREBALL_COOLDOWN));
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
    void doesNotRecreateLobbyItemsDeletedFromANewerSchema() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".custom.command", "server hub");
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".custom.slot", 8);

        MainConfig.migrateLobbyItems(configuration, 25, null);

        assertFalse(configuration.isConfigurationSection(
                ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave"));
        assertEquals("server hub", configuration.getString(
                ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".custom.command"));
    }

    @Test
    void repairsOnlyTheSchemaWindowThatDeletedTheLobbyReturnItem() {
        YamlConfiguration broken = new YamlConfiguration();
        YamlConfiguration older = new YamlConfiguration();

        MainConfig.migrateLobbyItems(broken, 16, null);
        MainConfig.migrateLobbyItems(older, 14, null);

        String leave = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
        assertEquals("RED_BED", broken.getString(leave + ".material"));
        assertEquals(8, broken.getInt(leave + ".slot"));
        assertFalse(older.isConfigurationSection(leave));
    }

    @Test
    void brokenSchemaDoesNotDuplicateARenamedReturnItemOrOccupyACustomSlot() {
        String items = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH;
        YamlConfiguration renamed = new YamlConfiguration();
        renamed.set(items + ".hub.command", " BW LEAVE ");
        renamed.set(items + ".hub.slot", 6);
        YamlConfiguration occupied = new YamlConfiguration();
        occupied.set(items + ".custom.command", "menu open");
        occupied.set(items + ".custom.slot", 8);

        MainConfig.migrateLobbyItems(renamed, 16, null);
        MainConfig.migrateLobbyItems(occupied, 16, null);

        assertFalse(renamed.isConfigurationSection(items + ".leave"));
        assertFalse(occupied.isConfigurationSection(items + ".leave"));
    }

    @Test
    void restoresCustomLobbyReturnItemFromPreDeletionBackup() {
        String leave = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
        YamlConfiguration current = new YamlConfiguration();
        MainConfig.ensureLobbyItem(current, "leave", "bw leave", false, "RED_BED", 0, 8);
        YamlConfiguration backup = new YamlConfiguration();
        backup.set(leave + ".command", "server main-lobby");
        backup.set(leave + ".material", "ENDER_PEARL");
        backup.set(leave + ".data", 0);
        backup.set(leave + ".enchanted", true);
        backup.set(leave + ".slot", 7);
        backup.set(leave + ".custom-model", 1208);

        assertTrue(MainConfig.recoverLegacyLobbyReturnItem(current, backup, 25));

        assertEquals("server main-lobby", current.getString(leave + ".command"));
        assertEquals("ENDER_PEARL", current.getString(leave + ".material"));
        assertTrue(current.getBoolean(leave + ".enchanted"));
        assertEquals(7, current.getInt(leave + ".slot"));
        assertEquals(1208, current.getInt(leave + ".custom-model"));
        assertFalse(MainConfig.recoverLegacyLobbyReturnItem(current, backup, 25));
    }

    @Test
    void recoveryDoesNotReplaceANewerCustomLobbyReturnItem() {
        String leave = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
        YamlConfiguration current = new YamlConfiguration();
        MainConfig.ensureLobbyItem(current, "leave", "bw leave", false, "BLUE_BED", 0, 5);
        YamlConfiguration backup = new YamlConfiguration();
        MainConfig.ensureLobbyItem(backup, "leave", "server old-hub", true, "ENDER_PEARL", 0, 7);

        assertFalse(MainConfig.recoverLegacyLobbyReturnItem(current, backup, 25));
        assertEquals("BLUE_BED", current.getString(leave + ".material"));
        assertEquals(5, current.getInt(leave + ".slot"));
    }

    @Test
    void recoveryRespectsCurrentDeletionAndExplicitDefault() {
        YamlConfiguration backup = new YamlConfiguration();
        MainConfig.ensureLobbyItem(backup, "leave", "server old-hub", true, "ENDER_PEARL", 0, 7);
        YamlConfiguration deletedAfterRepair = new YamlConfiguration();
        YamlConfiguration explicitDefaultInBrokenWindow = new YamlConfiguration();
        MainConfig.ensureLobbyItem(explicitDefaultInBrokenWindow,
                "leave", "bw leave", false, "RED_BED", 0, 8);

        assertFalse(MainConfig.recoverLegacyLobbyReturnItem(deletedAfterRepair, backup, 25));
        assertFalse(MainConfig.recoverLegacyLobbyReturnItem(explicitDefaultInBrokenWindow, backup, 16));
    }

    @Test
    void recoveryPreservesExtraFieldsOnAnOtherwiseDefaultCurrentItem() {
        String leave = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
        YamlConfiguration current = new YamlConfiguration();
        MainConfig.ensureLobbyItem(current, "leave", "bw leave", false, "RED_BED", 0, 8);
        current.set(leave + ".custom-model", 42);
        YamlConfiguration backup = new YamlConfiguration();
        MainConfig.ensureLobbyItem(backup, "leave", "server old-hub", true, "ENDER_PEARL", 0, 7);

        assertFalse(MainConfig.recoverLegacyLobbyReturnItem(current, backup, 25));
        assertEquals(42, current.getInt(leave + ".custom-model"));
    }

    @Test
    void recoveryTreatsExtraBackupFieldsAsCustomization() {
        String leave = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
        YamlConfiguration current = new YamlConfiguration();
        MainConfig.ensureLobbyItem(current, "leave", "bw leave", false, "RED_BED", 0, 8);
        YamlConfiguration backup = new YamlConfiguration();
        MainConfig.ensureLobbyItem(backup, "leave", "bw leave", false, "RED_BED", 0, 8);
        backup.set(leave + ".custom-model", 73);

        assertTrue(MainConfig.recoverLegacyLobbyReturnItem(current, backup, 25));
        assertEquals(73, current.getInt(leave + ".custom-model"));
    }

    @Test
    void parsesOnlyVersionedMainConfigBackupNames() {
        assertEquals(14, MainConfig.backupVersion("config.yml.v14.bak"));
        assertEquals(-1, MainConfig.backupVersion("config.yml.bak"));
        assertEquals(-1, MainConfig.backupVersion("zh_cn.yml.v14.bak"));
    }

    @Test
    void selectsTheNewestCustomSnapshotAcrossTheKnownDeletionWindow(@TempDir Path directory) throws IOException {
        saveLobbyReturnBackup(directory, 14, "ENDER_PEARL", "server old-hub");
        saveLobbyReturnBackup(directory, 15, "NETHER_STAR", "server current-hub");
        new YamlConfiguration().save(directory.resolve("config.yml.v16.bak").toFile());

        MainConfig.LegacyLobbyItemHistory backup = MainConfig.findLegacyLobbyItemHistory(directory.toFile(), 25);

        assertNotNull(backup);
        assertFalse(backup.deleted());
        assertEquals(15, backup.version());
        assertEquals("NETHER_STAR", backup.configuration().getString(
                ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave.material"));
    }

    @Test
    void doesNotFallBackPastTheLastPreDeletionSnapshot(@TempDir Path directory) throws IOException {
        saveLobbyReturnBackup(directory, 13, "ENDER_PEARL", "server obsolete-hub");
        new YamlConfiguration().save(directory.resolve("config.yml.v14.bak").toFile());

        MainConfig.LegacyLobbyItemHistory history =
                MainConfig.findLegacyLobbyItemHistory(directory.toFile(), 25);

        assertNotNull(history);
        assertTrue(history.deleted());
    }

    @Test
    void brokenWindowMissingSnapshotsKeepThePreDeletionCandidate(@TempDir Path directory) throws IOException {
        saveLobbyReturnBackup(directory, 14, "ENDER_PEARL", "server old-hub");
        new YamlConfiguration().save(directory.resolve("config.yml.v15.bak").toFile());
        new YamlConfiguration().save(directory.resolve("config.yml.v16.bak").toFile());
        new YamlConfiguration().save(directory.resolve("config.yml.v17.bak").toFile());

        MainConfig.LegacyLobbyItemHistory backup = MainConfig.findLegacyLobbyItemHistory(directory.toFile(), 25);

        assertNotNull(backup);
        assertEquals(14, backup.version());
    }

    @Test
    void explicitDefaultInsideBrokenWindowClearsTheOldCandidate(@TempDir Path directory) throws IOException {
        saveLobbyReturnBackup(directory, 14, "ENDER_PEARL", "server old-hub");
        saveBuiltInLobbyReturnBackup(directory, 15);
        new YamlConfiguration().save(directory.resolve("config.yml.v16.bak").toFile());

        assertNull(MainConfig.findLegacyLobbyItemHistory(directory.toFile(), 25));
    }

    @Test
    void postRepairDeletionBlocksHistoricalRecovery(@TempDir Path directory) throws IOException {
        saveLobbyReturnBackup(directory, 14, "ENDER_PEARL", "server old-hub");
        new YamlConfiguration().save(directory.resolve("config.yml.v18.bak").toFile());

        MainConfig.LegacyLobbyItemHistory history =
                MainConfig.findLegacyLobbyItemHistory(directory.toFile(), 25);

        assertNotNull(history);
        assertTrue(history.deleted());
    }

    @Test
    void ignoresSnapshotsFromTheCurrentOrANewerSchema(@TempDir Path directory) throws IOException {
        saveLobbyReturnBackup(directory, 14, "ENDER_PEARL", "server old-hub");
        new YamlConfiguration().save(directory.resolve("config.yml.v17.bak").toFile());

        MainConfig.LegacyLobbyItemHistory backup = MainConfig.findLegacyLobbyItemHistory(directory.toFile(), 16);

        assertNotNull(backup);
        assertEquals(14, backup.version());
    }

    @Test
    void preDeletionTombstoneBlocksBrokenWindowDefaultRepair(@TempDir Path directory) throws IOException {
        new YamlConfiguration().save(directory.resolve("config.yml.v14.bak").toFile());
        MainConfig.LegacyLobbyItemHistory history =
                MainConfig.findLegacyLobbyItemHistory(directory.toFile(), 16);
        YamlConfiguration current = new YamlConfiguration();

        MainConfig.migrateLobbyItems(current, 16, history);

        assertFalse(current.isConfigurationSection(
                ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave"));
    }

    @Test
    void postRepairTombstoneRemovesAnAutomaticallyRecreatedDefault(@TempDir Path directory) throws IOException {
        new YamlConfiguration().save(directory.resolve("config.yml.v18.bak").toFile());
        MainConfig.LegacyLobbyItemHistory history =
                MainConfig.findLegacyLobbyItemHistory(directory.toFile(), 25);
        YamlConfiguration current = new YamlConfiguration();
        MainConfig.ensureLobbyItem(current, "leave", "bw leave", false, "RED_BED", 0, 8);

        MainConfig.migrateLobbyItems(current, 25, history);

        assertFalse(current.isConfigurationSection(
                ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave"));
    }

    private static void saveLobbyReturnBackup(Path directory, int version, String material,
                                              String command) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        MainConfig.ensureLobbyItem(configuration, "leave", command, false, material, 0, 7);
        configuration.save(directory.resolve("config.yml.v" + version + ".bak").toFile());
    }

    private static void saveBuiltInLobbyReturnBackup(Path directory, int version) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        MainConfig.ensureLobbyItem(configuration, "leave", "bw leave", false, "RED_BED", 0, 8);
        configuration.save(directory.resolve("config.yml.v" + version + ".bak").toFile());
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
