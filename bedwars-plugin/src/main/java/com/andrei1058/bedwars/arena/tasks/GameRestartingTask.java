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

package com.andrei1058.bedwars.arena.tasks;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.generator.IGenerator;
import com.andrei1058.bedwars.api.arena.shop.ShopHolo;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.api.tasks.RestartingTask;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.configuration.Sounds;
import com.andrei1058.bedwars.support.paper.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class GameRestartingTask implements Runnable, RestartingTask {

    private static final Set<Integer> CHAT_COUNTDOWN_SECONDS = Set.of(60, 30, 15, 10, 5, 4, 3, 2, 1, 0);

    private Arena arena;
    private int restarting = Math.max(0, BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_RESTART));
    private final BukkitTask task;

    public GameRestartingTask(@NotNull Arena arena) {
        this.arena = arena;
        task = Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this, 0, 20L);
        Sounds.playSound("game-end", arena.getPlayers());
        Sounds.playSound("game-end", arena.getSpectators());

        // teleport to alive players
        if (arena.getConfig().getGameOverridableBoolean(ConfigPath.GENERAL_GAME_END_TELEPORT_ELIMINATED)) {
            if (!arena.getPlayers().isEmpty()) {
                Random r = new Random();
                for (Player spectator : arena.getSpectators()) {
                    Player target = arena.getPlayers().get(r.nextInt(arena.getPlayers().size()));
                    Location loc = target.getLocation().clone();
                    loc.setDirection(target.getLocation().getDirection().multiply(-1));
                    loc.add(0,2,0);

                    TeleportManager.teleportC(spectator, loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            }
        }

        // show eliminated players
        if (arena.getConfig().getGameOverridableBoolean(ConfigPath.GENERAL_GAME_END_SHOW_ELIMINATED)) {
            for (Player spectator : arena.getSpectators()) {
                ITeam exTeam = arena.getExTeam(spectator.getUniqueId());
                if (null == exTeam) {
                    continue;
                }
                spectator.removePotionEffect(PotionEffectType.INVISIBILITY);
                for (Player player : arena.getPlayers()) {
                    BedWars.nms.spigotShowPlayer(player, spectator);
                    BedWars.nms.spigotShowPlayer(spectator, player);
                }
            }
        }
    }

    /**
     * Get task ID
     */
    public int getTask() {
        return task.getTaskId();
    }

    @Override
    public int getRestarting() {
        return restarting;
    }

    public Arena getArena() {
        return arena;
    }

    @Override
    public BukkitTask getBukkitTask() {
        return task;
    }

    @Override
    public void run() {
        if (getArena().getPlayers().isEmpty() && getArena().getSpectators().isEmpty() && restarting > 9) {
            restarting = 9;
        }
        announceRestartCountdown();

        if (restarting == 4) {
            prepareArenaReset();
        } else if (restarting == 0) {
            finishArenaReset();
            task.cancel();
            arena = null;
            return;
        }

        restarting--;
    }

    public void cancel() {
        task.cancel();
    }

    private void announceRestartCountdown() {
        if (!shouldAnnounceRestartCountdown(restarting)) return;
        LinkedHashSet<Player> audience = new LinkedHashSet<>(getArena().getPlayers());
        audience.addAll(getArena().getSpectators());
        for (Player player : audience) {
            Language language = Language.getPlayerLanguage(player);
            String template = language.exists(Messages.ARENA_RESTART_COUNTDOWN)
                    ? getMsg(player, Messages.ARENA_RESTART_COUNTDOWN)
                    : "§e竞技场将在 §c{time} §e秒后重置";
            player.sendMessage(template.replace("{time}", String.valueOf(restarting)));
        }
    }

    static boolean shouldAnnounceRestartCountdown(int seconds) {
        return CHAT_COUNTDOWN_SECONDS.contains(seconds);
    }

    private void prepareArenaReset() {
        ShopHolo.clearForArena(getArena());
        for (IGenerator generator : getArena().getOreGenerators()) {
            generator.disable();
        }
        for (ITeam team : getArena().getTeams()) {
            for (IGenerator generator : team.getGenerators()) {
                generator.disable();
            }
        }
    }

    private void finishArenaReset() {
        Arena currentArena = getArena();
        for (Player player : new ArrayList<>(currentArena.getPlayers())) {
            currentArena.removePlayer(player, BedWars.getServerType() == ServerType.BUNGEE);
        }
        for (Player spectator : new ArrayList<>(currentArena.getSpectators())) {
            currentArena.removeSpectator(spectator, BedWars.getServerType() == ServerType.BUNGEE);
        }
        for (Entity entity : currentArena.getWorld().getEntities()) {
            if (entity.getType() != EntityType.PLAYER) continue;
            Player player = (Player) entity;
            Misc.moveToLobbyOrKick(player, currentArena, true);
            if (currentArena.isSpectator(player)) currentArena.removeSpectator(player, false);
            if (currentArena.isPlayer(player)) currentArena.removePlayer(player, false);
        }
        currentArena.restart();
    }

}
