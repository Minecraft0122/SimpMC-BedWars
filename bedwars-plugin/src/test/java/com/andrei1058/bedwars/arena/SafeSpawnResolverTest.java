package com.andrei1058.bedwars.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
        assertEquals(3, testWorld.blockReads());
    }

    @Test
    void usesNearbySafeBlockWhenConfiguredSpawnIsObstructed() {
        TestWorld testWorld = new TestWorld();
        testWorld.load(-1, -1);
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

    @Test
    void skipsUnloadedChunksWhileKeepingLoadedSafeSpawnSearch() {
        TestWorld testWorld = new TestWorld();
        testWorld.set(15, 63, 0, Material.STONE);
        testWorld.set(15, 64, 0, Material.STONE);
        testWorld.set(16, 63, 0, Material.STONE);
        testWorld.set(13, 63, 0, Material.STONE);

        SafeSpawnResolver.Result result = SafeSpawnResolver.resolve(
                new Location(testWorld.world(), 15.5, 64, 0.5)
        );

        assertFalse(result.crawling());
        assertEquals(13.5, result.location().getX());
        assertEquals(64, result.location().getBlockY());
        assertEquals(0.5, result.location().getZ());
        assertTrue(testWorld.wasChunkChecked(1, 0));
        assertEquals(0, testWorld.unloadedBlockReads());
    }

    @Test
    void returnsConfiguredLocationWithoutReadingAnUnloadedSearchArea() {
        TestWorld testWorld = new TestWorld();
        testWorld.unload(0, 0);
        Location configured = new Location(testWorld.world(), 8.25, 64, 8.75, 45, 10);

        SafeSpawnResolver.Result result = SafeSpawnResolver.resolve(configured);

        assertNotSame(configured, result.location());
        assertEquals(configured, result.location());
        assertFalse(result.crawling());
        assertEquals(0, testWorld.blockReads());
        assertEquals(0, testWorld.unloadedBlockReads());
    }

    private record BlockPosition(int x, int y, int z) {
    }

    private record ChunkPosition(int x, int z) {
    }

    private static final class TestWorld {
        private final Map<BlockPosition, Material> blocks = new HashMap<>();
        private final Set<ChunkPosition> loadedChunks = new HashSet<>();
        private final Set<ChunkPosition> checkedChunks = new HashSet<>();
        private final World world;
        private int blockReads;
        private int unloadedBlockReads;

        private TestWorld() {
            loadedChunks.add(new ChunkPosition(0, 0));
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
                        case "isChunkLoaded" -> isChunkLoaded((int) args[0], (int) args[1]);
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

        private void load(int x, int z) {
            loadedChunks.add(new ChunkPosition(x, z));
        }

        private void unload(int x, int z) {
            loadedChunks.remove(new ChunkPosition(x, z));
        }

        private boolean wasChunkChecked(int x, int z) {
            return checkedChunks.contains(new ChunkPosition(x, z));
        }

        private int blockReads() {
            return blockReads;
        }

        private int unloadedBlockReads() {
            return unloadedBlockReads;
        }

        private boolean isChunkLoaded(int x, int z) {
            ChunkPosition chunk = new ChunkPosition(x, z);
            checkedChunks.add(chunk);
            return loadedChunks.contains(chunk);
        }

        private Block block(int x, int y, int z) {
            blockReads++;
            if (!loadedChunks.contains(new ChunkPosition(Math.floorDiv(x, 16), Math.floorDiv(z, 16)))) {
                unloadedBlockReads++;
                throw new AssertionError("Attempted to read an unloaded chunk at " + x + ", " + z);
            }
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
