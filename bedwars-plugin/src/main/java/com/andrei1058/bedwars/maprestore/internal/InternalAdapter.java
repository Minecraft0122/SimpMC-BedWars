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

package com.andrei1058.bedwars.maprestore.internal;

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.server.ISetupSession;
import com.andrei1058.bedwars.api.server.RestoreAdapter;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.api.util.ZipFileUtil;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.VoidChunkGenerator;
import com.andrei1058.bedwars.maprestore.internal.files.WorldZipper;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.BedWars.plugin;

public class InternalAdapter extends RestoreAdapter {

    private static final int SETUP_CLOSE_MAX_ATTEMPTS = 240;
    private static final long SETUP_CLOSE_RETRY_TICKS = 5L;
    public static File backupFolder = new File(BedWars.plugin.getDataFolder() + "/Cache");
    private final WorldStorageLayout storageLayout;

    private enum PreparationResult {
        EXISTING,
        MISSING,
        FAILED
    }

    public InternalAdapter(Plugin plugin) {
        super(plugin);
        storageLayout = WorldStorageLayout.detect();
    }

    private static void keepSpawnLoaded(World world) {
        if (world == null) return;
        Location spawn = world.getSpawnLocation();
        world.setChunkForceLoaded(spawn.getBlockX() >> 4, spawn.getBlockZ() >> 4, true);
    }

    private File legacyWorldFolder(String worldName) {
        return storageLayout.legacyWorldFolder(worldName);
    }

    private File archiveFile(String worldName) {
        return WorldZipper.backupFile(worldName);
    }

    private void deleteDirectory(File directory) throws IOException {
        WorldStorageFiles.deleteDirectory(directory);
    }

    private PreparationResult prepareWorldFiles(String worldName, String sourceName) {
        File source = legacyWorldFolder(sourceName);
        File archive = archiveFile(sourceName);
        try {
            if (!archive.exists()) {
                if (!source.isDirectory()) {
                    return PreparationResult.MISSING;
                }
                if (!WorldStorageFiles.isLegacyWorld(source)) {
                    throw new IOException("源地图不是旧版 Bukkit 世界格式（必须包含根目录 level.dat 和 region）："
                            + source.getPath());
                }
                new WorldZipper(sourceName, true, source);
                if (!archive.isFile()) {
                    throw new IOException("无法创建缓存 " + archive.getPath());
                }
            }
            if (!WorldArchiveFormat.isLegacyWorld(archive)) {
                throw new IOException("缓存不是旧版 Bukkit 世界格式（必须包含根目录 level.dat 和 region）："
                        + archive.getPath());
            }

            File legacyTarget = legacyWorldFolder(worldName);
            deleteDirectory(legacyTarget);
            ZipFileUtil.unzipFileIntoDirectory(archive, legacyTarget);
            return PreparationResult.EXISTING;
        } catch (IOException exception) {
            getOwner().getLogger().severe("无法准备世界 " + worldName + " 的运行副本：" + exception.getMessage());
            return PreparationResult.FAILED;
        }
    }

    private WorldCreator createWorldCreator(String worldName) {
        WorldCreator creator = storageLayout.createWorldCreator(worldName);
        creator.generateStructures(false);
        creator.generator(new VoidChunkGenerator());
        return creator;
    }

