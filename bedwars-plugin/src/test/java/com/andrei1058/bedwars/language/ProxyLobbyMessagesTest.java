package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.util.AdventureText;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 代理大厅提示的冷却与后备文本行为。 */
class ProxyLobbyMessagesTest {

    @Test
    void sendsLanguageFallbackAndDoesNotResendInsideCooldownWindow() {
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        try (var text = mockStatic(AdventureText.class)) {
            ProxyLobbyMessages.sendUnavailable(player);
            ProxyLobbyMessages.sendUnavailable(player);
            text.verify(() -> AdventureText.send(player, ProxyLobbyMessages.UNAVAILABLE_FALLBACK));
            assertTrue(ProxyLobbyMessages.UNAVAILABLE_FALLBACK.contains("代理大厅"));
        }
    }

    @Test
    void differentPlayersArePromptedIndependently() {
        Player first = mock(Player.class);
        Player second = mock(Player.class);
        when(first.getUniqueId()).thenReturn(UUID.randomUUID());
        when(second.getUniqueId()).thenReturn(UUID.randomUUID());
        try (var text = mockStatic(AdventureText.class)) {
            ProxyLobbyMessages.sendUnavailable(first);
            ProxyLobbyMessages.sendUnavailable(second);
            text.verify(() -> AdventureText.send(first, ProxyLobbyMessages.UNAVAILABLE_FALLBACK));
            text.verify(() -> AdventureText.send(second, ProxyLobbyMessages.UNAVAILABLE_FALLBACK));
        }
    }
}
