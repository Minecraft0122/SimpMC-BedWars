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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.tasks.ReJoinTask;
import com.andrei1058.bedwars.configuration.Sounds;
import com.andrei1058.bedwars.lobbysocket.ArenaSocket;
import com.andrei1058.bedwars.shop.ShopCache;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class ReJoin {

    private UUID player;
    private IArena arena;
    private ITeam bwt;
    private ReJoinTask task = null;
    /** Stable token used by the lobby to distinguish successive leases. */
    private final String reservationId = UUID.randomUUID().toString();
    private long expiresAtMillis;
    private final ArrayList<ShopCache.CachedItem> permanentsAndNonDowngradables = new ArrayList<>();

    private static final List<ReJoin> reJoinList = new ArrayList<>();

    /**
     * Make rejoin possible for a player
     */
    public ReJoin(Player player, IArena arena, ITeam bwt, List<ShopCache.CachedItem> cachedArmor) {
        ReJoin rj = getPlayer(player);
        if (rj != null) {
            rj.destroy(false);
        }
        if (bwt == null) return;
        if (bwt.isBedDestroyed()) return;
        this.bwt = bwt;
        this.player = player.getUniqueId();
        this.arena = arena;
        long rejoinSeconds = Math.min(86_400L, Math.max(1L, BedWars.config.getYml().getLong(
                ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, 30L)));
        this.expiresAtMillis = System.currentTimeMillis() + rejoinSeconds * 1_000L;
        reJoinList.add(this);
        BedWars.debug("Created ReJoin for " + player.getName() + " " + player.getUniqueId() + " at " + arena.getArenaName());
        int timeoutSeconds = Math.max(1, BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME));
        List<Player> recipients = new ArrayList<>(arena.getPlayers());
        recipients.addAll(arena.getSpectators());
        Set<UUID> notified = new HashSet<>();
        for (Player recipient : recipients) {
            if (recipient.getUniqueId().equals(player.getUniqueId()) || !notified.add(recipient.getUniqueId())) continue;
            recipient.sendMessage(rejoinNotice(getMsg(recipient, Messages.REJOIN_WAITING), player.getName(), timeoutSeconds));
        }
        if (bwt.getMembers().isEmpty()) task = new ReJoinTask(this, arena, bwt);
        this.permanentsAndNonDowngradables.addAll(cachedArmor);

        if (BedWars.autoscale) {
            ArenaSocket.sendMessage(ArenaSocket.formatRejoinCreateMessage(this));
        }
    }

    /**
     * Check if a player has stored data
     */
    public static boolean exists(@NotNull Player pl) {
        BedWars.debug("ReJoin exists check " + pl.getUniqueId());
        for (ReJoin rj : getReJoinList()) {
            BedWars.debug("ReJoin exists check list scroll: " + rj.getPl().toString());
            if (rj.getPl().equals(pl.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a player ReJoin
     */
    @Nullable
    public static ReJoin getPlayer(@NotNull Player player) {
        return getPlayer(player.getUniqueId());
    }

    /** Look up a reconnect lease without requiring the player to be online. */
    @Nullable
    public static ReJoin getPlayer(@NotNull UUID playerUuid) {
        BedWars.debug("ReJoin getPlayer " + playerUuid);
        for (ReJoin rj : getReJoinList()) {
            if (rj.getPl().equals(playerUuid)) {
                return rj;
            }
        }
        return null;
    }

    /**
     * Check if can reJoin
     */
    public boolean canReJoin() {
        BedWars.debug("ReJoin canReJoin  check.");
        if (!isActiveReservation(reJoinList, this)) {
            BedWars.debug("ReJoin canReJoin reservation is no longer active");
            return false;
        }
        if (expiresAtMillis <= System.currentTimeMillis()) {
            BedWars.debug("ReJoin canReJoin reservation has expired");
            destroy(true);
            return false;
        }
        if (arena == null) {
            BedWars.debug("ReJoin canReJoin arena is null " + player.toString());
            destroy(true);
            return false;
        }
        if (arena.getStatus() != GameState.playing) {
            BedWars.debug("ReJoin canReJoin status is restarting " + player.toString());
            destroy(true);
            return false;
        }
        if (bwt == null) {
            BedWars.debug("ReJoin canReJoin bwt is null " + player.toString());
            destroy(true);
            return false;
        }
        if (bwt.isBedDestroyed()) {
            BedWars.debug("ReJoin canReJoin bed is destroyed " + player.toString());
            destroy(false);
            return false;
        }
        return true;
    }

    /**
     * Make a player re-join the arena
     */
    public boolean reJoin(Player player) {
        if (player == null || this.player == null || !this.player.equals(player.getUniqueId()) || !canReJoin()) {
            return false;
        }

        if (!arena.reJoin(player)) {
            return false;
        }

        Sounds.playSound("rejoin-allowed", player);
        player.sendMessage(Language.getMsg(player, Messages.REJOIN_ALLOWED).replace("{arena}", getArena().getDisplayName()));

        return true;
    }

    static boolean isActiveReservation(List<ReJoin> reservations, ReJoin candidate) {
        if (candidate == null) return false;
        for (ReJoin active : reservations) {
            if (active == candidate) return true;
        }
        return false;
    }

    static String rejoinNotice(String template, String playerName, int timeoutSeconds) {
        return template.replace("{player}", playerName)
                .replace("{time}", String.valueOf(Math.max(1, timeoutSeconds)));
    }

    private boolean removeActiveReservation() {
        Iterator<ReJoin> iterator = reJoinList.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == this) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Destroy data and rejoin possibility
     */
    public void destroy(boolean destroyTeam) {
        if (!removeActiveReservation()) {
            if (task != null) {
                task.destroy();
                task = null;
            }
            return;
        }
        BedWars.debug("ReJoin destroy for " + player.toString());
        if (task != null) {
            task.destroy();
            task = null;
        }
        if (BedWars.autoscale) {
            ArenaSocket.sendMessage(ArenaSocket.formatRejoinRemoveMessage(this));
        }
        if (arena != null && bwt != null && destroyTeam
                && bwt.getMembers().isEmpty() && !bwt.isBedDestroyed()) {
            bwt.setBedDestroyed(true);
            for (Player p2 : arena.getPlayers()) {
                p2.sendMessage(getMsg(p2, Messages.TEAM_ELIMINATED_CHAT).replace("{TeamColor}", bwt.getColor().chat().toString())
                        .replace("{TeamName}", bwt.getDisplayName(Language.getPlayerLanguage(p2))));
            }
            for (Player p2 : arena.getSpectators()) {
                p2.sendMessage(getMsg(p2, Messages.TEAM_ELIMINATED_CHAT).replace("{TeamColor}", bwt.getColor().chat().toString())
                        .replace("{TeamName}", bwt.getDisplayName(Language.getPlayerLanguage(p2))));
            }
            arena.checkWinner();
        }
    }

    /**
     * Get Player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * Get player team
     */
    public ITeam getBwt() {
        return bwt;
    }

    /**
     * Get arena
     */
    public IArena getArena() {
        return arena;
    }

    public ReJoinTask getTask() {
        return task;
    }

    public UUID getPl() {
        return player;
    }

    /** Stable identifier for this reconnect lease. */
    public String getReservationId() {
        return reservationId;
    }

    /** Absolute expiration timestamp sent to the lobby. */
    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    /** Whether this lease is still present and inside its reconnect window. */
    public boolean isLeaseActive() {
        return isActiveReservation(reJoinList, this)
                && expiresAtMillis > System.currentTimeMillis();
    }

    /** Expire the local lease and finalize an abandoned empty team. */
    public void expire() {
        if (!isActiveReservation(reJoinList, this)) return;
        destroy(true);
    }

    @SuppressWarnings("WeakerAccess")
    public List<ShopCache.CachedItem> getPermanentsAndNonDowngradables() {
        return permanentsAndNonDowngradables;
    }

    public static List<ReJoin> getReJoinList() {
        return Collections.unmodifiableList(reJoinList);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof ReJoin)) return false;
        ReJoin reJoin = (ReJoin) o;
        return reJoin.getPl().equals(getPl());
    }
}
