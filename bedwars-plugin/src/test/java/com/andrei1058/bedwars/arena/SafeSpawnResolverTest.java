package com.andrei1058.bedwars.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeSpawnResolverTest {

    @Test
    void acceptsOneBlockHighSpaceAsCrawlingSpawn() {
        TestWorld testWorld = new TestWorld();
        testWorld.set(0, 63, 0, Material.STONE);
        testWorld.set(0, 65, 0, Material.STONE);

        SafeSpawnResolver.Result result = SafeSpawnResolver.resolve(
                new Location(testWorld.world(), 0.5, 64, 0.5)
        );

        assertTrue(result.crawling());
        assertEquals(0.5, result.location().getX());
        assertEquals(64, result.location().getBlockY());
        assertEquals(0.5, result.location().getZ());
    }

    @Test
    void usesNearbySafeBlockWhenConfiguredSpawnIsObstructed() {
        TestWorld testWorld = new TestWorld();
        testWorld.set(0, 63, 0, Material.STONE);
        testWorld.set(0, 64, 0, Material.STONE);
        testWorld.set(-1, 63, -1, Material.STONE);

        SafeSpawnResolver.Result result = SafeSpawnResolver.resolve(
                new Location(testWorld.world(), 0.5, 64, 0.5)
        );

        assertFalse(result.crawling());
        assertEquals(-0.5, result.location().getX());
        assertEquals(64, result.location().getBlockY());
        assertEquals(-0.5, result.location().getZ());
    }

    private record BlockPosition(int x, int y, int z) {
    }

    private static final class TestWorld {
        private final Map<BlockPosition, Material> blocks = new HashMap<>();
        private final World world;

        private TestWorld() {
            WorldBorder border = (WorldBorder) Proxy.newProxyInstance(
                    WorldBorder.class.getClassLoader(),
                    new Class<?>[]{WorldBorder.class},
                    (proxy, method, args) -> method.getName().equals("isInside")
                            ? true
                            : defaultValue(method.getReturnType())
            );
            world = (World) Proxy.newProxyInstance(
                    World.class.getClassLoader(),
                    new Class<?>[]{World.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getBlockAt" -> block((int) args[0], (int) args[1], (int) args[2]);
                        case "getWorldBorder" -> border;
                        case "getName" -> "test";
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        private World world() {
            return world;
        }

        private void set(int x, int y, int z, Material material) {
            blocks.put(new BlockPosition(x, y, z), material);
        }

        private Block block(int x, int y, int z) {
            Material material = blocks.getOrDefault(new BlockPosition(x, y, z), Material.AIR);
            return (Block) Proxy.newProxyInstance(
                    Block.class.getClassLoader(),
                    new Class<?>[]{Block.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getType" -> material;
                        case "isPassable" -> material == Material.AIR;
                        case "isLiquid" -> material == Material.WATER || material == Material.LAVA;
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == char.class) return '\0';
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0F;
            if (type == double.class) return 0.0D;
            throw new IllegalArgumentException("Unsupported primitive: " + type);
        }
    }
}
