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

package com.andrei1058.bedwars.listeners.dropshandler;

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class PlayerDrops {

    private PlayerDrops() {
    }

    /**
     * if bedWars should handle drops behavior.
     *
     * @return true if event drops must be cleared.
     */
    public static boolean handlePlayerDrops(IArena arena, Player victim, Player killer, ITeam victimsTeam, ITeam killersTeam, PlayerKillEvent.PlayerKillCause cause, List<ItemStack> inventory) {
        return handlePlayerDrops(arena, victim, killer, victimsTeam, killersTeam, cause, inventory,
                arena.getConfig().getBoolean(ConfigPath.ARENA_NORMAL_DEATH_DROPS));
    }

    static boolean handlePlayerDrops(IArena arena, Player victim, Player killer, ITeam victimsTeam,
                                     ITeam killersTeam, PlayerKillEvent.PlayerKillCause cause,
                                     List<ItemStack> inventory, boolean vanillaDeathDrops) {
        if (vanillaDeathDrops) {
            return false;
        }
        if (cause.isFinalKill()) {
            // Final-kill inventory must not become a ground drop. Clear the
            // ender chest as well so its contents are not retained after the
            // player has been eliminated.
            if (victimsTeam != null) {
                victim.getEnderChest().clear();
            }
        }

        // victim's inventory

        if (victimsTeam != null && !victimsTeam.isBedDestroyed()
                && !(victimsTeam.equals(killersTeam) && victim.equals(killer))) {
            // Only the explicit resource reward below is transferred to a
            // surviving killer. All other inventory entries are discarded.
            if (!arena.isPlayer(killer)) return true;
            if (arena.isReSpawning(killer)) return true;
            Map<Material, Integer> materialDrops = new HashMap<>();
            for (ItemStack i : inventory) {
                if (i == null) continue;
                if (i.getType() == Material.AIR) continue;
                if (i.getType() == Material.DIAMOND || i.getType() == Material.EMERALD || i.getType() == Material.IRON_INGOT || i.getType() == Material.GOLD_INGOT) {

                    // add to killer inventory
                    killer.getInventory().addItem(i);

                    // count items
                    if (materialDrops.containsKey(i.getType())) {
                        materialDrops.replace(i.getType(), materialDrops.get(i.getType()) + i.getAmount());
                    } else {
                        materialDrops.put(i.getType(), i.getAmount());
                    }
                }
            }

            for (Map.Entry<Material, Integer> entry : materialDrops.entrySet()) {
                String msg = "";
                int amount = entry.getValue();
                switch (entry.getKey()) {
                    case DIAMOND:
                        msg = getMsg(killer, Messages.PLAYER_DIE_REWARD_DIAMOND).replace("{meaning}", amount == 1 ?
                                getMsg(killer, Messages.MEANING_DIAMOND_SINGULAR) : getMsg(killer, Messages.MEANING_DIAMOND_PLURAL));
                        break;
                    case EMERALD:
                        msg = getMsg(killer, Messages.PLAYER_DIE_REWARD_EMERALD).replace("{meaning}", amount == 1 ?
                                getMsg(killer, Messages.MEANING_EMERALD_SINGULAR) : getMsg(killer, Messages.MEANING_EMERALD_PLURAL));
                        break;
                    case IRON_INGOT:
                        msg = getMsg(killer, Messages.PLAYER_DIE_REWARD_IRON).replace("{meaning}", amount == 1 ?
                                getMsg(killer, Messages.MEANING_IRON_SINGULAR) : getMsg(killer, Messages.MEANING_IRON_PLURAL));
                        break;
                    case GOLD_INGOT:
                        msg = getMsg(killer, Messages.PLAYER_DIE_REWARD_GOLD).replace("{meaning}", amount == 1 ?
                                getMsg(killer, Messages.MEANING_GOLD_SINGULAR) : getMsg(killer, Messages.MEANING_GOLD_PLURAL));
                        break;
                }
                AdventureText.send(killer, msg.replace("{amount}", String.valueOf(amount)));
            }
            materialDrops.clear();
        }
        return true;
    }
}
