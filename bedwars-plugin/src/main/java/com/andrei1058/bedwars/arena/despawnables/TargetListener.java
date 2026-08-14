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

package com.andrei1058.bedwars.arena.despawnables;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.entity.Despawnable;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.support.version.common.DespawnableTargeting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class TargetListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        Despawnable despawnable = BedWars.nms.getDespawnablesList().get(e.getEntity().getUniqueId());
        if (despawnable != null) {
            ITeam ownerTeam = despawnable.getTeam();
            IArena arena = ownerTeam == null ? null : ownerTeam.getArena();
            Player target = arena == null ? null : DespawnableTargeting.resolveTarget(
                    arena, ownerTeam, e.getTarget(), e.getEntity().getLocation(), arena.getPlayers());
            if (e.getTarget() != target) {
                e.setTarget(target);
            }
            return;
        }

        if (!(e.getTarget() instanceof Player p)) return;
        IArena arena = Arena.getArenaByIdentifier(e.getEntity().getWorld().getName());
        if (arena == null) return;
        if (!arena.isPlayer(p)) {
            e.setCancelled(true);
            return;
        }
        if (arena.getStatus() != GameState.playing){
            e.setCancelled(true);
        }
    }
}
