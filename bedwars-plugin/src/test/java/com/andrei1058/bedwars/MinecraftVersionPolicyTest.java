package com.andrei1058.bedwars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftVersionPolicyTest {

    @Test
    void acceptsOnlyTheTwoVerifiedPaperMinecraftVersions() {
        assertTrue(MinecraftVersionPolicy.isSupported("1.21.11"));
        assertTrue(MinecraftVersionPolicy.isSupported("26.2"));
        assertTrue(MinecraftVersionPolicy.isSupported("26.2.build.112"));
        assertFalse(MinecraftVersionPolicy.isSupported("1.21.10"));
        assertFalse(MinecraftVersionPolicy.isSupported("26.1"));
        assertFalse(MinecraftVersionPolicy.isSupported("26.2.1"));
        assertFalse(MinecraftVersionPolicy.isSupported(null));
    }
}
