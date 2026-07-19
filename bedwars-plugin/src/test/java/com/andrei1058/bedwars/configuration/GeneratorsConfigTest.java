package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratorsConfigTest {

    @Test
    void upgradesOnlyUnchangedDefaultGeneratorDelays() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("Default." + ConfigPath.GENERATOR_IRON_DELAY, 2);
        configuration.set("Default." + ConfigPath.GENERATOR_GOLD_DELAY, 6);
        configuration.set("Solo." + ConfigPath.GENERATOR_IRON_DELAY, 2);
        configuration.set("Solo." + ConfigPath.GENERATOR_GOLD_DELAY, 6);

        GeneratorsConfig.migrateLegacyDefaults(configuration);

        assertEquals(1, configuration.getInt("Default." + ConfigPath.GENERATOR_IRON_DELAY));
        assertEquals(4, configuration.getInt("Default." + ConfigPath.GENERATOR_GOLD_DELAY));
        assertEquals(2, configuration.getInt("Solo." + ConfigPath.GENERATOR_IRON_DELAY));
        assertEquals(6, configuration.getInt("Solo." + ConfigPath.GENERATOR_GOLD_DELAY));
    }

    @Test
    void preservesCustomizedDefaultGeneratorDelays() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("Default." + ConfigPath.GENERATOR_IRON_DELAY, 3);
        configuration.set("Default." + ConfigPath.GENERATOR_GOLD_DELAY, 5);

        GeneratorsConfig.migrateLegacyDefaults(configuration);

        assertEquals(3, configuration.getInt("Default." + ConfigPath.GENERATOR_IRON_DELAY));
        assertEquals(5, configuration.getInt("Default." + ConfigPath.GENERATOR_GOLD_DELAY));
    }
}
