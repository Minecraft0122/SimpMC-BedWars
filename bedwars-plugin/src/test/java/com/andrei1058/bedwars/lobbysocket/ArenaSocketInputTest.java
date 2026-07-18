package com.andrei1058.bedwars.lobbysocket;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArenaSocketInputTest {

    @Test
    void readsOneBoundedMessageAtATime() throws IOException {
        StringReader input = new StringReader("{\"type\":\"Q\"}\nnext\n");

        assertEquals("{\"type\":\"Q\"}", ArenaSocket.readLimitedMessage(input, 64));
        assertEquals("next", ArenaSocket.readLimitedMessage(input, 64));
        assertNull(ArenaSocket.readLimitedMessage(input, 64));
    }

    @Test
    void rejectsOversizedSocketInput() {
        StringReader input = new StringReader("12345\n");

        assertThrows(IOException.class, () -> ArenaSocket.readLimitedMessage(input, 4));
    }
}
