package com.andrei1058.bedwars.sidebar;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabLayoutTest {

    @Test
    void widensOnlyNarrowTabTemplates() {
        assertEquals(List.of("&a标题", TabLayout.MINIMUM_WIDTH_LINE),
                TabLayout.ensureMinimumHeaderWidth(List.of("&a标题"), List.of("&7人数：1")));

        String wide = "--------------------------------";
        assertEquals(List.of(wide),
                TabLayout.ensureMinimumHeaderWidth(List.of(wide), List.of()));
    }

    @Test
    void ignoresColorCodesAndCountsChineseAsWide() {
        assertEquals(4, TabLayout.visibleWidth("&a大厅"));
        assertEquals(3, TabLayout.visibleWidth("&lTAB"));
    }
}
