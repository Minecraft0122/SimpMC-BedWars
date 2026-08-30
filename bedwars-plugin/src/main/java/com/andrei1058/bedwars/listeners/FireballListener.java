package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;

import static com.andrei1058.bedwars.BedWars.config;

public class FireballListener implements Listener {

    private static final double MIN_FIREBALL_EXPLOSION_SIZE = 0.1D;
    private static final double MAX_FIREBALL_EXPLOSION_SIZE = 16D;
    private static final double MAX_FIREBALL_KNOCKBACK = 8D;

    private final double fireballExplosionSize;
    private final boolean fireballMakeFire;
    private final double fireballHorizontal;
    private final double fireballVertical;

    private final double damageSelf;
    private final double damageEnemy;
    private final double damageTeammates;

    public FireballListener() {
        this.fireballExplosionSize = normalizeExplosionSize(
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE));
        this.fireballMakeFire = config.getYml().getBoolean(ConfigPath.GENERAL_FIREBALL_MAKE_FIRE);
        this.fireballHorizontal = boundedFiniteNonNegative(
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL));
        this.fireballVertical = boundedFiniteNonNegative(
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL));

        this.damageSelf = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF);
        this.damageEnemy = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY);
        this.damageTeammates = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_TEAMMATES);
    }

    @EventHandler
    public void fireballExplode(EntityExplodeEvent e) {
        if(!(e.getEntity() instanceof Fireball)) return;
        if (e.isCancelled()) return;
        Fireball fireball = (Fireball) e.getEntity();
        Location location = e.getLocation();

        ProjectileSource projectileSource = fireball.getShooter();
        if(!(projectileSource instanceof Player)) return;
        Player source = (Player) projectileSource;

        IArena arena = Arena.getArenaByPlayer(source);
        if (arena == null || !arena.isPlayer(source)) return;

        World world = location.getWorld();

        assert world != null;
        Collection<Entity> nearbyEntities = world
                .getNearbyEntities(location, fireballExplosionSize, fireballExplosionSize, fireballExplosionSize);
        for(Entity entity : nearbyEntities) {
            if(!(entity instanceof Player)) continue;
            Player player = (Player) entity;
            if(Arena.getArenaByPlayer(player) != arena || !arena.isPlayer(player)) continue;


            Vector playerVector = player.getLocation().toVector();
            if (!isWithinExplosionRadius(location.toVector(), playerVector, fireballExplosionSize)) continue;
            player.setVelocity(calculateKnockback(location.toVector(), playerVector,
                    fireballHorizontal, fireballVertical));

            LastHit lh = LastHit.getLastHit(player);
            if (lh != null) {
                lh.setDamager(source);
                lh.setTime(System.currentTimeMillis());
            } else {
                new LastHit(player, source, System.currentTimeMillis());
            }

            if(player.equals(source)) {
                if(damageSelf > 0) {
                    player.damage(damageSelf); // damage shooter
                }
            } else if(arena.getTeam(player).equals(arena.getTeam(source))) {
                if(damageTeammates > 0) {
                    player.damage(damageTeammates); // damage teammates
                }
            } else {
                if(damageEnemy > 0) {
                    player.damage(damageEnemy); // damage enemies
                }
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof Fireball)) return;
        if(!(e.getEntity() instanceof Player)) return;

        if(Arena.getArenaByPlayer((Player) e.getEntity()) == null) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void fireballPrime(ExplosionPrimeEvent e) {
        if(!(e.getEntity() instanceof Fireball)) return;
        ProjectileSource shooter = ((Fireball)e.getEntity()).getShooter();
        if(!(shooter instanceof Player)) return;
        Player player = (Player) shooter;

        if(Arena.getArenaByPlayer(player) == null) return;

        e.setFire(fireballMakeFire);
    }

    static Vector calculateKnockback(Vector explosion, Vector player, double horizontal, double vertical) {
        double safeHorizontal = boundedFiniteNonNegative(horizontal);
        double safeVertical = boundedFiniteNonNegative(vertical);
        Vector towardExplosion = explosion.clone().subtract(player);
        double lengthSquared = towardExplosion.lengthSquared();
        if (!Double.isFinite(lengthSquared) || lengthSquared <= 1.0E-12D) {
            return new Vector(0D, safeVertical * 1.5D, 0D);
        }

        towardExplosion.multiply(1D / Math.sqrt(lengthSquared));
        Vector knockback = towardExplosion.clone().multiply(-safeHorizontal);
        double y = towardExplosion.getY();
        if (y < 0) y += 1.5;
        y = y <= 0.5 ? safeVertical * 1.5D : y * safeVertical * 1.5D;
        return knockback.setY(y);
    }

    static boolean isWithinExplosionRadius(Vector explosion, Vector player, double radius) {
        if (explosion == null || player == null || !Double.isFinite(radius) || radius < 0D) return false;
        double distanceSquared = explosion.distanceSquared(player);
        double radiusSquared = radius > Math.sqrt(Double.MAX_VALUE)
                ? Double.MAX_VALUE : radius * radius;
        return Double.isFinite(distanceSquared) && distanceSquared <= radiusSquared;
    }

    static double normalizeExplosionSize(double value) {
        return Double.isFinite(value)
                ? Math.min(MAX_FIREBALL_EXPLOSION_SIZE, Math.max(MIN_FIREBALL_EXPLOSION_SIZE, value))
                : MIN_FIREBALL_EXPLOSION_SIZE;
    }

    private static double boundedFiniteNonNegative(double value) {
        return Double.isFinite(value) ? Math.min(MAX_FIREBALL_KNOCKBACK, Math.max(0D, value)) : 0D;
    }

}
