package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProxyLobbyConnectorTest {

    @Test
    void createsOfficialBungeeQueryPayloads() throws Exception {
        byte[] payload = ProxyLobbyConnector.requestPayload("GetServers");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals("GetServers", input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test
    void parsesAndDeduplicatesProxyServerList() {
        assertEquals(List.of("hub", "bedwars-1", "lobby"),
                ProxyLobbyConnector.parseServerNames("hub, bedwars-1,hub, lobby"));
        assertEquals(List.of(), ProxyLobbyConnector.parseServerNames("  "));
    }

    @Test
    void resolvesExactNameBeforeCompatibleCaseFallback() {
        List<String> servers = List.of("Hub", "bedwars-1", "hub");

        assertEquals("hub", ProxyLobbyConnector.resolveServerName("hub", servers));
        assertEquals("Hub", ProxyLobbyConnector.resolveServerName("HUB", servers));
        assertNull(ProxyLobbyConnector.resolveServerName("missing", servers));
    }
}
