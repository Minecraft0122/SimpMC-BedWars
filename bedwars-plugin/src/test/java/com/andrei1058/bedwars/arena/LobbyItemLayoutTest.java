package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LobbyItemLayoutTest {

    @Test
    void stableReturnItemWinsAConfiguredSlotConflict() {
        LobbyItemLayout.Result result = LobbyItemLayout.resolve(List.of(
                new LobbyItemLayout.Item("leave", 8, 2),
                new LobbyItemLayout.Item("custom", 8, 0),
                new LobbyItemLayout.Item("stats", 0, 0)
        ));

        assertEquals(List.of("leave", "stats"), result.items().stream()
                .map(LobbyItemLayout.Item::id).toList());
        assertEquals(1, result.conflicts().size());
        assertEquals("leave", result.conflicts().getFirst().selectedId());
    }

    @Test
    void stableIdOutranksLegacyCommandOnlyReturnItem() {
        LobbyItemLayout.Result result = LobbyItemLayout.resolve(List.of(
                new LobbyItemLayout.Item("leave", 8, 2),
                new LobbyItemLayout.Item("legacy-hub", 8, 1)
        ));

        assertEquals("leave", result.items().getFirst().id());
    }

    @Test
    void unchangedPriorityRetainsTheExistingYamlLastWriteRule() {
        LobbyItemLayout.Result result = LobbyItemLayout.resolve(List.of(
                new LobbyItemLayout.Item("first", 4, 0),
                new LobbyItemLayout.Item("second", 4, 0)
        ));

        assertEquals("second", result.items().getFirst().id());
        assertEquals(new LobbyItemLayout.Conflict(4, "first", "second"), result.conflicts().getFirst());
    }
}
