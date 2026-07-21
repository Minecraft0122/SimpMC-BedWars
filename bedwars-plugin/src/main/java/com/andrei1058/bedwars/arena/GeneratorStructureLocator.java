package com.andrei1058.bedwars.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Strict detector for assisted setup's 3x3x3 global-generator structures.
 * The base is either a solid resource layer or a single resource block at the center.
 */
public final class GeneratorStructureLocator {

    private GeneratorStructureLocator() {
    }

    public static @NotNull ScanResult findAll(@NotNull World world,
                                               int minX, int maxX, int minBaseY, int maxBaseY,
                                               int minZ, int maxZ) {
        int safeMinY = Math.max(world.getMinHeight(), minBaseY);
        int safeMaxY = Math.min(world.getMaxHeight() - 3, maxBaseY);
        List<Location> diamonds = new ArrayList<>();
        List<Location> emeralds = new ArrayList<>();

        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int y = safeMinY; y <= safeMaxY; y++) {
                for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                    Block baseCenter = world.getBlockAt(x, y, z);
                    Material resource = baseCenter.getType();
                    if (resource != Material.DIAMOND_BLOCK && resource != Material.EMERALD_BLOCK) continue;
                    if (!matchesStructure(resource,
                            (offsetX, offsetY, offsetZ) -> baseCenter.getRelative(offsetX, offsetY, offsetZ).getType())) {
                        continue;
                    }

                    Location generator = new Location(world, x + 0.5, y + 1.0, z + 0.5, 0.0F, 0.0F);
                    if (resource == Material.DIAMOND_BLOCK) {
                        diamonds.add(generator);
                    } else {
                        emeralds.add(generator);
                    }
                }
            }
        }
        return new ScanResult(diamonds, emeralds);
    }

    static boolean matchesStructure(Material resource, MaterialLookup blocks) {
        if (resource != Material.DIAMOND_BLOCK && resource != Material.EMERALD_BLOCK) return false;
        if (blocks.get(0, 0, 0) != resource) return false;

        int matchingBaseBlocks = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Material base = blocks.get(x, 0, z);
                if (base == resource) matchingBaseBlocks++;
                if ((x != 0 || z != 0) && base != resource && isGlobalResourceBlock(base)) {
                    return false;
                }

                Material middle = blocks.get(x, 1, z);
                if (x == 0 && z == 0) {
                    if (middle != Material.AIR) return false;
                } else if (!isStairs(middle)) {
                    return false;
                }

                if (blocks.get(x, 2, z) != Material.AIR) return false;
            }
        }
        return matchingBaseBlocks == 1 || matchingBaseBlocks == 9;
    }

    private static boolean isGlobalResourceBlock(Material material) {
        return material == Material.DIAMOND_BLOCK || material == Material.EMERALD_BLOCK;
    }

    static boolean isStairs(Material material) {
        return material != null && material.name().endsWith("_STAIRS");
    }

    @FunctionalInterface
    interface MaterialLookup {
        Material get(int x, int y, int z);
    }

    public record ScanResult(List<Location> diamondGenerators, List<Location> emeraldGenerators) {

        public ScanResult {
            diamondGenerators = List.copyOf(diamondGenerators);
            emeraldGenerators = List.copyOf(emeraldGenerators);
        }
    }
}
