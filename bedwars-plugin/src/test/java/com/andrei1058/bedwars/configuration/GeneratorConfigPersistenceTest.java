package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneratorConfigPersistenceTest {
    @TempDir Path directory;

    @ParameterizedTest @ValueSource(booleans={true, false})
    void emptyIslandSwitchSurvivesUpgradeAndRestart(boolean enabled) throws Exception {
        Path file = directory.resolve("arena.yml");
        Files.writeString(file, "config-version: 23\ndisable-generator-for-empty-teams: " + enabled + "\ncustom-option: retained\n");
        new ArenaConfig(plugin(), "arena", directory.toString());
        new ArenaConfig(plugin(), "arena", directory.toString());
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals(enabled, saved.getBoolean(ConfigPath.ARENA_DISABLE_GENERATOR_FOR_EMPTY_TEAMS));
        assertEquals(24, saved.getInt("config-version"));
        assertEquals("retained", saved.getString("custom-option"));
    }

    @Test void generatorMigrationPreservesAllExistingSettingsAndAddsOnlyMissingDefaults() throws Exception {
        Path file = directory.resolve("generators.yml");
        Files.writeString(file, """
                config-version: 6
                stack-items: true
                Default:
                  iron:
                    delay: 1
                    amount: 9
                    spawn-limit: 123
                  gold:
                    delay: 4
                Solo:
                  diamond:
                    tierI:
                      delay: 8
                      spawn-limit: 55
                """);
        new GeneratorsConfig(plugin(), "generators", directory.toString());
        new GeneratorsConfig(plugin(), "generators", directory.toString());
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals(7, saved.getInt("config-version"));
        assertEquals(1, saved.getInt("Default.iron.delay"));
        assertEquals(4, saved.getInt("Default.gold.delay"));
        assertEquals(9, saved.getInt("Default.iron.amount"));
        assertEquals(123, saved.getInt("Default.iron.spawn-limit"));
        assertEquals(55, saved.getInt("Solo.diamond.tierI.spawn-limit"));
        assertTrue(saved.getBoolean("stack-items"));
        new GeneratorsConfig(plugin(), "fresh", directory.toString());
        YamlConfiguration fresh = YamlConfiguration.loadConfiguration(directory.resolve("fresh.yml").toFile());
        assertEquals(2, fresh.getInt("Default.iron.delay"));
        assertEquals(6, fresh.getInt("Default.gold.delay"));
    }

    private Plugin plugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("SimpMC-BedWars");
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("SimpMC-BedWars", "test", "unused"));
        when(plugin.getLogger()).thenReturn(Logger.getLogger(getClass().getName()));
        return plugin;
    }
}