    @Override
    public void onEnable(IArena a) {
        Bukkit.getScheduler().runTask(getOwner(), () -> {
            if (Bukkit.getWorld(a.getWorldName()) != null) {
                Bukkit.getScheduler().runTask(getOwner(), () -> {
                    World w = Bukkit.getWorld(a.getWorldName());
                    a.init(w);
                });
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (prepareWorldFiles(a.getWorldName(), a.getArenaName()) != PreparationResult.EXISTING) {
                    Bukkit.getScheduler().runTask(plugin, () -> failArenaLoad(a, null));
                    return;
                }
                deleteWorldTrash(a.getWorldName());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    World w = null;
                    try {
                        w = Bukkit.createWorld(createWorldCreator(a.getWorldName()));
                    } catch (RuntimeException exception) {
                        failArenaLoad(a, exception);
                        return;
                    }
                    if (w == null) {
                        throw new IllegalStateException("World could not be created: " + a.getWorldName());
                    }
                    keepSpawnLoaded(w);
                    w.setAutoSave(false);
                });
            });
        });
    }

    private void failArenaLoad(IArena arena, RuntimeException exception) {
        if (exception == null) {
            getOwner().getLogger().severe("无法准备竞技场世界：" + arena.getWorldName());
        } else {
            getOwner().getLogger().log(java.util.logging.Level.SEVERE,
                    "无法加载竞技场世界 " + arena.getWorldName(), exception);
        }
        Arena.clearRestoring(arena.getArenaName());
        Arena.removeFromEnableQueue(arena);
    }

    @Override
    public void onRestart(IArena a) {
        Arena.markRestoring(a.getArenaName());
        Bukkit.getScheduler().runTask(getOwner(), () -> {
            if (BedWars.getServerType() == ServerType.BUNGEE) {
                if (Arena.getGamesBeforeRestart() == 0) {
                    if (Arena.getArenas().isEmpty()) {
                        plugin.getLogger().info("Dispatching command: " + config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_RESTART_CMD));
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_RESTART_CMD));
                    }
                } else {
                    if (Arena.getGamesBeforeRestart() != -1) {
                        Arena.setGamesBeforeRestart(Arena.getGamesBeforeRestart() - 1);
                    }
                    Bukkit.unloadWorld(a.getWorldName(), false);
                    if (Arena.canAutoScale(a.getArenaName())) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> new Arena(a.getArenaName(), null), 80L);
                    }
                }
            } else {
                Bukkit.unloadWorld(a.getWorldName(), false);
                Bukkit.getScheduler().runTaskLater(plugin, () -> new Arena(a.getArenaName(), null), 80L);
            }
            if (!a.getWorldName().equals(a.getArenaName())) {
                deleteWorld(a.getWorldName());
            }
        });
    }

    @Override
    public void onDisable(IArena a) {
        if(BedWars.isShuttingDown()) {
            Bukkit.unloadWorld(a.getWorldName(), false);
            return;
        }
        Bukkit.getScheduler().runTask(getOwner(), () -> Bukkit.unloadWorld(a.getWorldName(), false));
    }

    @Override
    public void onSetupSessionStart(ISetupSession s) {
        if (!storageLayout.supportsWorldName(s.getWorldName())) {
            AdventureText.send(s.getPlayer(), ChatColor.RED
                    + "世界名无效；不能包含目录分隔符或路径跳转。");
            s.close();
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(getOwner(), () -> {
            PreparationResult preparation = prepareWorldFiles(s.getWorldName(), s.getWorldName());
            if (preparation == PreparationResult.FAILED) {
                Bukkit.getScheduler().runTask(getOwner(), s::close);
                return;
            }
            boolean existing = preparation == PreparationResult.EXISTING;
            if (preparation == PreparationResult.MISSING) deleteWorldFiles(s.getWorldName(), false);
            Bukkit.getScheduler().runTask(getOwner(), () -> {
                try {
                    if (existing) {
                        AdventureText.send(s.getPlayer(), ChatColor.GREEN + "正在从旧版 Bukkit 世界目录加载 " + s.getWorldName() + "。");
                        deleteWorldTrash(s.getWorldName());
                        World w = Bukkit.createWorld(createWorldCreator(s.getWorldName()));
                        keepSpawnLoaded(w);
                    } else {
                        try {
                            AdventureText.send(s.getPlayer(), ChatColor.GREEN + "正在创建新的虚空地图：" + s.getWorldName());
                            World w = Bukkit.createWorld(createWorldCreator(s.getWorldName()));
                            keepSpawnLoaded(w);
                            Bukkit.getScheduler().runTaskLater(plugin, s::teleportPlayer, 20L);
                        } catch (Exception ex){
                            getOwner().getLogger().log(java.util.logging.Level.SEVERE,
                                    "无法创建设置会话世界 " + s.getWorldName(), ex);
                            s.close();
                        }
                        return;
                    }
                } catch (Exception ex) {
                    getOwner().getLogger().log(java.util.logging.Level.SEVERE,
                            "无法初始化设置会话世界 " + s.getWorldName(), ex);
                    s.close();
                    return;
                }
                Bukkit.getScheduler().runTaskLater(plugin, s::teleportPlayer, 20L);
            });
        });
    }

    @Override
    public void onSetupSessionClose(ISetupSession s) {
        Bukkit.getScheduler().runTask(getOwner(), () -> closeSetupWorld(s, 0));
    }

    private void closeSetupWorld(ISetupSession session, int attempt) {
        World world = Bukkit.getWorld(session.getWorldName());
        if (world != null && !world.getPlayers().isEmpty()) {
            retrySetupWorldClose(session, attempt,
                    "仍有玩家留在世界中（" + world.getPlayers().size() + " 人）");
            return;
        }

        if (world != null) {
            world.save();
            if (!Bukkit.unloadWorld(world, false)) {
                retrySetupWorldClose(session, attempt, "Bukkit 拒绝卸载世界");
                return;
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                new WorldZipper(session.getWorldName(), true, legacyWorldFolder(session.getWorldName())));
    }

    private void retrySetupWorldClose(ISetupSession session, int attempt, String reason) {
        if (attempt >= SETUP_CLOSE_MAX_ATTEMPTS) {
            getOwner().getLogger().severe("无法安全保存设置地图 " + session.getWorldName() + "：" + reason
                    + "。为避免损坏区域文件，本次不会压缩仍加载的世界。");
            return;
        }
        Bukkit.getScheduler().runTaskLater(getOwner(),
                () -> closeSetupWorld(session, attempt + 1), SETUP_CLOSE_RETRY_TICKS);
    }

    @Override
    public boolean isWorld(String name) {
        if (!WorldNameValidator.isSafe(name) || !storageLayout.supportsWorldName(name)) return false;
        if (WorldStorageFiles.isLegacyWorld(legacyWorldFolder(name))) return true;
        return archiveIsLegacyWorld(archiveFile(name));
    }

    @Override
    public void deleteWorld(String name) {
        if (!WorldNameValidator.isSafe(name) || !storageLayout.supportsWorldName(name)) {
            getOwner().getLogger().warning("Refused to delete unsafe world path: " + name);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(getOwner(), () -> {
            deleteWorldFiles(name, true);
        });
    }

    private void deleteWorldFiles(String name, boolean deleteArchive) {
        try {
            WorldStorageFiles.deleteWorldFiles(storageLayout, archiveFile(name), name, deleteArchive);
        } catch (IOException exception) {
            getOwner().getLogger().log(java.util.logging.Level.SEVERE,
                    "无法完整删除世界文件 " + name, exception);
        }
    }

    @Override
    public void cloneArena(String name1, String name2) {
        if (!WorldNameValidator.isSafe(name1) || !WorldNameValidator.isSafe(name2)
                || !storageLayout.supportsWorldName(name1) || !storageLayout.supportsWorldName(name2)) {
            getOwner().getLogger().warning("Refused to clone an unsafe world path.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File target = legacyWorldFolder(name2);
                deleteDirectory(target);
                File sourceArchive = archiveFile(name1);
                if (sourceArchive.isFile()) {
                    if (!WorldArchiveFormat.isLegacyWorld(sourceArchive)) {
                        throw new IOException("源缓存不是旧版 Bukkit 世界格式：" + sourceArchive.getPath());
                    }
                    ZipFileUtil.unzipFileIntoDirectory(sourceArchive, target);
                } else {
                    File source = legacyWorldFolder(name1);
                    if (!WorldStorageFiles.isLegacyWorld(source)) {
                        throw new IOException("源地图不是旧版 Bukkit 世界格式：" + source.getPath());
                    }
                    FileUtils.copyDirectory(source, target);
                }
                if (!WorldStorageFiles.isLegacyWorld(target)) {
                    throw new IOException("克隆结果不是旧版 Bukkit 世界格式：" + target.getPath());
                }
                deleteWorldIdentity(target);
                new WorldZipper(name2, true, target);
            } catch (IOException exception) {
                getOwner().getLogger().log(java.util.logging.Level.SEVERE,
                        "无法按旧版 Bukkit 格式克隆世界 " + name1 + " -> " + name2, exception);
            }
        });
    }

    @Override
    public List<String> getWorldsList() {
        Set<String> worlds = new LinkedHashSet<>();
        File dir = storageLayout.getWorldContainer();
        if (dir.exists()) {
            File[] fls = dir.listFiles();
            for (File fl : fls == null ? new File[0] : fls) {
                if (fl.isDirectory()) {
                    if (WorldStorageFiles.isLegacyWorld(fl) && !fl.getName().startsWith("bw_temp")
                            && storageLayout.supportsWorldName(fl.getName())) {
                        worlds.add(fl.getName());
                    }
                }
            }
        }
        File[] archives = backupFolder.listFiles((folder, name) -> name.endsWith(".zip"));
        if (archives != null) {
            for (File archive : archives) {
                String name = archive.getName().substring(0, archive.getName().length() - 4);
                if (!name.startsWith("bw_temp") && storageLayout.supportsWorldName(name)
                        && archiveIsLegacyWorld(archive)) {
                    worlds.add(name);
                }
            }
        }
        return new ArrayList<>(worlds);
    }

    @Override
    public void convertWorlds() {
        File dir = new File(plugin.getDataFolder(), "/Arenas");
        if (dir.exists()) {
            List<File> files = new ArrayList<>();
            File[] fls = dir.listFiles();
            for (File fl : Objects.requireNonNull(fls)) {
                if (fl.isFile()) {
                    if (fl.getName().contains(".yml")) {
                        files.add(fl);
                    }
                }
            }

            // lowerCase arena names - new 1.14 standard
            File folder, newName;

            List<File> toRemove = new ArrayList<>(), toAdd = new ArrayList<>();
            for (File file : files) {
                if (!file.getName().equals(file.getName().toLowerCase())) {
                    newName = new File(dir.getPath() + "/" + file.getName().toLowerCase());
                    if (!file.renameTo(newName)) {
                        toRemove.add(file);
                        BedWars.plugin.getLogger().severe("Could not rename " + file.getName() + " to " + file.getName().toLowerCase() + "! Please do it manually!");
                    } else {
                        toAdd.add(newName);
                        toRemove.add(file);
                    }
                    folder = legacyWorldFolder(file.getName().replace(".yml", ""));
                    if (folder.exists()) {
                        if (!folder.getName().equals(folder.getName().toLowerCase())) {
                            if (!folder.renameTo(legacyWorldFolder(folder.getName().toLowerCase()))) {
                                BedWars.plugin.getLogger().severe("Could not rename " + folder.getName() + " folder to " + folder.getName().toLowerCase() + "! Please do it manually!");
                                toRemove.add(file);
                                return;
                            }
                        }
                    }
                }
            }

            for (File f : toRemove) {
                files.remove(f);
            }

            files.addAll(toAdd);
        }
        Bukkit.getScheduler().runTaskAsynchronously(getOwner(), () -> {
            File[] files = storageLayout.getWorldContainer().listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f != null && f.isDirectory() && f.getName().startsWith("bw_temp_")) {
                    deleteWorldFiles(f.getName(), true);
                }
            }
        });
    }

    @Override
    public String getDisplayName() {
        return "Internal Restore Adapter";
    }

    private void deleteWorldTrash(String world) {
        File legacy = legacyWorldFolder(world);
        deleteWorldIdentity(legacy);
        for (File f : new File[]{new File(legacy, "level.dat"),
                new File(legacy, "level.dat_mcr"),
                new File(legacy, "level.dat_old")}) {
            if (f.exists()) {
                if (!f.delete()) {
                    getOwner().getLogger().warning("Could not delete: " + f.getPath());
                    getOwner().getLogger().warning("This may cause issues!");
                }
            }
        }
    }

    private void deleteWorldIdentity(File worldFolder) {
        try {
            WorldStorageFiles.deleteWorldIdentity(worldFolder);
        } catch (IOException exception) {
            getOwner().getLogger().warning("无法删除世界身份文件 " + worldFolder.getPath()
                    + "：" + exception.getMessage());
        }
    }

    private boolean archiveIsLegacyWorld(File archive) {
        if (!archive.isFile()) return false;
        try {
            return WorldArchiveFormat.isLegacyWorld(archive);
        } catch (IOException exception) {
            getOwner().getLogger().warning("无法读取世界缓存 " + archive.getName() + "：" + exception.getMessage());
            return false;
        }
    }

}
