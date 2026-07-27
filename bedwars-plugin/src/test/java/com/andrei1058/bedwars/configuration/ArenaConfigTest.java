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
    void clampsMinimumTeamSizeToTheConfiguredCapacity() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("maxInTeam", 3);
        config.set("minInTeam", 8);

        ArenaConfig.normalizeTeamLimits(config);

        assertEquals(3, config.getInt("maxInTeam"));
        assertEquals(3, config.getInt("minInTeam"));
    }

    @Test
    void repairsNonPositiveTeamLimits() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("maxInTeam", 0);
        config.set("minInTeam", -2);

        ArenaConfig.normalizeTeamLimits(config);

        assertEquals(1, config.getInt("maxInTeam"));
        assertEquals(1, config.getInt("minInTeam"));
    }

    @Test
    void migratesLegacySingleGroupToOrderedGroupList() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("group", "Solo");

        ArenaConfig.migrateArenaGroups(config);

        assertEquals(List.of("Solo"), config.getStringList("groups"));
        assertFalse(config.contains("group", true));
    }

    @Test
    void normalizesExistingMultipleGroupsDuringMigration() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("groups", List.of("Solo", "solo", " Featured "));
        config.set("group", "Doubles");

        ArenaConfig.migrateArenaGroups(config);

        assertEquals(List.of("Solo", "Featured"), config.getStringList("groups"));
        assertFalse(config.contains("group", true));
    }
}
