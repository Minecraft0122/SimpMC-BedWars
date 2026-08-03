package com.andrei1058.bedwars.commands.shout;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoutFormattingContextTest {

    @Test
    void contextExistsOnlyDuringTheNestedFormattingPass() {
        UUID playerId = UUID.randomUUID();

        assertFalse(ShoutFormattingContext.isFormatting(playerId));
        assertEquals("formatted", ShoutFormattingContext.format(playerId, () -> {
            assertTrue(ShoutFormattingContext.isFormatting(playerId));
            assertFalse(CompletableFuture.supplyAsync(
                    () -> ShoutFormattingContext.isFormatting(playerId)).join());
            return "formatted";
        }));
        assertFalse(ShoutFormattingContext.isFormatting(playerId));
    }

    @Test
    void nestedFormattingRestoresThePreviousPlayer() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        ShoutFormattingContext.format(first, () -> {
            ShoutFormattingContext.format(second, () -> {
                assertFalse(ShoutFormattingContext.isFormatting(first));
                assertTrue(ShoutFormattingContext.isFormatting(second));
                return null;
            });
            assertTrue(ShoutFormattingContext.isFormatting(first));
            assertFalse(ShoutFormattingContext.isFormatting(second));
            return null;
        });
    }

    @Test
    void exceptionsCannotLeakTheShoutMarker() {
        UUID playerId = UUID.randomUUID();

        assertThrows(IllegalStateException.class, () -> ShoutFormattingContext.format(playerId, () -> {
            throw new IllegalStateException("placeholder failed");
        }));
        assertFalse(ShoutFormattingContext.isFormatting(playerId));
    }
}
