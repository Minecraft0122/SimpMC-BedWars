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

package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.player.*;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShowEntityEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ScoreboardListener implements Listener {

    private final Set<UUID> awaitingInitialClientLoad = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(@NotNull EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player) e.getEntity();
        final IArena arena = Arena.getArenaByPlayer(player);

        if (arena == null) {
            return;
        }

        int health = (int) Math.ceil((player.getHealth() - e.getFinalDamage()));
        SidebarService.getInstance().refreshHealth(arena, player, health);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(@NotNull EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player) e.getEntity();
        final IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) {
            return;
        }

        int health = (int) Math.ceil(player.getHealth() + e.getAmount());
        SidebarService.getInstance().refreshHealth(arena, player, health);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReSpawn(@NotNull PlayerReSpawnEvent e) {
        final IArena arena = e.getArena();

        SidebarService.getInstance().refreshHealth(arena, e.getPlayer(), (int) Math.ceil(e.getPlayer().getHealth()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void arenaJoin(@NotNull PlayerJoinArenaEvent e) {
        SidebarService service = SidebarService.getInstance();
        // Remove any lobby/previous-arena row before deploying the new context.
        service.removePlayerFromTabs(e.getPlayer());
        service.handleJoin(e.getArena(), e.getPlayer(), e.isSpectator());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void serverJoin(@NotNull PlayerJoinEvent e) {
        Player player = e.getPlayer();
        awaitingInitialClientLoad.add(player.getUniqueId());
        // Paper broadcasts the new player's ADD_PLAYER entry only after
        // PlayerJoinEvent returns. This must cover BUNGEE too: that mode joins
        // its arena synchronously inside PlayerJoinEvent.
        Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
            if (player.isOnline()) SidebarService.getInstance().synchronizeJoinedPlayer(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void clientLoadedWorld(@NotNull PlayerClientLoadedWorldEvent event) {
        Player player = event.getPlayer();
        boolean initialLoad = awaitingInitialClientLoad.remove(player.getUniqueId());
        SidebarService.getInstance().handleClientLoadedWorld(player, initialLoad);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void clientChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        SidebarService.getInstance().markClientWorldChange(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerShown(@NotNull PlayerShowEntityEvent event) {
        if (event.getEntity() instanceof Player target) {
            // Paper fires this event after ADD_PLAYER has rebuilt the target's
            // PlayerInfo entry, so the replacement display name is safe now.
            SidebarService.getInstance().handlePlayerShown(event.getPlayer(), target);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void serverQuit(@NotNull PlayerQuitEvent event) {
        awaitingInitialClientLoad.remove(event.getPlayer().getUniqueId());
        SidebarService.getInstance().removePlayerFromTabs(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void pluginDisable(@NotNull PluginDisableEvent event) {
        if (event.getPlugin() != BedWars.plugin) return;
        awaitingInitialClientLoad.clear();
        SidebarService service = SidebarService.getInstance();
        if (service != null) service.shutdown();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void arenaLeave(@NotNull PlayerLeaveArenaEvent e) {
        SidebarService.getInstance().removePlayerFromTabs(e.getPlayer());
        if (BedWars.getServerType() == ServerType.MULTIARENA || BedWars.getServerType() == ServerType.SHARED) {
            // add player to scoreboard tab list
            SidebarService.getInstance().applyLobbyTab(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedDestroy(@NotNull PlayerBedBreakEvent e) {
        // refresh placeholders in case placeholders refresh is disabled
        SidebarService.getInstance().refreshPlaceholders(e.getArena());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFinalKill(@NotNull PlayerKillEvent e) {
        if (!e.getCause().isFinalKill()) {
            return;
        }
        // refresh placeholders in case placeholders refresh is disabled
        SidebarService.getInstance().refreshPlaceholders(e.getArena());
    }
}
