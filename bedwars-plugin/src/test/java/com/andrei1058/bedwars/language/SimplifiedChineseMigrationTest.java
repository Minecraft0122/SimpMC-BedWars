package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SimplifiedChineseMigrationTest {

    @Test
    void replacesCompactTabTemplateWithOriginalRepositoryStyle() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(Messages.FORMATTING_SB_TAB_LOBBY_FOOTER,
                List.of("", "&f当前大厅共有 {on} 名玩家", "由 {poweredBy} 提供支持,&a{serverIp}", ""));
        defaults.set(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX,
                List.of(" {vPrefix}", " &c&o已淘汰", " {level}", " &c&o已淘汰"));
        YamlConfiguration language = new YamlConfiguration();
        language.setDefaults(defaults);
        language.set(Messages.FORMATTING_SB_TAB_LOBBY_FOOTER,
                List.of("&7大厅人数：&f{on}", "&8{poweredBy}"));
        language.set(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX,
                List.of(" &7[已淘汰]"));

        SimplifiedChinese.migrateOriginalTabMessages(language);

        assertEquals(defaults.getStringList(Messages.FORMATTING_SB_TAB_LOBBY_FOOTER),
                language.getStringList(Messages.FORMATTING_SB_TAB_LOBBY_FOOTER));
        assertEquals(defaults.getStringList(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX),
                language.getStringList(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX));
    }

    @Test
    void preservesCustomChineseTextAndItsConfiguredWidth() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(Messages.FORMATTING_SB_TAB_LOBBY_HEADER,
                List.of("                                                                                                        ", "&a{serverIp}", ""));
        YamlConfiguration language = new YamlConfiguration();
        language.setDefaults(defaults);
        List<String> custom = List.of("                                                                ", "&b自定义标题");
        language.set(Messages.FORMATTING_SB_TAB_LOBBY_HEADER, custom);

        SimplifiedChinese.migrateOriginalTabMessages(language);

        assertEquals(custom, language.getStringList(Messages.FORMATTING_SB_TAB_LOBBY_HEADER));
    }

    @Test
    void preservesAConfiguredTabEvenWhenItContainsALegacyWord() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(Messages.FORMATTING_SB_TAB_STARTING_HEADER,
                List.of("&a&l{serverIp}", "&7地图：&f{map}"));
        YamlConfiguration language = new YamlConfiguration();
        language.setDefaults(defaults);
        List<String> custom = List.of("&7Map: &f{map}", "&bCustom tournament");
        language.set(Messages.FORMATTING_SB_TAB_STARTING_HEADER, custom);

        SimplifiedChinese.migrateOriginalTabMessages(language);

        assertEquals(custom, language.getStringList(Messages.FORMATTING_SB_TAB_STARTING_HEADER));
    }

    @Test
    void spectatorStartingAffixesUseTheirOwnConfigurationPaths() {
        assertNotEquals(Messages.FORMATTING_SB_TAB_STARTING_PREFIX,
                Messages.FORMATTING_SB_TAB_STARTING_PREFIX_SPEC);
        assertNotEquals(Messages.FORMATTING_SB_TAB_STARTING_SUFFIX,
                Messages.FORMATTING_SB_TAB_STARTING_SUFFIX_SPEC);
    }
}
