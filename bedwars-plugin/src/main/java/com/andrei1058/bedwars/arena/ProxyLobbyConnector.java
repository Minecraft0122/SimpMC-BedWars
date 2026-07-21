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
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validates the BungeeCord-compatible plugin messaging route before moving a
 * lobby player. The protocol does not acknowledge {@code Connect}, so a plain
 * connect request would otherwise fail silently when Velocity compatibility is
 * disabled or {@code lobbyServer} does not match a proxy server name.
 */
public final class ProxyLobbyConnector implements PluginMessageListener {

    public static final String CHANNEL = "BungeeCord";
    private static final long RESPONSE_TIMEOUT_TICKS = 60L;

    private final BedWars plugin;
    private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();

    public ProxyLobbyConnector(@NotNull BedWars plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void close() {
        pendingRequests.clear();
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    /**
     * Validate the proxy route and configured lobby server, then connect.
     */
    public boolean connectWithDiagnostics(@NotNull Player player) {
        String requestedServer = configuredLobbyServer();
        if (requestedServer == null) {
            plugin.getLogger().warning("无法返回代理大厅：config.yml 的 lobbyServer 为空。");
            player.sendMessage(ChatColor.RED + "代理大厅服务器名称未配置，请联系管理员。");
            return false;
        }

        UUID playerId = player.getUniqueId();
        if (pendingRequests.putIfAbsent(playerId, new PendingRequest(requestedServer)) != null) {
            player.sendMessage(ChatColor.YELLOW + "正在检查代理大厅连接，请稍候……");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "正在检查代理大厅“" + requestedServer + "”……");
        try {
            player.sendPluginMessage(plugin, CHANNEL, requestPayload("GetServer"));
            player.sendPluginMessage(plugin, CHANNEL, requestPayload("GetServers"));
        } catch (RuntimeException exception) {
            pendingRequests.remove(playerId);
            plugin.getLogger().warning("无法发送代理查询消息：" + exception.getMessage());
            player.sendMessage(ChatColor.RED + "无法使用代理消息通道，请联系管理员检查代理配置。");
            return false;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> handleTimeout(playerId), RESPONSE_TIMEOUT_TICKS);
        return true;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!CHANNEL.equals(channel)) return;

        PendingRequest request = pendingRequests.get(player.getUniqueId());
        if (request == null) return;

        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String subChannel = input.readUTF();
            if ("GetServer".equals(subChannel)) {
                request.currentServer = input.readUTF().trim();
                request.receivedCurrentServer = true;
            } else if ("GetServers".equals(subChannel)) {
                request.availableServers = parseServerNames(input.readUTF());
                request.receivedAvailableServers = true;
            } else {
                return;
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("代理返回了无法解析的 BungeeCord 插件消息：" + exception.getMessage());
            return;
        }

        completeIfReady(player, request);
    }

    private void completeIfReady(Player player, PendingRequest request) {
        if (!request.receivedCurrentServer || !request.receivedAvailableServers) return;
        if (!pendingRequests.remove(player.getUniqueId(), request)) return;

        String targetServer = resolveServerName(request.requestedServer, request.availableServers);
        if (targetServer == null) {
            String available = request.availableServers.isEmpty()
                    ? "无"
                    : String.join(", ", request.availableServers);
            player.sendMessage(ChatColor.RED + "无法返回主大厅：代理中不存在服务器“"
                    + request.requestedServer + "”。");
            player.sendMessage(ChatColor.GRAY + "请把 config.yml 的 lobbyServer 改为代理 [servers] 中的名称。可用："
                    + available);
            plugin.getLogger().warning("代理大厅名称无效：lobbyServer=" + request.requestedServer
                    + "，代理返回的服务器列表=" + available);
            return;
        }

        if (targetServer.equalsIgnoreCase(request.currentServer)) {
            player.sendMessage(ChatColor.YELLOW + "你已经位于代理大厅“" + targetServer + "”。");
            return;
        }

        if (!targetServer.equals(request.requestedServer)) {
            plugin.getLogger().warning("lobbyServer 的大小写与代理配置不一致，已按代理名称“"
                    + targetServer + "”连接；建议修正 config.yml。");
        }

        player.sendMessage(ChatColor.GREEN + "正在连接到主大厅“" + targetServer + "”……");
        player.sendPluginMessage(plugin, CHANNEL, Misc.proxyConnectPayload(targetServer));
    }

    private void handleTimeout(UUID playerId) {
        PendingRequest request = pendingRequests.remove(playerId);
        if (request == null) return;

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.RED + "代理没有响应，无法返回主大厅。");
            player.sendMessage(ChatColor.GRAY
                    + "请确认通过代理地址进入；Velocity 还需在 velocity.toml 的 [advanced] 中设置 "
                    + "bungee-plugin-message-channel = true。");
        }
        plugin.getLogger().warning("玩家 " + playerId + " 的代理大厅查询超时。请检查是否绕过代理直连后端，"
                + "以及 Velocity 的 [advanced].bungee-plugin-message-channel 是否为 true。");
    }

    @Nullable
    private String configuredLobbyServer() {
        String configured = BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_SERVER, "hub");
        if (configured == null || configured.isBlank()) return null;
        return configured.trim();
    }

    static byte[] requestPayload(String subChannel) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(subChannel);
        return output.toByteArray();
    }

    static List<String> parseServerNames(String rawServers) {
        if (rawServers == null || rawServers.isBlank()) return List.of();

        LinkedHashSet<String> serverNames = new LinkedHashSet<>();
        for (String serverName : rawServers.split(",")) {
            String trimmed = serverName.trim();
            if (!trimmed.isEmpty()) serverNames.add(trimmed);
        }
        return new ArrayList<>(serverNames);
    }

    @Nullable
    static String resolveServerName(String requestedServer, Collection<String> availableServers) {
        for (String availableServer : availableServers) {
            if (availableServer.equals(requestedServer)) return availableServer;
        }
        for (String availableServer : availableServers) {
            if (availableServer.equalsIgnoreCase(requestedServer)) return availableServer;
        }
        return null;
    }

    private static final class PendingRequest {
        private final String requestedServer;
        private String currentServer = "";
        private List<String> availableServers = List.of();
        private boolean receivedCurrentServer;
        private boolean receivedAvailableServers;

        private PendingRequest(String requestedServer) {
            this.requestedServer = requestedServer;
        }
    }
}
