package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockPlacementResyncBufferTest {

    @Test
    void schedulesOnlyOnceAndDeduplicatesPositionsWithinBatch() {
        BlockPlacementResyncBuffer buffer = new BlockPlacementResyncBuffer();
        UUID player = UUID.randomUUID();
        UUID world = UUID.randomUUID();

        assertTrue(buffer.queue(player, world, 4, 70, -8));
        assertFalse(buffer.queue(player, world, 4, 70, -8));
        assertFalse(buffer.queue(player, world, 5, 70, -8));

        assertEquals(List.of(
                new BlockPlacementResyncBuffer.BlockPosition(world, 4, 70, -8),
                new BlockPlacementResyncBuffer.BlockPosition(world, 5, 70, -8)
        ), buffer.drain(player));
        assertTrue(buffer.queue(player, world, 6, 70, -8));
    }

    @Test
    void keepsWorldsSeparateAndDiscardsDisconnectedPlayers() {
        BlockPlacementResyncBuffer buffer = new BlockPlacementResyncBuffer();
        UUID player = UUID.randomUUID();
        UUID firstWorld = UUID.randomUUID();
        UUID secondWorld = UUID.randomUUID();

        assertTrue(buffer.queue(player, firstWorld, 0, 64, 0));
        assertFalse(buffer.queue(player, secondWorld, 0, 64, 0));
        buffer.discard(player);

        assertEquals(List.of(), buffer.drain(player));
        assertTrue(buffer.queue(player, secondWorld, 0, 64, 0));
    }
}
