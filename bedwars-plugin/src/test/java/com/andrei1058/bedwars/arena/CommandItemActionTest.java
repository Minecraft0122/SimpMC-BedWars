package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandItemActionTest {

    @Test
    void recognizesReturnItemsByStableIdOrLegacyCommand() {
        assertTrue(CommandItemAction.isLeaveItemDefinition("leave", "custom command", "bw"));
        assertTrue(CommandItemAction.isLeaveItemDefinition("custom", " BW LEAVE ", "bw"));
        assertFalse(CommandItemAction.isLeaveItemDefinition("stats", "bw stats", "bw"));
    }

    @Test
    void parsesOnlyKnownReturnTargets() {
        assertEquals(CommandItemAction.Target.PROXY_LOBBY,
                CommandItemAction.parseTarget("proxy_lobby"));
        assertEquals(CommandItemAction.Target.ARENA_LOBBY,
                CommandItemAction.parseTarget(" ARENA_LOBBY "));
        assertNull(CommandItemAction.parseTarget("unknown"));
        assertNull(CommandItemAction.parseTarget(null));
    }
}
