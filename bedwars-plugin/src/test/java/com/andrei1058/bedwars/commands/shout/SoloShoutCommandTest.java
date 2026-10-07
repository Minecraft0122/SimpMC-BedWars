package com.andrei1058.bedwars.commands.shout;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class SoloShoutCommandTest {
    @Test
    void soloCommandUsesOrdinaryChatWithoutShoutPermissionOrMarker() {
        Player player = mock(Player.class);
        IArena arena = mock(IArena.class);
        ITeam team = mock(ITeam.class);
        when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.getTeam(player)).thenReturn(team);
        when(arena.getTeamSizeAtGameStart(team)).thenReturn(1);
        MainConfig original = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        try (var arenas = mockStatic(Arena.class)) {
            arenas.when(() -> Arena.getArenaByPlayer(player)).thenReturn(arena);
            new ShoutCommand("shout").execute(player, "shout", new String[]{"你好", "对手"});
            verify(player).chat("你好 对手");
            verify(player, never()).hasPermission(anyString());
        } finally {
            BedWars.config = original;
        }
    }
}
