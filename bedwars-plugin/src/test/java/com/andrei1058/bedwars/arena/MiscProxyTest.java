package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
