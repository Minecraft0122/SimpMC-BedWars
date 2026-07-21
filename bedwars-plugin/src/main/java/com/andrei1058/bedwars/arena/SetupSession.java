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

package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.server.SetupSessionCloseEvent;
import com.andrei1058.bedwars.api.events.server.SetupSessionStartEvent;
import com.andrei1058.bedwars.api.server.ISetupSession;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.api.server.SetupType;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.ArenaConfig;
import com.andrei1058.bedwars.maprestore.internal.WorldNameValidator;
import com.andrei1058.bedwars.support.paper.TeleportManager;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.BedWars.plugin;
import static com.andrei1058.bedwars.commands.Misc.createArmorStand;
import static com.andrei1058.bedwars.commands.Misc.removeArmorStand;

public class SetupSession implements ISetupSession {

    private static List<SetupSession> setupSessions = new ArrayList<>();

    private Player player;
    private String worldName;
    private SetupType setupType;
    private ArenaConfig cm;
    private boolean started = false;

    public SetupSession(Player player, String worldName) {
        if (!WorldNameValidator.isSafe(worldName)) {
            throw new IllegalArgumentException("Unsafe setup world name: " + worldName);
        }
        this.player = player;
        this.worldName = worldName;
        getSetupSessions().add(this);
        openGUI(player);
    }

    public void setSetupType(SetupType setupType) {
        this.setupType = setupType;
    }

    @SuppressWarnings("WeakerAccess")
    public static List<SetupSession> getSetupSessions() {
        return setupSessions;
    }

    /**
     * Gets the setup type gui inv name
     */
    public static String getInvName() {
        return "§8Choose a setup method";
    }

    /**
     * Get advanced type item slot
     */
    public static int getAdvancedSlot() {
        return 5;
    }

    /**
     * Get assisted type item slot
     */
    public static int getAssistedSlot() {
        return 3;
    }

    public SetupType getSetupType() {
        return setupType;
    }

    public static boolean usesAutomaticAssistance(SetupType setupType) {
        return setupType == SetupType.ASSISTED;
    }

    public Player getPlayer() {
        return player;
    }

    public String getWorldName() {
        return worldName;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isStarted() {
        return started;
    }

    /**
     * Start setup session, loadStructure world etc
     *
     * @return return is broken. do not use it.
     */
    public boolean startSetup() {
        getPlayer().sendMessage("§6 ▪ §7正在加载 " + getWorldName());
        cm = new ArenaConfig(BedWars.plugin, getWorldName(), plugin.getDataFolder().getPath() + "/Arenas");
        BedWars.getAPI().getRestoreAdapter().onSetupSessionStart(this);
        return true;
    }

    private static void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, getInvName());
        ItemStack assisted = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta am = assisted.getItemMeta();
        am.setDisplayName("§e§l引导式设置");
        am.setLore(Arrays.asList("", "§a简单快速地创建竞技场", "§7适合首次配置竞技场的管理员", "", "§3仅显示必要选项"));
        assisted.setItemMeta(am);
        inv.setItem(getAssistedSlot(), assisted);

        ItemStack advanced = new ItemStack(Material.REDSTONE);
        ItemMeta amm = advanced.getItemMeta();
        amm.setDisplayName("§c§l高级设置");
        amm.setLore(Arrays.asList("", "§a完整配置竞技场", "§7适合熟悉插件的管理员", "", "§3显示全部高级选项"));
        advanced.setItemMeta(amm);
        inv.setItem(getAdvancedSlot(), advanced);

