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
import com.andrei1058.bedwars.api.arena.NextEvent;
import com.andrei1058.bedwars.api.arena.generator.IGenerator;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.region.Region;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.api.util.BlastProtectionUtil;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Sounds;
import com.andrei1058.bedwars.support.paper.TeleportManager;
import com.andrei1058.bedwars.popuptower.TowerEast;
import com.andrei1058.bedwars.popuptower.TowerNorth;
import com.andrei1058.bedwars.popuptower.TowerSouth;
import com.andrei1058.bedwars.popuptower.TowerWest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class BreakPlace implements Listener {

    static final long SHEARS_BREAK_COOLDOWN_MILLIS = 1000L;

    private static final Set<UUID> BUILD_SESSIONS = new HashSet<>();
    private final Map<UUID, Long> lastShearsBreakAt = new HashMap<>();
    private final boolean allowFireBreak;
    private final BlastProtectionUtil blastProtection;

    public BreakPlace() {
        allowFireBreak = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_ALLOW_FIRE_EXTINGUISH);
        blastProtection = new BlastProtectionUtil(nms, BedWars.getAPI());
    }

    @EventHandler
    public void onIceMelt(BlockFadeEvent e) {
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (Objects.requireNonNull(e.getBlock().getLocation().getWorld()).getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                e.setCancelled(true);
                return;
            }
        }
        if (e.getBlock().getType() == Material.ICE) {
            if (Arena.getArenaByIdentifier(e.getBlock().getWorld().getName()) != null) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCactus(BlockPhysicsEvent e) {
        if (e.getBlock().getType() == Material.CACTUS) {
            if (Arena.getArenaByIdentifier(e.getBlock().getWorld().getName()) != null) e.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onBurn(@NotNull BlockBurnEvent event) {
        IArena arena = Arena.getArenaByIdentifier(event.getBlock().getWorld().getName());
        if (arena == null) return;
        if (!arena.isAllowMapBreak() && !arena.isBlockPlaced(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        if (arena.isTeamBed(event.getBlock().getLocation())){
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireSpread(@NotNull BlockIgniteEvent event) {
        if (!isFireSpread(event.getCause())) return;
        if (Arena.getArenaByIdentifier(event.getBlock().getWorld().getName()) != null) {
            event.setCancelled(true);
        }
    }

    static boolean isFireSpread(BlockIgniteEvent.IgniteCause cause) {
        return cause == BlockIgniteEvent.IgniteCause.SPREAD;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.isCancelled()) return;

        Player p = e.getPlayer();
        if (SetupSession.isEditingWorld(p.getUniqueId(), e.getBlock().getWorld().getName())) return;

        //Prevent player from placing during the removal from the arena
        IArena arena = Arena.getArenaByIdentifier(e.getBlock().getWorld().getName());
        IArena playerArena = Arena.getArenaByPlayer(p);
        if (playerArena != null && playerArena != arena) {
            e.setCancelled(true);
            return;
        }
        if (arena != null) {
            if (arena.getStatus() != GameState.playing || playerArena != arena) {
                e.setCancelled(true);
                return;
            }
            if (e.getItemInHand().getType().equals(nms.materialFireball()) && e.getBlockPlaced().getType().equals(Material.FIRE)) {
                e.setCancelled(true);
            }
        }
        IArena a = playerArena;
        if (a != null) {
            if (a.isSpectator(p)) {
                e.setCancelled(true);
                return;
            }
            if (a.getRespawnSessions().containsKey(p)) {
                e.setCancelled(true);
                return;
            }
            if (a.getStatus() != GameState.playing) {
                e.setCancelled(true);
                return;
            }
            if (e.getBlockPlaced().getLocation().getBlockY() >= a.getConfig().getInt(ConfigPath.ARENA_CONFIGURATION_MAX_BUILD_Y)) {
                e.setCancelled(true);
                return;
            }

            if (hasProtectedPlacedBlock(a, e)) {
                e.setCancelled(true);
                AdventureText.send(p, getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                return;
            }

            // prevent modifying wood if protected
            // issue #531
            if (e.getBlockPlaced().getType().toString().contains("STRIPPED_") && e.getBlock().getType().toString().contains("_WOOD")) {
                if (null != arena && !arena.isAllowMapBreak()) {
                    e.setCancelled(true);
                    return;
                }
            }

            if (e.getBlock().getType() == Material.TNT) {
                if (config.getBoolean(ConfigPath.GENERAL_TNT_AUTO_IGNITE)) {
                    e.getBlockPlaced().setType(Material.AIR);
                    TNTPrimed tnt = Objects.requireNonNull(e.getBlock().getLocation().getWorld()).spawn(e.getBlock().getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
                    tnt.setFuseTicks(config.getInt(ConfigPath.GENERAL_TNT_FUSE_TICKS));
                    nms.setSource(tnt, p);
                    return;
                }
            } else if (BedWars.shop.getBoolean(ConfigPath.SHOP_SPECIAL_TOWER_ENABLE)) {
                if (e.getBlock().getType() == Material.valueOf(shop.getString(ConfigPath.SHOP_SPECIAL_TOWER_MATERIAL))) {

                    e.setCancelled(true);
                    Location loc = e.getBlock().getLocation();
                    IArena a1 = Arena.getArenaByPlayer(p);
                    TeamColor col = a1.getTeam(p).getColor();
                    double rotation = (p.getLocation().getYaw() - 90.0F) % 360.0F;
                    if (rotation < 0.0D) {
                        rotation += 360.0D;
                    }
                    if (45.0D <= rotation && rotation < 135.0D) {
                        new TowerSouth(loc, e.getBlockPlaced(), col, p, e.getHand());
                    } else if (225.0D <= rotation && rotation < 315.0D) {
                        new TowerNorth(loc, e.getBlockPlaced(), col, p, e.getHand());
                    } else if (135.0D <= rotation && rotation < 225.0D) {
                        new TowerWest(loc, e.getBlockPlaced(), col, p, e.getHand());
                    } else if (0.0D <= rotation && rotation < 45.0D) {
                        new TowerEast(loc, e.getBlockPlaced(), col, p, e.getHand());
                    } else if (315.0D <= rotation && rotation < 360.0D) {
                        new TowerEast(loc, e.getBlockPlaced(), col, p, e.getHand());
                    }
                }
            }
            return;
        }
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (Objects.requireNonNull(e.getBlock().getLocation().getWorld()).getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                if (!isBuildSession(p)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() != null && isFire(event.getClickedBlock().getType())) {
            IArena arena = Arena.getArenaByPlayer(player);
            if (arena != null && !allowFireBreak) {
                event.setCancelled(true);
                return;
            }
        }
        if (BedWars.getServerType() == ServerType.MULTIARENA
                && player.getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
            if (event.getClickedBlock() != null && isFire(event.getClickedBlock().getRelative(BlockFace.UP).getType())) {
                if (!isBuildSession(player)) {
                    event.setCancelled(true);
                    //return;
                }
            }
        }
    }

    @EventHandler
    public void onBlockDrop(ItemSpawnEvent event) {
        //WHEAT_SEEDS AND BEDs
        IArena arena = Arena.getArenaByIdentifier(event.getEntity().getWorld().getName());
        if (arena == null) return;
        Material material = event.getEntity().getItemStack().getType();
        if (nms.isBed(material) || material.toString().equalsIgnoreCase("SEEDS") || material.toString().equalsIgnoreCase("WHEAT_SEEDS")) {
            event.setCancelled(true);
        }
    }

    /**
     * Limit wool breaks to roughly one successful block per second while
     * leaving all protection decisions to the normal BlockBreakEvent handler.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShearsBlockDamage(@NotNull BlockDamageEvent event) {
        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing
                || !arena.isPlayer(player) || arena.isSpectator(player)
                || arena.getRespawnSessions().containsKey(player)) {
            return;
        }

        if (!isWool(event.getBlock().getType())) {
            return;
        }

        ItemStack heldItem = nms.getItemInHand(player);
        if (heldItem != null && heldItem.getType() == Material.SHEARS) {
            if (!isShearsBreakReady(lastShearsBreakAt.get(player.getUniqueId()), System.currentTimeMillis())) {
                event.setCancelled(true);
                return;
            }
            event.setInstaBreak(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShearsBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing
                || !arena.isPlayer(player) || arena.isSpectator(player)
                || arena.getRespawnSessions().containsKey(player)
                || !isWool(event.getBlock().getType())) {
            return;
        }

        ItemStack heldItem = nms.getItemInHand(player);
        if (heldItem != null && heldItem.getType() == Material.SHEARS) {
            lastShearsBreakAt.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        IArena worldArena = Arena.getArenaByIdentifier(e.getBlock().getWorld().getName());
        IArena a = Arena.getArenaByPlayer(p);
        if (worldArena != a && (worldArena != null || a != null)) {
            e.setCancelled(true);
            return;
        }
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (Objects.requireNonNull(e.getBlock().getLocation().getWorld()).getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                if (!isBuildSession(p)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        if (a != null) {
            if (!a.isPlayer(p)) {
                e.setCancelled(true);
                return;
            }
            if (a.getRespawnSessions().containsKey(p)) {
                e.setCancelled(true);
                return;
            }
            if (a.getStatus() != GameState.playing) {
                e.setCancelled(true);
                return;
            }

            // allow breaking of grass
            // drops are removed in another event
            switch (e.getBlock().getType().toString()) {
                case "LONG_GRASS":
                case "TALL_GRASS":
                case "TALL_SEAGRASS":
                case "SEAGRASS":
                case "SUGAR_CANE":
                case "SUGAR_CANE_BLOCK":
                case "GRASS_PATH":
                case "DOUBLE_PLANT":
                    if (e.isCancelled()) {
                        e.setCancelled(false);
                    }
                    return;
                case "FIRE":
                case "SOUL_FIRE":
                    if (allowFireBreak) {
                        e.setCancelled(false);
                        return;
                    }
                    break;
            }

            if (nms.isBed(e.getBlock().getType())) {
                for (ITeam t : a.getTeams()) {
                    for (int x = e.getBlock().getX() - 2; x < e.getBlock().getX() + 2; x++) {
                        for (int y = e.getBlock().getY() - 2; y < e.getBlock().getY() + 2; y++) {
                            for (int z = e.getBlock().getZ() - 2; z < e.getBlock().getZ() + 2; z++) {
                                if (t.getBed().getBlockX() == x && t.getBed().getBlockY() == y && t.getBed().getBlockZ() == z) {
                                    if (!t.isBedDestroyed()) {
                                        if (t.isMember(p)) {
                                            AdventureText.send(p, getMsg(p, Messages.INTERACT_CANNOT_BREAK_OWN_BED));
                                            e.setCancelled(true);
                                            if (e.getPlayer().getLocation().getBlock().getType().toString().contains("BED")) {
                                                TeleportManager.teleport(e.getPlayer(), e.getPlayer().getLocation().add(0, 0.5, 0));
                                            }
                                        } else {
                                            e.setCancelled(false);
                                            t.setBedDestroyed(true);
                                            PlayerBedBreakEvent breakEvent;
                                            Bukkit.getPluginManager().callEvent(breakEvent = new PlayerBedBreakEvent(e.getPlayer(), a.getTeam(p), t, a,
                                                    player -> {
                                                        if (t.isMember(player)) {
                                                            return getMsg(player, Messages.INTERACT_BED_DESTROY_CHAT_ANNOUNCEMENT_TO_VICTIM);
                                                        } else {
                                                            return getMsg(player, Messages.INTERACT_BED_DESTROY_CHAT_ANNOUNCEMENT);
                                                        }
                                                    },
                                                    player -> {
                                                        if (t.isMember(player)) {
                                                            return getMsg(player, Messages.INTERACT_BED_DESTROY_TITLE_ANNOUNCEMENT);
                                                        }
                                                        return null;
                                                    },
                                                    player -> {
                                                        if (t.isMember(player)) {
                                                            return getMsg(player, Messages.INTERACT_BED_DESTROY_SUBTITLE_ANNOUNCEMENT);
                                                        }
                                                        return null;
                                                    }));
                                            for (Player on : a.getWorld().getPlayers()) {
                                                if (breakEvent.getMessage() != null) {
                                                    AdventureText.send(on, breakEvent.getMessage().apply(on)
                                                            .replace("{TeamColor}", t.getColor().chat().toString())
                                                            .replace("{TeamName}", t.getDisplayName(Language.getPlayerLanguage(on)))
                                                            .replace("{PlayerColor}", a.getTeam(p).getColor().chat().toString())
                                                            .replace("{PlayerName}", AdventureText.displayName(p))
                                                            .replace("{PlayerNameUnformatted}", p.getName()));
                                                }
                                                if (breakEvent.getTitle() != null && breakEvent.getSubTitle() != null) {
                                                    nms.sendTitle(on, AdventureText.section(breakEvent.getTitle().apply(on)), AdventureText.section(breakEvent.getSubTitle().apply(on)), 0, 40, 10);
                                                }
                                                if (t.isMember(on))
                                                    Sounds.playSound(ConfigPath.SOUNDS_BED_DESTROY_OWN, on);
                                                else Sounds.playSound(ConfigPath.SOUNDS_BED_DESTROY, on);
                                            }
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            boolean placedBlock = a.isBlockPlaced(e.getBlock());
            boolean protectedRegion = false;
            if (!placedBlock) {
                for (Region region : a.getRegionsList()) {
                    if (region.isProtected() && region.isInRegion(e.getBlock().getLocation())) {
                        protectedRegion = true;
                        break;
                    }
                }
            }
            if (isProtectedMapBlock(placedBlock, protectedRegion, a.isAllowMapBreak())) {
                AdventureText.send(p, getMsg(p, Messages.INTERACT_CANNOT_BREAK_BLOCK));
                e.setCancelled(true);
                return;
            }
        }
    }

    /**
     * update game signs
     */
    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        if (e == null) return;
        Player p = e.getPlayer();
        String commandLine = AdventureText.plain(e.line(0));
        String arenaName = AdventureText.plain(e.line(1));
        if (commandLine.equalsIgnoreCase("[" + mainCmd + "]")) {
            File dir = new File(plugin.getDataFolder(), "/Arenas");
            boolean exists = false;
            if (dir.exists()) {
                for (File f : Objects.requireNonNull(dir.listFiles())) {
                    if (f.isFile()) {
                        if (f.getName().contains(".yml")) {
                            if (Objects.equals(arenaName, f.getName().replace(".yml", ""))) {
                                exists = true;
                            }
                        }
                    }
                }
                List<String> s;
                if (signs.getYml().get("locations") == null) {
                    s = new ArrayList<>();
                } else {
                    s = new ArrayList<>(signs.getYml().getStringList("locations"));
                }
                if (exists) {
                    s.add(arenaName + "," + signs.stringLocationConfigFormat(e.getBlock().getLocation()));
                    signs.set("locations", s);
                }
                IArena a = Arena.getArenaByName(arenaName);
                if (a != null) {
                    AdventureText.send(p, "§a▪ §7已保存竞技场告示牌：" + arenaName);
                    a.addSign(e.getBlock().getLocation());
                    Sign b = (Sign) e.getBlock().getState();
                    int line = 0;
                    for (String string : BedWars.signs.getList("format")) {
                        e.line(line, AdventureText.ampersand(string.replace("[on]", String.valueOf(a.getPlayers().size())).replace("[max]",
                                        String.valueOf(a.getMaxPlayers())).replace("[arena]", a.getDisplayName()).replace("[status]", a.getDisplayStatus(Language.getDefaultLanguage()))
                                .replace("[type]", String.valueOf(a.getMaxInTeam())).replace('\u00a7', '&')));
                        line++;
                    }
                    b.update(true);
                }
            } else {
                AdventureText.send(p, "§c▪ §7你还没有设置任何竞技场！");
            }
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (e.isCancelled()) return;
        Block clickedBlock = e.getBlockClicked();
        IArena worldArena = Arena.getArenaByIdentifier(e.getBlockClicked().getWorld().getName());
        IArena playerArena = Arena.getArenaByPlayer(e.getPlayer());
        if (worldArena != playerArena && (worldArena != null || playerArena != null)) {
            e.setCancelled(true);
            return;
        }
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (Objects.requireNonNull(e.getPlayer().getLocation().getWorld()).getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                if (!isBuildSession(e.getPlayer())) {
                    e.setCancelled(true);
                }
            }
        }
        IArena a = playerArena;
        if (a != null) {
            if (a.isSpectator(e.getPlayer()) || a.getStatus() != GameState.playing || a.getRespawnSessions().containsKey(e.getPlayer()))
                e.setCancelled(true);
            if (!e.isCancelled() && a.isBlockPlaced(clickedBlock)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (a.getStatus() == GameState.playing && clickedBlock.getType().isAir()) {
                        a.removePlacedBlock(clickedBlock);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (e.isCancelled()) return;

        // Lobby protection in MULTIARENA
        if (BedWars.getServerType() == ServerType.MULTIARENA) {
            if (Objects.requireNonNull(e.getPlayer().getLocation().getWorld()).getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                if (!isBuildSession(e.getPlayer())) {
                    e.setCancelled(true);
                }
            }
        }

        // Prevent player from placing during the removal from the arena
        IArena arena = Arena.getArenaByIdentifier(e.getBlockClicked().getWorld().getName());
        if (arena != null && arena.getStatus() != GameState.playing) {
            e.setCancelled(true);
            return;
        }

        Player p = e.getPlayer();
        IArena a = Arena.getArenaByPlayer(p);
        if (arena != a && (arena != null || a != null)) {
            e.setCancelled(true);
            return;
        }
        if (a != null) {
            // Restriction checks (spectator, respawning, not playing)
            if (isPlayerRestrictedInArena(a, p)) {
                e.setCancelled(true);
                return;
            }

            // Water placement target location
            Block waterBlock = e.getBlockClicked().getRelative(e.getBlockFace());
            Location waterLocation = waterBlock.getLocation();

            // Build height limit
            if (isAboveMaxBuildY(a, waterLocation)) {
                e.setCancelled(true);
                return;
            }

            // Protected areas around shops/upgrades/generators. Team spawn remains buildable so players can defend beds.
            if (isShopUpgradeOrGeneratorProtected(a, waterLocation)) {
                e.setCancelled(true);
                AdventureText.send(p, getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                return;
            }

            // Bucket placement does not emit BlockPlaceEvent. Track the
            // source block after the server applies it so subsequent fluid
            // flow is treated as player-built rather than map erosion.
            if (e.getBucket() == Material.WATER || e.getBucket() == Material.LAVA) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Other listeners may cancel this event after our normal
                    // priority handler. Only claim the source block when the
                    // final event state actually placed liquid.
                    if (!e.isCancelled() && a.getStatus() == GameState.playing
                            && !waterBlock.getType().isAir()) {
                        a.addPlacedBlock(waterBlock);
                    }
                });
            }

            // Remove one empty bucket from player's hand after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> nms.minusAmount(e.getPlayer(), e.getItemStack(), 1), 3L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlow(@NotNull EntityExplodeEvent e) {
        IArena a = Arena.getArenaByIdentifier(e.getLocation().getWorld().getName());
        if (a != null) {
            if (a.getStatus() == GameState.playing) {
                boolean fireball = e.getEntity() instanceof Fireball;
                e.blockList().removeIf((b) -> shouldProtectExplosionBlock(
                        fireball, a.isBlockPlaced(b), a.isTeamBed(b.getLocation()))
                        || blastProtection.isProtected(a, e.getLocation(), b, 0.3));
                return;
            }
            e.blockList().clear();
        }
    }

    @EventHandler
    public void onBlockExplode(@NotNull BlockExplodeEvent e) {
        if (e.isCancelled()) return;
        if (e.blockList().isEmpty()) return;

        IArena a = Arena.getArenaByIdentifier(e.blockList().get(0).getWorld().getName());
        if (a != null) {
            if (a.getNextEvent() != NextEvent.GAME_END) {
                e.blockList().removeIf((b) -> blastProtection.isProtected(a, e.getBlock().getLocation(), b, 0.3));
            }
        }
    }

    @EventHandler
    public void onPaintingRemove(HangingBreakByEntityEvent e) {
        IArena a = Arena.getArenaByIdentifier(e.getEntity().getWorld().getName());
        if (a == null) {
            if (BedWars.getServerType() == ServerType.SHARED) return;
            if (!BedWars.getLobbyWorld().equals(e.getEntity().getWorld().getName())) return;
        }
        if (e.getEntity().getType() == EntityType.PAINTING || e.getEntity().getType() == EntityType.ITEM_FRAME) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockCanBuildEvent(BlockCanBuildEvent e) {
        if (e.isBuildable()) return;
        IArena a = Arena.getArenaByIdentifier(e.getBlock().getWorld().getName());
        if (a != null) {
            boolean bed = false;
            for (ITeam t : a.getTeams()) {
                for (int x = e.getBlock().getX() - 1; x < e.getBlock().getX() + 1; x++) {
                    for (int z = e.getBlock().getZ() - 1; z < e.getBlock().getZ() + 1; z++) {

                        //Check bed block
                        if (t.getBed().getBlockX() == x && t.getBed().getBlockY() == e.getBlock().getY() && t.getBed().getBlockZ() == z) {
                            e.setBuildable(false);
                            bed = true;
                            break;
                        }
                    }
                }
                //Check bed hologram
                if (t.getBed().getBlockX() == e.getBlock().getX() && t.getBed().getBlockY() + 1 == e.getBlock().getY() && t.getBed().getBlockZ() == e.getBlock().getZ()) {
                    if (!bed) {
                        e.setBuildable(true);
                        break;
                    }
                }
            }
            //if (bed) return;
            /*Object[] players = e.getBlock().getWorld().getNearbyEntities(e.getBlock().getLocation(), 1, 1, 1).stream().filter(ee -> ee.getType() == EntityType.PLAYER).toArray();
            for (Object o : players) {
                Player p = (Player) o;
                if (a.isSpectator(p)) {
                    if (e.getBlock().getType() == Material.AIR) e.setBuildable(true);
                    return;
                }
            }**/
        }
    }

    //prevent farm breaking farm stuff
    @EventHandler
    public void soilChangeEntity(EntityChangeBlockEvent e) {
        if (e.getTo() == Material.DIRT) {
            if (e.getBlock().getType().toString().equals("FARMLAND") || e.getBlock().getType().toString().equals("SOIL")) {
                if ((Arena.getArenaByIdentifier(e.getBlock().getWorld().getName()) != null) || (e.getBlock().getWorld().getName().equals(BedWars.getLobbyWorld())))
                    e.setCancelled(true);
            }
        }
    }

    private boolean isPlayerRestrictedInArena(@NotNull IArena a, @NotNull Player p) {
        if (a.isSpectator(p)) return true;
        if (a.getRespawnSessions().containsKey(p)) return true;
        return a.getStatus() != GameState.playing;
    }

    private boolean isAboveMaxBuildY(@NotNull IArena a, @NotNull Location location) {
        return location.getBlockY() >= a.getConfig().getInt(ConfigPath.ARENA_CONFIGURATION_MAX_BUILD_Y);
    }

    private boolean isShopUpgradeOrGeneratorProtected(@NotNull IArena a, @NotNull Location location) {
        int shopRadius = Math.max(1, a.getConfig().getInt(ConfigPath.ARENA_SHOP_PROTECTION));
        int upgradeRadius = Math.max(1, a.getConfig().getInt(ConfigPath.ARENA_UPGRADES_PROTECTION));
        int generatorRadius = a.getConfig().getInt(ConfigPath.ARENA_GENERATOR_PROTECTION);
        for (ITeam team : a.getTeams()) {
            if (isWithinNpcProtection(location, team.getShop(), shopRadius)) return true;
            if (isWithinNpcProtection(location, team.getTeamUpgrades(), upgradeRadius)) return true;
            for (IGenerator generator : team.getGenerators()) {
                if (ConfigManager.isSameWorldWithin(location, generator.getLocation(), generatorRadius)) return true;
            }
        }
        for (IGenerator generator : a.getOreGenerators()) {
            if (ConfigManager.isSameWorldWithin(location, generator.getLocation(), generatorRadius)) return true;
        }
        return false;
    }

    /**
     * Player-created blocks take precedence over map protection. A protected
     * region describes original arena terrain; it must never turn a block that
     * was successfully created during this game into a permanent block.
     */
    static boolean isProtectedMapBlock(boolean placedBlock, boolean protectedRegion, boolean allowMapBreak) {
        return !placedBlock && (protectedRegion || !allowMapBreak);
    }

    /**
     * Fireballs may destroy player-built blocks, but never the original arena
     * structure. The generic blast ray sampler is intentionally retained for
     * placed blocks (glass and other blast barriers still apply); this direct
     * rule closes edge cases where a ray starts and ends inside one map block.
     */
    static boolean shouldProtectExplosionBlock(boolean fireball, boolean placedBlock, boolean teamBed) {
        return fireball && (teamBed || !placedBlock);
    }

    public static void consumeTowerItem(@NotNull PlayerInventory inventory, @Nullable EquipmentSlot hand) {
        boolean offHand = hand == EquipmentSlot.OFF_HAND;
        ItemStack tower = offHand ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
        if (tower == null || tower.getAmount() <= 0) return;

        ItemStack remaining = null;
        if (tower.getAmount() > 1) {
            tower.setAmount(tower.getAmount() - 1);
            remaining = tower;
        }
        if (offHand) inventory.setItemInOffHand(remaining);
        else inventory.setItemInMainHand(remaining);
    }

    private boolean hasProtectedPlacedBlock(@NotNull IArena arena, @NotNull BlockPlaceEvent event) {
        if (isShopUpgradeOrGeneratorProtected(arena, event.getBlockPlaced().getLocation())) return true;
        if (event instanceof BlockMultiPlaceEvent multiPlaceEvent) {
            for (BlockState replacedState : multiPlaceEvent.getReplacedBlockStates()) {
                if (isShopUpgradeOrGeneratorProtected(arena, replacedState.getLocation())) return true;
            }
        }
        return false;
    }

    /**
     * Compare integer block coordinates so an NPC stored at x.5/z.5 protects
     * every direction symmetrically. Villagers occupy their feet and head
     * blocks, so the vertical range includes one block around both.
     */
    static boolean isWithinNpcProtection(@NotNull Location blockLocation, Location npcLocation, int radius) {
        if (npcLocation == null || radius < 0 || blockLocation.getWorld() == null
                || !blockLocation.getWorld().equals(npcLocation.getWorld())) {
            return false;
        }

        int npcY = npcLocation.getBlockY();
        int blockY = blockLocation.getBlockY();
        return Math.abs(blockLocation.getBlockX() - npcLocation.getBlockX()) <= radius
                && Math.abs(blockLocation.getBlockZ() - npcLocation.getBlockZ()) <= radius
                && blockY >= npcY - radius
                && blockY <= npcY + 1 + radius;
    }

    public static boolean isBuildSession(Player p) {
        return BUILD_SESSIONS.contains(p.getUniqueId());
    }

    public static void addBuildSession(Player p) {
        BUILD_SESSIONS.add(p.getUniqueId());
    }

    public static void removeBuildSession(Player p) {
        BUILD_SESSIONS.remove(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBuildSession(event.getPlayer());
        lastShearsBreakAt.remove(event.getPlayer().getUniqueId());
    }

    private static boolean isFire(Material material) {
        return material == Material.FIRE || material == Material.SOUL_FIRE;
    }

    private static boolean isWool(@NotNull Material material) {
        String name = material.name();
        return "WOOL".equals(name) || name.endsWith("_WOOL");
    }

    static boolean isShearsBreakReady(Long lastBreakAt, long now) {
        return lastBreakAt == null || now - lastBreakAt >= SHEARS_BREAK_COOLDOWN_MILLIS;
    }
}
