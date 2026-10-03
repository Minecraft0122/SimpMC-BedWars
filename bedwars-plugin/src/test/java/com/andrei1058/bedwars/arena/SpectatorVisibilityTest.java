package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpectatorVisibilityTest {
    @Test
    void hidesLateAndEliminatedSpectatorsButKeepsRespawningPlayersAndSpectatorViewers() {
        IArena arena = mock(IArena.class);
        Player active = mock(Player.class);
        Player late = mock(Player.class);
        Player eliminated = mock(Player.class);
        Player respawning = mock(Player.class);
        when(arena.isPlayer(active)).thenReturn(true);
        when(arena.isPlayer(respawning)).thenReturn(true);
        when(arena.isReSpawning(respawning)).thenReturn(true);
        when(arena.isSpectator(late)).thenReturn(true);
        when(arena.isSpectator(eliminated)).thenReturn(true);

        assertTrue(SpectatorVisibility.hideIfSpectator(arena, active, late));
        assertTrue(SpectatorVisibility.hideIfSpectator(arena, active, eliminated));
        assertTrue(SpectatorVisibility.hideIfSpectator(arena, respawning, late));
        assertFalse(SpectatorVisibility.hideIfSpectator(arena, active, respawning));
        assertFalse(SpectatorVisibility.hideIfSpectator(arena, eliminated, late));
        verify(active).hidePlayer(BedWars.plugin, late);
        verify(active).hidePlayer(BedWars.plugin, eliminated);
        verify(active, never()).hidePlayer(BedWars.plugin, respawning);
        verifyNoInteractions(eliminated);
    }
}
