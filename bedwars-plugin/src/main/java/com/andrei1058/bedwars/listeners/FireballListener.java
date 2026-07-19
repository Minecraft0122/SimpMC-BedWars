package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Collection;

import static com.andrei1058.bedwars.BedWars.config;

public class FireballListener implements Listener {

    private final double fireballExplosionSize;
    private final boolean fireballMakeFire;
    private final double fireballHorizontal;
    private final double fireballVertical;

    private final double damageSelf;
    private final double damageEnemy;
    private final double damageTeammates;

    public FireballListener() {
        this.fireballExplosionSize = Math.max(0.1,
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE));
        this.fireballMakeFire = config.getYml().getBoolean(ConfigPath.GENERAL_FIREBALL_MAKE_FIRE);
        this.fireballHorizontal = Math.max(0,
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL));
        this.fireballVertical = Math.max(0,
                config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL));

        this.damageSelf = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF);
        this.damageEnemy = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY);
        this.damageTeammates = config.getYml().getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_TEAMMATES);
    }

    @EventHandler
    public void fireballHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;
        Location location = e.getEntity().getLocation();

        ProjectileSource projectileSource = e.getEntity().getShooter();
        if (!(projectileSource instanceof Player source)) return;

        IArena arena = Arena.getArenaByPlayer(source);
        if (arena == null || !arena.isPlayer(source)) return;
        ITeam sourceTeam = arena.getTeam(source);

        World world = location.getWorld();
        if (world == null) return;
        Collection<Entity> nearbyEntities = world
                .getNearbyEntities(location, fireballExplosionSize, fireballExplosionSize, fireballExplosionSize);
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Player player)) continue;
            if (Arena.getArenaByPlayer(player) != arena || !arena.isPlayer(player)) continue;

            player.setVelocity(calculateKnockback(location.toVector(), player.getLocation().toVector(),
                    fireballHorizontal, fireballVertical));

            boolean teammate = sourceTeam != null && sourceTeam.equals(arena.getTeam(player));
            if (!player.equals(source) && !teammate) {
                LastHit lastHit = LastHit.getLastHit(player);
                if (lastHit != null) {
                    lastHit.setDamager(source);
                    lastHit.setTime(System.currentTimeMillis());
                } else {
                    new LastHit(player, source, System.currentTimeMillis());
                }
            }

            if (player.equals(source)) {
                if (damageSelf > 0) {
                    player.damage(damageSelf); // damage shooter
                }
            } else if (teammate) {
                if (damageTeammates > 0) {
                    player.damage(damageTeammates); // damage teammates
                }
            } else {
                if (damageEnemy > 0) {
                    player.damage(damageEnemy); // damage enemies
                }
            }
        }
    }


    @EventHandler
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Fireball fireball)) return;
        if (!(e.getEntity() instanceof Player target)) return;
        if (!(fireball.getShooter() instanceof Player source)) return;
        IArena arena = Arena.getArenaByPlayer(source);
        if (arena == null || Arena.getArenaByPlayer(target) != arena) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void fireballPrime(ExplosionPrimeEvent e) {
        if (!(e.getEntity() instanceof Fireball fireball)) return;
        ProjectileSource shooter = fireball.getShooter();
        if (!(shooter instanceof Player player)) return;

        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.isPlayer(player)) return;

        e.setFire(fireballMakeFire);
        e.setRadius((float) fireballExplosionSize);
    }

    static Vector calculateKnockback(Vector explosion, Vector player, double horizontal, double vertical) {
        Vector towardExplosion = explosion.clone().subtract(player).normalize();
        Vector knockback = towardExplosion.clone().multiply(-horizontal);
        double y = towardExplosion.getY();
        if (y < 0) y += 1.5;
        y = y <= 0.5 ? vertical * 1.5 : y * vertical * 1.5;
        return knockback.setY(y);
    }

}
