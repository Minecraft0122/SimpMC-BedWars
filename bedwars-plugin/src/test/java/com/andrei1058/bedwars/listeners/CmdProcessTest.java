package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmdProcessTest {

    @Test
    void shoutIsAlwaysAvailableToArenaPlayers() {
        assertTrue(CmdProcess.isAllowedInArena("shout", List.of()));
        assertTrue(CmdProcess.isAllowedInArena("SHOUT", List.of("bw", "leave")));
        assertTrue(CmdProcess.isAllowedInArena("hh", List.of()));
        assertTrue(CmdProcess.isAllowedInArena("H", List.of()));
    }

    @Test
    void configuredCommandsAreComparedCaseInsensitively() {
        assertTrue(CmdProcess.isAllowedInArena("BW", List.of("bw", "leave")));
        assertFalse(CmdProcess.isAllowedInArena("plugins", List.of("bw", "leave")));
    }
}
