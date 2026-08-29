package com.andrei1058.bedwars.listeners;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CmdProcessTest {

    @Test
    public void rejoinIsAlwaysAvailableToArenaPlayers() {
        assertTrue(CmdProcess.isAllowedInArena("rejoin", Arrays.<String>asList()));
        assertTrue(CmdProcess.isAllowedInArena("REJOIN", Arrays.<String>asList()));
    }

    @Test
    public void configuredCommandsAreComparedCaseInsensitively() {
        List<String> configured = Arrays.asList("bw", "leave");
        assertTrue(CmdProcess.isAllowedInArena("BW", configured));
        assertFalse(CmdProcess.isAllowedInArena("plugins", configured));
    }
}
