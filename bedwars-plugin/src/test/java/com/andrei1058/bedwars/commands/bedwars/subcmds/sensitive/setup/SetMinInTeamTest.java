package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SetMinInTeamTest {

    @Test
    void suggestionsFollowConfiguredMaximumBeyondTwoPlayers() {
        assertEquals(List.of("1", "2", "3", "4", "5"),
                SetMinInTeam.suggestionsFor(5));
    }

    @Test
    void suggestionsRemainBoundedForInvalidOrExtremeConfiguration() {
        assertEquals(List.of("1"), SetMinInTeam.suggestionsFor(0));
        assertEquals("64", SetMinInTeam.suggestionsFor(Integer.MAX_VALUE).getLast());
        assertEquals(64, SetMinInTeam.suggestionsFor(Integer.MAX_VALUE).size());
    }
}
