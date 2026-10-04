package com.andrei1058.bedwars.listeners.chat;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.levels.Level;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.MainConfig;
import com.andrei1058.bedwars.support.vault.Chat;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SoloChatTest {
    @Test
    void ordinarySoloChatRemovesBuiltInAndCustomPlaceholderTeamPrefixes() {
        String result = format(1, "你好", "{team} %bw1058_player_team% {player}");
        assertFalse(result.contains("[红队]"));
        assertFalse(result.contains("{team}"));
        assertFalse(result.contains("%bw1058_player_team%"));
    }

    @Test
    void soloShoutAndTheShoutCommandChatPathKeepOnlyThePublicMarker() {
        String result = format(1, "!你好", "[公屏] {team} {player}");
        assertTrue(result.contains("[公屏]"));
        assertFalse(result.contains("[红队]"));
        assertFalse(result.contains("{team}"));
        assertFalse(result.contains("!你好"));
    }

    @Test
    void customSoloShoutPlaceholderKeepsThePublicMarker() {
        String result = format(1, "!你好", "%bw1058_player_team% {player}");
        assertTrue(result.contains("[公屏]"));
        assertFalse(result.contains("%bw1058_player_team%"));
    }

    @Test
    void teamChatKeepsItsPrefixEvenAfterOnlyOneTeammateRemains() {
        assertTrue(format(2, "你好", "{team} {player}").contains("[红队]"));
        assertTrue(format(2, "!你好", "[公屏] {team} {player}").contains("[红队]"));
    }

    private String format(int startSize, String message, String template) {
        Player player = mock(Player.class);
        Player opponent = mock(Player.class);
        Player spectator = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Alice");
        when(player.displayName()).thenReturn(Component.text("Alice"));
        when(player.hasPermission("bw.shout")).thenReturn(true);
        IArena arena = mock(IArena.class);
        ITeam team = mock(ITeam.class);
        when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.getTeam(player)).thenReturn(team);
        when(arena.getTeamSizeAtGameStart(team)).thenReturn(startSize);
        when(arena.getPlayers()).thenReturn(List.of(player, opponent));
        when(arena.getSpectators()).thenReturn(List.of(spectator));
        when(team.getMembers()).thenReturn(List.of(player));
        when(team.getColor()).thenReturn(TeamColor.RED);
        when(team.getDisplayName(any(Language.class))).thenReturn("红队");
        Language language = mock(Language.class);
        when(language.m(anyString())).thenReturn("");
        when(language.m(Messages.MEANING_SHOUT)).thenReturn("公屏");
        for (String key : List.of(Messages.FORMATTING_CHAT_LOBBY, Messages.FORMATTING_CHAT_TEAM,
                Messages.FORMATTING_CHAT_SHOUT)) {
            when(language.m(key)).thenReturn(template + " > {message}");
        }
        Chat chat = mock(Chat.class);
        when(chat.getPrefix(player)).thenReturn("");
        when(chat.getSuffix(player)).thenReturn("");
        Level level = mock(Level.class);
        when(level.getLevel(player)).thenReturn("");
        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        Component[] body = {Component.text(message)};
        when(event.message()).thenAnswer(call -> body[0]);
        doAnswer(call -> body[0] = call.getArgument(0)).when(event).message(any(Component.class));
        Set<Audience> viewers = new HashSet<>();
        when(event.viewers()).thenReturn(viewers);
        MainConfig originalConfig = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        try (MockedStatic<BedWars> bedWars = mockStatic(BedWars.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Arena> arenas = mockStatic(Arena.class);
             MockedStatic<Language> languages = mockStatic(Language.class)) {
            bedWars.when(BedWars::getServerType).thenReturn(ServerType.BUNGEE);
            bedWars.when(BedWars::getChatSupport).thenReturn(chat);
            bedWars.when(BedWars::getLevelSupport).thenReturn(level);
            bukkit.when(Bukkit::getConsoleSender).thenReturn(mock(ConsoleCommandSender.class));
            arenas.when(() -> Arena.getArenaByPlayer(player)).thenReturn(arena);
            languages.when(() -> Language.getPlayerLanguage(player)).thenReturn(language);
            languages.when(() -> Language.getMsg(player, Messages.FORMAT_PAPI_PLAYER_TEAM_TEAM))
                    .thenReturn("{TeamColor}[{TeamName}]");
            languages.when(() -> Language.getMsg(player, Messages.FORMAT_PAPI_PLAYER_TEAM_SHOUT))
                    .thenReturn("[公屏]");

            new ChatFormatting().onChat(event);
            ArgumentCaptor<ChatRenderer> renderer = ArgumentCaptor.forClass(ChatRenderer.class);
            verify(event).renderer(renderer.capture());
            String result = PlainTextComponentSerializer.plainText().serialize(renderer.getValue()
                    .render(player, player.displayName(), body[0], player));
            assertTrue(result.contains("Alice"));
            assertTrue(result.contains("你好"));
            assertEquals(startSize == 1 || message.startsWith("!"), viewers.contains(opponent));
            return result;
        } finally {
            BedWars.config = originalConfig;
        }
    }
}
