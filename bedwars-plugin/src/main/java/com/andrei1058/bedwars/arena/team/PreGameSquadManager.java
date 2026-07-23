/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.PreGameSquad;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Arena-local squads used only for the next team assignment. */
public final class PreGameSquadManager implements Listener, PreGameSquad {

    private static final long INVITE_TTL_MILLIS = 30_000L;
    private static final PreGameSquadManager INSTANCE = new PreGameSquadManager();

    private final Map<UUID, Squad> squadsByMember = new HashMap<>();
    private final Map<InviteKey, Invite> invites = new HashMap<>();

    private PreGameSquadManager() {
    }

    public static PreGameSquadManager getInstance() {
        return INSTANCE;
    }

    @Override
    public Result invite(@NotNull Player inviter, @NotNull Player target) {
        purgeExpiredInvites();
        IArena arena = preGameArena(inviter);
        if (arena == null) return Result.NOT_IN_PRE_GAME;
        if (inviter.getUniqueId().equals(target.getUniqueId())) return Result.CANNOT_INVITE_SELF;
        if (preGameArena(target) != arena) return Result.DIFFERENT_ARENA;

        Squad squad = squadsByMember.get(inviter.getUniqueId());
        if (squad != null && !squad.leader.equals(inviter.getUniqueId())) return Result.NOT_LEADER;
        if (squadsByMember.containsKey(target.getUniqueId())) return Result.TARGET_ALREADY_GROUPED;
        if ((squad == null ? 1 : squad.members.size()) >= arena.getMaxInTeam()) return Result.SQUAD_FULL;

        InviteKey key = new InviteKey(inviter.getUniqueId(), target.getUniqueId());
        if (invites.containsKey(key)) return Result.ALREADY_INVITED;
        invites.put(key, new Invite(arena, System.currentTimeMillis() + INVITE_TTL_MILLIS));
        return Result.SUCCESS;
    }

    @Override
    public Result accept(@NotNull Player target, @NotNull Player inviter) {
        IArena arena = preGameArena(target);
        if (arena == null) return Result.NOT_IN_PRE_GAME;
        if (preGameArena(inviter) != arena) return Result.DIFFERENT_ARENA;

        InviteKey key = new InviteKey(inviter.getUniqueId(), target.getUniqueId());
        Invite invite = invites.remove(key);
        if (invite == null) return Result.NO_INVITE;
        if (invite.expiresAt < System.currentTimeMillis()) return Result.INVITE_EXPIRED;
        if (invite.arena != arena) return Result.DIFFERENT_ARENA;
        if (squadsByMember.containsKey(target.getUniqueId())) return Result.TARGET_ALREADY_GROUPED;

        Squad squad = squadsByMember.get(inviter.getUniqueId());
        if (squad != null && !squad.leader.equals(inviter.getUniqueId())) return Result.NOT_LEADER;
        if (squad != null && squad.members.size() >= arena.getMaxInTeam()) return Result.SQUAD_FULL;
        if (squad == null) {
            squad = new Squad(arena, inviter.getUniqueId());
            squadsByMember.put(inviter.getUniqueId(), squad);
        }
        squad.members.add(target.getUniqueId());
        squadsByMember.put(target.getUniqueId(), squad);
        removeInvitesFor(target.getUniqueId());
        reevaluateArena(arena);
        return Result.SUCCESS;
    }

    @Override
    public Result decline(@NotNull Player target, @NotNull Player inviter) {
        IArena arena = preGameArena(target);
        if (arena == null) return Result.NOT_IN_PRE_GAME;
        if (preGameArena(inviter) != arena) return Result.DIFFERENT_ARENA;
        Invite invite = invites.remove(new InviteKey(inviter.getUniqueId(), target.getUniqueId()));
        if (invite == null) return Result.NO_INVITE;
        if (invite.arena != arena) return Result.DIFFERENT_ARENA;
        return invite.expiresAt < System.currentTimeMillis() ? Result.INVITE_EXPIRED : Result.SUCCESS;
    }

    @Override
    public Result leave(@NotNull Player player) {
        if (!squadsByMember.containsKey(player.getUniqueId())) return Result.NOT_IN_SQUAD;
        IArena arena = preGameArena(player);
        removePlayer(player.getUniqueId());
        reevaluateArena(arena);
        return Result.SUCCESS;
    }

    @Override
    public boolean isLeader(@NotNull Player player) {
        Squad squad = squadsByMember.get(player.getUniqueId());
        return squad == null || squad.leader.equals(player.getUniqueId());
    }

    @Override
    public boolean isGrouped(@NotNull Player player) {
        return squadsByMember.containsKey(player.getUniqueId());
    }

    @Override
    public List<Player> getMembers(@NotNull Player player) {
        Squad squad = squadsByMember.get(player.getUniqueId());
        if (squad == null) return List.of(player);
        return List.copyOf(onlineArenaMembers(squad));
    }

