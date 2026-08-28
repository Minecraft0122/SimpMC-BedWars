package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CmdTeamTabCompletionTest {

    @Test
    void filtersPlayerNamesByCaseInsensitivePrefixAndSortsThem() {
        assertEquals(List.of("alex", "ALexandra", "Alice"),
                CmdTeam.filterPlayerSuggestions(List.of("ALexandra", "Alice", "Bob", "alex"), "al"));
    }

    @Test
    void emptyPrefixReturnsAllNamesAndNullEntriesAreIgnored() {
        assertEquals(List.of("Alpha", "beta"),
                CmdTeam.filterPlayerSuggestions(Arrays.asList("beta", null, "Alpha"), ""));
    }

}
