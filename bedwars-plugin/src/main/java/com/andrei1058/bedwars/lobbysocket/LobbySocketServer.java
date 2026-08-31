/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.lobbysocket;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * TCP listener used by a BUNGEE-LOBBY node. It accepts only authenticated
 * arena-node sessions and keeps all Bukkit work outside the socket threads.
 */
public final class LobbySocketServer implements AutoCloseable {

    private final BedWars plugin;
    private final ArenaDirectory directory = new ArenaDirectory();
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "SimpMC-BedWars-lobby-socket");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<String, NodeSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, NodeSession> sessionsByServer = new ConcurrentHashMap<>();
    private volatile Consumer<ReadyMessage> readyHandler = ignored -> {
    };
    private volatile ServerSocket listener;
    private volatile boolean running;

    public LobbySocketServer(BedWars plugin) {
        this.plugin = plugin;
    }

    public ArenaDirectory directory() {
        return directory;
    }

    public void setReadyHandler(Consumer<ReadyMessage> readyHandler) {
        this.readyHandler = readyHandler == null ? ignored -> {
        } : readyHandler;
    }

    /** Start binding without making the Bukkit enable thread wait on a socket. */
    public synchronized void start() {
        if (running) return;
        running = true;
        ioExecutor.execute(this::bindAndAccept);
    }

    private void bindAndAccept() {
        String host = BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_LISTEN_HOST, "0.0.0.0");
        int port = BedWars.config.getYml().getInt(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_LISTEN_PORT, 2019);
        if (host == null || host.isBlank()) host = "0.0.0.0";
        if (port < 1 || port > 65535) {
            plugin.getLogger().severe("LOBBY 套接字端口无效（必须为 1-65535）：" + port);
            running = false;
            return;
        }
        String socketSecret = BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_SOCKET_SECRET, "");
        if (socketSecret == null || socketSecret.isBlank()) {
            plugin.getLogger().warning(
                    "LOBBY 套接字当前使用空共享密钥；请设置随机 socket-secret 并限制监听端口来源。空密钥仅保留旧版内网兼容。 ");
        }
        ServerSocket bound = null;
        try {
            bound = new ServerSocket();
            bound.setReuseAddress(true);
            bound.bind(new InetSocketAddress(host.trim(), port));
            listener = bound;
            plugin.getLogger().info("LOBBY 套接字已监听 " + host.trim() + ":" + port + "，等待竞技场节点连接。");
            while (running) {
                Socket socket = bound.accept();
                socket.setTcpNoDelay(true);
                ioExecutor.execute(() -> new NodeSession(socket).run());
            }
        } catch (IOException exception) {
            if (running) {
                plugin.getLogger().log(Level.SEVERE, "无法启动 LOBBY 套接字监听器。请检查端口是否被占用。", exception);
            }
        } finally {
            if (listener == bound) {
                listener = null;
            }
        }
    }

    private NodeSession sessionFor(ArenaNodeSnapshot snapshot) {
        if (snapshot == null) return null;
        NodeSession session = sessions.get(snapshot.sessionId());
        return session != null && session.isOpen() ? session : null;
    }

    boolean isSessionOpen(ArenaNodeSnapshot snapshot) {
        return sessionFor(snapshot) != null;
    }

    CompletableFuture<Boolean> send(ArenaNodeSnapshot snapshot, String message) {
        NodeSession session = sessionFor(snapshot);
        if (session == null) return CompletableFuture.completedFuture(false);
        return session.queueSend(message);
    }

    /** Write a final control message before the listener is closed. */
    boolean sendNow(ArenaNodeSnapshot snapshot, String message) {
        NodeSession session = sessionFor(snapshot);
        if (session == null) return false;
        try {
            /* Shutdown is initiated from Bukkit's main thread. Bound the
             * wait so a stalled child cannot block plugin disable forever. */
            return Boolean.TRUE.equals(session.queueSend(message).get(250, TimeUnit.MILLISECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException exception) {
            return false;
        }
    }

    private void register(NodeSession session) {
        sessions.put(session.sessionId, session);
        NodeSession previous = sessionsByServer.put(session.serverId, session);
        if (previous != null && previous != session) previous.close("同一 server-id 的新连接已建立");
    }

    private void unregister(NodeSession session) {
        sessions.remove(session.sessionId, session);
        sessionsByServer.remove(session.serverId, session);
        directory.removeSession(session.sessionId);
    }

    private void handleReady(NodeSession session, JsonObject json) {
        if (!json.has("request_id") || !json.has("uuid")) return;
        String requestId = string(json, "request_id");
        String uuid = string(json, "uuid");
        if (requestId.isBlank() || uuid.isBlank()) return;
        boolean accepted = booleanValue(json, "accepted", false);
        String reason = string(json, "reason");
        readyHandler.accept(new ReadyMessage(requestId, uuid, accepted, reason, session.sessionId));
    }

    private final class NodeSession {
        private final Socket socket;
        private final String sessionId = UUID.randomUUID().toString();
        private volatile boolean open = true;
        private String serverId = "";
        private String proxyServer = "";
        private String nodeInstanceId = "";
        private int protocolVersion;
        private PrintWriter writer;
        private BufferedReader reader;
        private int invalidMessages;
        /** Serializes PLD, cancellation, heartbeat and status responses. */
        private final Object sendOrderLock = new Object();
        private CompletableFuture<Boolean> sendTail = CompletableFuture.completedFuture(true);

        private NodeSession(Socket socket) {
            this.socket = socket;
        }

        private void run() {
            try {
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                socket.setSoTimeout(5_000);
                String hello = ArenaSocket.readLimitedMessage(reader, ArenaSocket.MAX_INCOMING_MESSAGE_LENGTH);
                if (!authenticate(hello)) return;
                socket.setSoTimeout(0);
                register(this);
                write(welcomeMessage());
                write(requestSnapshotMessage());
                while (running && open) {
                    String message = ArenaSocket.readLimitedMessage(reader, ArenaSocket.MAX_INCOMING_MESSAGE_LENGTH);
                    if (message == null) break;
                    if (message.isBlank()) continue;
                    handleMessage(message);
                }
            } catch (IOException exception) {
                if (open && running) {
                    BedWars.debug("LOBBY 节点套接字断开：" + exception.getMessage());
                }
            } finally {
                close("连接结束");
            }
        }

        private boolean authenticate(String payload) {
            if (payload == null || payload.isBlank()) {
                reject("缺少 HELLO");
                return false;
            }
            final JsonObject json;
            try {
                JsonElement element = new JsonParser().parse(payload);
                if (!element.isJsonObject()) throw new JsonSyntaxException("not an object");
                json = element.getAsJsonObject();
            } catch (RuntimeException exception) {
                reject("HELLO 不是有效 JSON");
                return false;
            }
            if (!"HELLO".equalsIgnoreCase(string(json, "type"))) {
                reject("首条消息必须为 HELLO");
                return false;
            }
            protocolVersion = intValue(json, "protocol_version", 1);
            if (protocolVersion < 1 || protocolVersion > BungeeProtocol.CURRENT_VERSION) {
                reject("不支持的协议版本");
                return false;
            }
            if (!BungeeProtocol.ARENA_ROLE.equalsIgnoreCase(string(json, "role"))) {
                reject("只接受 ARENA 节点");
                return false;
            }
            serverId = boundedString(json, "server_id", 128);
            nodeInstanceId = boundedString(json, "node_instance_id", 128);
            proxyServer = boundedString(json, "proxy_server", 128);
            if (serverId.isBlank() || nodeInstanceId.isBlank()) {
                reject("HELLO 缺少节点身份");
                return false;
            }
            if (proxyServer.isBlank()) proxyServer = serverId;
            String expectedSecret = BedWars.config.getYml().getString(
                    ConfigPath.GENERAL_CONFIGURATION_BUNGEE_SOCKET_SECRET, "");
            String suppliedSecret = string(json, "secret");
            if (!constantTimeEquals(expectedSecret == null ? "" : expectedSecret, suppliedSecret)) {
                reject("共享密钥不匹配");
                return false;
            }
            return true;
        }

        private void handleMessage(String payload) {
            final JsonObject json;
            try {
                JsonElement element = new JsonParser().parse(payload);
                if (!element.isJsonObject()) throw new JsonSyntaxException("not an object");
                json = element.getAsJsonObject();
            } catch (RuntimeException exception) {
                invalid("消息不是有效 JSON");
                return;
            }
            String type = string(json, "type").toUpperCase(Locale.ROOT);
            switch (type) {
                case "UPDATE" -> handleUpdate(json);
                case "REMOVE" -> handleRemove(json);
                case "PLD_READY" -> handleReady(this, json);
                case "PING", "HEARTBEAT" -> queueSend(pongMessage());
                default -> {
                    // Keep the protocol forward-compatible with old proxy
                    // integrations; unknown messages are simply ignored.
                }
            }
        }

        private void handleUpdate(JsonObject json) {
            int advertisedProtocol = intValue(json, "protocol_version", protocolVersion);
            if (advertisedProtocol != protocolVersion) {
                invalid("UPDATE 的 protocol_version 与 HELLO 不一致");
                return;
            }
            String messageServer = boundedString(json, "server_name", 128);
            if (!messageServer.isBlank() && !messageServer.equals(serverId)) {
                invalid("UPDATE 的 server_id 与 HELLO 不一致");
                return;
            }
            String arenaName = boundedString(json, "arena_name", 128);
            String arenaIdentifier = boundedString(json, "arena_identifier", 256);
            String status = boundedString(json, "arena_status", 32).toUpperCase(Locale.ROOT);
            String group = boundedString(json, "arena_group", 64).toUpperCase(Locale.ROOT);
            if (arenaName.isBlank() || arenaIdentifier.isBlank() || group.isBlank()
                    || !(status.equals("WAITING") || status.equals("STARTING")
                    || status.equals("PLAYING") || status.equals("RESTARTING"))) {
                invalid("UPDATE 缺少有效竞技场字段");
                return;
            }
            int currentPlayers = boundedInt(json, "arena_current_players", 0, 10_000);
            int maxPlayers = boundedInt(json, "arena_max_players", 1, 10_000);
            int maxInTeam = boundedInt(json, "arena_max_in_team", 1, 1_000);
            long sequence = longValue(json, "sequence", System.currentTimeMillis());
            if (sequence < 0 || currentPlayers > maxPlayers || maxInTeam > maxPlayers) {
                invalid("UPDATE 的人数或序号字段无效");
                return;
            }
            String advertisedInstance = boundedString(json, "node_instance_id", 128);
            if (!advertisedInstance.isBlank() && !advertisedInstance.equals(nodeInstanceId)) {
                invalid("UPDATE 的 node_instance_id 与 HELLO 不一致");
                return;
            }
            Set<String> groups = new HashSet<>();
            groups.add(group);
            JsonElement groupElement = json.get("arena_groups");
            if (groupElement != null && groupElement.isJsonArray()) {
                JsonArray array = groupElement.getAsJsonArray();
                for (JsonElement value : array) {
                    if (value != null && value.isJsonPrimitive()) {
                        String normalized = value.getAsString().trim();
                        if (!normalized.isBlank() && normalized.length() <= 64) {
                            groups.add(normalized.toUpperCase(Locale.ROOT));
                        }
                    }
                }
            }
            directory.upsert(new ArenaNodeSnapshot(sessionId, serverId, proxyServer, nodeInstanceId,
                    arenaName, arenaIdentifier, status, currentPlayers, maxPlayers, maxInTeam,
                    groups, booleanValue(json, "spectate", false), sequence,
                    System.currentTimeMillis(), protocolVersion >= BungeeProtocol.CURRENT_VERSION));
        }

        private void handleRemove(JsonObject json) {
            int advertisedProtocol = intValue(json, "protocol_version", protocolVersion);
            if (advertisedProtocol != protocolVersion) {
                invalid("REMOVE 的 protocol_version 与 HELLO 不一致");
                return;
            }
            String advertisedInstance = boundedString(json, "node_instance_id", 128);
            if (!advertisedInstance.isBlank() && !advertisedInstance.equals(nodeInstanceId)) {
                invalid("REMOVE 的 node_instance_id 与 HELLO 不一致");
                return;
            }
            String arenaIdentifier = boundedString(json, "arena_identifier", 256);
            if (arenaIdentifier.isBlank()) {
                invalid("REMOVE 缺少竞技场标识");
                return;
            }
            directory.remove(sessionId, nodeInstanceId, arenaIdentifier);
        }

        private String welcomeMessage() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "WELCOME");
            json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
            json.addProperty("session_id", sessionId);
            return json.toString();
        }

        private String pongMessage() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "PONG");
            json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
            return json.toString();
        }

        private String requestSnapshotMessage() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "REQUEST_SNAPSHOT");
            json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
            return json.toString();
        }

        /**
         * Append one line to this session's send chain. Every continuation
         * runs on the shared I/O executor, while the chain itself guarantees
         * that a cancellation cannot overtake its corresponding PLD message.
         */
        private CompletableFuture<Boolean> queueSend(String message) {
            if (message == null || message.isBlank()) return CompletableFuture.completedFuture(false);
            synchronized (sendOrderLock) {
                CompletableFuture<Boolean> next = sendTail
                        .handle((ignored, error) -> null)
                        .thenApplyAsync(ignored -> write(message), ioExecutor);
                sendTail = next.handle((ignored, error) -> true);
                return next;
            }
        }

        private synchronized boolean write(String message) {
            if (!open || writer == null || message == null || message.isBlank()) return false;
            writer.println(message);
            return !writer.checkError();
        }

        private boolean isOpen() {
            return open && !socket.isClosed();
        }

        private void reject(String reason) {
            if (writer != null) {
                JsonObject json = new JsonObject();
                json.addProperty("type", "REJECT");
                json.addProperty("reason", reason);
                write(json.toString());
            }
            close(reason);
        }

        private void invalid(String reason) {
            invalidMessages++;
            if (invalidMessages >= 3) close(reason);
        }

        private synchronized void close(String reason) {
            if (!open) return;
            open = false;
            unregister(this);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static String boundedString(JsonObject json, String key, int maxLength) {
        String value = string(json, key).trim();
        if (value.length() > maxLength || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) return "";
        return value;
    }

    private static int intValue(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int boundedInt(JsonObject json, String key, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, intValue(json, key, minimum)));
    }

    private static long longValue(JsonObject json, String key, long fallback) {
        try {
            return json.has(key) ? json.get(key).getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean booleanValue(JsonObject json, String key, boolean fallback) {
        try {
            return json.has(key) ? json.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                (supplied == null ? "" : supplied).getBytes(StandardCharsets.UTF_8));
    }

    public record ReadyMessage(String requestId, String uuid, boolean accepted, String reason, String sessionId) {
    }

    @Override
    public synchronized void close() {
        if (!running && listener == null && ioExecutor.isShutdown()) return;
        running = false;
        ServerSocket bound = listener;
        listener = null;
        if (bound != null) {
            try {
                bound.close();
            } catch (IOException ignored) {
            }
        }
        for (NodeSession session : sessions.values()) session.close("大厅关闭");
        sessions.clear();
        sessionsByServer.clear();
        directory.snapshot().forEach(snapshot -> directory.removeSession(snapshot.sessionId()));
        ioExecutor.shutdownNow();
    }
}
