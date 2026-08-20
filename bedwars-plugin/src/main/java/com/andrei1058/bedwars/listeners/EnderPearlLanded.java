package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.Sounds;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Objects;
import java.util.function.Function;

public class EnderPearlLanded implements Listener {

    private final Function<Player, IArena> arenaResolver;

    public EnderPearlLanded() {
        this(Arena::getArenaByPlayer);
    }

    EnderPearlLanded(Function<Player, IArena> arenaResolver) {
        this.arenaResolver = Objects.requireNonNull(arenaResolver, "arenaResolver");
    }

    @EventHandler
    public void onPearlHit(ProjectileHitEvent e){

        if (!(e.getEntity() instanceof EnderPearl)) return;
        if (!(e.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) e.getEntity().getShooter();
        IArena iArena = arenaResolver.apply(player);

        if (iArena == null || iArena.isSpectator(player)) return;

        Sounds.playSound("ender-pearl-landed", iArena.getPlayers());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPearlTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;

        IArena arena = arenaResolver.apply(event.getPlayer());
        if (arena != null && shouldCancelTeleport(event.getCause(), event.getTo().getBlockY(), arena.getYKillHeight())) {
            event.setCancelled(true);
        }
    }

    static boolean shouldCancelTeleport(PlayerTeleportEvent.TeleportCause cause,
                                        int destinationBlockY, int killHeight) {
        return cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && destinationBlockY <= killHeight;
    }
}
