package com.andrei1058.bedwars.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaConfigTest {

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
