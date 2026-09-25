package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.configuration.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MiscReturnRoutingTest {
    private MainConfig previousConfig;
    private BedWars previousPlugin;
    private ServerType previousServerType;
    private String previousLobbyWorld;
    private boolean previousAutoscale;
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<Arena> arenas;
    private Player player;
    private World lobbyWorld;
    private Location lobbyLocation;

    @BeforeEach
    void setUp() {
        previousConfig = BedWars.config;
        previousPlugin = BedWars.plugin;
        previousServerType = BedWars.getServerType();
        previousLobbyWorld = BedWars.getLobbyWorld();
        previousAutoscale = BedWars.autoscale;
        BedWars.config = mock(MainConfig.class);
        BedWars.plugin = mock(BedWars.class);
        BedWars.setServerType(ServerType.MULTIARENA);
        BedWars.setLobbyWorld("lobby");
        when(BedWars.plugin.getLogger()).thenReturn(Logger.getLogger(getClass().getName()));
        YamlConfiguration config = new YamlConfiguration();
        config.set(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_SERVER, "  login  ");
        when(BedWars.config.getYml()).thenReturn(config);
        when(BedWars.config.getLobbyWorldName()).thenReturn("lobby");
        lobbyWorld = mock(World.class);
        when(lobbyWorld.getName()).thenReturn("lobby");
        lobbyLocation = new Location(lobbyWorld, 0, 64, 0);
        when(BedWars.config.getConfigLoc("lobbyLoc")).thenReturn(lobbyLocation);
        player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(lobbyWorld);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld("lobby")).thenReturn(lobbyWorld);
        bukkit.when(Bukkit::getLogger).thenReturn(Logger.getLogger(getClass().getName()));
        arenas = mockStatic(Arena.class);
    }

    @AfterEach
    void tearDown() {
        try {
            if (arenas != null) arenas.close();
        } finally {
            try {
                if (bukkit != null) bukkit.close();
            } finally {
                BedWars.config = previousConfig;
                BedWars.plugin = previousPlugin;
                BedWars.setServerType(previousServerType);
                BedWars.setAutoscale(previousAutoscale);
                BedWars.setLobbyWorld(previousLobbyWorld);
            }
        }
    }

    @Test
    void configuredLobbySendsConnectWithoutLocalTeleport() throws Exception {
        Misc.moveToLobbyOrKick(player, null, false);

        assertConnectToLogin();
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void fallbackLobbySendsConnectWhenConfiguredWorldIsUnloaded() throws Exception {
        placePlayerIn("world");
        bukkit.when(() -> Bukkit.getWorld("lobby")).thenReturn(null);
        when(BedWars.config.getConfigLoc("lobbyLoc")).thenReturn(new Location(null, 0, 64, 0));

        Misc.moveToLobbyOrKick(player, null, false);

        assertConnectToLogin();
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void arenaPlayerIsRemovedLocallyBeforeTheNextLobbyLeaveConnects() throws Exception {
        placePlayerIn("arena-solo");
        IArena arena = mock(IArena.class);
        arenas.when(() -> Arena.isInArena(player)).thenReturn(true);
        when(player.teleport(lobbyLocation)).thenAnswer(call -> {
            when(player.getWorld()).thenReturn(lobbyWorld);
            return true;
        });

        Misc.moveToLobbyOrKick(player, arena, false);

        verify(player).teleport(lobbyLocation);
        verify(arena).removePlayer(player, false);
        verify(player, never()).sendPluginMessage(any(), anyString(), any(byte[].class));
        arenas.when(() -> Arena.isInArena(player)).thenReturn(false);

        Misc.moveToLobbyOrKick(player, null, false);

        assertConnectToLogin();
    }

    @Test
    void arenaMembershipStillRequiresCleanupIfAlreadyInLobbyWorld() {
        IArena arena = mock(IArena.class);
        arenas.when(() -> Arena.isInArena(player)).thenReturn(true);
        when(player.teleport(lobbyLocation)).thenReturn(true);

        Misc.moveToLobbyOrKick(player, arena, false);

        verify(arena).removePlayer(player, false);
        verify(player, never()).sendPluginMessage(any(), anyString(), any(byte[].class));
    }

    @Test
    void otherWorldReturnsLocallyWhileConfiguredLobbyIsAvailable() {
        placePlayerIn("survival");
        when(player.teleport(lobbyLocation)).thenReturn(true);

        Misc.moveToLobbyOrKick(player, null, false);

        verify(player).teleport(lobbyLocation);
        arenas.verify(() -> Arena.enterLobby(player));
        verify(player, never()).sendPluginMessage(any(), anyString(), any(byte[].class));
    }

    private void placePlayerIn(String name) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        when(player.getWorld()).thenReturn(world);
    }

    private void assertConnectToLogin() throws Exception {
        ArgumentCaptor<byte[]> payload = ArgumentCaptor.forClass(byte[].class);
        verify(player).sendPluginMessage(eq(BedWars.plugin), eq(ProxyLobbyConnector.CHANNEL), payload.capture());
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload.getValue()))) {
            assertEquals("Connect", input.readUTF());
            assertEquals("login", input.readUTF());
            assertEquals(0, input.available());
        }
    }
}
