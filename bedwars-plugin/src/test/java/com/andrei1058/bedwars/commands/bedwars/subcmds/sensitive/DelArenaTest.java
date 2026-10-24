package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelArenaTest {

    @Test
    void confirmationExpiresAndIsConsumedOnce() {
        UUID player = UUID.randomUUID();
        Map<UUID, Long> confirmations = new HashMap<>();

        assertFalse(DelArena.consumeConfirmation(confirmations, player, 1_000L));
        assertTrue(DelArena.consumeConfirmation(confirmations, player, 2_000L));
        assertFalse(DelArena.consumeConfirmation(confirmations, player, 2_001L));
        assertFalse(DelArena.consumeConfirmation(confirmations, UUID.randomUUID(), 4_002L));
        assertFalse(confirmations.containsKey(player));
        assertTrue(confirmations.size() == 1);
    }
}
