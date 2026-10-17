package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

}
