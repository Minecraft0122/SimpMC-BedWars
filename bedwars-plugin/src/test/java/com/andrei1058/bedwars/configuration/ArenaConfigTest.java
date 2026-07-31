package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArenaConfigTest {

    @Test
    void forcesLocatorBarOffAcrossLegacyRuleSpellings() {
        List<String> rules = new ArrayList<>(List.of(
                "doDaylightCycle:false", "locator_bar:true", "LOCATORBAR:true"));

        ArenaConfig.forceBooleanRule(rules, "locatorBar", false);

        assertEquals(List.of("doDaylightCycle:false", "locatorBar:false"), rules);
    }

    @Test
    void forcesDaylightCycleOffAcrossLegacyRuleSpellings() {
        List<String> rules = new ArrayList<>(List.of(
                "doDaylightCycle:true", "do_daylight_cycle:true"));

        ArenaConfig.forceBooleanRule(rules, "doDaylightCycle", false);

        assertEquals(List.of("doDaylightCycle:false"), rules);
    }

    @Test
    void migratesObsoleteFireRuleAndForcesSpreadRadiusToZero() {
        List<String> rules = new ArrayList<>(List.of(
                "doDaylightCycle:false", "doFireTick:true",
                "fire_spread_radius_around_player:12"));

        ArenaConfig.forceNoFireSpread(rules);

        assertEquals(List.of("doDaylightCycle:false", "fireSpreadRadiusAroundPlayer:0"), rules);
    }

    @Test
    void migratesLegacyAquaTeamsWithoutChangingOtherColors() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("Team.Cyan.Color", "aqua");
        config.set("Team.Blue.Color", "BLUE");
        ConfigurationSection teams = config.getConfigurationSection("Team");

        ArenaConfig.migrateLegacyTeamColors(config, teams);

        assertEquals("CYAN", config.getString("Team.Cyan.Color"));
        assertEquals("BLUE", config.getString("Team.Blue.Color"));
    }

    @Test
    void migrationPreservesExistingTeamLimits() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("maxInTeam", 2);
        config.set("minInTeam", 1);

        ArenaConfig.migrateLegacyConfig(null, config);

        assertEquals(2, config.getInt("maxInTeam"));
        assertEquals(1, config.getInt("minInTeam"));
    }

    @Test
    void forcesRandomTicksOffAcrossLegacyRuleSpellings() {
        List<String> rules = new ArrayList<>(List.of(
                "randomTickSpeed:3", "random_tick_speed:12"));

        ArenaConfig.forceIntegerRule(rules, "randomTickSpeed", 0);

        assertEquals(List.of("randomTickSpeed:0"), rules);
    }

    @Test
    void keepsLegacySingleGroup() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("group", "Solo");

        ArenaConfig.migrateArenaGroups(config);

        assertEquals("Solo", config.getString("group"));
        assertFalse(config.contains("groups", true));
    }

    @Test
    void migratesMultipleGroupsToTheirPrimaryEntry() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("groups", List.of("Solo", "solo", " Featured "));
        config.set("group", "Doubles");

        ArenaConfig.migrateArenaGroups(config);

        assertEquals("Solo", config.getString("group"));
        assertFalse(config.contains("groups", true));
    }
}
