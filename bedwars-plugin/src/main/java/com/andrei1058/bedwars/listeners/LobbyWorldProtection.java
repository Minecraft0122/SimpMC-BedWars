package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.arena.GameRules;
import com.andrei1058.bedwars.support.paper.TeleportManager;
import io.papermc.paper.event.world.WorldDifficultyChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.*;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Environmental protection for the configured or actual fallback lobby only. */
public final class LobbyWorldProtection implements Listener {
    private final Set<UUID> pendingVoidReturns = ConcurrentHashMap.newKeySet();
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!protectedWorld(event.getLocation().getWorld())) return;
        event.blockList().clear();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!protectedWorld(event.getBlock().getWorld())) return;
        event.blockList().clear();
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!protectedWorld(event.getEntity().getWorld())) return;
        event.setCancelled(true);
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            returnFromVoid(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && protectedWorld(player.getWorld())) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (protectedWorld(event.getLocation().getWorld())
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (protectedWorld(event.getBlock().getWorld())
                && (event.getPlayer() == null || !BreakPlace.isBuildSession(event.getPlayer()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (protectedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (protectedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (protectedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (protectedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (protectedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (denyBuilding(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (denyBuilding(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (denyBuilding(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (denyBuilding(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!protectedWorld(event.getEntity().getWorld())) return;
        if (event instanceof HangingBreakByEntityEvent hit && hit.getRemover() instanceof Player player
                && BreakPlace.isBuildSession(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisplayInteract(PlayerInteractEntityEvent event) {
        if ((event.getRightClicked() instanceof ArmorStand || event.getRightClicked() instanceof ItemFrame)
                && denyBuilding(event.getPlayer(), event.getRightClicked().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisplayInteractAt(PlayerInteractAtEntityEvent event) {
        onDisplayInteract(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (denyBuilding(event.getPlayer(), event.getRightClicked().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDifficultyChange(WorldDifficultyChangeEvent event) {
        if (event.getDifficulty() == Difficulty.PEACEFUL || !protectedWorld(event.getWorld())) return;
        // This is a notification event. Restore once after the change has applied.
        Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
            World world = event.getWorld();
            if (Bukkit.getWorld(world.getUID()) == world && protectedWorld(world)) {
                GameRules.enforceLobbyEnvironment(world);
            }
        });
    }

    private static boolean denyBuilding(Player player, World world) {
        return protectedWorld(world) && !BreakPlace.isBuildSession(player);
    }

    private void returnFromVoid(Player player) {
        UUID id = player.getUniqueId();
        if (!pendingVoidReturns.add(id)) return;
        Location target = BedWars.config == null ? null : BedWars.config.getConfigLoc("lobbyLoc");
        if (target == null || target.getWorld() != player.getWorld()) target = player.getWorld().getSpawnLocation();
        player.setFallDistance(0);
        TeleportManager.teleport(player, target).whenComplete((success, error) -> pendingVoidReturns.remove(id));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingVoidReturns.remove(event.getPlayer().getUniqueId());
    }

    private static boolean protectedWorld(World world) {
        return BedWarsWorldEnvironment.isLobbyManagedWorld(world);
    }
}
