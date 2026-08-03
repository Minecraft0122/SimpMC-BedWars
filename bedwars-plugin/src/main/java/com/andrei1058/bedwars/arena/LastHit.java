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

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LastHit {

    private final UUID victim;
    private volatile Entity damager;
    private volatile long time;
    private static final ConcurrentHashMap<UUID, LastHit> LAST_HITS = new ConcurrentHashMap<>();

    public LastHit(@NotNull Player victim, Entity damager, long time) {
        this(victim.getUniqueId(), damager, time);
        LAST_HITS.put(this.victim, this);
    }

    private LastHit(UUID victim, Entity damager, long time) {
        this.victim = victim;
        this.damager = damager;
        this.time = time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setDamager(Entity damager) {
        this.damager = damager;
    }

    public Entity getDamager() {
        return damager;
    }

    public UUID getVictim() {
        return victim;
    }

    public void remove() {
        LAST_HITS.remove(victim, this);
    }

    public long getTime() {
        return time;
    }

    public static LastHit record(@NotNull Player victim, Entity damager, long time) {
        return record(victim.getUniqueId(), damager, time);
    }

    static LastHit record(UUID victim, Entity damager, long time) {
        return LAST_HITS.compute(victim, (ignored, current) -> {
            if (current == null) return new LastHit(victim, damager, time);
            current.damager = damager;
            current.time = time;
            return current;
        });
    }

    public static LastHit getLastHit(@NotNull Player player) {
        return LAST_HITS.get(player.getUniqueId());
    }
}
