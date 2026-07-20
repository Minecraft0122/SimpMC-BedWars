package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplifiedChineseMigrationTest {

    @Test
    void arenaTabKeepsUpstreamWidthWhileLobbyIsWider() {
        assertEquals(104, SimplifiedChinese.ORIGINAL_TAB_WIDTH);
        assertEquals(104, SimplifiedChinese.ORIGINAL_TAB_WIDTH_SPACER.length());
        assertTrue(SimplifiedChinese.ORIGINAL_TAB_WIDTH_SPACER.chars()
                .allMatch(character -> character == ' '));
        assertEquals(128, SimplifiedChinese.LOBBY_TAB_WIDTH);
        assertEquals(128, SimplifiedChinese.LOBBY_TAB_WIDTH_SPACER.length());
        assertTrue(SimplifiedChinese.LOBBY_TAB_WIDTH > SimplifiedChinese.ORIGINAL_TAB_WIDTH);
    }

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
    void spectatorStartingAffixesUseTheExactUpstreamPaths() {
        assertEquals(Messages.FORMATTING_SB_TAB_STARTING_PREFIX,
                Messages.FORMATTING_SB_TAB_STARTING_PREFIX_SPEC);
        assertEquals(Messages.FORMATTING_SB_TAB_STARTING_SUFFIX,
                Messages.FORMATTING_SB_TAB_STARTING_SUFFIX_SPEC);
    }

    @Test
    void widensOnlyTheOldBuiltInLobbyHeader() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(Messages.FORMATTING_SB_TAB_LOBBY_HEADER,
                List.of(SimplifiedChinese.ORIGINAL_TAB_WIDTH_SPACER, "&a{serverIp}", ""));

        SimplifiedChinese.widenBuiltInLobbyTabHeader(language);

        assertEquals(List.of(SimplifiedChinese.LOBBY_TAB_WIDTH_SPACER, "&a{serverIp}", ""),
                language.getStringList(Messages.FORMATTING_SB_TAB_LOBBY_HEADER));
    }

    @Test
    void lobbyWidthMigrationPreservesCustomHeader() {
        YamlConfiguration language = new YamlConfiguration();
        List<String> custom = List.of("custom width", "&b自定义大厅");
        language.set(Messages.FORMATTING_SB_TAB_LOBBY_HEADER, custom);

        SimplifiedChinese.widenBuiltInLobbyTabHeader(language);

        assertEquals(custom, language.getStringList(Messages.FORMATTING_SB_TAB_LOBBY_HEADER));
    }

    @Test
    void migratesOnlyBuiltInArenaJoinAndPlayingTeamMessages() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(Messages.COMMAND_JOIN_PLAYER_JOIN_MSG,
                "{prefix}&7{player}&e加入了游戏(&b{on}&e/&b{max}&e)！");
        language.set(Messages.FORMATTING_SB_TAB_PLAYING_FOOTER,
                List.of("", "&f你正在为 {teamColor}{teamName}队 &f而战", "&a{serverIp}", "&f由 {poweredBy} 提供支持", ""));
        language.set(Messages.FORMATTING_SB_TAB_PLAYING_PREFIX, List.of("{teamColor}{teamName} "));
        language.set(Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX,
                List.of("&6&l⭐ {teamColor}{teamName} "));

        SimplifiedChinese.migrateCurrentMessageDefaults(language);

        assertEquals("&e[BW] &f玩家 &b{playername} &f加入了竞技场 &a{arena}",
                language.getString(Messages.COMMAND_JOIN_PLAYER_JOIN_MSG));
        assertEquals("&f你属于 {teamColor}{teamName}队",
                language.getStringList(Messages.FORMATTING_SB_TAB_PLAYING_FOOTER).get(1));
        assertEquals(List.of(""), language.getStringList(Messages.FORMATTING_SB_TAB_PLAYING_PREFIX));
        assertEquals(List.of(""), language.getStringList(Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX));
    }

    @Test
    void preservesCustomArenaJoinAndPlayingTeamMessages() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(Messages.COMMAND_JOIN_PLAYER_JOIN_MSG, "custom join");
        language.set(Messages.FORMATTING_SB_TAB_PLAYING_FOOTER, List.of("custom footer"));

        SimplifiedChinese.migrateCurrentMessageDefaults(language);

        assertEquals("custom join", language.getString(Messages.COMMAND_JOIN_PLAYER_JOIN_MSG));
        assertEquals(List.of("custom footer"),
                language.getStringList(Messages.FORMATTING_SB_TAB_PLAYING_FOOTER));
    }

    @Test
    void addsArenaInviteToTheUnchangedBuiltInCommandHelp() {
        YamlConfiguration language = new YamlConfiguration();
        language.set(Messages.COMMAND_MAIN, List.of("", "&2▪ &7/bw stats", "&2▪ &7/bw join &o<游戏/模式>",
                "&2▪ &7/bw leave", "&2▪ &7/bw lang", "&2▪ &7/bw gui", "&2▪ &7/bw start &3（赞助者）"));

        SimplifiedChinese.migrateCommandHelp(language, "bw");

        assertTrue(language.getStringList(Messages.COMMAND_MAIN).contains("&2▪ &7/bw invite &o<大厅玩家>"));
    }
}
