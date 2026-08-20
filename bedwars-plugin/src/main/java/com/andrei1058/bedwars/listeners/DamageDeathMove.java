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
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.generator.IGenerator;
import com.andrei1058.bedwars.api.arena.shop.ShopHolo;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.entity.Despawnable;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.events.team.TeamEliminatedEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.RestartingPlayerState;
import com.andrei1058.bedwars.arena.LastHit;
import com.andrei1058.bedwars.arena.InvisibilityManager;
import com.andrei1058.bedwars.arena.PlayerMotion;
import com.andrei1058.bedwars.arena.SafeSpawnResolver;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.arena.team.BedWarsTeam;
import com.andrei1058.bedwars.configuration.Sounds;
import com.andrei1058.bedwars.listeners.dropshandler.PlayerDrops;
import com.andrei1058.bedwars.support.paper.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.tag.DamageTypeTags;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.getMsg;
import static com.andrei1058.bedwars.arena.LastHit.getLastHit;

public class DamageDeathMove implements Listener {

    private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("00.#");
    private final Map<UUID, Location> deathLocations = new HashMap<>();
    private final Set<UUID> voidRespawns = new HashSet<>();
    private final Map<UUID, Boolean> respawnEligibleAtDeath = new HashMap<>();
    private final double tntJumpBarycenterAlterationInY;
    private final double tntJumpStrengthReductionConstant;
    private final double tntJumpYAxisReductionConstant;
    private final double tntDamageSelf;
    private final double tntDamageTeammates;
    private final double tntDamageOthers;

