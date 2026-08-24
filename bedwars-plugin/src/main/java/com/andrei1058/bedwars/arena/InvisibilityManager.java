package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.events.player.PlayerInvisibilityPotionEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.andrei1058.bedwars.BedWars.nms;

public final class InvisibilityManager {

    private InvisibilityManager() {
    }

    public static boolean activate(IArena arena, Player player, int durationSeconds) {
        if (arena == null || player == null || durationSeconds <= 0) return false;
        ITeam team = arena.getTeam(player);
        if (team == null) return false;

        arena.getShowTime().put(player, durationSeconds);
        synchronizePlayerEquipment(arena, player);
        Bukkit.getPluginManager().callEvent(new PlayerInvisibilityPotionEvent(
                PlayerInvisibilityPotionEvent.Type.ADDED, team, player, arena));
        return true;
    }

    public static boolean remove(IArena arena, Player player) {
        if (arena == null || player == null) return false;
        boolean tracked = arena.getShowTime().remove(player) != null;
        boolean affected = player.hasPotionEffect(PotionEffectType.INVISIBILITY);
        if (!tracked && !affected) return false;

        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        for (Player viewer : viewers(arena)) {
            if (!viewer.equals(player)) {
                nms.showArmor(player, viewer);
            }
        }
        Bukkit.getPluginManager().callEvent(new PlayerInvisibilityPotionEvent(
                PlayerInvisibilityPotionEvent.Type.REMOVED, arena.getTeam(player), player, arena));
        return true;
    }

    public static void synchronizePlayerEquipment(IArena arena, Player invisible) {
        if (!hasHiddenEquipment(arena, invisible)) return;
        for (Player viewer : viewers(arena)) {
            synchronizeEquipment(arena, invisible, viewer);
        }
    }

    public static void synchronizeViewer(IArena arena, Player viewer) {
        if (arena == null || viewer == null) return;
        for (Player respawning : arena.getRespawnSessions().keySet()) {
            if (!respawning.equals(viewer)) {
                nms.spigotHidePlayer(respawning, viewer);
            }
        }
        for (Player invisible : hiddenEquipmentPlayers(arena)) {
            synchronizeEquipment(arena, invisible, viewer);
        }
    }

    /** Hide a respawning player from every current arena viewer. */
    public static void hideRespawningPlayer(IArena arena, Player player) {
        if (arena == null || player == null || nms == null) return;
        for (Player viewer : viewers(arena)) {
            if (!player.equals(viewer)) {
                nms.spigotHidePlayer(player, viewer);
            }
        }
    }

    /** Restore a respawning player's entity and equipment to every arena viewer. */
    public static void showRespawningPlayer(IArena arena, Player player) {
        if (arena == null || player == null || nms == null) return;
        for (Player viewer : viewers(arena)) {
            if (!player.equals(viewer)) {
                nms.spigotShowPlayer(player, viewer);
                nms.showArmor(player, viewer);
            }
        }
    }

    private static void synchronizeEquipment(IArena arena, Player invisible, Player viewer) {
        if (invisible == null || viewer == null || invisible.equals(viewer)) return;
        if (shouldHideEquipment(arena, invisible, viewer)) {
            nms.hideArmor(invisible, viewer);
        } else {
            nms.showArmor(invisible, viewer);
        }
    }

    static boolean shouldHideEquipment(IArena arena, Player invisible, Player viewer) {
        if (arena == null || invisible == null || viewer == null || invisible.equals(viewer)) return false;
        if (arena.isReSpawning(invisible)) {
            return arena.isPlayer(viewer) || arena.isSpectator(viewer);
        }
        if (arena.isSpectator(viewer)) return true;
        if (!arena.isPlayer(viewer)) return false;
        ITeam invisibleTeam = arena.getTeam(invisible);
        return invisibleTeam != null && !invisibleTeam.equals(arena.getTeam(viewer));
    }

    static boolean hasHiddenEquipment(IArena arena, Player player) {
        return arena != null && player != null
                && (arena.getShowTime().containsKey(player) || arena.isReSpawning(player));
    }

    static Set<Player> hiddenEquipmentPlayers(IArena arena) {
        Set<Player> hidden = new LinkedHashSet<>(arena.getShowTime().keySet());
        hidden.addAll(arena.getRespawnSessions().keySet());
        return hidden;
    }

    private static Set<Player> viewers(IArena arena) {
        Set<Player> viewers = new LinkedHashSet<>(arena.getPlayersSnapshot());
        viewers.addAll(arena.getSpectatorsSnapshot());
        return viewers;
    }
}
