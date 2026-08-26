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

package com.andrei1058.bedwars.support.version.v1_8_R3;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import net.minecraft.server.v1_8_R3.Enchantment;
import net.minecraft.server.v1_8_R3.EnchantmentManager;
import net.minecraft.server.v1_8_R3.EntityArrow;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Bridges the 1.8 collision flag and arrow hit detection.
 *
 * <p>Spigot 1.8 has no scoreboard collision rule. Disabling a player with
 * {@code Player.Spigot#setCollidesWithEntities(false)} also makes that player
 * invisible to {@link EntityArrow#t_()}. The replacement arrow below exposes
 * only valid enemy players for the duration of that NMS tick.</p>
 */
public final class LegacyArrowListener implements Listener {

    private final BedWars api;

    public LegacyArrowListener(BedWars api) {
        this.api = api;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Entity projectile = event.getProjectile();
        if (!(projectile instanceof CraftArrow)) {
            return;
        }

        EntityArrow original = ((CraftArrow) projectile).getHandle();
        if (original instanceof BedWarsArrow || !(original.shooter instanceof EntityPlayer)) {
            return;
        }

        Player shooter = (Player) event.getEntity();
        IArena arena = findArena(shooter);
        if (!isActivePlayer(arena, shooter)) {
            return;
        }

        EntityPlayer nmsShooter = (EntityPlayer) original.shooter;
        boolean infinite = nmsShooter.abilities.canInstantlyBuild;
        if (!infinite && event.getBow() != null) {
            net.minecraft.server.v1_8_R3.ItemStack bow = CraftItemStack.asNMSCopy(event.getBow());
            infinite = bow != null
                    && EnchantmentManager.getEnchantmentLevel(Enchantment.ARROW_INFINITE.id, bow) > 0;
        }

        BedWarsArrow replacement = new BedWarsArrow(original, nmsShooter, event.getForce(), arena, infinite);
        replacement.projectileSource = original.projectileSource != null
                ? original.projectileSource
                : (ProjectileSource) shooter;

        // Replace the event reference first so ItemBow never falls back to
        // adding the original arrow, even when ProjectileLaunchEvent rejects
        // the replacement during World#addEntity.
        event.setProjectile(replacement.getBukkitEntity());
        if (!original.world.addEntity(replacement)) {
            replacement.die();
            return;
        }
    }

    private IArena findArena(Player player) {
        if (api == null || api.getArenaUtil() == null) {
            return null;
        }
        return api.getArenaUtil().getArenaByPlayer(player);
    }

    private boolean isActivePlayer(IArena arena, Player player) {
        return arena != null
                && arena.getStatus() == GameState.playing
                && arena.isPlayer(player)
                && !arena.isSpectator(player)
                && !arena.isReSpawning(player.getUniqueId());
    }

    private static final class BedWarsArrow extends EntityArrow {

        private final IArena arena;

        private BedWarsArrow(EntityArrow original, EntityLiving shooter, float force, IArena arena, boolean infinite) {
            super(original.world, shooter, force * 2.0F);
            this.arena = arena;
            copyState(original, infinite);
        }

        private void copyState(EntityArrow original, boolean infinite) {
            setPositionRotation(original.locX, original.locY, original.locZ, original.yaw, original.pitch);
            lastX = original.lastX;
            lastY = original.lastY;
            lastZ = original.lastZ;
            motX = original.motX;
            motY = original.motY;
            motZ = original.motZ;
            yaw = original.yaw;
            pitch = original.pitch;
            lastYaw = original.lastYaw;
            lastPitch = original.lastPitch;
            ticksLived = original.ticksLived;
            fireTicks = original.fireTicks;
            maxFireTicks = original.maxFireTicks;
            setCritical(original.isCritical());
            b(original.j());
            setKnockbackStrength(original.knockbackStrength);

            // ItemBow assigns this after EntityShootBowEvent returns. Since the
            // original arrow is never added, preserve the same pickup behavior.
            if (shooter instanceof EntityHuman) {
                EntityHuman human = (EntityHuman) shooter;
                fromPlayer = infinite || human.abilities.canInstantlyBuild ? 2 : 1;
            } else {
                fromPlayer = 1;
            }
        }

        @Override
        public void t_() {
            Map<EntityPlayer, Boolean> changed = new IdentityHashMap<>();
            try {
                exposeEnemyPlayers(changed);
                super.t_();
            } finally {
                for (Map.Entry<EntityPlayer, Boolean> entry : changed.entrySet()) {
                    entry.getKey().collidesWithEntities = entry.getValue();
                }
            }
        }

        private void exposeEnemyPlayers(Map<EntityPlayer, Boolean> changed) {
            if (arena == null || arena.getStatus() != GameState.playing || !(shooter instanceof EntityPlayer)) {
                return;
            }

            EntityPlayer nmsShooter = (EntityPlayer) shooter;
            Player shooterPlayer = (Player) nmsShooter.getBukkitEntity();
            if (!arena.isPlayer(shooterPlayer)
                    || arena.isSpectator(shooterPlayer)
                    || arena.isReSpawning(shooterPlayer.getUniqueId())) {
                return;
            }

            ITeam shooterTeam = arena.getTeam(shooterPlayer);
            if (shooterTeam == null) {
                return;
            }

            for (Player target : arena.getPlayers()) {
                if (!isEnemyTarget(target, shooterPlayer, shooterTeam)) {
                    continue;
                }

                EntityPlayer nmsTarget = ((CraftPlayer) target).getHandle();
                if (nmsTarget.world != world || changed.containsKey(nmsTarget)) {
                    continue;
                }

                changed.put(nmsTarget, nmsTarget.collidesWithEntities);
                nmsTarget.collidesWithEntities = true;
            }
        }

        private boolean isEnemyTarget(Player target, Player shooterPlayer, ITeam shooterTeam) {
            if (target == null || target.equals(shooterPlayer) || !target.isOnline() || target.isDead()) {
                return false;
            }
            if (!arena.isPlayer(target)
                    || arena.isSpectator(target)
                    || arena.isReSpawning(target.getUniqueId())) {
                return false;
            }
            ITeam targetTeam = arena.getTeam(target);
            return targetTeam != null && !shooterTeam.equals(targetTeam);
        }
    }
}
