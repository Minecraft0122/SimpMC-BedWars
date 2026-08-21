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

package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.arena.team.BedWarsTeam;
import com.andrei1058.bedwars.arena.matchmaking.ArenaInviteManager;
import com.andrei1058.bedwars.commands.bedwars.subcmds.regular.CmdStats;
import com.andrei1058.bedwars.commands.party.PartyCommand;
import com.andrei1058.bedwars.sidebar.SidebarService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.andrei1058.bedwars.BedWars.*;

public class QuitAndTeleportListener implements Listener {

    @EventHandler
    public void onLeave(@NotNull PlayerQuitEvent e) {
        Player p = e.getPlayer();
        boolean wasInLobby = LobbyAnnouncements.isLobbyPlayer(p);
        e.setQuitMessage(null);
        // Announce before arena/database cleanup so an unrelated listener or
        // adapter failure cannot swallow a genuine lobby disconnect message.
        LobbyAnnouncements.playerQuit(p, wasInLobby);
        // Remove from arena
        IArena a = Arena.getArenaByPlayer(p);
        if (a != null) {
            if (a.isPlayer(p)) {
                removeArenaPlayerOnQuit(a, p, e.getReason());
            } else if (a.isSpectator(p)) {
                a.removeSpectator(p, true);
            }
        }

        // Persist the single supported player language. A stale value from an
        // older proxy/database is migrated to Simplified Chinese on quit.
        if (Language.getLangByPlayer().containsKey(p.getUniqueId())) {
            final UUID u = p.getUniqueId();
            Language.getLangByPlayer().remove(u);
            String languageToSave = Language.SIMPLIFIED_CHINESE_ISO;
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> BedWars.getRemoteDatabase().setLanguage(u, languageToSave));
        }
        // Manage internal parties
        if (getParty().isInternal()) {
            if (getParty().hasParty(p)) {
                getParty().removeFromParty(p);
            }
        }
        PartyCommand.clearPlayer(p.getUniqueId());
        // Check if was doing a setup and remove the session
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss != null) {
            ss.cancel();
        }

        SidebarService.getInstance().remove(e.getPlayer());

        BedWarsTeam.reSpawnInvulnerability.remove(e.getPlayer().getUniqueId());

        LastHit lh = LastHit.getLastHit(p);
        if (lh != null) {
            lh.remove();
        }

        CmdStats.getStatsCoolDown().remove(e.getPlayer().getUniqueId());
        ArenaInviteManager.getInstance().clearPlayer(e.getPlayer().getUniqueId());
    }

    static void removeArenaPlayerOnQuit(IArena arena, Player player, PlayerQuitEvent.QuitReason reason) {
        arena.removePlayer(player, true);
        if (PlayerQuitPolicy.abandonsGame(reason)) arena.abandonGame(player);
    }

    /**
     * Handle players teleported outside.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(@NotNull PlayerChangedWorldEvent e) {

        // Re-evaluate on the next tick: arena/setup mappings may be changed by
        // other handlers during this world-change event. This also avoids
        // announcing a player who is being moved while disconnecting.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!e.getPlayer().isOnline()) {
                LobbyAnnouncements.playerLeftLobby(e.getPlayer());
            } else if (LobbyAnnouncements.isLobbyPlayer(e.getPlayer())) {
                // Portals and add-ons can move a player into the lobby without
                // going through the arena leave callback. Apply the same state
                // and proxy-return item as a normal BedWars transition.
                Arena.enterLobby(e.getPlayer());
            } else {
                LobbyAnnouncements.playerLeftLobby(e.getPlayer());
            }
        });

        // if player was teleported outside arena
        IArena arena = Arena.getArenaByPlayer(e.getPlayer());

        if (null == arena) {
            return;
        }

        if (e.getPlayer().getWorld().getName().equals(arena.getWorldName())) {
            return;
        }

        if (arena.isPlayer(e.getPlayer())) {
            // it will teleport you to the lobby world or cached location
            arena.removePlayer(e.getPlayer(), false);
        }

        if (arena.isSpectator(e.getPlayer())) {
            // it will teleport you to the lobby world or cached location
            arena.removeSpectator(e.getPlayer(), false);
        }
    }
}
