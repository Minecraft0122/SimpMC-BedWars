package com.andrei1058.bedwars.arena;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RequiredTeamGeneratorValidatorTest {

    @Test
    void reportsMissingIronAndGoldPerTeam() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("Team.Red.Iron", List.of("world,1,2,3"));
        configuration.set("Team.Red.Gold", List.of("world,1,2,3"));
        configuration.set("Team.Blue.Iron", "world,4,5,6");
        configuration.set("Team.Green.Gold", List.of("world,7,8,9"));

        Map<String, List<String>> missing = RequiredTeamGeneratorValidator.findMissing(
                configuration, List.of("Red", "Blue", "Green"));

        assertFalse(missing.containsKey("Red"));
        assertEquals(List.of("金"), missing.get("Blue"));
        assertEquals(List.of("铁"), missing.get("Green"));
    }

    @Test
    void blankOrEmptyLocationsStillCountAsMissing() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("Team.Red.Iron", List.of(" "));
        configuration.set("Team.Red.Gold", List.of());

        assertEquals(List.of("铁", "金"), RequiredTeamGeneratorValidator.findMissing(
                configuration, List.of("Red")).get("Red"));
    }
}
