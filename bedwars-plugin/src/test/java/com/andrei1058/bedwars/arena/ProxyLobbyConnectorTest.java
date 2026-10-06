package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProxyLobbyConnectorTest {
    @Test
    void validatesNamesExactlyWithoutAcceptingPartialOrCaseChangedNames() {
        assertTrue(ProxyLobbyConnector.serverExists("hub, login, bw", "login"));
        assertFalse(ProxyLobbyConnector.serverExists("hub2, login", "hub"));
        assertFalse(ProxyLobbyConnector.serverExists("Hub", "hub"));
        assertFalse(ProxyLobbyConnector.serverExists("", "login"));
    }

    @Test
    void validatesProxyReplyBeforeConnectingAndReportsMissingTargetWithoutLeakingIt() throws Exception {
        BedWars plugin = mock(BedWars.class);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask timeout = mock(BukkitTask.class);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(60L))).thenReturn(timeout);
        try (var bukkit = mockStatic(Bukkit.class); var text = mockStatic(AdventureText.class); var misc = mockStatic(Misc.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            ProxyLobbyConnector connector = new ProxyLobbyConnector(plugin);
            assertTrue(connector.connect(player, "missing"));
            assertTrue(connector.connect(player, "missing"));
            verify(player, times(1)).sendPluginMessage(eq(plugin), eq("BungeeCord"), any(byte[].class));
            misc.verifyNoInteractions();
            connector.onPluginMessageReceived("BungeeCord", player, servers("hub, login"));
            verify(logger).warning(contains("您配置的lobbyServer不存在！"));
            text.verify(() -> AdventureText.send(player, "§c执行操作时发生异常，请联系服务器管理员"));
            misc.verifyNoInteractions();
            misc.when(() -> Misc.connectToProxyServer(player, "login")).thenReturn(true);
            assertTrue(connector.connect(player, " login "));
            connector.onPluginMessageReceived("BungeeCord", player, servers("hub, login"));
            misc.verify(() -> Misc.connectToProxyServer(player, "login"));
            verify(timeout, times(2)).cancel();
        }
    }

    @Test
    void malformedReplyAndNoProxyResponseTimeoutWithoutConnecting() {
        BedWars plugin = mock(BedWars.class);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        AtomicReference<Runnable> task = new AtomicReference<>();
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(60L))).thenAnswer(call -> {
            task.set(call.getArgument(1));
            return mock(BukkitTask.class);
        });
        try (var bukkit = mockStatic(Bukkit.class); var text = mockStatic(AdventureText.class); var misc = mockStatic(Misc.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            ProxyLobbyConnector connector = new ProxyLobbyConnector(plugin);
            connector.connect(player, "hub");
            connector.onPluginMessageReceived("BungeeCord", player, new byte[]{0});
            task.get().run();
            verify(logger).warning(contains("代理未响应 GetServers"));
            text.verify(() -> AdventureText.send(player, "§c执行操作时发生异常，请联系服务器管理员"));
            misc.verifyNoInteractions();
            assertTrue(connector.connect(player, "hub"));
            connector.onQuit(new org.bukkit.event.player.PlayerQuitEvent(player, (net.kyori.adventure.text.Component) null));
            task.get().run();
            verify(logger, times(1)).warning(contains("代理未响应 GetServers"));
        }
    }

    private static byte[] servers(String servers) throws Exception {
        var output = ByteStreams.newDataOutput();
        output.writeUTF("GetServers");
        output.writeUTF(servers);
        byte[] data = output.toByteArray();
        try (var input = new DataInputStream(new ByteArrayInputStream(data))) { assertEquals("GetServers", input.readUTF()); }
        return data;
    }
}
