package com.andrei1058.bedwars.arena;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaGroupPolicyTest {

    @Test
    void readsSingleGroupAndNormalizesWhitespace() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("group", " Solo ");

        assertEquals("Solo", ArenaGroupPolicy.read(config));
        assertEquals("Default", ArenaGroupPolicy.normalize("  "));
    }

    @Test
    void migrationReadsOnlyThePrimaryLegacyListEntry() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("groups", List.of(" ", " Featured ", "Daily"));
        config.set("group", "Doubles");

        assertEquals("Featured", ArenaGroupPolicy.read(config));
    }

    @Test
    void resolvesConfiguredNameAndFallsBackToDefault() {
        assertEquals("Featured", ArenaGroupPolicy.resolveConfigured("featured", List.of("Solo", "Featured")));
        assertEquals("Default", ArenaGroupPolicy.resolveConfigured("missing", List.of("Solo")));
        assertTrue(ArenaGroupPolicy.matches("Featured", "featured"));
        assertFalse(ArenaGroupPolicy.matches("Featured", "Daily"));
    }

    @Test
    void compatibilityListUsesOnlyFirstNonBlankEntry() {
        assertEquals("Doubles", ArenaGroupPolicy.first(List.of(" ", "Doubles", "Featured")));
        assertEquals("Default", ArenaGroupPolicy.first(List.of()));
    }
}
