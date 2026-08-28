package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/** Establishes the player-state invariant before a restarting arena is rendered. */
public final class RestartingPlayerState {

    private RestartingPlayerState() {
    }

    public static void prepare(@NotNull IArena arena) {
        prepare(arena, RestartingPlayerState::clearRespawnSideEffects,
                InvisibilityManager::showRespawningPlayer);
    }

    static void prepare(@NotNull IArena arena,
                        @NotNull BiConsumer<IArena, Player> respawnSideEffectCleaner) {
        // Tests and integrations that provide their own cleanup callback can
        // manage visibility independently of the Bukkit/NMS runtime.
        prepare(arena, respawnSideEffectCleaner, (ignoredArena, ignoredPlayer) -> {
        });
    }

    private static void prepare(@NotNull IArena arena,
                                @NotNull BiConsumer<IArena, Player> respawnSideEffectCleaner,
                                @NotNull BiConsumer<IArena, Player> respawnVisibilityRestorer) {
        Set<Player> respawningPlayers = new LinkedHashSet<>(arena.getRespawnSessions().keySet());
        for (Player respawning : respawningPlayers) {
            respawnVisibilityRestorer.accept(arena, respawning);
        }
        arena.getRespawnSessions().clear();

        Set<Player> participants = new LinkedHashSet<>(arena.getPlayers());
        participants.addAll(arena.getSpectators());
        for (Player player : participants) {
            preparePlayer(arena, player, respawningPlayers.contains(player), respawnSideEffectCleaner);
        }
    }

    /**
     * Re-applies the restarting invariant after a late Bukkit respawn event.
     * The player may already have been removed from the arena respawn map by
     * the normal state transition, so this method is intentionally idempotent.
     */
    public static void preparePlayer(@NotNull IArena arena, @NotNull Player player) {
        preparePlayer(arena, player, !arena.isSpectator(player),
                RestartingPlayerState::clearRespawnSideEffects);
    }

    private static void preparePlayer(@NotNull IArena arena, @NotNull Player player,
                                      boolean restoreRespawnSideEffects,
                                      @NotNull BiConsumer<IArena, Player> respawnSideEffectCleaner) {
        if (!player.isOnline()) return;

        try {
            arena.getRespawnSessions().remove(player);
            restoreInteractiveMode(arena, player, arena.isSpectator(player), restoreRespawnSideEffects,
                    respawnSideEffectCleaner);
        } catch (RuntimeException exception) {
            logFailure("无法恢复玩家 " + player.getName() + " 的结算模式。", exception);
        }

        try {
            arena.sendSpectatorCommandItems(player);
        } catch (RuntimeException exception) {
            // A malformed per-arena item must not prevent the restarting task
            // from being created for every other participant.
            logFailure("无法为玩家 " + player.getName() + " 发放结算返回物品。", exception);
        }
    }

    private static void restoreInteractiveMode(IArena arena, Player player, boolean spectator,
                                               boolean restoreRespawnSideEffects,
                                               BiConsumer<IArena, Player> respawnSideEffectCleaner) {
        if (player.getGameMode() != GameMode.ADVENTURE) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        if (player.getGameMode() != GameMode.ADVENTURE) {
            logFailure("无法将 " + player.getName()
                    + " 恢复为结算交互模式；请检查是否有其他插件取消了游戏模式变更。", null);
            return;
        }

        player.setCanPickupItems(true);
        if (spectator) {
            PlayerMotion.enableFlight(player);
            player.setCollidable(false);
        } else {
            PlayerMotion.disableFlight(player);
            player.setCollidable(true);
            if (restoreRespawnSideEffects || arena.getShowTime().containsKey(player)) {
                respawnSideEffectCleaner.accept(arena, player);
            }
        }
    }

    private static void clearRespawnSideEffects(IArena arena, Player player) {
        InvisibilityManager.remove(arena, player);
    }

    private static void logFailure(String message, RuntimeException exception) {
        if (BedWars.plugin == null) return;
        if (exception == null) {
            BedWars.plugin.getLogger().warning(message);
        } else {
            BedWars.plugin.getLogger().log(Level.WARNING, message, exception);
        }
    }
}