        player.openInventory(inv);
    }

    /**
     * Cancel setup
     */
    public void cancel() {
        getSetupSessions().remove(this);
        if (isStarted()) {
            player.sendMessage("§6 ▪ §7已取消 " + getWorldName() + " 的设置。");
            done();
        }
    }

    /**
     * End setup session
     */
    public void done() {
        BedWars.getAPI().getRestoreAdapter().onSetupSessionClose(this);
        getSetupSessions().remove(this);
        Player setupPlayer = getPlayer();
        setupPlayer.setGameMode(BedWars.getServerType() == ServerType.MULTIARENA
                ? GameMode.ADVENTURE : GameMode.SURVIVAL);
        setupPlayer.setFlying(false);
        setupPlayer.setAllowFlight(false);
        setupPlayer.removePotionEffect(PotionEffectType.SPEED);

        if (BedWars.getServerType() != ServerType.BUNGEE) {
            Location lobby = config.getConfigLoc("lobbyLoc");
            if (lobby == null || lobby.getWorld() == null) {
                lobby = Bukkit.getWorlds().getFirst().getSpawnLocation();
            }
            TeleportManager.teleportC(setupPlayer, lobby, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    .whenComplete((success, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (error != null || !Boolean.TRUE.equals(success) || !setupPlayer.isOnline()) return;
                        if (BedWars.getServerType() == ServerType.MULTIARENA) {
                            Arena.enterLobby(setupPlayer);
                        }
                    }));
        }
        Bukkit.getPluginManager().callEvent(new SetupSessionCloseEvent(this));
    }

    /**
     * Check if a player is in setup session
     */
    public static boolean isInSetupSession(UUID player) {
        for (SetupSession ss : getSetupSessions()) {
            if (ss.getPlayer().getUniqueId().equals(player)) return true;
        }
        return false;
    }

    /**
     * Check whether a player is editing the given world in an arena setup
     * session. Matching the world as well as the player prevents a setup
     * session from bypassing protection after the player leaves its map.
     */
    public static boolean isEditingWorld(UUID player, String worldName) {
        SetupSession session = getSession(player);
        return session != null && worldNamesMatch(session.getWorldName(), worldName);
    }

    static boolean worldNamesMatch(String setupWorld, String targetWorld) {
        return setupWorld != null && targetWorld != null && setupWorld.equalsIgnoreCase(targetWorld);
    }

    /**
     * Get a player session
     */
    public static SetupSession getSession(UUID p) {
        for (SetupSession ss : getSetupSessions()) {
            if (ss.getPlayer().getUniqueId().equals(p)) return ss;
        }
        return null;
    }

    public static boolean isSetupWorld(String worldName) {
        return worldName != null && getSetupSessions().stream()
                .anyMatch(session -> worldName.equals(session.getWorldName()));
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * Get arena configuration
     */
    public ArenaConfig getConfig() {
        return cm;
    }

    @Override
    public void teleportPlayer() {
        World w = Bukkit.getWorld(getWorldName());
        if (w == null) {
            player.sendMessage(getPrefix() + ChatColor.RED + "无法加载竞技场世界。");
            return;
        }
        player.getInventory().clear();
        TeleportManager.teleport(player, w.getSpawnLocation());
        player.setGameMode(GameMode.CREATIVE);
        Bukkit.getScheduler().runTaskLater(plugin, ()->{
            player.setAllowFlight(true);
            player.setFlying(true);
        }, 5L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
        player.sendMessage("\n" + ChatColor.WHITE + "\n");

        for (int x = 0; x < 10; x++) {
            getPlayer().sendMessage(" ");
        }
        player.sendMessage(ChatColor.GREEN + "已传送到 " + ChatColor.GOLD + getWorldName() + ChatColor.GREEN + " 的出生点。");
        if (getSetupType() == SetupType.ASSISTED && getConfig().getYml().get("waiting.Loc") == null) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "你好，" + player.getDisplayName() + "！");
            player.sendMessage(ChatColor.WHITE + "请先设置等待大厅出生点。");
            player.sendMessage(ChatColor.WHITE + "玩家将在这里等待游戏开始。");
            player.spigot().sendMessage(Misc.msgHoverClick(ChatColor.BLUE + "     ▪     " + ChatColor.GOLD + "点击设置等待大厅    " + ChatColor.BLUE + " ▪", ChatColor.LIGHT_PURPLE + "点击设置等待大厅出生点", "/" + BedWars.mainCmd + " setWaitingSpawn", ClickEvent.Action.RUN_COMMAND));
            player.spigot().sendMessage(MainCommand.createTC(ChatColor.YELLOW + "或输入：" + ChatColor.GRAY + "/" + BedWars.mainCmd + " 查看命令列表。", "/" + BedWars.mainCmd, ChatColor.WHITE + "显示命令列表"));
        } else {
            Bukkit.dispatchCommand(player, BedWars.mainCmd + " cmds");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> w.getEntities().stream()
                .filter(e -> e.getType() != EntityType.PLAYER).filter(e -> e.getType() != EntityType.PAINTING)
                .filter(e -> e.getType() != EntityType.ITEM_FRAME).forEach(Entity::remove), 30L);
        w.setAutoSave(false);
        GameRules.setBoolean(w, "doMobSpawning", false);
        GameRules.setBoolean(w, "doDaylightCycle", false);
        w.setTime(6000L);
        Bukkit.getPluginManager().callEvent(new SetupSessionStartEvent(this));
        setStarted(true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (String team : getTeams()) {
                for (String gen : new String[]{"Iron", "Gold", "Emerald"}) {
                    if (getConfig().getYml().get("Team." + team + "." + gen) != null) {
                        for (String loc : getConfig().getList("Team." + team + "." + gen)) {
                            createArmorStand(ChatColor.GOLD + "已添加 " + gen + " 资源点，队伍：" + getTeamColor(team) + team, getConfig().convertStringToArenaLocation(loc), loc);
                        }
                    }
                }
                if (getConfig().getYml().get("Team." + team + ".Spawn") != null) {
                    createArmorStand(getTeamColor(team) + team + " " + ChatColor.GOLD + "出生点已设置", getConfig().getArenaLoc("Team." + team + ".Spawn"), getConfig().getString("Team." + team + ".Spawn"));
                }
                if (getConfig().getYml().get("Team." + team + ".Bed") != null) {
                    createArmorStand(getTeamColor(team) + team + " " + ChatColor.GOLD + "床位已设置", getConfig().getArenaLoc("Team." + team + ".Bed"), getConfig().getString("Team." + team + ".Bed"));
                }
                if (getConfig().getYml().get("Team." + team + ".Shop") != null) {
                    createArmorStand(getTeamColor(team) + team + " " + ChatColor.GOLD + "商店已设置", getConfig().getArenaLoc("Team." + team + ".Shop"), null);
                }
                if (getConfig().getYml().get("Team." + team + ".Upgrade") != null) {
                    createArmorStand(getTeamColor(team) + team + " " + ChatColor.GOLD + "升级商人已设置", getConfig().getArenaLoc("Team." + team + ".Upgrade"), null);
                }
                if (getConfig().getYml().get("Team." + team + "." + ConfigPath.ARENA_TEAM_KILL_DROPS_LOC) != null) {
                    createArmorStand(ChatColor.GOLD + "死亡掉落点 " + team, getConfig().getArenaLoc("Team." + team + "." + ConfigPath.ARENA_TEAM_KILL_DROPS_LOC), null);
                }
            }

            for (String type : new String[]{"Emerald", "Diamond"}) {
                if (getConfig().getYml().get("generator." + type) != null) {
                    for (String loc : getConfig().getList("generator." + type)) {
                        createArmorStand(ChatColor.GOLD + type + " SET", getConfig().convertStringToArenaLocation(loc), loc);
                    }
                }
            }
        }, 90L);
    }

    @Override
    public void close() {
        cancel();
    }

    public String getPrefix() {
        return ChatColor.GREEN + "[" + getWorldName() + ChatColor.GREEN + "] " + ChatColor.GOLD;
    }

    /**
     * Get a team color.
     *
     * @param team team name.
     * @return team color.
     */
    public ChatColor getTeamColor(String team) {
        return TeamColor.getChatColor(getConfig().getString("Team." + team + ".Color"));
    }

    /**
     * Show available teams.
     */
    public void displayAvailableTeams() {
        List<String> teams = getTeams();
        if (teams.isEmpty()) {
            getPlayer().sendMessage(getPrefix() + ChatColor.YELLOW + "当前地图还没有队伍。");
            return;
        }
        getPlayer().sendMessage(getPrefix() + "当前队伍（共 " + teams.size() + " 支）：");
        for (String team : teams) {
            getPlayer().sendMessage(getPrefix() + TeamColor.getChatColor(Objects.requireNonNull(getConfig().getYml().getString("Team." + team + ".Color"))) + team);
        }
    }

    /**
     * Get nearest team name.
     *
     * @return empty if not found.
     */
    public String getNearestTeam() {
        String foundTeam = "";
        ConfigurationSection cs = getConfig().getYml().getConfigurationSection("Team");
        if (cs == null) return foundTeam;
        double distance = 100;
        for (String team : cs.getKeys(false)) {
            if (getConfig().getYml().get("Team." + team + ".Spawn") == null) continue;
            double dis = getConfig().getArenaLoc("Team." + team + ".Spawn").distance(getPlayer().getLocation());
            if (dis <= getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS)) {
                if (dis < distance) {
                    distance = dis;
                    foundTeam = team;
                }
            }
        }
        return foundTeam;
    }

    public String dot() {
        return ChatColor.BLUE + " " + '▪' + " " + ChatColor.GRAY + "/" + BedWars.mainCmd + " ";
    }

    public List<String> getTeams() {
        if (getConfig().getYml().get("Team") == null) return new ArrayList<>();
        return new ArrayList<>(getConfig().getYml().getConfigurationSection("Team").getKeys(false));
    }

    /**
     * Detect and store the bed nearest to a team's configured spawn.
     */
    public Location autoDetectBed(String team, boolean replaceExisting) {
        String bedPath = "Team." + team + ".Bed";
        if (!replaceExisting && getConfig().getYml().isString(bedPath)) {
            Location configured = getConfig().getArenaLoc(bedPath);
            if (configured != null && BedWars.nms.isBed(configured.getBlock().getType())) {
                return configured;
            }
        }

        String spawnPath = "Team." + team + ".Spawn";
        if (!getConfig().getYml().isString(spawnPath)) {
            return null;
        }
        Location found = BedLocator.findNearestBed(getConfig().getArenaLoc(spawnPath),
                Math.max(1, getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS)));
        if (found == null) {
            return null;
        }

        if (getConfig().getYml().isString(bedPath)) {
            removeArmorStand("床位", getConfig().getArenaLoc(bedPath), null);
        }
        getConfig().getYml().set(bedPath, getConfig().stringLocationArenaFormat(found));
        getConfig().save();
        createArmorStand(getTeamColor(team) + team + " " + ChatColor.GOLD + "BED AUTO-DETECTED", found, null);
        return found;
    }

    /**
     * Ensure every configured team has a valid bed before setup is saved.
     *
     * @return team names whose bed could not be found.
     */
    public List<String> autoDetectAllBeds() {
        List<String> missing = new ArrayList<>();
        boolean changed = false;
        for (String team : getTeams()) {
            String bedPath = "Team." + team + ".Bed";
            if (getConfig().getYml().isString(bedPath)) {
                Location configured = getConfig().getArenaLoc(bedPath);
                if (configured != null && BedWars.nms.isBed(configured.getBlock().getType())) {
                    continue;
                }
            }

            String spawnPath = "Team." + team + ".Spawn";
            Location spawn = getConfig().getYml().isString(spawnPath) ? getConfig().getArenaLoc(spawnPath) : null;
            Location found = BedLocator.findNearestBed(spawn,
                    Math.max(1, getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS)));
            if (found == null) {
                missing.add(team);
                continue;
            }
            getConfig().getYml().set(bedPath, getConfig().stringLocationArenaFormat(found));
            changed = true;
        }
        if (changed) {
            getConfig().save();
        }
        return missing;
    }

    /**
     * List teams whose configured bed is missing or no longer points to a bed.
     * This method never changes configuration and is used by advanced setup.
     */
    public List<String> findTeamsWithoutValidBeds() {
        List<String> missing = new ArrayList<>();
        for (String team : getTeams()) {
            String bedPath = "Team." + team + ".Bed";
            Location configured = getConfig().getYml().isString(bedPath)
                    ? getConfig().getArenaLoc(bedPath) : null;
            if (configured == null || !BedWars.nms.isBed(configured.getBlock().getType())) {
                missing.add(team);
            }
        }
        return missing;
    }

    /**
     * Detect strict diamond/emerald structures and merge their center-air blocks
     * into global generator configuration. Assisted setup only.
     */
    public GeneratorStructureLocator.ScanResult autoDetectGlobalGenerators() {
        if (!usesAutomaticAssistance(getSetupType())) {
            return new GeneratorStructureLocator.ScanResult(List.of(), List.of());
        }
        World world = Bukkit.getWorld(getWorldName());
        if (world == null) {
            return new GeneratorStructureLocator.ScanResult(List.of(), List.of());
        }

        List<Location> teamSpawns = getTeams().stream()
                .map(team -> "Team." + team + ".Spawn")
                .filter(path -> getConfig().getYml().isString(path))
                .map(getConfig()::getArenaLoc)
                .filter(Objects::nonNull)
                .toList();
        if (teamSpawns.isEmpty()) {
            return new GeneratorStructureLocator.ScanResult(List.of(), List.of());
        }

        int margin = Math.max(16, getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS));
        int minX = teamSpawns.stream().mapToInt(Location::getBlockX).min().orElse(0) - margin;
        int maxX = teamSpawns.stream().mapToInt(Location::getBlockX).max().orElse(0) + margin;
        int minZ = teamSpawns.stream().mapToInt(Location::getBlockZ).min().orElse(0) - margin;
        int maxZ = teamSpawns.stream().mapToInt(Location::getBlockZ).max().orElse(0) + margin;
        int maxBaseY = getConfig().getInt(ConfigPath.ARENA_CONFIGURATION_MAX_BUILD_Y);
        GeneratorStructureLocator.ScanResult result = GeneratorStructureLocator.findAll(
                world, minX, maxX, Math.max(0, world.getMinHeight()), maxBaseY, minZ, maxZ);
        boolean changed = mergeGeneratorLocations("Diamond", result.diamondGenerators());
        changed |= mergeGeneratorLocations("Emerald", result.emeraldGenerators());
        if (changed) getConfig().save();
        return result;
    }

    private boolean mergeGeneratorLocations(String type, List<Location> detected) {
        String path = "generator." + type;
        List<Location> merged = new ArrayList<>(getConfig().getArenaLocations(path));
        boolean changed = false;
        for (Location candidate : detected) {
            boolean duplicate = merged.stream().anyMatch(existing -> getConfig().compareArenaLoc(existing, candidate));
            if (duplicate) continue;
            merged.add(candidate);
            changed = true;
        }
        if (changed) {
            getConfig().getYml().set(path, merged.stream()
                    .map(getConfig()::stringLocationArenaFormat)
                    .toList());
        }
        return changed;
    }
}
