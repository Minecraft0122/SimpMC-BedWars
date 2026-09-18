package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ArenaPlayerListNames {

    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private ArenaPlayerListNames() {
    }

    static void synchronize(@NotNull Player player, @Nullable IArena arena) {
        if (arena == null || !player.isOnline()) {
            release(player);
            return;
        }
        boolean spectator = arena.isSpectator(player);
        ITeam team = spectator ? null : BwTabList.resolvePlayerListTeam(arena, player);
        if (!spectator && team == null) {
            release(player);
            return;
        }
        Component desired = Component.text(player.getName(),
                        spectator ? NamedTextColor.GRAY : team.getColor().textColor())
                .decoration(TextDecoration.ITALIC, spectator || arena.isReSpawning(player));
        Entry entry = ACTIVE.get(player.getUniqueId());
        if (entry == null || entry.player != player) {
            if (entry != null) entry.restore();
            entry = new Entry(player, player.playerListName());
            ACTIVE.put(player.getUniqueId(), entry);
        }
        if (!desired.equals(player.playerListName())) player.playerListName(desired);
        entry.applied = desired;
    }

    static void synchronize(@NotNull IArena arena) {
        arena.getPlayers().forEach(player -> synchronize(player, arena));
        arena.getSpectators().forEach(player -> synchronize(player, arena));
    }

    static void refresh() {
        for (Entry entry : List.copyOf(ACTIVE.values())) {
            synchronize(entry.player, Arena.getArenaByPlayer(entry.player));
        }
    }

    static void release(@NotNull Player player) {
        Entry entry = ACTIVE.get(player.getUniqueId());
        if (entry == null || entry.player != player) return;
        ACTIVE.remove(player.getUniqueId());
        entry.restore();
    }

    static void release(@NotNull IArena arena) {
        arena.getPlayers().forEach(ArenaPlayerListNames::release);
        arena.getSpectators().forEach(ArenaPlayerListNames::release);
    }

    static void clear() {
        ACTIVE.values().forEach(Entry::restore);
        ACTIVE.clear();
    }

    private static final class Entry {
        private final Player player;
        private final Component original;
        private Component applied;

        private Entry(@NotNull Player player, @Nullable Component original) {
            this.player = player;
            this.original = original;
        }

        private void restore() {
            if (applied != null && applied.equals(player.playerListName())) {
                player.playerListName(original);
            }
        }
    }
}
