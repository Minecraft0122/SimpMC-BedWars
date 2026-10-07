package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

/** Owns the entity collision flag for BedWars player lifecycle states. */
public final class PlayerCollisionState {

    // Paper 1.21.11 将此值映射为 pushOwnTeam；实际判定是排除同队碰撞。
    // FOR_OTHER_TEAMS 反而排除敌队碰撞，不能按枚举名称直译为允许互推的对象。
    public static final Team.OptionStatus ACTIVE_TEAM_RULE = Team.OptionStatus.FOR_OWN_TEAM;

    private PlayerCollisionState() {
    }

    public static boolean shouldCollide(@NotNull GameState state, boolean spectator, boolean respawning) {
        return state == GameState.playing && !spectator && !respawning;
    }

    public static void apply(@NotNull Player player, @NotNull GameState state,
                      boolean spectator, boolean respawning) {
        // 这里只维护服务器实体生命周期；客户端玩家碰撞仍由 Sidebar 的
        // scoreboard Team.COLLISION_RULE 决定，不能由这个标志单独替代。
        player.setCollidable(shouldCollide(state, spectator, respawning));
    }

    /** The shared client/server team used while a player is not active. */
    @NotNull
    public static String inactiveCollisionGroupName() {
        return "bw_waiting";
    }

    /**
     * Derive the same bounded team name for the private TAB scoreboard and the
     * server main scoreboard. A real game team must have one shared identity
     * for every viewer; using a player UUID here would make teammates collide
     * on the client while the server treats them as one team.
     */
    @NotNull
    public static String collisionGroupName(@NotNull IArena arena, @NotNull ITeam team) {
        int arenaHash = Objects.hash(arena.getArenaName(), arena.getWorldName());
        int suffixHash = team.getName().toLowerCase(Locale.ROOT).hashCode();
        String name = "bwt" + Integer.toUnsignedString(arenaHash, 36)
                + Integer.toUnsignedString(suffixHash, 36);
        return name.substring(0, Math.min(16, name.length()));
    }

    /** Apply the managed-lobby side of the lifecycle. */
    public static void applyManagedLobby(@NotNull Player player) {
        player.setCollidable(false);
    }
}