    public DamageDeathMove() {
        this.tntJumpBarycenterAlterationInY = config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_BARYCENTER_IN_Y);
        this.tntJumpStrengthReductionConstant = config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_STRENGTH_REDUCTION);
        this.tntJumpYAxisReductionConstant = config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_Y_REDUCTION);
        this.tntDamageSelf = config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_SELF);
        this.tntDamageTeammates = config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES);
        this.tntDamageOthers = config.getYml().getDouble(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (isMultiArenaLobby(e.getEntity().getWorld())) {
            e.setCancelled(true);
            return;
        }
        if (e.getEntity() instanceof Player p) {
            IArena worldArena = Arena.getArenaByIdentifier(p.getWorld().getName());
            if (worldArena != null && Arena.getArenaByPlayer(p) != worldArena) {
                e.setCancelled(true);
                return;
            }
            IArena a = Arena.getArenaByPlayer(p);
            if (a != null) {
                if (a.isSpectator(p)) {
                    e.setCancelled(true);
                    return;
                }
                if (a.isReSpawning(p)) {
                    e.setCancelled(true);
                    return;
                }
                if (a.getStatus() != GameState.playing) {
                    e.setCancelled(true);
                    return;
                }

                // todo why did I set this to 1? disabled for now
                /*if (e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                    e.setDamage(1);
                    return;
                }*/
                //if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (isRespawnProtected(p)) e.setCancelled(true);
                //}

            }
        }
    }

    // show player health on bow hit
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBowHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!(e.getDamager() instanceof Projectile projectile)) return;
        if (!(projectile.getShooter() instanceof Player damager)) return;

        IArena a = Arena.getArenaByPlayer(p);
        if (a == null) return;
        if (a.getStatus() != GameState.playing) return;

        // projectile hit message #696, #711
        ITeam team = a.getTeam(p);
        Language lang = Language.getPlayerLanguage(damager);
        String template = lang.m(Messages.PLAYER_HIT_BOW);
        if (template.isEmpty()) return;
        String message = template
                .replace("{amount}", HEALTH_FORMAT.format(p.getHealth() - e.getFinalDamage()))
                .replace("{TeamColor}", team.getColor().chat().toString())
                .replace("{TeamName}", team.getDisplayName(lang))
                .replace("{PlayerName}", ChatColor.stripColor(p.getDisplayName()));
        damager.sendMessage(message);
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (isMultiArenaLobby(e.getEntity().getWorld())) {
            e.setCancelled(true);
            return;
        }
        IArena worldArena = Arena.getArenaByIdentifier(e.getEntity().getWorld().getName());
        Player responsiblePlayer = getResponsiblePlayer(e.getDamager());
        if (worldArena != null && responsiblePlayer != null
                && Arena.getArenaByPlayer(responsiblePlayer) != worldArena) {
            e.setCancelled(true);
            return;
        }
        if (e.getEntity() instanceof Player p) {
            IArena a = Arena.getArenaByPlayer(p);
            if (worldArena != null && a != worldArena) {
                e.setCancelled(true);
                return;
            }
            if (a != null) {
                if (a.getStatus() != GameState.playing) {
                    e.setCancelled(true);
                    return;
                }
                if (a.isSpectator(p) || a.isReSpawning(p)) {
                    e.setCancelled(true);
                    return;
                }

                ITeam victimTeam = a.getTeam(p);
                Player damager = null;
                ITeam damagerTeam = null;
                if (e.getDamager() instanceof Player) {
                    damager = (Player) e.getDamager();
                } else if (e.getDamager() instanceof Projectile) {
                    ProjectileSource shooter = ((Projectile) e.getDamager()).getShooter();
                    if (shooter instanceof Player) {
                        damager = (Player) shooter;
                    } else return;
                } else if (e.getDamager() instanceof TNTPrimed) {
                    TNTPrimed tnt = (TNTPrimed) e.getDamager();
                    if (tnt.getSource() != null) {
                        if (tnt.getSource() instanceof Player) {
                            damager = (Player) tnt.getSource();
                            if (damager.equals(p)) {
                                if (tntDamageSelf > -1) {
                                    e.setDamage(tntDamageSelf);
                                }
                                // tnt jump. credits to feargames.it
                                LivingEntity damaged = (LivingEntity) e.getEntity();
                                Vector distance = damaged.getLocation().subtract(0, tntJumpBarycenterAlterationInY, 0).toVector().subtract(tnt.getLocation().toVector());
                                Vector direction = distance.clone().normalize();
                                double force = ((tnt.getYield() * tnt.getYield()) / (tntJumpStrengthReductionConstant + distance.length()));
                                Vector resultingForce = direction.clone().multiply(force);
                                resultingForce.setY(resultingForce.getY() / (distance.length() + tntJumpYAxisReductionConstant));
                                damaged.setVelocity(resultingForce);
                            } else {
                                damagerTeam = a.getTeam(damager);
                                if (victimTeam != null && victimTeam.equals(damagerTeam)) {
                                    if (tntDamageTeammates > -1) {
                                        e.setDamage(tntDamageTeammates);
                                    }
                                } else {
                                    if (tntDamageOthers > -1) {
                                        e.setDamage(tntDamageOthers);
                                    }
                                }
                            }
                        } else return;
                    }
                } else if ((e.getDamager() instanceof Silverfish) || (e.getDamager() instanceof IronGolem)) {
                    LastHit.record(p, e.getDamager(), System.currentTimeMillis());
                }
                if (damager != null) {
                    if (a.isSpectator(damager) || a.isReSpawning(damager.getUniqueId())) {
                        e.setCancelled(true);
                        return;
                    }

                    if (damagerTeam == null) damagerTeam = a.getTeam(damager);
                    if (victimTeam != null && victimTeam.equals(damagerTeam)) {
                        if (!(e.getDamager() instanceof TNTPrimed)) {
                            e.setCancelled(true);
                        }
                        return;
                    }

                    // protection after re-spawn
                    if (isRespawnProtected(p)) {
                        e.setCancelled(true);
                        return;
                    }
                    // but if the damageR is the re-spawning player remove protection
                    BedWarsTeam.reSpawnInvulnerability.remove(damager.getUniqueId());

                    LastHit.record(p, damager, System.currentTimeMillis());

                }
            }
        } else if (nms.isDespawnable(e.getEntity())) {
            Player damager;
            if (e.getDamager() instanceof Player) {
                damager = (Player) e.getDamager();
            } else if (e.getDamager() instanceof Projectile) {
                Projectile proj = (Projectile) e.getDamager();
                damager = (Player) proj.getShooter();
            } else if (e.getDamager() instanceof TNTPrimed) {
                TNTPrimed tnt = (TNTPrimed) e.getDamager();
                if (tnt.getSource() instanceof Player) {
                    damager = (Player) tnt.getSource();
                } else return;
            } else return;
            IArena a = Arena.getArenaByPlayer(damager);
            if (a != null) {
                if (a.isPlayer(damager)) {
                    // do not hurt own mobs
                    Despawnable despawnable = nms.getDespawnablesList().get(e.getEntity().getUniqueId());
                    if (despawnable != null && a.getTeam(damager) == despawnable.getTeam()) {
                        e.setCancelled(true);
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        } /*else if (e.getEntity() instanceof IronGolem) {
            Player damager;
            if (e.getDamager() instanceof Player) {
                damager = (Player) e.getDamager();
            } else if (e.getDamager() instanceof Projectile) {
                Projectile proj = (Projectile) e.getDamager();
                damager = (Player) proj.getShooter();
            } else {
                return;
            }
            Arena a = Arena.getArenaByPlayer(damager);
            if (a != null) {
                if (a.isPlayer(damager)) {
                    if (nms.isDespawnable(e.getEntity())) {
                        if (a.getTeam(damager) == ((OwnedByTeam) nms.getDespawnablesList().get(e.getEntity().getUniqueId())).getOwner()) {
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }*/
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInvisibilityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        IArena arena = Arena.getArenaByPlayer(victim);
        if (arena == null || !arena.getShowTime().containsKey(victim)) return;
        Player damager = getResponsiblePlayer(event.getDamager());
        if (!isEnemyAttack(arena, victim, damager)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!dealtDamage(event.isCancelled(), event.getFinalDamage())) return;
            if (!victim.isOnline() || Arena.getArenaByPlayer(victim) != arena) return;
            if (InvisibilityManager.remove(arena, victim)) {
                victim.sendMessage(getMsg(victim, Messages.INTERACT_INVISIBILITY_REMOVED_DAMGE_TAKEN));
            }
        });
    }

    @EventHandler
    public void onDeath(@NotNull PlayerDeathEvent e) {
        Player victim = e.getEntity(), killer = e.getEntity().getKiller();
        respawnEligibleAtDeath.remove(victim.getUniqueId());
        ITeam killersTeam = null;
        IArena a = Arena.getArenaByPlayer(victim);
        if ((BedWars.getServerType() == ServerType.MULTIARENA && BedWars.getLobbyWorld().equals(e.getEntity().getWorld().getName())) || a != null) {
            e.setDeathMessage(null);
        }
        if (a != null) {
            e.setDroppedExp(0);
            e.setNewExp(0);
            e.setNewLevel(0);
            e.setNewTotalExp(0);
            if (a.isSpectator(victim)) {
                e.getDrops().clear();
                requestRespawnIfNeeded(victim);
                return;
            }
            if (a.getStatus() != GameState.playing) {
                e.getDrops().clear();
                requestRespawnIfNeeded(victim);
                return;
            }
            EntityDamageEvent damageEvent = e.getEntity().getLastDamageCause();

            ITeam victimsTeam = a.getTeam(victim);
            if (victimsTeam == null) {
                e.getDrops().clear();
                requestRespawnIfNeeded(victim);
                return;
            }
            // PlayerRespawnEvent is deferred, so keep the death-time decision stable
            // while bed destruction and the configured respawn countdown continue.
            respawnEligibleAtDeath.put(victim.getUniqueId(), !victimsTeam.isBedDestroyed());

            boolean voidDeath = isVoidDeath(victim, damageEvent, a);
            if (voidDeath) {
                voidRespawns.add(victim.getUniqueId());
                deathLocations.remove(victim.getUniqueId());
            } else {
                voidRespawns.remove(victim.getUniqueId());
                deathLocations.put(victim.getUniqueId(), victim.getLocation().clone());
            }

            BedWars.nms.clearArrowsFromPlayerBody(victim);
            String message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_UNKNOWN_REASON_FINAL_KILL : Messages.PLAYER_DIE_UNKNOWN_REASON_REGULAR;
            PlayerKillEvent.PlayerKillCause cause = victimsTeam.isBedDestroyed() ? PlayerKillEvent.PlayerKillCause.UNKNOWN_FINAL_KILL : PlayerKillEvent.PlayerKillCause.UNKNOWN;
            if (voidDeath) {
                killer = null;
                LastHit lh = getLastHit(victim);
                if (lh != null && lh.getTime() >= System.currentTimeMillis() - 15000
                        && lh.getDamager() instanceof Player lastDamager
                        && !lastDamager.getUniqueId().equals(victim.getUniqueId())) {
                    killer = lastDamager;
                }
                if (killer == null) {
                    message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_VOID_FALL_FINAL_KILL
                            : Messages.PLAYER_DIE_VOID_FALL_REGULAR_KILL;
                } else {
                    message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_KNOCKED_IN_VOID_FINAL_KILL
                            : Messages.PLAYER_DIE_KNOCKED_IN_VOID_REGULAR_KILL;
                }
                cause = victimsTeam.isBedDestroyed() ? PlayerKillEvent.PlayerKillCause.VOID_FINAL_KILL
                        : PlayerKillEvent.PlayerKillCause.VOID;
            } else if (damageEvent != null) {
                if (isExplosionDamage(damageEvent)) {
                    LastHit lh = getLastHit(victim);
                    killer = causingPlayer(damageEvent, victim);
                    if (killer == null && isRecent(lh, System.currentTimeMillis(), 15000)
                            && lh.getDamager() instanceof Player lastDamager
                            && !lastDamager.getUniqueId().equals(victim.getUniqueId())) {
                        killer = lastDamager;
                    }
                    if (killer == null) {
                        message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_EXPLOSION_WITHOUT_SOURCE_FINAL_KILL : Messages.PLAYER_DIE_EXPLOSION_WITHOUT_SOURCE_REGULAR;
                    } else {
                        if (killer != victim) {
                            message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_EXPLOSION_WITH_SOURCE_FINAL_KILL : Messages.PLAYER_DIE_EXPLOSION_WITH_SOURCE_REGULAR_KILL;
                        } else {
                            message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_EXPLOSION_WITHOUT_SOURCE_FINAL_KILL : Messages.PLAYER_DIE_EXPLOSION_WITHOUT_SOURCE_REGULAR;
                        }
                    }
                    cause = victimsTeam.isBedDestroyed() ? PlayerKillEvent.PlayerKillCause.EXPLOSION_FINAL_KILL : PlayerKillEvent.PlayerKillCause.EXPLOSION;

                } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    if (killer == null) {
                        LastHit lh = getLastHit(victim);
                        if (lh != null) {
                            if (lh.getTime() >= System.currentTimeMillis() - 15000) {
                                if (nms.isDespawnable(lh.getDamager())) {
                                    Despawnable d = nms.getDespawnablesList().get(lh.getDamager().getUniqueId());
                                    killersTeam = d.getTeam();
                                    message = d.getEntity().getType() == EntityType.IRON_GOLEM ? victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_IRON_GOLEM_FINAL_KILL : Messages.PLAYER_DIE_IRON_GOLEM_REGULAR : victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_DEBUG_FINAL_KILL : Messages.PLAYER_DIE_DEBUG_REGULAR;
                                    cause = victimsTeam.isBedDestroyed() ? d.getDeathFinalCause() : d.getDeathRegularCause();
                                }
                            }
                        }
                    } else {
                        message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_PVP_FINAL_KILL : Messages.PLAYER_DIE_PVP_REGULAR_KILL;
                        cause = victimsTeam.isBedDestroyed() ? PlayerKillEvent.PlayerKillCause.PVP_FINAL_KILL : PlayerKillEvent.PlayerKillCause.PVP;
                    }
                } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                    if (killer != null) {
                        message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_SHOOT_FINAL_KILL : Messages.PLAYER_DIE_SHOOT_REGULAR;
                        cause = victimsTeam.isBedDestroyed() ? PlayerKillEvent.PlayerKillCause.PLAYER_SHOOT_FINAL_KILL : PlayerKillEvent.PlayerKillCause.PLAYER_SHOOT;
                    }
                } else if (damageEvent.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    LastHit lh = getLastHit(victim);
                    if (lh != null) {
                        // check if kicked off in the last 10 seconds
                        if (lh.getTime() >= System.currentTimeMillis() - 10000) {
                            if (lh.getDamager() instanceof Player) killer = (Player) lh.getDamager();
                            if (killer != null && killer.getUniqueId().equals(victim.getUniqueId())) killer = null;
                            if (killer != null) {
                                if (killer != victim) {
                                    message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_KNOCKED_BY_FINAL_KILL : Messages.PLAYER_DIE_KNOCKED_BY_REGULAR_KILL;
                                } else {
                                    message = victimsTeam.isBedDestroyed() ? Messages.PLAYER_DIE_VOID_FALL_FINAL_KILL : Messages.PLAYER_DIE_VOID_FALL_REGULAR_KILL;
                                }
                            }
                            cause = victimsTeam.isBedDestroyed() ? PlayerKillEvent.PlayerKillCause.PLAYER_PUSH_FINAL : PlayerKillEvent.PlayerKillCause.PLAYER_PUSH;
                        }
                    }
                }
            }

            if (killer != null) killersTeam = a.getTeam(killer);
            String finalMessage = message;

            PlayerKillEvent playerKillEvent = new PlayerKillEvent(a, victim, victimsTeam, killer, killersTeam,
                    player -> Language.getMsg(player, finalMessage), cause
            );
            Bukkit.getPluginManager().callEvent(playerKillEvent);

            if (killer != null && playerKillEvent.playSound()) {
                Sounds.playSound(ConfigPath.SOUNDS_KILL, killer);
            }

            if (null != playerKillEvent.getMessage()) {
                for (Player on : a.getPlayers()) {
                    Language lang = Language.getPlayerLanguage(on);
                    on.sendMessage(playerKillEvent.getMessage().apply(on).
                            replace("{PlayerColor}", victimsTeam.getColor().chat().toString())
                            .replace("{PlayerName}", victim.getDisplayName())
                            .replace("{PlayerNameUnformatted}", victim.getName())
                            .replace("{PlayerTeamName}", victimsTeam.getDisplayName(lang))
                            .replace("{KillerColor}", killersTeam == null ? "" : killersTeam.getColor().chat().toString())
                            .replace("{KillerName}", killer == null ? "" : killer.getDisplayName())
                            .replace("{KillerNameUnformatted}", killer == null ? "" : killer.getName())
                            .replace("{KillerTeamName}", killersTeam == null ? "" : killersTeam.getDisplayName(lang)));
                }
            }

            if (null != playerKillEvent.getMessage()) {
                for (Player on : a.getSpectators()) {
                    Language lang = Language.getPlayerLanguage(on);
                    on.sendMessage(playerKillEvent.getMessage().apply(on).
                            replace("{PlayerColor}", victimsTeam.getColor().chat().toString())
                            .replace("{PlayerName}", victim.getDisplayName())
                            .replace("{PlayerNameUnformatted}", victim.getName())
                            .replace("{KillerColor}", killersTeam == null ? "" : killersTeam.getColor().chat().toString())
                            .replace("{PlayerTeamName}", victimsTeam.getDisplayName(lang))
                            .replace("{KillerName}", killer == null ? "" : killer.getDisplayName())
                            .replace("{KillerNameUnformatted}", killer == null ? "" : killer.getName())
                            .replace("{KillerTeamName}", killersTeam == null ? "" : killersTeam.getDisplayName(lang)));
                }
            }

            // handle drops
            if (PlayerDrops.handlePlayerDrops(a, victim, killer, victimsTeam, killersTeam, cause, e.getDrops())) {
                e.getDrops().clear();
            }

            requestRespawnIfNeeded(victim);

            // reset last damager
            LastHit lastHit = LastHit.getLastHit(victim);
            if (lastHit != null) {
                lastHit.setDamager(null);
            }


            if (victimsTeam.isBedDestroyed() && victimsTeam.getSize() == 1 && a.getConfig().getBoolean(ConfigPath.ARENA_DISABLE_GENERATOR_FOR_EMPTY_TEAMS)) {
                for (IGenerator g : victimsTeam.getGenerators()) {
                    g.disable();
                }
                victimsTeam.getGenerators().clear();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        UUID playerId = e.getPlayer().getUniqueId();
        Location deathLocation = deathLocations.remove(playerId);
        boolean voidDeath = voidRespawns.remove(playerId);
        Boolean eligibleAtDeath = respawnEligibleAtDeath.remove(playerId);
        IArena a = Arena.getArenaByPlayer(e.getPlayer());
        if (a == null) {
            SetupSession ss = SetupSession.getSession(e.getPlayer().getUniqueId());
            if (ss != null) {
                e.setRespawnLocation(e.getPlayer().getWorld().getSpawnLocation());
            }
        } else {
            if (a.getStatus() == GameState.restarting) {
                e.setRespawnLocation(a.isSpectator(e.getPlayer())
                        ? a.getSpectatorLocation()
                        : e.getPlayer().getLocation());
                RestartingPlayerState.preparePlayer(a, e.getPlayer());
                return;
            }
            if (a.isSpectator(e.getPlayer())) {
                e.setRespawnLocation(a.getSpectatorLocation());
                a.sendSpectatorCommandItems(e.getPlayer());
                return;
            }
            ITeam t = a.getTeam(e.getPlayer());
            if (t == null) {
                e.setRespawnLocation(a.getReSpawnLocation());
                plugin.getLogger().severe(e.getPlayer().getName() + " re-spawn error on " + a.getArenaName() + "[" + a.getWorldName() + "] because the team was NULL and he was not spectating!");
                plugin.getLogger().severe("This is caused by one of your plugins: remove or configure any re-spawn related plugins.");
                a.removePlayer(e.getPlayer(), false);
                a.removeSpectator(e.getPlayer(), false);
                return;
            }
            if (!canRespawnAfterDeath(eligibleAtDeath, t.isBedDestroyed())) {
                e.setRespawnLocation(a.getSpectatorLocation());
                a.addSpectator(e.getPlayer(), true, null);
                t.getMembers().remove(e.getPlayer());
                e.getPlayer().sendMessage(getMsg(e.getPlayer(), Messages.PLAYER_DIE_ELIMINATED_CHAT));
                if (t.getMembers().isEmpty()) {
                    Bukkit.getPluginManager().callEvent(new TeamEliminatedEvent(a, t));
                    for (Player p : a.getWorld().getPlayers()) {
                        p.sendMessage(getMsg(p, Messages.TEAM_ELIMINATED_CHAT).replace("{TeamColor}", t.getColor().chat().toString()).replace("{TeamName}", t.getDisplayName(Language.getPlayerLanguage(p))));
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, a::checkWinner, 40L);
                }
            } else {
                //respawn session
                int respawnTime = config.getInt(ConfigPath.GENERAL_CONFIGURATION_RE_SPAWN_COUNTDOWN);
                if (respawnTime > 1) {
                    if (voidDeath) {
                        e.setRespawnLocation(SafeSpawnResolver.resolve(t.getSpawn()).location());
                    } else {
                        e.setRespawnLocation(deathLocation == null ? e.getPlayer().getLocation() : deathLocation);
                    }
                    a.startReSpawnSession(e.getPlayer(), respawnTime);
                } else {
                    // instant respawn configuration
                    SafeSpawnResolver.Result safeSpawn = SafeSpawnResolver.resolve(t.getSpawn());
                    e.setRespawnLocation(safeSpawn.location());
                    SafeSpawnResolver.applyPose(e.getPlayer(), safeSpawn.crawling());
                    t.respawnMember(e.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        deathLocations.remove(playerId);
        voidRespawns.remove(playerId);
        respawnEligibleAtDeath.remove(playerId);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!e.hasChangedPosition()) return;

        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();
        Arena.afkCheck.remove(playerId);
        if (BedWars.getAPI().getAFKUtil().isPlayerAFK(player)) {
            BedWars.getAPI().getAFKUtil().setPlayerAFK(player, false);
        }

        // Paper 1.21.11 exposes exact block-change checks; everything below is block based.
        if (!e.hasChangedBlock()) return;

        Location from = e.getFrom();
        Location to = e.getTo();
        IArena a = Arena.getArenaByPlayer(player);
        if (a != null) {

            if (changedChunk(from, to)) {

                /* update armor-stands hidden by nms */
                String iso = Language.getPlayerLanguage(player).getIso();
                for (IGenerator o : a.getOreGenerators()) {
                    o.updateHolograms(player, iso);
                }
                for (ITeam t : a.getTeams()) {
                    for (IGenerator o : t.getGenerators()) {
                        o.updateHolograms(player, iso);
                    }
                }
                for (ShopHolo sh : ShopHolo.getShopHolo()) {
                    if (sh.getA() == a) {
                        sh.updateForPlayer(player, iso);
                    }
                }

                // hide armor for those with invisibility potions
                if (!a.getShowTime().isEmpty()) {
                    InvisibilityManager.synchronizeViewer(a, player);
                }
            }

            if (a.isSpectator(player) || a.isReSpawning(player)) {
                if (to.getY() < 0) {
                    Location destination = a.isSpectator(player)
                            ? a.getSpectatorLocation() : a.getReSpawnLocation();
                    PlayerMotion.enableFlight(player);
                    TeleportManager.teleportC(player, destination, PlayerTeleportEvent.TeleportCause.PLUGIN)
                            .whenComplete((success, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                                if (error != null || !Boolean.TRUE.equals(success) || !player.isOnline()) return;
                                IArena current = Arena.getArenaByPlayer(player);
                                if (current == a && (a.isSpectator(player) || a.isReSpawning(player))) {
                                    PlayerMotion.enableFlight(player);
                                }
                            }));
                }
            } else {
                if (a.getStatus() == GameState.playing) {
                    if (to.getBlockY() <= a.getYKillHeight()) {
                        voidRespawns.add(playerId);
                        nms.voidKill(player);
                        return;
                    }
                    if (a.getTeam(player) instanceof BedWarsTeam playerTeam) {
                        BedWarsTeam.BedHolo bedHolo = playerTeam.getBedHolo(player);
                        if (bedHolo != null) {
                            boolean nearOwnBed = ConfigManager.isSameWorldWithin(to, playerTeam.getBed(), 4);
                            if (nearOwnBed && !bedHolo.isHidden()) {
                                bedHolo.hide();
                            } else if (!nearOwnBed && bedHolo.isHidden()) {
                                bedHolo.show();
                            }
                        }
                    }
                } else {
                    if (to.getBlockY() <= 0) {
                        ITeam bwt = a.getTeam(player);
                        if (bwt != null) {
                            SafeSpawnResolver.teleport(player, bwt.getSpawn());
                        } else {
                            TeleportManager.teleport(player, a.getSpectatorLocation());
                        }
                    }
                }
            }
        } else {
            if (config.getBoolean(ConfigPath.LOBBY_VOID_TELEPORT_ENABLED) && player.getWorld().getName().equalsIgnoreCase(config.getLobbyWorldName()) && BedWars.getServerType() == ServerType.MULTIARENA) {
                if (to.getY() < config.getInt(ConfigPath.LOBBY_VOID_TELEPORT_HEIGHT)) {
                    TeleportManager.teleportC(player, config.getConfigLoc("lobbyLoc"), PlayerTeleportEvent.TeleportCause.PLUGIN)
                            .whenComplete((success, error) -> Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                                if (error == null && Boolean.TRUE.equals(success)) Arena.enterLobby(player);
                            }));
                }
            }
        }
    }

    static boolean changedChunk(Location from, Location to) {
        return from.getWorld() != to.getWorld()
                || (from.getBlockX() >> 4) != (to.getBlockX() >> 4)
                || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4);
    }

    private static boolean isExplosionDamage(EntityDamageEvent event) {
        return isExplosionDamage(event.getCause(),
                DamageTypeTags.IS_EXPLOSION.isTagged(event.getDamageSource().getDamageType()));
    }

    static boolean isExplosionDamage(EntityDamageEvent.DamageCause cause, boolean explosionDamageType) {
        return cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || explosionDamageType;
    }

    private static Player causingPlayer(EntityDamageEvent event, Player victim) {
        if (!(event.getDamageSource().getCausingEntity() instanceof Player player)) return null;
        return player.getUniqueId().equals(victim.getUniqueId()) ? null : player;
    }

    static boolean isRecent(LastHit lastHit, long now, long maximumAge) {
        return lastHit != null && isRecent(lastHit.getTime(), now, maximumAge);
    }

    static boolean isRecent(long hitTime, long now, long maximumAge) {
        return hitTime >= now - maximumAge && hitTime <= now;
    }

    private static boolean isMultiArenaLobby(org.bukkit.World world) {
        return BedWars.getServerType() == ServerType.MULTIARENA
                && world != null
                && world.getName().equalsIgnoreCase(BedWars.config.getLobbyWorldName());
    }

    private static Player getResponsiblePlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) return player;
        return null;
    }

    static boolean isEnemyAttack(IArena arena, Player victim, Player damager) {
        if (arena == null || victim == null || damager == null || victim.equals(damager)) return false;
        if (!arena.isPlayer(victim) || !arena.isPlayer(damager)) return false;
        if (arena.isSpectator(damager) || arena.isReSpawning(damager.getUniqueId())) return false;
        ITeam victimTeam = arena.getTeam(victim);
        ITeam damagerTeam = arena.getTeam(damager);
        return victimTeam != null && damagerTeam != null && !victimTeam.equals(damagerTeam);
    }

    static boolean dealtDamage(boolean cancelled, double finalDamage) {
        return !cancelled && finalDamage > 0;
    }

    static boolean canRespawnAfterDeath(Boolean eligibleAtDeath, boolean bedDestroyedAtRespawn) {
        return eligibleAtDeath != null ? eligibleAtDeath : !bedDestroyedAtRespawn;
    }

    private static void requestRespawnIfNeeded(Player player) {
        Boolean immediateRespawn = player.getWorld().getGameRuleValue(org.bukkit.GameRules.IMMEDIATE_RESPAWN);
        if (!requiresManualRespawn(immediateRespawn)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.isDead()) {
                player.spigot().respawn();
            }
        });
    }

    static boolean requiresManualRespawn(Boolean immediateRespawn) {
        return !Boolean.TRUE.equals(immediateRespawn);
    }

    private static boolean isRespawnProtected(Player player) {
        UUID playerId = player.getUniqueId();
        Long expiresAt = BedWarsTeam.reSpawnInvulnerability.get(playerId);
        if (expiresAt == null) return false;
        if (expiresAt > System.currentTimeMillis()) return true;
        BedWarsTeam.reSpawnInvulnerability.remove(playerId, expiresAt);
        return false;
    }

    private boolean isVoidDeath(Player player, EntityDamageEvent damageEvent, IArena arena) {
        return voidRespawns.contains(player.getUniqueId())
                || (damageEvent != null && damageEvent.getCause() == EntityDamageEvent.DamageCause.VOID)
                || player.getLocation().getBlockY() <= arena.getYKillHeight();
    }

    @EventHandler
    public void onProjHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        if (proj == null) return;
        if (e.getEntity().getShooter() instanceof Player) {
            IArena a = Arena.getArenaByPlayer((Player) e.getEntity().getShooter());
            if (a != null) {
                if (!a.isPlayer((Player) e.getEntity().getShooter())) return;
                String utility = "";
                if (proj instanceof Snowball) {
                    utility = "silverfish";
                }
                if (!utility.isEmpty()) {
                    spawnUtility(utility, e.getEntity().getLocation(), a.getTeam((Player) e.getEntity().getShooter()), (Player) e.getEntity().getShooter());
                }
            }
        }
    }

    @EventHandler
    public void onItemFrameDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity().getType() == EntityType.ITEM_FRAME) {
            IArena a = Arena.getArenaByIdentifier(e.getEntity().getWorld().getName());
            if (a != null) {
                e.setCancelled(true);
            }
            if (BedWars.getServerType() == ServerType.MULTIARENA) {
                if (BedWars.getLobbyWorld().equals(e.getEntity().getWorld().getName())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (Arena.getArenaByIdentifier(e.getEntity().getLocation().getWorld().getName()) != null) {
            if (e.getEntityType() == EntityType.IRON_GOLEM || e.getEntityType() == EntityType.SILVERFISH) {
                e.getDrops().clear();
                e.setDroppedExp(0);
            }
        }

        // clean if necessary
        nms.getDespawnablesList().remove(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onEat(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == nms.materialCake()) {
            if (Arena.getArenaByIdentifier(e.getPlayer().getWorld().getName()) != null) {
                e.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("unused")
    private static void spawnUtility(String s, Location loc, ITeam t, Player p) {
        if ("silverfish".equalsIgnoreCase(s)) {
            nms.spawnSilverfish(loc, t, shop.getYml().getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_SPEED), shop.getYml().getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_HEALTH),
                    shop.getInt(ConfigPath.SHOP_SPECIAL_SILVERFISH_DESPAWN),
                    BedWars.shop.getYml().getDouble(ConfigPath.SHOP_SPECIAL_SILVERFISH_DAMAGE));
        }
    }
}
