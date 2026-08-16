package com.andrei1058.bedwars.api.language;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageTabPlayerRowsMigrationTest {

    @Test
    void schemaSevenEmptyBuiltInPrefixesGainTheVisibleTeamFallback() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(ConfigManager.CONFIG_VERSION_PATH, 7);
        for (String path : List.of(
                Messages.FORMATTING_SB_TAB_PLAYING_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_ELM_PREFIX)) {
            language.set(path, List.of(""));
        }

        assertTrue(ConfigManager.applyVersionedMigration(language, 8,
                Language::migrateBuiltInTabPlayerRows));

        for (String path : List.of(
                Messages.FORMATTING_SB_TAB_PLAYING_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_ELM_PREFIX)) {
            assertEquals(List.of("{teamColor}[{teamLetter}] "), language.getStringList(path), path);
        }
    }

    @Test
    void latestSchemaUsesCompactPrefixesForKnownBuiltInTeamLabels() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(ConfigManager.CONFIG_VERSION_PATH, 5);
        language.set(Messages.FORMATTING_SB_TAB_PLAYING_PREFIX,
                List.of("{teamColor}{teamName} "));
        language.set(Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX,
                List.of("&6&l⭐ {teamColor}{teamName} "));
        language.set(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_PREFIX,
                List.of("&6★ {teamColor}[{teamLetter}] "));
        language.set(Messages.FORMATTING_SB_TAB_RESTARTING_ELM_PREFIX,
                List.of("{teamColor}[{teamLetter}] "));
        language.set(Messages.FORMATTING_SB_TAB_PLAYING_ELM_SUFFIX,
                List.of(" &c&oEliminated {teamColor}&o{teamName}",
                        " {teamColor}&oEliminated {vPrefix}",
                        "{teamColor}&oEliminated {level}"));

        assertTrue(ConfigManager.applyVersionedMigration(language, 8,
                Language::migrateBuiltInTabPlayerRows));

        for (String path : List.of(
                Messages.FORMATTING_SB_TAB_PLAYING_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_PREFIX,
                Messages.FORMATTING_SB_TAB_RESTARTING_ELM_PREFIX)) {
            assertEquals(List.of("{teamColor}[{teamLetter}] "), language.getStringList(path), path);
        }
        assertEquals(List.of(" &c&oEliminated", " {teamColor}&oEliminated {vPrefix}",
                        "{teamColor}&oEliminated {level}"),
                language.getStringList(Messages.FORMATTING_SB_TAB_PLAYING_ELM_SUFFIX));
        assertEquals(8, language.getInt(ConfigManager.CONFIG_VERSION_PATH));
    }

    @Test
    void latestSchemaPreservesAdministratorPlayerRows() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(ConfigManager.CONFIG_VERSION_PATH, 5);
        List<String> customPrefix = List.of("&d[联赛] {teamColor}{teamName} ");
        List<String> customSuffix = List.of(" &e自定义");
        language.set(Messages.FORMATTING_SB_TAB_PLAYING_PREFIX, customPrefix);
        language.set(Messages.FORMATTING_SB_TAB_PLAYING_ELM_SUFFIX, customSuffix);

        assertTrue(ConfigManager.applyVersionedMigration(language, 8,
                Language::migrateBuiltInTabPlayerRows));

        assertEquals(customPrefix, language.getStringList(Messages.FORMATTING_SB_TAB_PLAYING_PREFIX));
        assertEquals(customSuffix, language.getStringList(Messages.FORMATTING_SB_TAB_PLAYING_ELM_SUFFIX));
    }

    @Test
    void historicalChineseEliminatedRowDoesNotBecomeEnglish() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(Messages.FORMATTING_SB_TAB_PLAYING_ELM_SUFFIX,
                List.of(" &c&o已淘汰 {teamColor}&o{teamName}",
                        " {teamColor}&o已淘汰 {vPrefix}",
                        "{teamColor}&o已淘汰 {level}"));

        Language.migrateBuiltInTabPlayerRows(language);

        assertEquals(List.of(""),
                language.getStringList(Messages.FORMATTING_SB_TAB_PLAYING_ELM_SUFFIX));
    }
}
