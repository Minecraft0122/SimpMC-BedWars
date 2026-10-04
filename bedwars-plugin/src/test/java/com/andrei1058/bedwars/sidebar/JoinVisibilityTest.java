package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.MainConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class JoinVisibilityTest {
    @Test
    void lobbyJoinIsHiddenBeforeAnyScheduledTaskRuns() {
        checkJoin(false, false);
    }

    @Test
    void lateSpectatorIsHiddenBeforeAnyScheduledTaskRuns() {
        checkJoin(true, true);
    }

    @Test
    void rejoiningParticipantKeepsSameArenaPlayersVisibleAndHidesSpectators() {
        checkJoin(true, false);
    }

    private void checkJoin(boolean joinsArena, boolean spectating) {
        Player joined = mock(Player.class);
        Player active = mock(Player.class);
        Player spectator = mock(Player.class);
        Player lobby = mock(Player.class);
        Player otherArenaPlayer = mock(Player.class);
        IArena arena = mock(IArena.class);
        IArena otherArena = mock(IArena.class);
        when(joined.getUniqueId()).thenReturn(UUID.randomUUID());
        when(arena.isPlayer(active)).thenReturn(true);
        when(arena.isPlayer(joined)).thenReturn(joinsArena && !spectating);
        when(arena.isSpectator(spectator)).thenReturn(true);
        when(arena.isSpectator(joined)).thenReturn(spectating);
        MainConfig originalConfig = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        try (MockedStatic<Arena> arenas = mockStatic(Arena.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            arenas.when(() -> Arena.getArenaByPlayer(active)).thenReturn(arena);
            arenas.when(() -> Arena.getArenaByPlayer(spectator)).thenReturn(arena);
            arenas.when(() -> Arena.getArenaByPlayer(joined)).thenReturn(joinsArena ? arena : null);
            arenas.when(() -> Arena.getArenaByPlayer(otherArenaPlayer)).thenReturn(otherArena);
            bukkit.when(Bukkit::getOnlinePlayers)
                    .thenReturn(List.of(joined, active, spectator, lobby, otherArenaPlayer));
            // Deliberately never execute tasks: hiding must finish in PlayerJoinEvent.
            bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));
            new ScoreboardListener().serverJoin(new PlayerJoinEvent(joined, (Component) null));

            verify(active, times(!joinsArena || spectating ? 1 : 0)).hidePlayer(BedWars.plugin, joined);
            verify(spectator, times(joinsArena ? 0 : 1)).hidePlayer(BedWars.plugin, joined);
            verify(joined, times(!joinsArena || !spectating ? 1 : 0)).hidePlayer(BedWars.plugin, spectator);
            verify(lobby, times(joinsArena ? 1 : 0)).hidePlayer(BedWars.plugin, joined);
            verify(joined, times(joinsArena ? 1 : 0)).hidePlayer(BedWars.plugin, lobby);
            verify(otherArenaPlayer).hidePlayer(BedWars.plugin, joined);
            verify(joined).hidePlayer(BedWars.plugin, otherArenaPlayer);
            verify(joined, never()).hidePlayer(BedWars.plugin, joined);
        } finally {
            BedWars.config = originalConfig;
        }
    }
}
