package com.andrei1058.bedwars.arena;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

/**
 * Mutable legacy sign list with a chunk index for event-driven refreshes.
 * Mutations through {@link com.andrei1058.bedwars.api.arena.IArena#getSigns()}
 * remain supported and update the index as well.
 */
final class ArenaSignRegistry extends AbstractList<Block> implements RandomAccess {

    private final List<Block> blocks = new ArrayList<>();
    private final Map<World, Map<Long, List<Block>>> blocksByChunk = new IdentityHashMap<>();

    @Override
    public Block get(int index) {
        return blocks.get(index);
    }

    @Override
    public int size() {
        return blocks.size();
    }

    @Override
    public void add(int index, Block block) {
        blocks.add(index, block);
        index(block);
        modCount++;
    }

    @Override
    public Block set(int index, Block block) {
        Block previous = blocks.set(index, block);
        unindex(previous);
        index(block);
        return previous;
    }

    @Override
    public Block remove(int index) {
        Block removed = blocks.remove(index);
        unindex(removed);
        modCount++;
        return removed;
    }

    @Override
    public void clear() {
        if (blocks.isEmpty()) return;
        blocks.clear();
        blocksByChunk.clear();
        modCount++;
    }

    @NotNull List<Block> inChunk(@NotNull Chunk chunk) {
        Map<Long, List<Block>> worldIndex = blocksByChunk.get(chunk.getWorld());
        if (worldIndex == null) return List.of();
        List<Block> matches = worldIndex.get(chunkKey(chunk.getX(), chunk.getZ()));
        return matches == null ? List.of() : List.copyOf(matches);
    }

    @NotNull List<Block> inChunkAndNeighbors(@NotNull Chunk chunk) {
        Map<Long, List<Block>> worldIndex = blocksByChunk.get(chunk.getWorld());
        if (worldIndex == null) return List.of();

        LinkedHashSet<Block> matches = new LinkedHashSet<>();
        addChunk(matches, worldIndex, chunk.getX(), chunk.getZ());
        addChunk(matches, worldIndex, chunk.getX() - 1, chunk.getZ());
        addChunk(matches, worldIndex, chunk.getX() + 1, chunk.getZ());
        addChunk(matches, worldIndex, chunk.getX(), chunk.getZ() - 1);
        addChunk(matches, worldIndex, chunk.getX(), chunk.getZ() + 1);
        return List.copyOf(matches);
    }

    private static void addChunk(LinkedHashSet<Block> matches,
                                 Map<Long, List<Block>> worldIndex,
                                 int chunkX, int chunkZ) {
        List<Block> indexed = worldIndex.get(chunkKey(chunkX, chunkZ));
        if (indexed != null) matches.addAll(indexed);
    }

    private void index(Block block) {
        if (block == null) return;
        blocksByChunk
                .computeIfAbsent(block.getWorld(), ignored -> new HashMap<>())
                .computeIfAbsent(chunkKey(block.getX() >> 4, block.getZ() >> 4), ignored -> new ArrayList<>())
                .add(block);
    }

    private void unindex(Block block) {
        if (block == null) return;
        Map<Long, List<Block>> worldIndex = blocksByChunk.get(block.getWorld());
        if (worldIndex == null) return;
        long chunkKey = chunkKey(block.getX() >> 4, block.getZ() >> 4);
        List<Block> indexed = worldIndex.get(chunkKey);
        if (indexed == null) return;
        indexed.remove(block);
        if (indexed.isEmpty()) worldIndex.remove(chunkKey);
        if (worldIndex.isEmpty()) blocksByChunk.remove(block.getWorld());
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }
}
