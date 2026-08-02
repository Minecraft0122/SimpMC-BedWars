package com.andrei1058.bedwars.listeners.blockstatus;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStatusListenerTest {

    @Test
    void neverReadsAnUnloadedSignChunk() {
        World world = world(false);
        Block sign = block(world, -1, 32);

        assertFalse(BlockStatusListener.canReadSign(sign, null));
    }

    @Test
    void chunkReplayOnlyAcceptsSignsFromTheLoadedChunk() {
        World world = world(true);
        Block sign = block(world, -1, 32);

        assertTrue(BlockStatusListener.canReadSign(sign, chunk(world, -1, 2)));
        assertTrue(BlockStatusListener.canReadSign(sign, chunk(world, 0, 2)),
                "an adjacent support chunk may trigger a wall-sign replay");
        assertFalse(BlockStatusListener.canReadSign(sign, chunk(world, 1, 2)));
        assertFalse(BlockStatusListener.canReadSign(sign, chunk(world(true), -1, 2)));
    }

    @Test
    void wallSignBackgroundWaitsForAndReplaysFromItsSupportChunk() {
        Set<ChunkPosition> loaded = new HashSet<>();
        loaded.add(new ChunkPosition(0, 0));
        World world = world(loaded);
        Block signBlock = block(world, 15, 0);
        Sign sign = wallSign(signBlock, BlockFace.WEST);

        assertFalse(BlockStatusListener.canUpdateSignBackground(sign, chunk(world, 0, 0)));

        loaded.add(new ChunkPosition(1, 0));
        assertTrue(BlockStatusListener.canUpdateSignBackground(sign, chunk(world, 0, 0)));
        assertTrue(BlockStatusListener.canUpdateSignBackground(sign, chunk(world, 1, 0)));
        assertFalse(BlockStatusListener.canUpdateSignBackground(sign, chunk(world, 2, 0)));
    }

    private record ChunkPosition(int x, int z) {
    }

    private static World world(boolean loaded) {
        if (!loaded) return world(Set.of());
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isChunkLoaded" -> true;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static World world(Set<ChunkPosition> loadedChunks) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isChunkLoaded" -> loadedChunks.contains(
                            new ChunkPosition((int) args[0], (int) args[1]));
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Block block(World world, int x, int z) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    case "getX" -> x;
                    case "getZ" -> z;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Chunk chunk(World world, int x, int z) {
        return (Chunk) Proxy.newProxyInstance(
                Chunk.class.getClassLoader(), new Class<?>[]{Chunk.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    case "getX" -> x;
                    case "getZ" -> z;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Sign wallSign(Block block, BlockFace facing) {
        WallSign data = (WallSign) Proxy.newProxyInstance(
                WallSign.class.getClassLoader(), new Class<?>[]{WallSign.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getFacing" -> facing;
                    default -> defaultValue(method.getReturnType());
                });
        return (Sign) Proxy.newProxyInstance(
                Sign.class.getClassLoader(), new Class<?>[]{Sign.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBlock" -> block;
                    case "getBlockData" -> data;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
