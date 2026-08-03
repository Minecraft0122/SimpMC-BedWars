package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.tag.DamageTypeTags;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.andrei1058.bedwars.BedWars.config;

public class FireballListener implements Listener {

    private final double fireballExplosionSize;
    private final boolean fireballMakeFire;
    private final double fireballHorizontal;
    private final double fireballVertical;

    private final double damageSelf;
    private final double damageEnemy;
    private final double damageTeammates;
    private final Set<DamageKey> activeCustomExplosions = new HashSet<>();

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
        if (!(e.getEntity() instanceof Fireball fireball)) return;
        Location location = fireball.getLocation();

        ProjectileSource projectileSource = fireball.getShooter();
        if (!(projectileSource instanceof Player source)) return;

        IArena arena = Arena.getArenaByPlayer(source);
        if (arena == null || !arena.isPlayer(source)) return;
        ITeam sourceTeam = arena.getTeam(source);

        World world = location.getWorld();
        if (world == null) return;
        Collection<Player> nearbyPlayers = world.getNearbyPlayers(location,
                fireballExplosionSize, fireballExplosionSize, fireballExplosionSize,
                player -> Arena.getArenaByPlayer(player) == arena && arena.isPlayer(player));
        Vector explosionPosition = location.toVector();
        long hitTime = System.currentTimeMillis();
        DamageSource playerExplosion = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                .withCausingEntity(source)
                .withDirectEntity(fireball)
                .build();
        DamageSource unattributedExplosion = DamageSource.builder(DamageType.EXPLOSION).build();
        for (Player player : nearbyPlayers) {

            player.setVelocity(calculateKnockback(explosionPosition,
                    new Vector(player.getX(), player.getY(), player.getZ()),
                    fireballHorizontal, fireballVertical));

            boolean self = player.equals(source);
            boolean teammate = sourceTeam != null && sourceTeam.equals(arena.getTeam(player));
            if (shouldCreditShooter(self, teammate)) {
                LastHit.record(player, source, hitTime);
            }

            if (self) {
                if (damageSelf > 0) {
                    player.damage(damageSelf, unattributedExplosion); // damage shooter without kill credit
                }
            } else if (teammate) {
                if (damageTeammates > 0) {
                    player.damage(damageTeammates, unattributedExplosion); // do not credit friendly fire
                }
            } else {
                if (damageEnemy > 0) {
                    damageEnemy(player, fireball, playerExplosion);
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Fireball fireball)) return;
        if (!(e.getEntity() instanceof Player target)) return;
        // The custom area damage also names the fireball as its direct entity.
        // Only its synchronous, explicitly marked damage call is allowed through;
        // the projectile's native contact and native explosion damage stay cancelled.
        DamageKey damageKey = new DamageKey(fireball.getUniqueId(), target.getUniqueId());
        boolean explosionDamage = DamageTypeTags.IS_EXPLOSION.isTagged(
                e.getDamageSource().getDamageType());
        if (!shouldCancelDirectHit(explosionDamage, activeCustomExplosions.contains(damageKey))) return;
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

    static boolean shouldCreditShooter(boolean self, boolean teammate) {
        return !self && !teammate;
    }

    static boolean shouldCancelDirectHit(boolean explosionDamage, boolean activeCustomExplosion) {
        return !explosionDamage || !activeCustomExplosion;
    }

    private void damageEnemy(Player target, Fireball fireball, DamageSource source) {
        DamageKey damageKey = new DamageKey(fireball.getUniqueId(), target.getUniqueId());
        boolean firstSettlement = activeCustomExplosions.add(damageKey);
        try {
            target.damage(damageEnemy, source);
        } finally {
            if (firstSettlement) activeCustomExplosions.remove(damageKey);
        }
    }

    private record DamageKey(UUID fireball, UUID target) {
    }

}
