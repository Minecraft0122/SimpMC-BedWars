package com.andrei1058.bedwars.shop.listeners;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecallScrollChannelStoreTest {

    @Test
    void blocksASecondChannelUntilTheCurrentOneEnds() {
        RecallScrollChannelStore<String> channels = new RecallScrollChannelStore<>();
        UUID playerId = UUID.randomUUID();

        assertTrue(channels.start(playerId, "first"));
        assertTrue(channels.isActive(playerId));
        assertFalse(channels.start(playerId, "second"));
        assertEquals("first", channels.remove(playerId));
        assertFalse(channels.isActive(playerId));
        assertTrue(channels.start(playerId, "third"));
    }
}
