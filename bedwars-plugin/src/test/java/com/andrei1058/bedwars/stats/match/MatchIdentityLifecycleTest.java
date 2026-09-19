package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent;
import com.andrei1058.bedwars.api.events.server.ArenaRestartEvent;
import com.andrei1058.bedwars.api.stats.MatchInfo;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.LongConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchIdentityLifecycleTest {
    private MainConfig previousConfig;
    private MockedStatic<Bukkit> bukkit;
    private MockedConstruction<MatchStatsStore> stores;
    private MatchStatsRecorder recorder;
    private final List<Runnable> settlementTasks = new ArrayList<>();
    private final List<Runnable> nextTickTasks = new ArrayList<>();
    private final List<MatchRecordSnapshot> finishes = new ArrayList<>();
    private final List<LongConsumer> numberCallbacks = new ArrayList<>();
    private final List<Player> registeredPlayers = new ArrayList<>();
    private Player winner;
    private Player victim;
    private ITeam red;
    private ITeam blue;
    private IArena arena;

    @BeforeEach
    void setUp() {
        previousConfig = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID, "test-server");
        configuration.set(ConfigPath.MATCH_STATISTICS_VIOLATIONS_ENABLED, false);
        configuration.set(ConfigPath.MATCH_STATISTICS_FINISH_GRACE_TICKS, 0);
        when(BedWars.config.getYml()).thenReturn(configuration);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenAnswer(call -> {
            assertTrue((long) call.getArgument(2) >= 1, "settlement must wait for the synchronous final event");
            settlementTasks.add(call.getArgument(1));
            return mock(BukkitTask.class);
        });
        when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(call -> {
            nextTickTasks.add(call.getArgument(1));
            return mock(BukkitTask.class);
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        stores = mockConstruction(MatchStatsStore.class, (store, context) -> {
            when(store.enqueueStart(any(), any(LongConsumer.class))).thenAnswer(call -> {
                numberCallbacks.add(call.getArgument(1));
                return true;
            });
            when(store.enqueueFinish(any(), anySet())).thenAnswer(call -> {
                finishes.add(call.getArgument(0));
                return true;
            });
            when(store.enqueueFinishForShutdown(any(), anySet())).thenAnswer(call -> {
                finishes.add(call.getArgument(0));
                return true;
            });
        });
        recorder = new MatchStatsRecorder(mock(BedWars.class), mock(MatchStatsDatabase.class));
        winner = player("Winner");
        victim = player("Victim");
        red = team("Red");
        blue = team("Blue");
        arena = arena("map-1", List.of(winner, victim));
    }

    @AfterEach
    void tearDown() {
        registeredPlayers.forEach(player -> Arena.getArenaByPlayer().remove(player));
        stores.close();
        bukkit.close();
        BedWars.config = previousConfig;
    }

    @Test
    void preservesFinalKillAndRejectsGameplayAfterGameEnd() {
        start(arena);
        recorder.onKill(kill(PlayerKillEvent.PlayerKillCause.PVP_FINAL_KILL));
        endNormally();
        recorder.onKill(kill(PlayerKillEvent.PlayerKillCause.PVP_FINAL_KILL));
        PlayerBedBreakEvent bed = mock(PlayerBedBreakEvent.class);
        when(bed.getArena()).thenReturn(arena);
        when(bed.getPlayer()).thenReturn(winner);
        when(bed.getPlayerTeam()).thenReturn(red);
        when(bed.getVictimTeam()).thenReturn(blue);
        recorder.onBedBreak(bed);
        settle();

        MatchRecordSnapshot result = finishes.getFirst();
        assertEquals("FINISHED", result.status());
        assertEquals(1, result.playerStats().player(winner.getUniqueId()).finalKills());
        assertEquals(1, result.playerStats().player(victim.getUniqueId()).deaths());
        assertEquals(0, result.playerStats().player(winner.getUniqueId()).bedsDestroyed());
    }

    @Test
    void acceptsOnlyTheSynchronousCombatLogoutAfterWinnerWasAnnounced() {
        start(arena);
        PlayerLeaveArenaEvent leave = mock(PlayerLeaveArenaEvent.class);
        when(leave.getArena()).thenReturn(arena);
        when(leave.getPlayer()).thenReturn(victim);
        when(leave.getLastDamager()).thenReturn(winner);
        recorder.onArenaLeave(leave);
        endNormally();
        recorder.onKill(kill(PlayerKillEvent.PlayerKillCause.PLAYER_DISCONNECT_FINAL));
        recorder.onKill(kill(PlayerKillEvent.PlayerKillCause.PLAYER_DISCONNECT_FINAL));
        settle();

        assertEquals(1, finishes.getFirst().playerStats().player(winner.getUniqueId()).finalKills());
        assertEquals(1, finishes.getFirst().playerStats().player(victim.getUniqueId()).deaths());
    }

    @Test
    void expiredDepartureCannotAddADelayedKillDuringResultCountdown() {
        start(arena);
        PlayerLeaveArenaEvent leave = mock(PlayerLeaveArenaEvent.class);
        when(leave.getArena()).thenReturn(arena);
        when(leave.getPlayer()).thenReturn(victim);
        when(leave.getLastDamager()).thenReturn(winner);
        recorder.onArenaLeave(leave);
        endNormally();
        nextTickTasks.forEach(Runnable::run);
        recorder.onKill(kill(PlayerKillEvent.PlayerKillCause.PLAYER_DISCONNECT_FINAL));
        settle();
        assertEquals(0, finishes.getFirst().playerStats().player(winner.getUniqueId()).finalKills());
    }

    @Test
    void missingEndEventAbortsOnceAndCloseDoesNotRepeatTheSettlement() {
        start(arena);
        when(arena.getStatus()).thenReturn(GameState.restarting);
        GameStateChangeEvent exit = new GameStateChangeEvent(arena, GameState.playing, GameState.restarting);
        recorder.onPlayingStateExit(exit);
        recorder.onPlayingStateExit(exit);
        settle();
        recorder.close();

        assertEquals(1, finishes.size());
        assertEquals("ABORTED", finishes.getFirst().status());
        assertEquals("STATE_RESTARTING", finishes.getFirst().endReason());
    }

    @Test
    void restartOfOneCloneDoesNotAbortAnotherCloneOfTheSameMap() {
        IArena second = arena("map-2", List.of());
        start(arena);
        start(second);
        UUID otherUuid = recorder.getCurrentMatch(second).orElseThrow().matchUuid();
        recorder.onArenaRestart(new ArenaRestartEvent("map", "map-1"));
        settle();

        assertEquals(1, finishes.size());
        assertEquals("ABORTED", finishes.getFirst().status());
        assertEquals("ARENA_RESTART", finishes.getFirst().endReason());
        assertTrue(recorder.getCurrentMatch(arena).isEmpty());
        assertEquals(otherUuid, recorder.getCurrentMatch(second).orElseThrow().matchUuid());
        assertEquals("RUNNING", recorder.getCurrentMatch(second).orElseThrow().status());
    }

    @Test
    void numberCanArriveAfterSettlementAndIdentityRemainsUntilTheArenaIsRemoved() {
        start(arena);
        UUID identity = recorder.getCurrentMatch(arena).orElseThrow().matchUuid();
        assertEquals(0, recorder.getCurrentMatch(arena).orElseThrow().matchNumber());
        endNormally();
        settle();
        numberCallbacks.getFirst().accept(42L);

        MatchInfo settled = recorder.getCurrentMatch(arena).orElseThrow();
        assertEquals(identity, settled.matchUuid());
        assertEquals(42L, settled.matchNumber());
        assertEquals("FINISHED", settled.status());
        assertNotNull(settled.endedAt());
        recorder.onArenaDisable(new ArenaDisableEvent("map", "map-1"));
        assertTrue(recorder.getCurrentMatch(arena).isEmpty());
        assertEquals(1, finishes.size());
    }

    @Test
    void nextMatchReleasesThePreviousFinishedIdentity() {
        start(arena);
        UUID first = recorder.getCurrentMatch(arena).orElseThrow().matchUuid();
        endNormally();
        settle();
        assertEquals(first, recorder.getCurrentMatch(arena).orElseThrow().matchUuid());

        when(arena.getStatus()).thenReturn(GameState.playing);
        start(arena);
        MatchInfo second = recorder.getCurrentMatch(arena).orElseThrow();
        assertNotEquals(first, second.matchUuid());
        assertEquals("RUNNING", second.status());
    }

    @Test
    void retirementBeforeEvacuationPreventsAFalseFinishedMatch() {
        start(arena);
        recorder.onArenaDisable(new ArenaDisableEvent("map", "map-1"));
        GameEndEvent end = new GameEndEvent(arena, List.of(winner.getUniqueId()),
                List.of(victim.getUniqueId()), red, List.of(winner.getUniqueId()));
        recorder.onGameEnd(end);
        settle();

        assertEquals(1, finishes.size());
        assertEquals("ABORTED", finishes.getFirst().status());
    }

    @Test
    void pluginClosePreservesAPendingAbortWithoutWaitingForTheGraceTask() {
        start(arena);
        recorder.onPlayingStateExit(new GameStateChangeEvent(arena, GameState.playing, GameState.restarting));
        recorder.close();
        settle();

        assertEquals(1, finishes.size());
        assertEquals("ABORTED", finishes.getFirst().status());
        assertEquals("STATE_RESTARTING", finishes.getFirst().endReason());
    }

    @Test
    void pureSpectatorDisconnectDoesNotCreateAParticipantRow() {
        start(arena);
        Player spectator = player("Spectator");
        Arena.setArenaByPlayer(spectator, arena);
        registeredPlayers.add(spectator);
        when(arena.isSpectator(spectator)).thenReturn(true);
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(spectator);
        recorder.onQuit(quit);
        endNormally();
        settle();

        assertEquals(2, finishes.getFirst().playerStats().players().size());
        assertNull(finishes.getFirst().playerStats().player(spectator.getUniqueId()));
    }

    private void start(IArena target) {
        recorder.onGameStateChange(new GameStateChangeEvent(target, GameState.starting, GameState.playing));
    }

    private void endNormally() {
        when(arena.getStatus()).thenReturn(GameState.restarting);
        recorder.onPlayingStateExit(new GameStateChangeEvent(arena, GameState.playing, GameState.restarting));
        GameEndEvent end = mock(GameEndEvent.class);
        UUID winnerUuid = winner.getUniqueId();
        UUID victimUuid = victim.getUniqueId();
        when(end.getArena()).thenReturn(arena);
        when(end.getWinners()).thenReturn(List.of(winnerUuid));
        when(end.getLosers()).thenReturn(List.of(victimUuid));
        when(end.getTeamWinner()).thenReturn(red);
        recorder.onGameEnd(end);
    }

    private PlayerKillEvent kill(PlayerKillEvent.PlayerKillCause cause) {
        PlayerKillEvent event = mock(PlayerKillEvent.class);
        when(event.getArena()).thenReturn(arena);
        when(event.getKiller()).thenReturn(winner);
        when(event.getVictim()).thenReturn(victim);
        when(event.getKillerTeam()).thenReturn(red);
        when(event.getVictimTeam()).thenReturn(blue);
        when(event.getCause()).thenReturn(cause);
        return event;
    }

    private IArena arena(String world, List<Player> players) {
        IArena result = mock(IArena.class);
        when(result.getArenaName()).thenReturn("map");
        when(result.getWorldName()).thenReturn(world);
        when(result.getGroup()).thenReturn("Solo");
        when(result.getStartTime()).thenReturn(Instant.parse("2026-09-19T00:00:00Z"));
        when(result.getStatus()).thenReturn(GameState.playing);
        when(result.getPlayersSnapshot()).thenReturn(players);
        when(result.getExTeam(winner.getUniqueId())).thenReturn(red);
        when(result.getExTeam(victim.getUniqueId())).thenReturn(blue);
        return result;
    }

    private static Player player(String name) {
        Player result = mock(Player.class);
        when(result.getName()).thenReturn(name);
        when(result.getUniqueId()).thenReturn(UUID.randomUUID());
        return result;
    }

    private static ITeam team(String name) {
        ITeam result = mock(ITeam.class);
        when(result.getName()).thenReturn(name);
        return result;
    }

    private void settle() {
        List<Runnable> tasks = List.copyOf(settlementTasks);
        settlementTasks.clear();
        tasks.forEach(Runnable::run);
    }
}
