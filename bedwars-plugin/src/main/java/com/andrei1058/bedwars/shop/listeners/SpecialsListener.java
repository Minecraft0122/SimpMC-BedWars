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

package com.andrei1058.bedwars.shop.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.player.PlayerBedBugSpawnEvent;
import com.andrei1058.bedwars.api.events.player.PlayerDreamDefenderSpawnEvent;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import static com.andrei1058.bedwars.BedWars.nms;

public class SpecialsListener implements Listener {

    private final Material silverfishMaterial;
    private final boolean silverfishPlaceable;
    private final double silverfishSpeed;
    private final double silverfishHealth;
    private final double silverfishDamage;
    private final int silverfishDespawn;
    private final Material ironGolemMaterial;
    private final boolean ironGolemPlaceable;
    private final double ironGolemSpeed;
    private final double ironGolemHealth;
    private final int ironGolemDespawn;

    public SpecialsListener(ShopManager shop) {
        var config = shop.getYml();
        silverfishMaterial = Material.matchMaterial(config.getString(ConfigPath.SHOP_SPECIAL_SILVERFISH_MATERIAL, ""));
        silverfishPlaceable = config.getBoolean(ConfigPath.SHOP_SPECIAL_SILVERFISH_ENABLE)
                && silverfishMaterial != null && !Misc.isProjectile(silverfishMaterial);
        silverfishSpeed = config.getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_SPEED);
        silverfishHealth = config.getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_HEALTH);
        silverfishDamage = config.getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_DAMAGE);
        silverfishDespawn = config.getInt(ConfigPath.SHOP_SPECIAL_SILVERFISH_DESPAWN);

        ironGolemMaterial = Material.matchMaterial(config.getString(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_MATERIAL, ""));
        ironGolemPlaceable = config.getBoolean(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_ENABLE)
                && ironGolemMaterial != null && !Misc.isProjectile(ironGolemMaterial);
        ironGolemSpeed = config.getDouble(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_SPEED);
        ironGolemHealth = config.getDouble(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_HEALTH);
        ironGolemDespawn = config.getInt(ConfigPath.SHOP_SPECIAL_IRON_GOLEM_DESPAWN);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpecialInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack i = e.getItem();
        if (i == null) return;
        if (i.getType() == Material.AIR) return;
        IArena a = Arena.getArenaByPlayer(p);
        if (a == null) return;
        if (a.getRespawnSessions().containsKey(e.getPlayer())) return;
        if (!a.isPlayer(p)) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        if (silverfishPlaceable && i.getType() == silverfishMaterial) {
            e.setCancelled(true);
            ITeam playerTeam = a.getTeam(p);
            PlayerBedBugSpawnEvent event = new PlayerBedBugSpawnEvent(p, playerTeam, a);
            nms.spawnSilverfish(b.getLocation().add(0, 1, 0), playerTeam, silverfishSpeed,
                    silverfishHealth, silverfishDespawn, silverfishDamage);
            Bukkit.getPluginManager().callEvent(event);
            nms.minusAmount(p, i, 1);
            p.updateInventory();
        }
        if (ironGolemPlaceable && i.getType() == ironGolemMaterial) {
            e.setCancelled(true);
            ITeam playerTeam = a.getTeam(p);
            PlayerDreamDefenderSpawnEvent event = new PlayerDreamDefenderSpawnEvent(p, playerTeam, a);
            nms.spawnIronGolem(b.getLocation().add(0, 1, 0), playerTeam, ironGolemSpeed,
                    ironGolemHealth, ironGolemDespawn);
            Bukkit.getPluginManager().callEvent(event);
            nms.minusAmount(p, i, 1);
            p.updateInventory();
        }
    }
}
