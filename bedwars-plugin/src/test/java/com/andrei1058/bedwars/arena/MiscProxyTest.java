package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiscProxyTest {

    @Test
    void createsBungeeConnectPayloadForConfiguredLobby() throws Exception {
        byte[] payload = Misc.proxyConnectPayload("  hub  ");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals("Connect", input.readUTF());
            assertEquals("hub", input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test
    void recognizesTheConfiguredLocalLobbyBeforeProxyReturn() {
        assertTrue(Misc.isConfiguredLobbyWorld("Lobby", "lobby"));
        assertFalse(Misc.isConfiguredLobbyWorld("arena-solo", "lobby"));
        assertFalse(Misc.isConfiguredLobbyWorld("lobby", ""));
        assertFalse(Misc.isConfiguredLobbyWorld(null, "lobby"));
        assertFalse(Misc.isConfiguredLobbyWorld("lobby", null));
    }
}
