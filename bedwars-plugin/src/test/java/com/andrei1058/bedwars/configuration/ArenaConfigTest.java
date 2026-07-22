package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaConfigTest {

    @Test
    void forcesLocatorBarOffAcrossLegacyRuleSpellings() {
        List<String> rules = new ArrayList<>(List.of(
                "doDaylightCycle:false", "locator_bar:true", "LOCATORBAR:true"));

        ArenaConfig.forceBooleanRule(rules, "locatorBar", false);

        assertEquals(List.of("doDaylightCycle:false", "locatorBar:false"), rules);
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
}
