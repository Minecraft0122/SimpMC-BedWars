package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaPluginMetadataTest {

    @Test
    void descriptorPinsTheArenaEntrypoint() throws IOException {
        String descriptor;
        try (InputStream input = getClass().getResourceAsStream("/plugin.yml")) {
            assertTrue(input != null, "Arena plugin.yml must be packaged in test resources");
            descriptor = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(descriptor.contains("version: 6.0.1"));
        assertTrue(descriptor.contains("main: com.andrei1058.bedwars.arena.ArenaBedWarsPlugin"));
        assertTrue(descriptor.contains("竞技场插件"));
    }
}
