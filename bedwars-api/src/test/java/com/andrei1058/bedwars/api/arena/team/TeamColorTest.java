package com.andrei1058.bedwars.api.arena.team;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TeamColorTest {

    @Test
    void cyanUsesCyanBlocksAndDye() {
        TeamColor cyan = TeamColor.CYAN;

        assertSame(DyeColor.CYAN, cyan.dye());
        assertSame(Material.CYAN_WOOL, cyan.woolMaterial());
        assertSame(Material.CYAN_BED, cyan.bedMaterial());
        assertSame(Material.CYAN_STAINED_GLASS, cyan.glassMaterial());
        assertSame(Material.CYAN_STAINED_GLASS_PANE, cyan.glassPaneMaterial());
        assertSame(Material.CYAN_TERRACOTTA, cyan.glazedTerracottaMaterial());
        assertSame(ChatColor.AQUA, cyan.chat(), "Minecraft has no separate cyan chat color");
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyAquaInputNormalizesToCyanWithoutBreakingAddons() {
        assertSame(TeamColor.CYAN, TeamColor.fromName("aqua"));
        assertSame(Material.CYAN_WOOL, TeamColor.AQUA.woolMaterial());
        assertFalse(Arrays.asList(TeamColor.selectableValues()).contains(TeamColor.AQUA));
    }

    @Test
    void automaticTeamDetectionRecognizesCyanWoolOnly() {
        assertEquals("Cyan", TeamColor.enName("CYAN_WOOL"));
        assertEquals("", TeamColor.enName("LIGHT_BLUE_WOOL"));
        assertEquals("Cyan", TeamColor.enName((byte) 9));
    }

    @Test
    void automaticTeamDetectionStrictlyDistinguishesSimilarWoolColors() {
        assertSame(TeamColor.GREEN, TeamColor.fromWool(Material.LIME_WOOL));
        assertSame(TeamColor.DARK_GREEN, TeamColor.fromWool(Material.GREEN_WOOL));
        assertSame(TeamColor.GRAY, TeamColor.fromWool(Material.LIGHT_GRAY_WOOL));
        assertSame(TeamColor.DARK_GRAY, TeamColor.fromWool(Material.GRAY_WOOL));

        assertEquals("Green", TeamColor.enName("LIME_WOOL"));
        assertEquals("Dark_Green", TeamColor.enName("GREEN_WOOL"));
        assertEquals("Gray", TeamColor.enName("LIGHT_GRAY_WOOL"));
        assertEquals("Dark_Gray", TeamColor.enName("GRAY_WOOL"));
        assertNull(TeamColor.fromWool(Material.LIGHT_BLUE_WOOL));
    }

    @Test
    void everySelectableTeamColorHasAnExactWoolRoundTrip() {
        for (TeamColor color : TeamColor.selectableValues()) {
            assertSame(color, TeamColor.fromWool(color.woolMaterial()), color.name());
        }
    }
}
