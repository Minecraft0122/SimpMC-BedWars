package com.andrei1058.bedwars.arena;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaGroupMembershipTest {

    @Test
    void normalizesOrderedMembershipWithoutCaseInsensitiveDuplicates() {
        assertEquals(List.of("Solo", "Doubles"),
                ArenaGroupMembership.normalize(List.of(" Solo ", "solo", "", "Doubles")));
    }

    @Test
    void resolvesConfiguredNamesAndDropsUnknownGroups() {
        assertEquals(List.of("Solo", "Featured"), ArenaGroupMembership.resolveConfigured(
                List.of("solo", "missing", "FEATURED"),
                List.of("Solo", "Featured")));
        assertEquals(List.of("Default"),
                ArenaGroupMembership.resolveConfigured(List.of("missing"), List.of("Solo")));
    }

    @Test
    void readsLegacySingleGroupAndSupportsMultipleMemberships() {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("group", "Solo");
        assertEquals(List.of("Solo"), ArenaGroupMembership.read(legacy));

        List<String> groups = ArenaGroupMembership.withPrimary(List.of("Solo", "Featured"), "Doubles");
        assertEquals(List.of("Doubles", "Solo", "Featured"), groups);
        assertEquals(List.of("Solo"),
                ArenaGroupMembership.withPrimary(List.of("Default"), "Solo"));
        assertTrue(ArenaGroupMembership.contains(groups, "featured"));
        assertFalse(ArenaGroupMembership.contains(groups, "Missing"));
    }

    @Test
    void combinedGroupQueryReturnsOneBooleanMatchForMultiGroupArena() {
        List<String> memberships = List.of("Solo", "Featured");

        assertTrue(ArenaGroupMembership.matchesAny(memberships, "Solo+Featured"));
        assertTrue(ArenaGroupMembership.matchesAny(memberships, "Daily+Featured"));
        assertFalse(ArenaGroupMembership.matchesAny(memberships, "Doubles+Daily"));
    }
}
