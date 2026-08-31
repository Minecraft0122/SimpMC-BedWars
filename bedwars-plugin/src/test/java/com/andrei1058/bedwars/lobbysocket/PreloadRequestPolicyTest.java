/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.lobbysocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreloadRequestPolicyTest {

    @Test
    void matchingRequestCanBeCancelled() {
        assertTrue(PreloadRequestPolicy.matches("request-1", "request-1"));
    }

    @Test
    void lateCancellationCannotDeleteNewRequest() {
        assertFalse(PreloadRequestPolicy.matches("request-2", "request-1"));
    }

    @Test
    void legacyMessagesWithoutTokensRemainCompatible() {
        assertTrue(PreloadRequestPolicy.matches("", "request-1"));
        assertTrue(PreloadRequestPolicy.matches("request-1", ""));
    }
}
