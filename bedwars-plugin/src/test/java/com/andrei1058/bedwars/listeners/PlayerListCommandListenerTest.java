package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListCommandListenerTest {

    @Test
    void recognizesOnlyRootListCommands() {
        assertTrue(PlayerListCommandListener.isListCommand("/list"));
        assertTrue(PlayerListCommandListener.isListCommand("LIST"));
        assertTrue(PlayerListCommandListener.isListCommand("/minecraft:list extra"));
        assertFalse(PlayerListCommandListener.isListCommand("/bw list"));
        assertFalse(PlayerListCommandListener.isListCommand("/bukkit:list"));
        assertFalse(PlayerListCommandListener.isListCommand(null));
    }
}
