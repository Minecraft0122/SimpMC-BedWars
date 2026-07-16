package com.andrei1058.bedwars.arena.upgrades;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.BedWars.plugin;

public class HealPoolTask extends BukkitRunnable {

    /**
     * A fixed sample budget keeps the effect cost independent from island volume.
     * The old implementation scanned every block in a (2r + 1)^3 cube.
     */
    private static final int PARTICLE_SAMPLES_PER_RUN = 24;
    private static final Map<ITeam, HealPoolTask> TASKS = new IdentityHashMap<>();

    private final ITeam team;
    private final IArena arena;
    private final int maxX;
    private final int minX;
    private final int maxY;
    private final int minY;
    private final int maxZ;
    private final int minZ;

    public HealPoolTask(ITeam team) {
        if (team == null || team.getSpawn() == null || team.getArena() == null || team.getArena().getWorld() == null) {
            throw new IllegalArgumentException("Heal pool requires a team with an arena and spawn");
        }

        this.team = team;
        this.arena = team.getArena();

        int radius = arena.getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS);
        Location teamSpawn = team.getSpawn();
        this.maxX = teamSpawn.getBlockX() + radius;
        this.minX = teamSpawn.getBlockX() - radius;
        this.maxY = Math.min(arena.getWorld().getMaxHeight() - 1, teamSpawn.getBlockY() + radius);
        this.minY = Math.max(arena.getWorld().getMinHeight(), teamSpawn.getBlockY() - radius);
        this.maxZ = teamSpawn.getBlockZ() + radius;
        this.minZ = teamSpawn.getBlockZ() - radius;

        this.runTaskTimer(plugin, 0L, 80L);
        TASKS.put(team, this);
    }

    @Override
    public void run() {
        if (team.getSpawn() == null || team.getArena() != arena || arena.getWorld() == null) {
            stop();
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Player> viewers = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_SEEN_TEAM_ONLY)
                ? new ArrayList<>(team.getMembers())
                : new ArrayList<>(arena.getPlayers());

        if (viewers.isEmpty()) {
            return;
        }

        for (int sample = 0; sample < PARTICLE_SAMPLES_PER_RUN; sample++) {
            Location location = new Location(
                    arena.getWorld(),
                    random.nextInt(minX, maxX + 1) + 0.5,
                    random.nextInt(minY, maxY + 1) + 0.5,
                    random.nextInt(minZ, maxZ + 1) + 0.5
            );

            if (!location.getBlock().isEmpty()) {
                continue;
            }

            for (Player viewer : viewers) {
                if (viewer.isOnline()) {
                    BedWars.nms.playVillagerEffect(viewer, location);
                }
            }
        }
    }

    public static boolean exists(IArena arena, ITeam team) {
        HealPoolTask task = TASKS.get(team);
        return task != null && task.arena == arena;
    }

    public static void removeForArena(IArena arena) {
        if (arena == null) {
            return;
        }
        stopMatching(task -> task.arena == arena);
    }

    public static void removeForArena(String worldName) {
        if (worldName == null) {
            return;
        }
        stopMatching(task -> worldName.equals(task.arena.getWorldName()));
    }

    public static void removeForTeam(ITeam team) {
        HealPoolTask task = TASKS.get(team);
        if (task != null) {
            task.stop();
        }
    }

    private static void stopMatching(java.util.function.Predicate<HealPoolTask> predicate) {
        for (HealPoolTask task : new ArrayList<>(TASKS.values())) {
            if (predicate.test(task)) {
                task.stop();
            }
        }
    }

    private void stop() {
        TASKS.remove(team, this);
        cancel();
    }

    public ITeam getBwt() {
        return team;
    }

    public IArena getArena() {
        return arena;
    }
}
