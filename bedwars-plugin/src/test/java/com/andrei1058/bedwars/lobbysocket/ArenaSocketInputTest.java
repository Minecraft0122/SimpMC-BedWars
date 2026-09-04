package com.andrei1058.bedwars.lobbysocket;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ArenaSocketInputTest {

    @Test
    public void advertisesTheCurrentArenaProtocol() {
        assertEquals(2, BungeeProtocol.CURRENT_VERSION);
        assertEquals("ARENA", BungeeProtocol.ARENA_ROLE);
    }

    @Test
    public void readsOneBoundedMessageAtATime() throws IOException {
        StringReader input = new StringReader("{\"type\":\"Q\"}\r\nnext\n");

        assertEquals("{\"type\":\"Q\"}", ArenaSocket.readLimitedMessage(input, 64));
        assertEquals("next", ArenaSocket.readLimitedMessage(input, 64));
        assertNull(ArenaSocket.readLimitedMessage(input, 64));
    }

    @Test
    public void rejectsOversizedSocketInput() throws IOException {
        StringReader input = new StringReader("12345\n");

        try {
            ArenaSocket.readLimitedMessage(input, 4);
            fail("oversized messages must be rejected");
        } catch (IOException expected) {
            assertEquals("incoming message exceeds 4 characters", expected.getMessage());
        }
    }
}
