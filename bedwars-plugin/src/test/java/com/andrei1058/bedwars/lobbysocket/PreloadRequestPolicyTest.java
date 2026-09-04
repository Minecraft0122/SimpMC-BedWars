package com.andrei1058.bedwars.lobbysocket;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreloadRequestPolicyTest {

    @Test
    public void matchingRequestCanBeCancelled() {
        assertTrue(PreloadRequestPolicy.matches("request-1", "request-1"));
    }

    @Test
    public void lateCancellationCannotDeleteNewRequest() {
        assertFalse(PreloadRequestPolicy.matches("request-2", "request-1"));
    }

    @Test
    public void legacyMessagesWithoutTokensRemainCompatible() {
        assertTrue(PreloadRequestPolicy.matches("", "request-1"));
        assertTrue(PreloadRequestPolicy.matches("request-1", ""));
    }
}
