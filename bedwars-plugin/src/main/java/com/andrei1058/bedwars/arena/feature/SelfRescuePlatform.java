package com.andrei1058.bedwars.arena.feature;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent;
import com.andrei1058.bedwars.api.events.server.ArenaRestartEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SelfRescuePlatform implements Listener, Runnable {
    public static final String ITEM_DATA = "SELF_RESCUE_PLATFORM";
    private static final long LIFETIME_TICKS = 16L * 20L;
    private static final int[][] PATTERN = {
            {0, 1, 0, 1, 0},
            {1, 1, 1, 1, 1},
            {0, 1, 1, 1, 0},
            {1, 1, 1, 1, 1},
            {0, 1, 0, 1, 0}
    };
    private static SelfRescuePlatform instance;

    private final Set<UUID> automaticDeployments = new HashSet<>();
    private final Map<String, TemporaryBlock> temporaryBlocks = new HashMap<>();
    private final Map<Long, List<String>> platforms = new HashMap<>();
    private final Map<Long, BukkitTask> platformTasks = new HashMap<>();
    private final Map<UUID, RescueArea> rescueAreas = new HashMap<>();
    private long nextPlatformId;

    public SelfRescuePlatform() {
        instance = this;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || event.getFrom().getBlockY() == to.getBlockY()) return;
        tryAutomaticDeploy(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        IArena arena = activeArena(player);
        if (arena == null || !isItem(event.getItem())) return;
        event.setCancelled(true);

        Location center = player.getLocation();
        int targetY = center.getBlockY() - 3;
        if (!isValidY(center.getWorld(), targetY) || !canDeploy(center, targetY) || !consume(player)) return;

        boolean blocksAutomaticDeployment = center.getY() <= automaticTriggerY();
        if (blocksAutomaticDeployment) automaticDeployments.add(player.getUniqueId());
        deploy(arena, player, center, targetY, blocksAutomaticDeployment);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectFromBreaking(BlockBreakEvent event) {
        if (temporaryBlocks.containsKey(key(event.getBlock()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectFromExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> temporaryBlocks.containsKey(key(block)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectFromBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> temporaryBlocks.containsKey(key(block)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectFromPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> temporaryBlocks.containsKey(key(block)))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectFromPiston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> temporaryBlocks.containsKey(key(block)))) event.setCancelled(true);
    }

    @EventHandler
    public void onGameEnd(GameEndEvent event) {
        removeArena(event.getArena(), true);
    }

    @EventHandler
    public void onStateChange(GameStateChangeEvent event) {
        if (event.getNewState() != GameState.playing) removeArena(event.getArena(), true);
    }

    @EventHandler
    public void onArenaDisable(ArenaDisableEvent event) {
        discardWorld(event.getWorldName(), true);
    }

    @EventHandler
    public void onArenaRestart(ArenaRestartEvent event) {
        discardWorld(event.getWorldName(), true);
    }

    @EventHandler
    public void onLeave(PlayerLeaveArenaEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        automaticDeployments.remove(uuid);
        rescueAreas.remove(uuid);
    }

    @Override
    public void run() {
        for (IArena arena : new ArrayList<>(Arena.getArenas())) {
            if (arena == null || arena.getStatus() != GameState.playing) continue;
            for (Player player : new ArrayList<>(arena.getPlayers())) {
                tryAutomaticDeploy(player, player.getLocation());
            }
        }
    }

    public static boolean isItem(ItemStack item) {
        return item != null && item.getType() == Material.BLAZE_ROD && BedWars.nms != null
                && BedWars.nms.isCustomBedWarsItem(item) && ITEM_DATA.equals(BedWars.nms.getCustomData(item));
    }

    public static boolean preventsVoidKill(Player player, IArena arena, Location location) {
        return instance != null && instance.preventVoidKill(player, arena, location);
    }

    public static void installLanguageFallbacks() {
        String namePath = Messages.SHOP_CONTENT_TIER_ITEM_NAME
                .replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY)
                .replace("%content%", "self-rescue-platform");
        for (Language language : Language.getLanguages()) {
            if (language.exists(namePath)) continue;
            Language.addContentMessages(language.getYml(), "self-rescue-platform", ConfigPath.SHOP_PATH_CATEGORY_UTILITY,
                    "{color}Self-Rescue Platform", Arrays.asList("&7Cost: {cost} {currency}", "",
                            "&7Deploys a temporary slime platform", "&7to save you from the void.", "",
                            "{quick_buy}", "{buy_status}"));
            language.getYml().options().copyDefaults(true);
            language.save();
        }
    }

    public static void localizeItem(Player player, ItemStack item) {
        if (!isItem(item) || item.getItemMeta() == null) return;
        String path = Messages.SHOP_CONTENT_TIER_ITEM_NAME
                .replace("%category%", ConfigPath.SHOP_PATH_CATEGORY_UTILITY)
                .replace("%content%", "self-rescue-platform");
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                Language.getMsg(player, path).replace("{color}", "&b").replace("{tier}", "")));
        item.setItemMeta(meta);
    }

    private boolean preventVoidKill(Player player, IArena arena, Location location) {
        if (activeArena(player) != arena) return false;
        tryAutomaticDeploy(player, location);

        RescueArea area = rescueAreas.get(player.getUniqueId());
        if (isInsideActiveArea(area, arena, location)) return true;

        // Custom arenas may kill players above the fixed rescue trigger. Let a player who can
        // still be rescued reach that trigger; the normal void kill resumes if deployment fails.
        return location.getY() > automaticTriggerY() && hasItem(player);
    }

    private void tryAutomaticDeploy(Player player, Location location) {
        IArena arena = activeArena(player);
        if (arena == null) {
            automaticDeployments.remove(player.getUniqueId());
            rescueAreas.remove(player.getUniqueId());
            return;
        }
        double triggerY = automaticTriggerY();
        UUID uuid = player.getUniqueId();
        RescueArea area = rescueAreas.get(uuid);
        if (location.getY() > triggerY) {
            if (automaticDeployments.contains(uuid) && isInsideActiveArea(area, arena, location)) return;
            automaticDeployments.remove(uuid);
            return;
        }
        if (isInsideActiveArea(area, arena, location)) return;
        automaticDeployments.remove(uuid);

        int targetY = automaticPlatformY();
        if (location.getY() < targetY - 3) return;
        if (!canDeploy(location, targetY) || !consume(player)) return;
        automaticDeployments.add(uuid);
        deploy(arena, player, location, targetY, true);
    }

    private boolean isInsideActiveArea(RescueArea area, IArena arena, Location location) {
        return area != null && area.arena == arena && platforms.containsKey(area.platformId)
                && location.getY() >= area.y - 3 && area.contains(location);
    }

    private IArena activeArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing || !arena.isPlayer(player)
                || arena.isSpectator(player) || arena.isReSpawning(player) || player.isDead()
                || arena.getWorld() == null || !arena.getWorld().equals(player.getWorld())) return null;
        return arena;
    }

    private boolean consume(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!isItem(item)) continue;
            if (item.getAmount() <= 1) inventory.setItem(slot, null);
            else item.setAmount(item.getAmount() - 1);
            player.updateInventory();
            return true;
        }
        return false;
    }

    private boolean hasItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isItem(item)) return true;
        }
        return false;
    }

    private boolean canDeploy(Location center, int y) {
        if (!isValidY(center.getWorld(), y)) return false;
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        for (int row = 0; row < PATTERN.length; row++) {
            for (int column = 0; column < PATTERN[row].length; column++) {
                if (PATTERN[row][column] == 0) continue;
                Block block = center.getWorld().getBlockAt(centerX + column - 2, y, centerZ + row - 2);
                if (isAir(block)) continue;
                if (block.getType() == Material.SLIME_BLOCK && temporaryBlocks.containsKey(key(block))) continue;
                return false;
            }
        }
        return true;
    }

    private void deploy(IArena arena, Player player, Location center, int y, boolean blocksAutomaticDeployment) {
        long platformId = ++nextPlatformId;
        List<String> placed = new ArrayList<>();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        for (int row = 0; row < PATTERN.length; row++) {
            for (int column = 0; column < PATTERN[row].length; column++) {
                if (PATTERN[row][column] == 0) continue;
                Block block = center.getWorld().getBlockAt(centerX + column - 2, y, centerZ + row - 2);
                String key = key(block);
                TemporaryBlock existing = temporaryBlocks.get(key);
                if (existing != null) {
                    existing.platformIds.add(platformId);
                    if (isAir(block)) block.setType(Material.SLIME_BLOCK, false);
                    placed.add(key);
                    continue;
                }
                if (!isAir(block)) continue;

                boolean addedToPlaced = !arena.isBlockPlaced(block);
                block.setType(Material.SLIME_BLOCK, false);
                if (addedToPlaced) arena.addPlacedBlock(block);
                TemporaryBlock temporaryBlock = new TemporaryBlock(arena, block, addedToPlaced);
                temporaryBlock.platformIds.add(platformId);
                temporaryBlocks.put(key, temporaryBlock);
                placed.add(key);
            }
        }
        if (placed.isEmpty()) return;

        platforms.put(platformId, placed);
        rescueAreas.put(player.getUniqueId(), new RescueArea(platformId, arena, centerX, centerZ, y,
                blocksAutomaticDeployment));
        platformTasks.put(platformId, Bukkit.getScheduler().runTaskLater(BedWars.plugin,
                () -> removePlatform(platformId), LIFETIME_TICKS));
    }

    private void removePlatform(long platformId) {
        List<String> placed = platforms.remove(platformId);
        if (placed == null) return;
        platformTasks.remove(platformId);
        for (String key : placed) {
            TemporaryBlock temporaryBlock = temporaryBlocks.get(key);
            if (temporaryBlock == null) continue;
            temporaryBlock.platformIds.remove(platformId);
            if (temporaryBlock.platformIds.isEmpty()) removeBlock(key, temporaryBlock, true);
        }
        rescueAreas.entrySet().removeIf(entry -> {
            if (entry.getValue().platformId != platformId) return false;
            if (entry.getValue().blocksAutomaticDeployment) automaticDeployments.remove(entry.getKey());
            return true;
        });
    }

    private void removeArena(IArena arena, boolean changeBlocks) {
        Set<Long> platformIds = new HashSet<>();
        for (Map.Entry<String, TemporaryBlock> entry : new ArrayList<>(temporaryBlocks.entrySet())) {
            TemporaryBlock temporaryBlock = entry.getValue();
            if (temporaryBlock.arena != arena) continue;
            platformIds.addAll(temporaryBlock.platformIds);
            removeBlock(entry.getKey(), temporaryBlock, changeBlocks);
        }
        for (Long platformId : platformIds) {
            BukkitTask task = platformTasks.remove(platformId);
            if (task != null) task.cancel();
            platforms.remove(platformId);
        }
        Set<UUID> affectedPlayers = new HashSet<>();
        rescueAreas.entrySet().removeIf(entry -> {
            if (entry.getValue().arena != arena) return false;
            affectedPlayers.add(entry.getKey());
            return true;
        });
        if (Bukkit.getWorld(arena.getWorldName()) != null) {
            for (Player player : new ArrayList<>(arena.getPlayers())) affectedPlayers.add(player.getUniqueId());
        }
        automaticDeployments.removeAll(affectedPlayers);
    }

    private void discardWorld(String worldName, boolean changeBlocks) {
        Set<IArena> arenas = new HashSet<>();
        for (TemporaryBlock temporaryBlock : temporaryBlocks.values()) {
            if (temporaryBlock.worldName.equals(worldName)) arenas.add(temporaryBlock.arena);
        }
        for (IArena arena : arenas) removeArena(arena, changeBlocks);
    }

    private void removeBlock(String key, TemporaryBlock temporaryBlock, boolean changeBlock) {
        Block block = temporaryBlock.block;
        World loadedWorld = Bukkit.getWorld(temporaryBlock.worldId);
        if (loadedWorld != null) {
            if (changeBlock && block.getType() == Material.SLIME_BLOCK) block.setType(Material.AIR, false);
            if (temporaryBlock.addedToPlaced && isAir(block) && temporaryBlock.arena.getPlaced() != null) {
                temporaryBlock.arena.getPlaced().remove(new Vector(block.getX(), block.getY(), block.getZ()));
            }
        }
        temporaryBlocks.remove(key);
    }

    private boolean isAir(Block block) {
        return block.getType() == Material.AIR;
    }

    private boolean isValidY(World world, int y) {
        return world != null && y >= minimumY() && y < world.getMaxHeight();
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private int automaticTriggerY() {
        return isLegacy() ? 5 : -60;
    }

    private int automaticPlatformY() {
        return minimumY();
    }

    private int minimumY() {
        return isLegacy() ? 0 : -64;
    }

    private boolean isLegacy() {
        return "v1_8_R3".equals(BedWars.getServerVersion());
    }

    private static final class TemporaryBlock {
        private final IArena arena;
        private final Block block;
        private final UUID worldId;
        private final String worldName;
        private final boolean addedToPlaced;
        private final Set<Long> platformIds = new HashSet<>();

        private TemporaryBlock(IArena arena, Block block, boolean addedToPlaced) {
            this.arena = arena;
            this.block = block;
            this.worldId = block.getWorld().getUID();
            this.worldName = block.getWorld().getName();
            this.addedToPlaced = addedToPlaced;
        }
    }

    private static final class RescueArea {
        private final long platformId;
        private final IArena arena;
        private final int centerX;
        private final int centerZ;
        private final int y;
        private final boolean blocksAutomaticDeployment;

        private RescueArea(long platformId, IArena arena, int centerX, int centerZ, int y,
                           boolean blocksAutomaticDeployment) {
            this.platformId = platformId;
            this.arena = arena;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.y = y;
            this.blocksAutomaticDeployment = blocksAutomaticDeployment;
        }

        private boolean contains(Location location) {
            int column = location.getBlockX() - centerX + 2;
            int row = location.getBlockZ() - centerZ + 2;
            return row >= 0 && row < PATTERN.length && column >= 0 && column < PATTERN[row].length
                    && PATTERN[row][column] == 1;
        }
    }
}
