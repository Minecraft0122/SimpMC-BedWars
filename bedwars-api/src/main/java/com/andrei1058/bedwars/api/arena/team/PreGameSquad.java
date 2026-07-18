package com.andrei1058.bedwars.api.arena.team;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Arena-local invitation groups used by the next automatic team assignment.
 * A group exists only while its arena is waiting or starting and is cleared
 * when the game starts, restarts, or the arena is disabled.
 */
public interface PreGameSquad {

    enum Result {
        SUCCESS,
        NOT_IN_PRE_GAME,
        DIFFERENT_ARENA,
        CANNOT_INVITE_SELF,
        NOT_LEADER,
        TARGET_ALREADY_GROUPED,
        SQUAD_FULL,
        ALREADY_INVITED,
        NO_INVITE,
        INVITE_EXPIRED,
        NOT_IN_SQUAD
    }

    Result invite(@NotNull Player inviter, @NotNull Player target);

    Result accept(@NotNull Player target, @NotNull Player inviter);

    Result decline(@NotNull Player target, @NotNull Player inviter);

    Result leave(@NotNull Player player);

    boolean isLeader(@NotNull Player player);

    boolean isGrouped(@NotNull Player player);

    /**
     * Get an immutable snapshot. Ungrouped players are returned as a solo list.
     */
    @NotNull List<Player> getMembers(@NotNull Player player);

    /**
     * Get the online leader, or the player itself when ungrouped.
     */
    @Nullable Player getLeader(@NotNull Player player);

    /**
     * Get an immutable snapshot of players this leader can currently invite.
     */
    @NotNull List<Player> getAvailableTargets(@NotNull Player player);
}
