package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.events.player.PlayerLangChangeEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LangListenerTest {
    @Test
    void persistsSelectedLanguageEvenWhenPlayerIsOutsideTheConfiguredLobby() {
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(false);
        Language english = mock(Language.class);
        when(english.getIso()).thenReturn("en");
        Database database = mock(Database.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        AtomicReference<Runnable> delayed = new AtomicReference<>();
        when(scheduler.runTaskLater(isNull(), any(Runnable.class), eq(1L))).thenAnswer(invocation -> {
            delayed.set(invocation.getArgument(1));
            return task;
        });
        when(scheduler.runTaskAsynchronously(isNull(), any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return task;
        });

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Language> language = mockStatic(Language.class);
             MockedStatic<BedWars> bedWars = mockStatic(BedWars.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            language.when(() -> Language.getPlayerLanguage(playerId)).thenReturn(english);
            bedWars.when(BedWars::getRemoteDatabase).thenReturn(database);

            new LangListener().onLanguageChangeEvent(new PlayerLangChangeEvent(player, "zh_cn", "en"));
            assertNotNull(delayed.get());
            delayed.get().run();

            verify(database).setLanguage(playerId, "en");
        }
    }
}
