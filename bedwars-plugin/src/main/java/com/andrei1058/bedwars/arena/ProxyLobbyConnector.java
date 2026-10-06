/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理用于返回代理大厅的 BungeeCord 兼容出站通道。
 * 返回操作先通过 {@code GetServers} 校验目标，只向玩家显示通用错误，不暴露代理配置。
 */
public final class ProxyLobbyConnector implements PluginMessageListener, Listener {

    public static final String CHANNEL = "BungeeCord";

    private final BedWars plugin;
    private final Map<UUID, PendingConnect> pending = new HashMap<>();

    public ProxyLobbyConnector(@NotNull BedWars plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void close() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        pending.values().forEach(request -> request.timeout().cancel());
        pending.clear();
    }

    /** 校验配置后返回代理大厅；true 表示请求已提交，不代表切服成功。 */
    public boolean connect(@NotNull Player player) {
        return Misc.connectToProxyLobby(player);
    }

    public boolean connect(@NotNull Player player, String serverName) {
        if (!player.isOnline()) return false;
        if (serverName == null || serverName.isBlank()) {
            fail(player, "您配置的lobbyServer不存在！目标服务器名称为空。");
            return false;
        }
        if (pending.containsKey(player.getUniqueId())) return true;
        String target = serverName.trim();
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingConnect request = pending.remove(player.getUniqueId());
            if (request != null && player.isOnline()) {
                fail(player, "无法验证 lobbyServer=" + target + "：代理未响应 GetServers，请检查代理通道和连接方式。");
            }
        }, 60L);
        pending.put(player.getUniqueId(), new PendingConnect(target, timeout));
        try {
            var output = ByteStreams.newDataOutput();
            output.writeUTF("GetServers");
            player.sendPluginMessage(plugin, CHANNEL, output.toByteArray());
            return true;
        } catch (RuntimeException exception) {
            discard(player.getUniqueId());
            fail(player, "无法查询代理服务器列表：" + exception.getMessage());
            return false;
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!CHANNEL.equals(channel) || !pending.containsKey(player.getUniqueId())) return;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            if (!"GetServers".equals(input.readUTF())) return;
            String servers = input.readUTF();
            PendingConnect request = pending.remove(player.getUniqueId());
            request.timeout().cancel();
            if (!player.isOnline()) return;
            if (!serverExists(servers, request.serverName())) {
                fail(player, "您配置的lobbyServer不存在！请将 lobbyServer=" + request.serverName()
                        + " 改为代理服务器列表中的正确键名。");
            } else if (!Misc.connectToProxyServer(player, request.serverName())) {
                fail(player, "无法发送玩家到配置的 lobbyServer=" + request.serverName() + '。');
            }
        } catch (IOException exception) {
            // 不消费格式错误的响应，保留超时保护。
            plugin.getLogger().warning("代理 GetServers 响应格式无效。");
        }
    }

    static boolean serverExists(String servers, String target) {
        for (String server : servers.split(",")) if (server.trim().equals(target)) return true;
        return false;
    }

    private void fail(Player player, String diagnostic) {
        plugin.getLogger().warning(diagnostic);
        AdventureText.send(player, "§c执行操作时发生异常，请联系服务器管理员");
    }

    private void discard(UUID playerId) {
        PendingConnect request = pending.remove(playerId);
        if (request != null) request.timeout().cancel();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        discard(event.getPlayer().getUniqueId());
    }

    private record PendingConnect(String serverName, BukkitTask timeout) { }
}
