package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SimplifiedChineseMigrationTest {

    @Test
    void replacesLegacyEnglishTabTemplateWithCompactChineseDefault() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(Messages.FORMATTING_SB_TAB_LOBBY_FOOTER,
                List.of("&7大厅人数：&f{on}", "&8{poweredBy}"));
        defaults.set(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX,
                List.of(" &7[已淘汰]"));
        YamlConfiguration language = new YamlConfiguration();
        language.setDefaults(defaults);
        language.set(Messages.FORMATTING_SB_TAB_LOBBY_FOOTER,
                List.of("&fThere are {on} players on this lobby", "Powered by {poweredBy},&a{serverIp}"));
        language.set(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX,
                List.of(" {vPrefix}", " &c&oEliminated", " {level}", " &c&oEliminated"));

        SimplifiedChinese.migrateCompactTabMessages(language);

        assertEquals(defaults.getStringList(Messages.FORMATTING_SB_TAB_LOBBY_FOOTER),
                language.getStringList(Messages.FORMATTING_SB_TAB_LOBBY_FOOTER));
        assertEquals(defaults.getStringList(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX),
                language.getStringList(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX));
    }

    @Test
    void removesWidthSpacerWithoutOverwritingCustomChineseText() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set(Messages.FORMATTING_SB_TAB_LOBBY_HEADER, List.of("&a&l{serverIp}"));
        YamlConfiguration language = new YamlConfiguration();
        language.setDefaults(defaults);
        language.set(Messages.FORMATTING_SB_TAB_LOBBY_HEADER,
                List.of("                                                                ", "&b自定义标题"));

        SimplifiedChinese.migrateCompactTabMessages(language);

        assertEquals(List.of("&b自定义标题"),
                language.getStringList(Messages.FORMATTING_SB_TAB_LOBBY_HEADER));
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

        SimplifiedChinese.migrateCompactTabMessages(language);

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
