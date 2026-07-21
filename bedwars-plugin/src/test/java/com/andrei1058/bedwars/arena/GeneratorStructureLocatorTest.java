package com.andrei1058.bedwars.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorStructureLocatorTest {

    @Test
    void acceptsSolidAndCenterOnlyBasesWithMixedStairs() {
        Map<String, Material> solidDiamond = solidStructure(Material.DIAMOND_BLOCK);
        Map<String, Material> centerOnlyDiamond = centerOnlyStructure(Material.DIAMOND_BLOCK);
        Map<String, Material> centerOnlyEmerald = centerOnlyStructure(Material.EMERALD_BLOCK);

        assertTrue(matches(Material.DIAMOND_BLOCK, solidDiamond));
        assertTrue(matches(Material.DIAMOND_BLOCK, centerOnlyDiamond));
        assertTrue(matches(Material.EMERALD_BLOCK, centerOnlyEmerald));
        assertTrue(GeneratorStructureLocator.isStairs(Material.STONE_BRICK_STAIRS));
        assertTrue(GeneratorStructureLocator.isStairs(Material.BIRCH_STAIRS));
        assertFalse(GeneratorStructureLocator.isStairs(Material.STONE_BRICKS));
    }

    @Test
    void rejectsEveryMalformedLayer() {
        Map<String, Material> missingBase = solidStructure(Material.DIAMOND_BLOCK);
        missingBase.put(key(1, 0, 1), Material.AIR);
        assertFalse(matches(Material.DIAMOND_BLOCK, missingBase));

        Map<String, Material> partialBase = centerOnlyStructure(Material.DIAMOND_BLOCK);
        partialBase.put(key(1, 0, 1), Material.DIAMOND_BLOCK);
        assertFalse(matches(Material.DIAMOND_BLOCK, partialBase));

        Map<String, Material> mixedResourceBase = centerOnlyStructure(Material.DIAMOND_BLOCK);
        mixedResourceBase.put(key(1, 0, 1), Material.EMERALD_BLOCK);
        assertFalse(matches(Material.DIAMOND_BLOCK, mixedResourceBase));

        Map<String, Material> missingCenter = centerOnlyStructure(Material.DIAMOND_BLOCK);
        missingCenter.put(key(0, 0, 0), Material.STONE);
        assertFalse(matches(Material.DIAMOND_BLOCK, missingCenter));

        Map<String, Material> filledCenter = solidStructure(Material.DIAMOND_BLOCK);
        filledCenter.put(key(0, 1, 0), Material.STONE_BRICK_STAIRS);
        assertFalse(matches(Material.DIAMOND_BLOCK, filledCenter));

        Map<String, Material> brokenStairRing = solidStructure(Material.DIAMOND_BLOCK);
        brokenStairRing.put(key(-1, 1, 0), Material.STONE_BRICKS);
        assertFalse(matches(Material.DIAMOND_BLOCK, brokenStairRing));

        Map<String, Material> blockedTop = solidStructure(Material.DIAMOND_BLOCK);
        blockedTop.put(key(0, 2, 1), Material.GLASS);
        assertFalse(matches(Material.DIAMOND_BLOCK, blockedTop));
        assertFalse(matches(Material.GOLD_BLOCK, solidStructure(Material.GOLD_BLOCK)));
    }

    @Test
    void scannerReturnsTheMiddleAirBlockAtBlockCenter() {
        Map<String, Material> blocks = centerOnlyStructure(Material.DIAMOND_BLOCK);
        World world = world(blocks);

        GeneratorStructureLocator.ScanResult result = GeneratorStructureLocator.findAll(
                world, -1, 1, 0, 0, -1, 1);

        assertEquals(1, result.diamondGenerators().size());
        assertEquals(0, result.emeraldGenerators().size());
        Location generator = result.diamondGenerators().getFirst();
        assertEquals(0.5, generator.getX());
        assertEquals(1.0, generator.getY());
        assertEquals(0.5, generator.getZ());
        assertEquals(0.0F, generator.getYaw());
        assertEquals(0.0F, generator.getPitch());
    }

    private static boolean matches(Material resource, Map<String, Material> blocks) {
        return GeneratorStructureLocator.matchesStructure(resource,
                (x, y, z) -> blocks.getOrDefault(key(x, y, z), Material.AIR));
    }

    private static Map<String, Material> solidStructure(Material resource) {
        return structure(resource, true);
    }

    private static Map<String, Material> centerOnlyStructure(Material resource) {
        return structure(resource, false);
    }

    private static Map<String, Material> structure(Material resource, boolean solidBase) {
        Map<String, Material> blocks = new HashMap<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                blocks.put(key(x, 0, z), solidBase || (x == 0 && z == 0) ? resource : Material.STONE);
                if (x != 0 || z != 0) {
                    blocks.put(key(x, 1, z), (x + z & 1) == 0
                            ? Material.STONE_BRICK_STAIRS : Material.BIRCH_STAIRS);
                }
            }
        }
        return blocks;
    }

    private static World world(Map<String, Material> blocks) {
        final World[] holder = new World[1];
        holder[0] = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMinHeight" -> -64;
                    case "getMaxHeight" -> 320;
                    case "getBlockAt" -> block(holder[0], blocks,
                            (int) args[0], (int) args[1], (int) args[2]);
                    case "getName" -> "generator-test";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
        return holder[0];
    }

    private static Block block(World world, Map<String, Material> blocks, int x, int y, int z) {
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getType" -> blocks.getOrDefault(key(x, y, z), Material.AIR);
                    case "getRelative" -> block(world, blocks,
                            x + (int) args[0], y + (int) args[1], z + (int) args[2]);
                    case "getWorld" -> world;
                    case "getX" -> x;
                    case "getY" -> y;
                    case "getZ" -> z;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == char.class) return '\0';
        return null;
    }
}
