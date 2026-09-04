package com.andrei1058.bedwars.lobbysocket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PreloadTimeoutPolicyTest {

    @Test
    public void legacyFiveSecondTimeoutCoversDispatchAndHandoff() {
        assertEquals(23_000L, PreloadTimeoutPolicy.effectiveTimeoutMillis(5_000L, 8));
    }

    @Test
    public void administratorTimeoutAboveMinimumIsPreserved() {
        assertEquals(30_000L, PreloadTimeoutPolicy.effectiveTimeoutMillis(30_000L, 8));
    }

    @Test
    public void invalidValuesStillGetAUsableBudget() {
        assertEquals(16_000L, PreloadTimeoutPolicy.effectiveTimeoutMillis(0L, 0));
    }
}
