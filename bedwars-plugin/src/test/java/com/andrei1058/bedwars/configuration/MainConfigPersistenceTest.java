package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MainConfigPersistenceTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 14, 18, 27, 33, 34})
    void preservesConfiguredProxyLobbyAcrossUpgradeSaveAndRestart(int storedVersion,
                                                                @TempDir Path directory) throws IOException {
        Path configFile = directory.resolve("config.yml");
        Files.writeString(configFile, "config-version: " + storedVersion
                + "\nlobbyServer: login\nlanguage: zh_cn\nserverType: MULTIARENA\n");

        BedWars previousPlugin = BedWars.plugin;
        BedWars plugin = mock(BedWars.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("BedWars", "test", "unused"));
        when(plugin.getLogger()).thenReturn(Logger.getLogger(getClass().getName()));
        Language language = mock(Language.class);
        BedWars.plugin = plugin;

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Language> languages = mockStatic(Language.class);
             MockedStatic<BedWars> bedWars = mockStatic(BedWars.class)) {
            bukkit.when(Bukkit::getWorldContainer).thenReturn(directory.resolve("worlds").toFile());
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of());
            languages.when(() -> Language.isSimplifiedChineseIso("zh_cn")).thenReturn(true);
            languages.when(() -> Language.getLang("zh_cn")).thenReturn(language);

            MainConfig upgraded = new MainConfig(plugin, "config");
            upgraded.save();
            assertEquals("login", upgraded.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_SERVER));

            YamlConfiguration saved = YamlConfiguration.loadConfiguration(configFile.toFile());
            assertEquals("login", saved.getString("lobbyServer"));
            assertTrue(saved.getInt(ConfigManager.CONFIG_VERSION_PATH) >= storedVersion);

            MainConfig restarted = new MainConfig(plugin, "config");
            restarted.save();
            restarted.reload();

            assertEquals("login", restarted.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_SERVER));
            assertEquals("login", YamlConfiguration.loadConfiguration(configFile.toFile()).getString("lobbyServer"));
        } finally {
            BedWars.plugin = previousPlugin;
        }
    }
}
