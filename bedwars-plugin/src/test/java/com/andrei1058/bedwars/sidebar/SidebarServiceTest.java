package com.andrei1058.bedwars.sidebar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidebarServiceTest {

    @Test
    void keepsLobbyTabIndependentFromTheSidebarObjective() {
        assertTrue(SidebarService.shouldKeepLobbyTabContext(true, false));
        assertTrue(SidebarService.shouldKeepLobbyTabContext(false, true));
        assertFalse(SidebarService.shouldKeepLobbyTabContext(false, false));
    }
}
