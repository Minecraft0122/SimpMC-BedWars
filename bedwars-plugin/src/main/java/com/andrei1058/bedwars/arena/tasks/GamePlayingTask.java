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
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.generator.IGenerator;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.InvisibilityManager;
import com.andrei1058.bedwars.arena.AfkPolicy;
import com.andrei1058.bedwars.discipline.DisciplineService;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.api.tasks.PlayingTask;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class GamePlayingTask implements Runnable, PlayingTask {

    private Arena arena;
    private BukkitTask task;
    private int beds_destroy_countdown, dragon_spawn_countdown, game_end_countdown;
    private final Map<UUID, AfkPolicy.State> afkStates = new HashMap<>();
    private final AfkPolicy afkPolicy;

    public GamePlayingTask(Arena arena) {
        this.arena = arena;
        this.beds_destroy_countdown = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_BEDS_DESTROY_COUNTDOWN);
        this.dragon_spawn_countdown = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_DRAGON_SPAWN_COUNTDOWN);
        this.game_end_countdown = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_GAME_END_COUNTDOWN);
        this.afkPolicy = createAfkPolicy();
        this.task = Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this, 0, 20L);
    }

    public Arena getArena() {
        return arena;
    }

    @Override
    public BukkitTask getBukkitTask() {
        return task;
    }

    /**
     * Get task ID
     */
    public int getTask() {
        return task.getTaskId();
    }

    public int getBedsDestroyCountdown() {
        return beds_destroy_countdown;
    }

    public int getDragonSpawnCountdown() {
        return dragon_spawn_countdown;
    }

    public int getGameEndCountdown() {
        return game_end_countdown;
    }

    @Override
    public void run() {
        switch (getArena().getNextEvent()) {
            case EMERALD_GENERATOR_TIER_II:
            case EMERALD_GENERATOR_TIER_III:
            case DIAMOND_GENERATOR_TIER_II:
            case DIAMOND_GENERATOR_TIER_III:
                if (getArena().upgradeDiamondsCount > 0) {
                    getArena().upgradeDiamondsCount--;
                    if (getArena().upgradeDiamondsCount == 0) {
                        getArena().updateNextEvent();
                    }
                }
                if (getArena().upgradeEmeraldsCount > 0) {
                    getArena().upgradeEmeraldsCount--;
                    if (getArena().upgradeEmeraldsCount == 0) {
                        getArena().updateNextEvent();
                    }
                }
                break;
            case BEDS_DESTROY:
                beds_destroy_countdown--;
                if (getBedsDestroyCountdown() == 0) {
                    for (Player p : getArena().getPlayers()) {
                        nms.sendTitle(p, AdventureText.section(getMsg(p, Messages.NEXT_EVENT_TITLE_ANNOUNCE_BEDS_DESTROYED)), AdventureText.section(getMsg(p, Messages.NEXT_EVENT_SUBTITLE_ANNOUNCE_BEDS_DESTROYED)), 0, 40, 10);
                        AdventureText.send(p, getMsg(p, Messages.NEXT_EVENT_CHAT_ANNOUNCE_BEDS_DESTROYED));
                    }
                    for (Player p : getArena().getSpectators()) {
                        nms.sendTitle(p, AdventureText.section(getMsg(p, Messages.NEXT_EVENT_TITLE_ANNOUNCE_BEDS_DESTROYED)), AdventureText.section(getMsg(p, Messages.NEXT_EVENT_SUBTITLE_ANNOUNCE_BEDS_DESTROYED)), 0, 40, 10);
                        AdventureText.send(p, getMsg(p, Messages.NEXT_EVENT_CHAT_ANNOUNCE_BEDS_DESTROYED));
                    }
                    for (ITeam t : getArena().getTeams()) {
                        t.setBedDestroyed(true);
                    }
                    getArena().updateNextEvent();
                }
                break;
            case ENDER_DRAGON:
                dragon_spawn_countdown--;
                if (getDragonSpawnCountdown() == 0) {
                    for (Player p : getArena().getPlayers()) {
                        nms.sendTitle(p, AdventureText.section(getMsg(p, Messages.NEXT_EVENT_TITLE_ANNOUNCE_SUDDEN_DEATH)), AdventureText.section(getMsg(p, Messages.NEXT_EVENT_SUBTITLE_ANNOUNCE_SUDDEN_DEATH)), 0, 40, 10);
                        for (ITeam t : getArena().getTeams()) {
                            if (t.getMembers().isEmpty()) continue;
                            AdventureText.send(p, getMsg(p, Messages.NEXT_EVENT_CHAT_ANNOUNCE_SUDDEN_DEATH).replace("{TeamDragons}", String.valueOf(t.getDragons()))
                                    .replace("{TeamColor}", t.getColor().chat().toString()).replace("{TeamName}", t.getDisplayName(Language.getPlayerLanguage(p))));
                        }
                    }
                    for (Player p : getArena().getSpectators()) {
                        nms.sendTitle(p, AdventureText.section(getMsg(p, Messages.NEXT_EVENT_TITLE_ANNOUNCE_SUDDEN_DEATH)), AdventureText.section(getMsg(p, Messages.NEXT_EVENT_SUBTITLE_ANNOUNCE_SUDDEN_DEATH)), 0, 40, 10);
                        for (ITeam t : getArena().getTeams()) {
                            if (t.getMembers().isEmpty()) continue;
                            AdventureText.send(p, getMsg(p, Messages.NEXT_EVENT_CHAT_ANNOUNCE_SUDDEN_DEATH).replace("{TeamDragons}", String.valueOf(t.getDragons()))
                                    .replace("{TeamColor}", t.getColor().chat().toString()).replace("{TeamName}", t.getDisplayName(Language.getPlayerLanguage(p))));
                        }
                    }
                    getArena().updateNextEvent();
                    for (ITeam team : arena.getTeams()){
                        for (IGenerator o : team.getGenerators()) {
                            Location l = o.getLocation();
                            for (int y = 0; y < 20; y++) {
                                l.clone().subtract(0, y, 0).getBlock().setType(Material.AIR);
                            }
                        }
                    }
                    for (ITeam t : getArena().getTeams()) {
                        if (t.getMembers().isEmpty()) continue;
                        for (int x = 0; x < t.getDragons(); x++) {
                            nms.spawnDragon(getArena().getConfig().getArenaLoc("waiting.Loc").add(0, 10, 0), t);
                        }
                    }
                }
                break;
            case GAME_END:
                game_end_countdown--;
                if (getGameEndCountdown() == 0) {
                    getArena().checkWinner();
                    getArena().changeStatus(GameState.restarting);
                }
                break;
        }
        int distance = 0;
        for (ITeam t : getArena().getTeams()) {
            if (t.getSize() > 1) {
                for (Player p : t.getMembers()) {
                    for (Player p2 : t.getMembers()) {
                        if (p2 == p) continue;
                        if (distance == 0) {
                            distance = (int) p.getLocation().distance(p2.getLocation());
                        } else if ((int) p.getLocation().distance(p2.getLocation()) < distance) {
                            distance = (int) p.getLocation().distance(p2.getLocation());
                        }
                    }
                    nms.playAction(p, AdventureText.ampersand(getMsg(p, Messages.FORMATTING_ACTION_BAR_TRACKING).replace("{team}", t.getColor().chat() + t.getDisplayName(Language.getPlayerLanguage(p)))
                            .replace("{distance}", t.getColor().chat().toString() + distance)));
                }
            }

            // spawn items
            for (IGenerator o : t.getGenerators()) {
                o.spawn();
            }
        }

        runAfkDiscipline();

        /* RESPAWN SESSION */
        if (!getArena().getRespawnSessions().isEmpty()) {
            for (Map.Entry<Player, Integer> e : getArena().getRespawnSessions().entrySet()) {
                if (e.getValue() <= 0) {
                    IArena a = Arena.getArenaByPlayer(e.getKey());
                    if (a == null) {
                        getArena().getRespawnSessions().remove(e.getKey());
                        continue;
                    }
                    ITeam t = a.getTeam(e.getKey());
                    if (t == null){
                        a.getRespawnSessions().remove(e.getKey());
                        a.addSpectator(e.getKey(), true, null);
                    } else {
                        t.respawnMember(e.getKey());
                    }
                } else {
                    nms.sendTitle(e.getKey(), AdventureText.section(getMsg(e.getKey(), Messages.PLAYER_DIE_RESPAWN_TITLE).replace("{time}",
                            String.valueOf(e.getValue()))), AdventureText.section(getMsg(e.getKey(), Messages.PLAYER_DIE_RESPAWN_SUBTITLE).replace("{time}",
                            String.valueOf(e.getValue()))), 0, 30, 10);
                    AdventureText.send(e.getKey(), getMsg(e.getKey(), Messages.PLAYER_DIE_RESPAWN_CHAT).replace("{time}", String.valueOf(e.getValue())));
                    getArena().getRespawnSessions().replace(e.getKey(), e.getValue() - 1);
                }
            }
        }

        /* INVISIBILITY FOR ARMOR */
        if (!getArena().getShowTime().isEmpty()) {
            for (Map.Entry<Player, Integer> e : getArena().getShowTime().entrySet()) {
                if (e.getValue() <= 0) {
                    InvisibilityManager.remove(getArena(), e.getKey());
                } else {
                    getArena().getShowTime().replace(e.getKey(), e.getValue() - 1);
                }
            }
        }

        /* SPAWN ITEMS */
        for (IGenerator o : getArena().getOreGenerators()) {
            o.spawn();
        }
    }

    public void cancel() {
        task.cancel();
        afkStates.clear();
    }

    /**
     * Evaluate the immutable AFK policy once per second. Existing activity
     * listeners mark a separate concurrent activity signal; the legacy
     * Arena.afkCheck map remains available to API integrations while the
     * actual timer and warning stages live in AfkPolicy.State.
     */
    private void runAfkDiscipline() {
        if (BedWars.plugin == null) return;
        DisciplineService discipline = BedWars.plugin.getDisciplineService();
        if (discipline != null) {
            if (!discipline.afkEnabled()) return;
        } else if (!BedWars.config.getYml().getBoolean(ConfigPath.DISCIPLINE_AFK_ENABLED, true)) {
            return;
        }
        Instant now = Instant.now();
        Set<UUID> present = new HashSet<>();
        List<Player> players = List.copyOf(getArena().getPlayers());
        for (Player player : players) {
            UUID uuid = player.getUniqueId();
            present.add(uuid);
            // Keep the public legacy map populated with elapsed seconds for
            // integrations which still read Arena.afkCheck. Activity itself
            // is consumed from the separate dirty set below.
            int legacyIdleSeconds = Arena.afkCheck.getOrDefault(uuid, 0);
            boolean paused = !player.isOnline() || player.isDead()
                    || getArena().getRespawnSessions().containsKey(player);
            AfkPolicy.State state = afkStates.get(uuid);
            boolean activity = Arena.consumeAfkActivity(uuid);
            if (state == null) {
                state = afkPolicy.initialState(now);
                legacyIdleSeconds = 0;
            } else if (activity) {
                state = afkPolicy.recordActivity(state, now);
                legacyIdleSeconds = 0;
                if (BedWars.getAPI().getAFKUtil().isPlayerAFK(player)) {
                    BedWars.getAPI().getAFKUtil().setPlayerAFK(player, false);
                }
            } else if (!paused && legacyIdleSeconds < Integer.MAX_VALUE) {
                legacyIdleSeconds++;
            }
            Arena.afkCheck.put(uuid, legacyIdleSeconds);

            AfkPolicy.Evaluation evaluation = afkPolicy.evaluate(state, now, paused);
            afkStates.put(uuid, evaluation.state());
            switch (evaluation.decision()) {
                case WARNING -> AdventureText.send(player, getMsg(player, Messages.DISCIPLINE_AFK_WARNING)
                        .replace("{seconds}", String.valueOf(Math.max(1L,
                                afkPolicy.removalSeconds() - evaluation.idleSeconds()))));
                case FINAL_WARNING -> AdventureText.send(player, getMsg(player, Messages.DISCIPLINE_AFK_FINAL_WARNING));
                case REMOVE -> {
                    AdventureText.send(player, getMsg(player, Messages.DISCIPLINE_AFK_REMOVED));
                    if (discipline != null) discipline.markAfk(player, getArena());
                    afkStates.remove(uuid);
                    Arena.afkCheck.remove(uuid);
                    Arena.clearAfkActivity(uuid);
                    getArena().removePlayer(player, false);
                }
                case NONE -> { }
            }
        }
        afkStates.keySet().removeIf(uuid -> !present.contains(uuid));
    }

    private AfkPolicy createAfkPolicy() {
        if (BedWars.plugin == null || BedWars.plugin.getDisciplineService() == null) {
            return AfkPolicy.defaults();
        }
        DisciplineService discipline = BedWars.plugin.getDisciplineService();
        try {
            return new AfkPolicy(true, discipline.afkWarningSeconds(),
                    discipline.afkFinalWarningSeconds(), discipline.afkRemovalSeconds());
        } catch (IllegalArgumentException exception) {
            BedWars.plugin.getLogger().warning("挂机时间配置无效，已回退为 60/120/180 秒。");
            return AfkPolicy.defaults();
        }
    }
}