    @Override
    public Player getLeader(@NotNull Player player) {
        Squad squad = squadsByMember.get(player.getUniqueId());
        return squad == null ? player : Bukkit.getPlayer(squad.leader);
    }

    @Override
    public List<Player> getAvailableTargets(@NotNull Player player) {
        IArena arena = preGameArena(player);
        if (arena == null || !isLeader(player)) return List.of();
        return arena.getPlayers().stream()
                .filter(candidate -> !candidate.equals(player))
                .filter(candidate -> !isGrouped(candidate))
                .toList();
    }

    /**
     * Build unique groups. Arena squads take priority, existing global Party
     * integrations are kept compatible, and everybody else becomes a solo group.
     */
    public List<List<Player>> getAssignmentGroups(@NotNull IArena arena) {
        LinkedHashMap<UUID, Player> remaining = new LinkedHashMap<>();
        arena.getPlayers().forEach(player -> remaining.put(player.getUniqueId(), player));
        List<List<Player>> groups = new ArrayList<>();

        Set<Squad> seenSquads = new HashSet<>();
        for (Player player : arena.getPlayers()) {
            Squad squad = squadsByMember.get(player.getUniqueId());
            if (squad == null || squad.arena != arena || !seenSquads.add(squad)) continue;
            List<Player> members = onlineArenaMembers(squad).stream()
                    .filter(member -> remaining.containsKey(member.getUniqueId()))
                    .toList();
            if (members.size() < 2) continue;
            groups.add(new ArrayList<>(members));
            members.forEach(member -> remaining.remove(member.getUniqueId()));
        }

        for (Player player : new ArrayList<>(remaining.values())) {
            if (!remaining.containsKey(player.getUniqueId())) continue;
            if (!BedWars.getParty().hasParty(player)) continue;
            List<Player> partyMembers = BedWars.getParty().getMembers(player);
            if (partyMembers == null) continue;
            LinkedHashSet<Player> members = new LinkedHashSet<>();
            for (Player member : partyMembers) {
                if (member != null && arena.isPlayer(member) && remaining.containsKey(member.getUniqueId())) {
                    members.add(member);
                }
            }
            if (members.size() < 2) continue;
            groups.add(new ArrayList<>(members));
            members.forEach(member -> remaining.remove(member.getUniqueId()));
        }

        remaining.values().forEach(player -> groups.add(new ArrayList<>(List.of(player))));
        return groups;
    }

    public void clearArena(@NotNull IArena arena) {
        Set<Squad> squads = new HashSet<>(squadsByMember.values());
        squads.stream().filter(squad -> squad.arena == arena).forEach(this::removeSquad);
        invites.entrySet().removeIf(entry -> entry.getValue().arena == arena);
    }

    @EventHandler
    public void onPlayerLeave(PlayerLeaveArenaEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onArenaDisable(ArenaDisableEvent event) {
        Set<Squad> squads = new HashSet<>(squadsByMember.values());
        squads.stream()
                .filter(squad -> squad.arena.getWorldName().equals(event.getWorldName()))
                .forEach(this::removeSquad);
        invites.entrySet().removeIf(entry -> entry.getValue().arena.getWorldName().equals(event.getWorldName()));
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getNewState() == GameState.playing || event.getNewState() == GameState.restarting) {
            clearArena(event.getArena());
        }
    }

    private IArena preGameArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.isPlayer(player)) return null;
        return arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting ? arena : null;
    }

    private List<Player> onlineArenaMembers(Squad squad) {
        List<Player> members = new ArrayList<>();
        for (UUID uuid : squad.members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && squad.arena.isPlayer(player)) members.add(player);
        }
        return members;
    }

    private void removePlayer(UUID player) {
        removeInvitesFor(player);
        Squad squad = squadsByMember.remove(player);
        if (squad == null) return;
        squad.members.remove(player);
        if (squad.members.size() < 2) {
            removeSquad(squad);
            return;
        }
        if (squad.leader.equals(player)) squad.leader = squad.members.iterator().next();
    }

    private void removeSquad(Squad squad) {
        squad.members.forEach(squadsByMember::remove);
        squad.members.clear();
    }

    private void removeInvitesFor(UUID player) {
        invites.keySet().removeIf(key -> key.inviter.equals(player) || key.target.equals(player));
    }

    private void purgeExpiredInvites() {
        long now = System.currentTimeMillis();
        invites.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
    }

    private static void reevaluateArena(IArena arena) {
        if (arena instanceof Arena concreteArena) concreteArena.reevaluateStartEligibility();
    }

    private record InviteKey(UUID inviter, UUID target) {
    }

    private record Invite(IArena arena, long expiresAt) {
    }

    private static final class Squad {
        private final IArena arena;
        private UUID leader;
        private final LinkedHashSet<UUID> members = new LinkedHashSet<>();

        private Squad(IArena arena, UUID leader) {
            this.arena = arena;
            this.leader = leader;
            members.add(leader);
        }
    }
}
