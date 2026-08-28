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

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.Sounds;
import com.andrei1058.bedwars.shop.ShopCache;
import com.andrei1058.bedwars.shop.listeners.InventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class Interact implements Listener {

    private final double fireballSpeedMultiplier;
    private final double fireballSneakSpeedMultiplier;
    private final double fireballSneakAccelerationMultiplier;
    private final double fireballSneakRecoil;
    private final double fireballMinimumFlightDistance;
    private final double fireballMaximumFlightDistance;
    private final double fireballCooldown;
    private final float fireballExplosionSize;
    private final boolean craftingDisabled;
    private final boolean enchantingDisabled;
    private final boolean furnaceDisabled;
    private final boolean brewingStandDisabled;
    private final boolean anvilDisabled;

    public Interact() {
        this.fireballSpeedMultiplier = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER);
        this.fireballSneakSpeedMultiplier = config.getYml().getDouble(
                ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER);
        this.fireballSneakAccelerationMultiplier = config.getYml().getDouble(
                ConfigPath.GENERAL_FIREBALL_SNEAK_ACCELERATION_MULTIPLIER);
        this.fireballSneakRecoil = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_SNEAK_RECOIL);
        FireballLaunchPhysics.FlightRange flightRange = FireballLaunchPhysics.normalizeFlightRange(
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_FLIGHT_RANGE_MIN),
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_FLIGHT_RANGE_MAX));
        this.fireballMinimumFlightDistance = flightRange.min();
        this.fireballMaximumFlightDistance = flightRange.max();
        this.fireballCooldown = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_COOLDOWN);
        this.fireballExplosionSize = (float) FireballListener.normalizeExplosionSize(
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE));
        this.craftingDisabled = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_DISABLE_CRAFTING);
        this.enchantingDisabled = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_DISABLE_ENCHANTING);
        this.furnaceDisabled = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_DISABLE_FURNACE);
        this.brewingStandDisabled = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_DISABLE_BREWING_STAND);
        this.anvilDisabled = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_DISABLE_ANVIL);
    }

    @EventHandler
    /* Handle custom items with commands on them */
    public void onItemCommand(PlayerInteractEvent e) {
        if (e == null) return;
        // Command items are issued in the main hand. Ignoring the off-hand
        // mirror event prevents a single right-click from scheduling the
        // return action twice and keeps the client inventory stable when the
        // cancelled interaction is replayed by Paper.
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack i = BedWars.nms.getItemInHand(p);
            if (!nms.isCustomBedWarsItem(i)) return;
            final String[] customData = nms.getCustomData(i).split("_", 2);
            if (customData.length >= 2) {
                if (customData[0].equals("RUNCOMMAND")) {
                    e.setCancelled(true);
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(p, customData[1].trim()));
                }
            }
        }
    }

    @EventHandler
    //Check if player is opening an inventory
    public void onInventoryInteract(PlayerInteractEvent e) {
        if (e == null) return;
        if (e.isCancelled()) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        if ((BedWars.getServerType() == ServerType.MULTIARENA && b.getWorld().getName().equals(BedWars.getLobbyWorld()) && !BreakPlace.isBuildSession(e.getPlayer())) || Arena.getArenaByPlayer(e.getPlayer()) != null) {
            if (b.getType() == Material.CRAFTING_TABLE && craftingDisabled) {
                e.setCancelled(true);
            } else if (b.getType() == Material.ENCHANTING_TABLE && enchantingDisabled) {
                e.setCancelled(true);
            } else if (b.getType() == Material.FURNACE && furnaceDisabled) {
                e.setCancelled(true);
            } else if (b.getType() == Material.BREWING_STAND && brewingStandDisabled) {
                e.setCancelled(true);
            } else if (b.getType() == Material.ANVIL && anvilDisabled) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeamChestQuickDeposit(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || !event.getPlayer().isSneaking()) return;
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || !isChest(block.getType()) || !(block.getState() instanceof Chest chest)) return;

        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing || arena.isSpectator(player)
                || arena.getRespawnSessions().containsKey(player)) return;

        ITeam team = arena.getTeam(player);
        if (team == null || findChestOwner(arena, block) != team) return;

        // A left click would otherwise start breaking the team's map chest.
        event.setCancelled(true);

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR || held.getAmount() <= 0) return;

        ShopCache shopCache = ShopCache.getShopCache(player.getUniqueId());
        if (InventoryListener.shouldCancelMovement(held, shopCache)) return;

        int offeredAmount = held.getAmount();
        Map<Integer, ItemStack> leftovers = chest.getInventory().addItem(held.clone());
        int transferred = TeamChestQuickDeposit.transferredAmount(offeredAmount, leftovers);
        if (transferred <= 0) return;

        if (transferred == offeredAmount) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            held.setAmount(offeredAmount - transferred);
            player.getInventory().setItemInMainHand(held);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e == null) return;
        Player p = e.getPlayer();
        Arena.afkCheck.remove(p.getUniqueId());
        if (BedWars.getAPI().getAFKUtil().isPlayerAFK(e.getPlayer())) {
            BedWars.getAPI().getAFKUtil().setPlayerAFK(e.getPlayer(), false);
        }
        IArena playerArena = Arena.getArenaByPlayer(p);
        if (playerArena != null && playerArena.isReSpawning(p)) {
            e.setCancelled(true);
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b == null) return;
            if (b.getType() == Material.AIR) return;
            IArena a = playerArena;
            if (a != null) {
                if (a.getRespawnSessions().containsKey(e.getPlayer())) {
                    e.setCancelled(true);
                    return;
                }
                if (nms.isBed(b.getType())) {
                    if (p.isSneaking()) {
                        ItemStack i = nms.getItemInHand(p);
                        if (i == null) {
                            e.setCancelled(true);
                        } else if (i.getType() == Material.AIR) {
                            e.setCancelled(true);
                        }
                    } else {
                        e.setCancelled(true);
                    }
                    return;
                }
                if (isChest(b.getType())) {
                    if (a.isSpectator(p) || a.getRespawnSessions().containsKey(p)) {
                        e.setCancelled(true);
                        return;
                    }
                    //make it so only team members can open chests while team is alive, and all when is eliminated
                    ITeam owner = findChestOwner(a, b);
                    if (owner != null) {
                        if (!owner.isMember(p)) {
                            if (!(owner.getMembers().isEmpty() && owner.isBedDestroyed())) {
                                e.setCancelled(true);
                                AdventureText.send(p, getMsg(p, Messages.INTERACT_CHEST_CANT_OPEN_TEAM_ELIMINATED));
                            }
                        }
                    }
                }
                if (a.isSpectator(p) || a.getRespawnSessions().containsKey(p)) {
                    switch (b.getType().toString()) {
                        case "CHEST":
                        case "ENDER_CHEST":
                        case "ANVIL":
                        case "WORKBENCH":
                        case "HOPPER":
                        case "TRAPPED_CHEST":
                        case "CRAFTING_TABLE":
                            e.setCancelled(true);
                            break;
                    }
                    if (b.getBlockData() instanceof Openable) {
                        e.setCancelled(true);
                    }
                }
            }
            if (b.getState() instanceof Sign) {
                for (IArena a1 : Arena.getArenas()) {
                    if (a1.getSigns().contains(b)) {
                        if (a1.addPlayer(p, false)) {
                            Sounds.playSound("join-allowed", p);
                        } else {
                            Sounds.playSound("join-denied", p);
                        }
                        return;
                    }
                }
            }
        }
        //check hand
        ItemStack inHand = e.getItem();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (inHand == null) return;
            IArena a = Arena.getArenaByPlayer(p);
            if (a != null) {
                if (a.isPlayer(p)) {
                    if (inHand.getType() == Material.FIRE_CHARGE) {

                        e.setCancelled(true);

                        long launchTime = System.currentTimeMillis();
                        if (FireballLaunchPhysics.cooldownElapsed(launchTime,
                                a.getFireballCooldowns().getOrDefault(p.getUniqueId(), 0L), fireballCooldown)) {
                            a.getFireballCooldowns().put(p.getUniqueId(), launchTime);
                            Vector direction = p.getEyeLocation().getDirection();
                            boolean sneaking = p.isSneaking() || p.getCurrentInput().isSneak();
                            Vector launchVelocity = FireballLaunchPhysics.launchVelocity(
                                    direction, fireballSpeedMultiplier, sneaking, fireballSneakSpeedMultiplier);
                            Vector launchAcceleration = FireballLaunchPhysics.launchAcceleration(
                                    direction, sneaking, fireballSneakAccelerationMultiplier);
                            double flightDistance = FireballLaunchPhysics.randomFlightDistance(
                                    fireballMinimumFlightDistance, fireballMaximumFlightDistance,
                                    ThreadLocalRandom.current());
                            Fireball fb = p.launchProjectile(Fireball.class, launchVelocity);
                            nms.setFireballAcceleration(fb, launchAcceleration);
                            // Paper's setter also updates the current velocity;
                            // restore the configured launch speed after applying
                            // the persistent acceleration vector.
                            fb.setVelocity(launchVelocity);
                            if (sneaking) {
                                p.setVelocity(p.getVelocity().add(
                                        FireballLaunchPhysics.sneakRecoil(launchVelocity, fireballSneakRecoil)));
                            }
                            //fb.setIsIncendiary(false); // apparently this on <12 makes the fireball not explode on hit. wtf bukkit?
                            fb.setYield(fireballExplosionSize);
                            fb.setMetadata("bw1058", new FixedMetadataValue(plugin, "ceva"));
                            FireballFlightTracker.start(plugin, fb, flightDistance);
                            nms.minusAmount(p, inHand, 1);
                        }

                    }
                }
            }
        }
    }



    @EventHandler
    public void disableItemFrameRotation(PlayerInteractEntityEvent e) {
        if (e == null) return;
        if (e.getRightClicked().getType() == EntityType.ITEM_FRAME) {
            if (((ItemFrame) e.getRightClicked()).getItem().getType().equals(Material.AIR)) {
                //prevent from putting upgradable items in it
                ItemStack i = nms.getItemInHand(e.getPlayer());
                if (i != null) {
                    if (i.getType() != Material.AIR) {
                        ShopCache sc = ShopCache.getShopCache(e.getPlayer().getUniqueId());
                        if (sc != null) {
                            if (InventoryListener.shouldCancelMovement(i, sc)) {
                                e.setCancelled(true);
                            }
                        }
                    }
                }
                return;
            }
            IArena a = Arena.getArenaByIdentifier(e.getPlayer().getWorld().getName());
            if (a != null) {
                e.setCancelled(true);
            }
            if (BedWars.getServerType() == ServerType.MULTIARENA) {
                if (BedWars.getLobbyWorld().equals(e.getPlayer().getWorld().getName()) && !BreakPlace.isBuildSession(e.getPlayer())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (e == null) return;
        IArena a = Arena.getArenaByPlayer(e.getPlayer());
        if (a == null) return;
        if (a.isReSpawning(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }
        Location l = e.getRightClicked().getLocation();
        for (ITeam t : a.getTeams()) {
            Location l2 = t.getShop(), l3 = t.getTeamUpgrades();
            if (l.getBlockX() == l2.getBlockX() && l.getBlockY() == l2.getBlockY() && l.getBlockZ() == l2.getBlockZ()) {
                e.setCancelled(true);
            } else if (l.getBlockX() == l3.getBlockX() && l.getBlockY() == l3.getBlockY() && l.getBlockZ() == l3.getBlockZ()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent e) {
        if (e == null) return;
        if (Arena.getArenaByPlayer(e.getPlayer()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorManipulate(PlayerArmorStandManipulateEvent e) {
        if (e == null) return;
        if (e.isCancelled()) return;
        //prevent from breaking generators
        if (Arena.getArenaByPlayer(e.getPlayer()) != null) {
            e.setCancelled(true);
        }

        //prevent from stealing from armor stands in lobby
        if (BedWars.getServerType() == ServerType.MULTIARENA && e.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld()) && !BreakPlace.isBuildSession(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCrafting(PrepareItemCraftEvent e) {
        if (e == null) return;
        if (Arena.getArenaByPlayer((Player) e.getView().getPlayer()) != null) {
            if (craftingDisabled) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    private static boolean isChest(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }

    private static ITeam findChestOwner(IArena arena, Block block) {
        double radius = Math.max(0, arena.getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS));
        double radiusSquared = radius * radius;
        Location chestLocation = block.getLocation();
        ITeam nearestTeam = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ITeam team : arena.getTeams()) {
            Location spawn = team.getSpawn();
            if (spawn == null || spawn.getWorld() == null || !spawn.getWorld().equals(chestLocation.getWorld())) continue;

            double distance = spawn.distanceSquared(chestLocation);
            if (distance <= radiusSquared && distance < nearestDistance) {
                nearestDistance = distance;
                nearestTeam = team;
            }
        }
        return nearestTeam;
    }
}
