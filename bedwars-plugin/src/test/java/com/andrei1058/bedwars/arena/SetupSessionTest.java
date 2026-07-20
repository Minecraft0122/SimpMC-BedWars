package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupSessionTest {

    @Test
    void setupWorldMatchingIsCaseInsensitive() {
        assertTrue(SetupSession.worldNamesMatch("Summer_Gelato", "summer_gelato"));
    }

    @Test
    void setupWorldDoesNotMatchAnotherOrMissingWorld() {
        assertFalse(SetupSession.worldNamesMatch("summer_gelato", "lobby"));
        assertFalse(SetupSession.worldNamesMatch(null, "summer_gelato"));
        assertFalse(SetupSession.worldNamesMatch("summer_gelato", null));
    }
}
