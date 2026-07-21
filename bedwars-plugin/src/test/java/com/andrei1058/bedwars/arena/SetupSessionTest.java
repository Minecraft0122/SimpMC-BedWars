package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.server.SetupType;
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

    @Test
    void automaticAssistanceIsRestrictedToAssistedSetup() {
        assertTrue(SetupSession.usesAutomaticAssistance(SetupType.ASSISTED));
        assertFalse(SetupSession.usesAutomaticAssistance(SetupType.ADVANCED));
        assertFalse(SetupSession.usesAutomaticAssistance(null));
    }
}
