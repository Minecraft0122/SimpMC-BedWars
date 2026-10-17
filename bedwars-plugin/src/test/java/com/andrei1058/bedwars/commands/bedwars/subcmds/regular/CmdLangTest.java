package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.MainConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CmdLangTest {
    @Test
    void listIsAvailableInAnArenaAndDoesNotFallThroughToUnknownCommand() {
        Player player = mock(Player.class);
        try (MockedStatic<MainCommand> main = mockStatic(MainCommand.class);
             MockedStatic<Language> language = mockStatic(Language.class)) {
            CmdLang command = command(main);
            language.when(Language::getLanguages).thenReturn(List.of());
            language.when(() -> Language.getMsg(player, Messages.COMMAND_LANG_LIST_HEADER))
                    .thenReturn("&2Available languages:");
            language.when(() -> Language.getMsg(player, Messages.COMMAND_LANG_USAGE))
                    .thenReturn("&7Usage: /bw lang <iso>");

            assertTrue(command.execute(new String[0], player));
            assertTrue(command.canSee(player, null));
            verify(player, times(2)).sendMessage(any(Component.class));
        }
    }

    @Test
    void changingLanguageInArenaExplainsWhyItIsDenied() {
        Player player = mock(Player.class);
        BedWars.config = mock(MainConfig.class);
        try (MockedStatic<MainCommand> main = mockStatic(MainCommand.class);
             MockedStatic<Language> language = mockStatic(Language.class);
             MockedStatic<Arena> arena = mockStatic(Arena.class)) {
            CmdLang command = command(main);
            Arena currentArena = mock(Arena.class);
            arena.when(() -> Arena.getArenaByPlayer(player)).thenReturn(currentArena);
            language.when(() -> Language.getMsg(player, Messages.COMMAND_LANG_USAGE_DENIED))
                    .thenReturn("&cCannot change language during a match.");

            assertTrue(command.execute(new String[]{"en"}, player));
            ArgumentCaptor<Component> sent = ArgumentCaptor.forClass(Component.class);
            verify(player).sendMessage(sent.capture());
            assertTrue(AdventureText.plain(sent.getValue()).contains("Cannot change language"));
            language.verify(() -> Language.setPlayerLanguage(any(), anyString()), never());
        }
    }

    private static CmdLang command(MockedStatic<MainCommand> main) {
        MainCommand mainCommand = mock(MainCommand.class);
        when(mainCommand.getName()).thenReturn("bw");
        main.when(MainCommand::getInstance).thenReturn(mainCommand);
        ParentCommand parent = mock(ParentCommand.class);
        when(parent.getName()).thenReturn("bw");
        return new CmdLang(parent, "lang");
    }
}
