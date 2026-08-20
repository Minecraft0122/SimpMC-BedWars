package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BuiltInLanguageTabDefaultsTest {

    private static final List<String> BUILT_IN_LANGUAGES = List.of(
            "Bangla", "English", "Hindi", "Indonesia", "Italian", "Persian", "Polish",
            "Portuguese", "Romanian", "Russian", "SimplifiedChinese", "Spanish", "Turkish"
    );

    private static final Map<String, String> TEAM_PREFIX_PATHS = Map.of(
            "FORMATTING_SB_TAB_PLAYING_PREFIX", Messages.FORMATTING_SB_TAB_PLAYING_PREFIX,
            "FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX", Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX,
            "FORMATTING_SB_TAB_RESTARTING_WIN2_PREFIX", Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_PREFIX,
            "FORMATTING_SB_TAB_RESTARTING_ELM_PREFIX", Messages.FORMATTING_SB_TAB_RESTARTING_ELM_PREFIX
    );

    @Test
    void everyBuiltInLanguageUsesFullTeamNameForManagedTeamPrefixes() throws Exception {
        Path testClasses = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        Path moduleRoot = testClasses.getParent().getParent();
        Path languageSources = moduleRoot.resolve(
                "src/main/java/com/andrei1058/bedwars/language");

        for (String language : BUILT_IN_LANGUAGES) {
            String source = Files.readString(languageSources.resolve(language + ".java"));
            for (String constant : TEAM_PREFIX_PATHS.keySet()) {
                Pattern declaration = Pattern.compile(
                        "yml\\.addDefault\\(Messages\\." + constant
                                + ",\\s*List\\.of\\(\"\\{teamColor}\\{teamName} \"\\)\\);"
                );
                assertEquals(1, declaration.matcher(source).results().count(),
                        language + " must declare the full team-name default for " + constant);
            }
        }
    }

    @Test
    void currentSchemaMissingKeysFallBackToFullTeamNameDefaults() {
        for (String language : BUILT_IN_LANGUAGES) {
            int currentSchema = language.equals("SimplifiedChinese") ? 19 : 9;
            YamlConfiguration current = new YamlConfiguration();
            current.set(ConfigManager.CONFIG_VERSION_PATH, currentSchema);
            TEAM_PREFIX_PATHS.values().forEach(path ->
                    current.addDefault(path, List.of("{teamColor}{teamName} ")));

            TEAM_PREFIX_PATHS.values().forEach(path ->
                    assertFalse(current.contains(path, true),
                            language + " must leave the current-schema key absent: " + path));
            assertFalse(ConfigManager.applyVersionedMigration(current, currentSchema,
                            Language::migrateBuiltInTabPlayerRows),
                    language + " must not rerun migration at its current schema");
            TEAM_PREFIX_PATHS.values().forEach(path ->
                    assertEquals(List.of("{teamColor}{teamName} "), current.getStringList(path),
                            language + " must fall back to the full team-name default for " + path));
        }
    }
}
