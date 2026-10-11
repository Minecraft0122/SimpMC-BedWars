package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LanguageScoreboardFallbackTest {
    @Test
    void readsTheResolvedGroupNameRatherThanTheMissingOriginalPath() {
        Player player = mock(Player.class);
        Language language = mock(Language.class);
        try (MockedStatic<Language> languages = mockStatic(Language.class, CALLS_REAL_METHODS)) {
            languages.when(() -> Language.getPlayerLanguage(player)).thenReturn(language);
            when(language.exists("sidebar.default.playing")).thenReturn(false);
            when(language.exists("sidebar.Default.playing")).thenReturn(true);
            when(language.l("sidebar.Default.playing")).thenReturn(List.of("resolved"));

            assertEquals(List.of("resolved"), Language.getScoreboard(player,
                    "sidebar.default.playing", "sidebar.fallback.playing"));
            verify(language).l("sidebar.Default.playing");
        }
    }
}
