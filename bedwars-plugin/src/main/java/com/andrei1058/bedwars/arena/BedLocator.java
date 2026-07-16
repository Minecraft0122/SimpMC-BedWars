package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;

public final class BedLocator {

    private BedLocator() {
    }

    /**
     * Search outwards from a team spawn and stop as soon as the nearest cube
     * shell containing a bed is reached. Normal maps therefore inspect only a
     * small fraction of the old full island-volume scan.
     */
    public static Location findNearestBed(Location origin, int maximumRadius) {
        if (origin == null || origin.getWorld() == null || maximumRadius < 0) {
            return null;
        }
        World world = origin.getWorld();
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();

        for (int radius = 0; radius <= maximumRadius; radius++) {
            Block best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    int blockY = originY + y;
                    if (blockY < world.getMinHeight() || blockY >= world.getMaxHeight()) {
                        continue;
                    }
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) != radius) {
                            continue;
                        }
                        Block candidate = world.getBlockAt(originX + x, blockY, originZ + z);
                        if (!BedWars.nms.isBed(candidate.getType())) {
                            continue;
                        }
                        double distance = x * x + y * y + z * z;
                        if (distance < bestDistance) {
                            best = candidate;
                            bestDistance = distance;
                        }
                    }
                }
            }
            if (best != null) {
                Block foot = getFoot(best);
                return new Location(world, foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5);
            }
        }
        return null;
    }

    private static Block getFoot(Block bedBlock) {
        BlockData data = bedBlock.getBlockData();
        if (data instanceof Bed bed && bed.getPart() == Bed.Part.HEAD) {
            Block foot = bedBlock.getRelative(bed.getFacing().getOppositeFace());
            if (BedWars.nms.isBed(foot.getType())) {
                return foot;
            }
        }
        return bedBlock;
    }
}
