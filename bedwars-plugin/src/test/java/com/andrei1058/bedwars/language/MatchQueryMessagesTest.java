package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchQueryMessagesTest {
    @Test
    void updatesOnlyTheUnchangedBuiltInLanguageUsage() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(ConfigManager.CONFIG_VERSION_PATH, 22);
        language.set(Messages.COMMAND_LANG_USAGE, "{prefix}&用法：/lang &f&o<iso>");
        language.addDefault(Messages.COMMAND_LANG_USAGE, "{prefix}&7用法：/bw lang &f&o<iso>");
        language.addDefault(MatchQueryMessages.Key.HISTORY_ROW.path(), "&e#{number} {arena}");

        assertTrue(ConfigManager.applyVersionedMigration(language, 23,
                MatchQueryMessages::migrateLegacyLangUsage));
        assertEquals("{prefix}&7用法：/bw lang &f&o<iso>",
                language.getString(Messages.COMMAND_LANG_USAGE));
        assertEquals("&e#{number} {arena}",
                language.getString(MatchQueryMessages.Key.HISTORY_ROW.path()));

        YamlConfiguration custom = new YamlConfiguration();
        custom.set(ConfigManager.CONFIG_VERSION_PATH, 22);
        custom.set(Messages.COMMAND_LANG_USAGE, "&b我的语言命令帮助");
        custom.addDefault(Messages.COMMAND_LANG_USAGE, "{prefix}&7用法：/bw lang &f&o<iso>");
        ConfigManager.applyVersionedMigration(custom, 23, MatchQueryMessages::migrateLegacyLangUsage);
        assertEquals("&b我的语言命令帮助", custom.getString(Messages.COMMAND_LANG_USAGE));
    }

    @Test
    void newMatchTemplatesCanBeRenderedInEitherLanguage() {
        assertEquals("&6对局历史 &7全部记录 · 第 2 页",
                MatchQueryMessages.defaults(true).text(MatchQueryMessages.Key.HISTORY_HEADER,
                        "{target}", "全部记录", "{page}", "2"));
        assertEquals("&6Match history &7All matches · Page 2",
                MatchQueryMessages.defaults(false).text(MatchQueryMessages.Key.HISTORY_HEADER,
                        "{target}", "All matches", "{page}", "2"));
    }

    @Test
    void newKeysArePlacedBesideTheExistingCommandKeys() {
        Language language = mock(Language.class);
        YamlConfiguration config = new YamlConfiguration();
        when(language.getIso()).thenReturn("en");
        when(language.getYml()).thenReturn(config);
        config.addDefault(Messages.COMMAND_LANG_USAGE_DENIED, "old command key");
        MatchQueryMessages.registerDefaults(language);
        config.addDefault(Messages.COMMAND_JOIN_USAGE, "next command key");

        var keys = new java.util.ArrayList<>(config.getDefaults().getKeys(false));
        assertTrue(keys.indexOf(Messages.COMMAND_LANG_USAGE_DENIED)
                < keys.indexOf(MatchQueryMessages.Key.HISTORY_ROW.path()));
        assertTrue(keys.indexOf(MatchQueryMessages.Key.HISTORY_ROW.path())
                < keys.indexOf(Messages.COMMAND_JOIN_USAGE));
    }
}
