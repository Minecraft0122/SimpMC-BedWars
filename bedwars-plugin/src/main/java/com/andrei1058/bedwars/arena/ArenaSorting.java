package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 竞技场选择和随机加入共用的稳定排序。 */
final class ArenaSorting {

    private static final Comparator<IArena> COMPARATOR = Comparator
            .comparingInt((IArena arena) -> statusPriority(arena.getStatus()))
            .thenComparing(Comparator.comparingInt((IArena arena) -> arena.getPlayers().size()).reversed())
            .thenComparing(IArena::getArenaName, String.CASE_INSENSITIVE_ORDER);

    private ArenaSorting() {
    }

    static List<IArena> sorted(List<IArena> arenas) {
        List<IArena> result = new ArrayList<>(arenas);
        result.sort(COMPARATOR);
        return result;
    }

    private static int statusPriority(GameState status) {
        return switch (status) {
            case starting -> 0;
            case waiting -> 1;
            case playing -> 2;
            case restarting -> 3;
        };
    }
}
