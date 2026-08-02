package com.andrei1058.bedwars.arena;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaSignRegistryTest {

    @Test
    void indexesLegacyListMutationsByWorldAndChunk() {
        World world = world();
        World otherWorld = world();
        Block west = block(world, 15, 0);
        Block east = block(world, 16, 0);
        Block elsewhere = block(otherWorld, 16, 0);
        ArenaSignRegistry registry = new ArenaSignRegistry();

        registry.add(west);
        registry.add(east);
        registry.add(elsewhere);

        assertEquals(java.util.List.of(west), registry.inChunk(chunk(world, 0, 0)));
        assertEquals(java.util.List.of(east, west), registry.inChunkAndNeighbors(chunk(world, 1, 0)));

        registry.remove(west);
        assertTrue(registry.inChunk(chunk(world, 0, 0)).isEmpty());
        registry.clear();
        assertTrue(registry.inChunk(chunk(world, 1, 0)).isEmpty());
    }

    private static World world() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Block block(World world, int x, int z) {
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    case "getX" -> x;
                    case "getZ" -> z;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Chunk chunk(World world, int x, int z) {
        return (Chunk) Proxy.newProxyInstance(Chunk.class.getClassLoader(), new Class<?>[]{Chunk.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    case "getX" -> x;
                    case "getZ" -> z;
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
