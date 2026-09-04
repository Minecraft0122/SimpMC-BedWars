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
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.ReJoin;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * v2 TCP client used by a 1.8.x arena node to communicate with a 1.21.11
 * lobby. Messages are newline-delimited UTF-8 JSON. Bukkit state is always
 * read or changed on the primary thread; socket I/O is kept asynchronous.
 */
public final class ArenaSocket {

    /** Maximum accepted line length; protects the reader from unbounded input. */
    static final int MAX_INCOMING_MESSAGE_LENGTH = 65_536;
    public static final List<String> lobbies = Collections.synchronizedList(new ArrayList<String>());
    private static final ConcurrentHashMap<String, RemoteLobby> sockets =
            new ConcurrentHashMap<String, RemoteLobby>();
    private static final Set<String> connecting = Collections.newSetFromMap(
            new ConcurrentHashMap<String, Boolean>());
    private static final String NODE_INSTANCE_ID = UUID.randomUUID().toString();
    private static final AtomicLong messageSequence = new AtomicLong();

    private ArenaSocket() {
    }

    /**
     * Send arena data to every configured lobby. A connection is established
     * lazily so a temporarily unavailable lobby never blocks a server tick.
     */
    public static void sendMessage(final String message) {
        if (message == null || message.trim().isEmpty() || BedWars.isShuttingDown()) return;

        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, new Runnable() {
                @Override
                public void run() {
                    sendMessage(message);
                }
            });
            return;
        }

        List<String> configuredLobbies;
        synchronized (lobbies) {
            configuredLobbies = new ArrayList<String>(lobbies);
        }

        for (String endpoint : configuredLobbies) {
            if (endpoint == null || endpoint.trim().isEmpty()) continue;
            String[] parts = endpoint.trim().split(":", 2);
            if (parts.length != 2 || parts[0].trim().isEmpty() || !Misc.isNumber(parts[1].trim())) {
                BedWars.debug("Ignoring invalid lobby socket endpoint: " + endpoint);
                continue;
            }

            int port;
            try {
                port = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (port < 1 || port > 65535) continue;

            RemoteLobby connected = sockets.get(endpoint);
            if (connected != null && connected.isOpen()) {
                connected.sendMessage(message);
                continue;
            }

            if (!connecting.add(endpoint)) continue;
            try {
                Socket socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(parts[0].trim(), port), 2_000);
                RemoteLobby remote = new RemoteLobby(socket, endpoint);
                // Do not publish this connection until HELLO is on the wire.
                // Otherwise a concurrent status event can become the first
                // line and the lobby must reject the session.
                if (remote.isOpen() && remote.sendMessage(formatHelloMessage())) {
                    RemoteLobby previous = sockets.put(endpoint, remote);
                    if (previous != null) previous.disable();
                    remote.startReader();
                    remote.sendMessage(message);
                } else {
                    socket.close();
                }
            } catch (IOException exception) {
                BedWars.debug("Could not connect to lobby socket " + endpoint + ": " + exception.getMessage());
            } finally {
                connecting.remove(endpoint);
            }
        }
    }

    /** Format one arena status snapshot for the v2 lobby directory. */
    public static String formatUpdateMessage(IArena arena) {
        if (arena == null || isBlank(arena.getWorldName())) return "";
        String arenaName = isBlank(arena.getArenaName()) ? arena.getWorldName() : arena.getArenaName();
        String group = isBlank(arena.getGroup()) ? "Default" : arena.getGroup();
        String status = arena.getStatus() == null ? "RESTARTING" : arena.getStatus().toString();
        JsonObject json = new JsonObject();
        json.addProperty("type", "UPDATE");
        json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        json.addProperty("message_id", UUID.randomUUID().toString());
        json.addProperty("node_instance_id", NODE_INSTANCE_ID);
        json.addProperty("sequence", messageSequence.incrementAndGet());
        json.addProperty("server_name", configuredServerId());
        json.addProperty("proxy_server", configuredProxyServer());
        json.addProperty("arena_name", arenaName);
        json.addProperty("arena_identifier", arena.getWorldName());
        json.addProperty("arena_status", status.toUpperCase(Locale.ROOT));
        json.addProperty("arena_current_players", arena.getPlayers().size());
        json.addProperty("arena_max_players", arena.getMaxPlayers());
        json.addProperty("arena_max_in_team", arena.getMaxInTeam());
        json.addProperty("arena_group", group.toUpperCase(Locale.ROOT));
        JsonArray groups = new JsonArray();
        groups.add(new JsonPrimitive(group.toUpperCase(Locale.ROOT)));
        json.add("arena_groups", groups);
        json.addProperty("spectate", arena.isAllowSpectate());
        return json.toString();
    }

    /** Publish a reconnect lease to every authenticated lobby. */
    public static String formatRejoinCreateMessage(ReJoin rejoin) {
        if (rejoin == null || rejoin.getPl() == null || rejoin.getArena() == null
                || isBlank(rejoin.getArena().getWorldName())) return "";
        JsonObject json = new JsonObject();
        json.addProperty("type", "RC");
        json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        json.addProperty("reservation_id", rejoin.getReservationId());
        json.addProperty("uuid", rejoin.getPl().toString());
        json.addProperty("arena_id", rejoin.getArena().getWorldName());
        json.addProperty("server", configuredServerId());
        json.addProperty("expires_at", rejoin.getExpiresAtMillis());
        json.addProperty("proxy_server", configuredProxyServer());
        return json.toString();
    }

    /** Publish removal of a reconnect lease to every authenticated lobby. */
    public static String formatRejoinRemoveMessage(ReJoin rejoin) {
        if (rejoin == null || rejoin.getPl() == null) return "";
        JsonObject json = new JsonObject();
        json.addProperty("type", "RD");
        json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        json.addProperty("reservation_id", rejoin.getReservationId());
        json.addProperty("uuid", rejoin.getPl().toString());
        json.addProperty("server", configuredServerId());
        return json.toString();
    }

    /** Tell the lobby to remove one runtime arena copy. */
    public static String formatRemoveMessage(String arenaIdentifier) {
        if (isBlank(arenaIdentifier)) return "";
        JsonObject json = new JsonObject();
        json.addProperty("type", "REMOVE");
        json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        json.addProperty("node_instance_id", NODE_INSTANCE_ID);
        json.addProperty("sequence", messageSequence.incrementAndGet());
        json.addProperty("server_name", configuredServerId());
        json.addProperty("arena_identifier", arenaIdentifier.trim());
        return json.toString();
    }

    /** Initial authenticated handshake sent to a lobby socket. */
    public static String formatHelloMessage() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "HELLO");
        json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        json.addProperty("role", BungeeProtocol.ARENA_ROLE);
        json.addProperty("node_instance_id", NODE_INSTANCE_ID);
        json.addProperty("server_id", configuredServerId());
        json.addProperty("proxy_server", configuredProxyServer());
        json.addProperty("secret", configuredSocketSecret());
        return json.toString();
    }

    private static String configuredServerId() {
        if (BedWars.config == null) return "bw1";
        String value = BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID, "bw1");
        return isBlank(value) ? "bw1" : value.trim();
    }

    private static String configuredProxyServer() {
        if (BedWars.config == null) return "bw1";
        String value = BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_PROXY_SERVER, "");
        return isBlank(value) ? configuredServerId() : value.trim();
    }

    private static String configuredSocketSecret() {
        if (BedWars.config == null) return "";
        String value = BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_SOCKET_SECRET, "");
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class RemoteLobby {
        private final Socket socket;
        private final String endpoint;
        private final PrintWriter writer;
        private final BufferedReader reader;
        private volatile boolean open = true;
        private volatile boolean authenticated;
        private int invalidMessages;

        private RemoteLobby(Socket socket, String endpoint) throws IOException {
            this.socket = socket;
            this.endpoint = endpoint;
            this.writer = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);
            this.reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            socket.setSoTimeout(5_000);
            BedWars.debug("Connected to lobby socket " + endpoint + " " + socket);
        }

        private void startReader() {
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, new Runnable() {
                @Override
                public void run() {
                    readLoop();
                }
            });
        }

        private boolean isOpen() {
            return open && !socket.isClosed();
        }

        private void readLoop() {
            while (isOpen()) {
                try {
                    String payload = readLimitedMessage(reader, MAX_INCOMING_MESSAGE_LENGTH);
                    if (payload == null) {
                        disable();
                        return;
                    }
                    if (payload.trim().isEmpty()) continue;
                    JsonObject json;
                    try {
                        JsonElement element = new JsonParser().parse(payload);
                        if (!element.isJsonObject()) throw new JsonSyntaxException("not an object");
                        json = element.getAsJsonObject();
                    } catch (RuntimeException exception) {
                        invalid("malformed JSON");
                        continue;
                    }
                    String type = value(json, "type").toUpperCase(Locale.ROOT);
                    if (type.isEmpty()) {
                        invalid("message has no type");
                        continue;
                    }

                    if (!authenticated) {
                        if ("WELCOME".equals(type) && acceptWelcome(json)) {
                            try {
                                socket.setSoTimeout(0);
                            } catch (IOException ignored) {
                            }
                        } else if ("REJECT".equals(type)) {
                            invalid("lobby rejected HELLO");
                            disable();
                        } else {
                            invalid("expected WELCOME before control messages");
                        }
                        continue;
                    }

                    handleMessage(type, json);
                } catch (SocketTimeoutException exception) {
                    invalid("lobby handshake timed out");
                    disable();
                } catch (IOException exception) {
                    if (isOpen()) BedWars.debug("Lobby socket closed " + endpoint + ": " + exception.getMessage());
                    disable();
                } catch (RuntimeException exception) {
                    invalid("malformed control message");
                }
            }
        }

        private boolean acceptWelcome(JsonObject json) {
            int protocol = intValue(json, "protocol_version", 0);
            String sessionId = value(json, "session_id").trim();
            if (protocol < 1 || protocol > BungeeProtocol.CURRENT_VERSION || isBlank(sessionId)) {
                invalid("WELCOME has invalid protocol or session id");
                return false;
            }
            try {
                UUID.fromString(sessionId);
            } catch (IllegalArgumentException exception) {
                invalid("WELCOME has invalid session id");
                return false;
            }
            authenticated = true;
            return true;
        }

        private void handleMessage(String type, JsonObject json) {
            switch (type) {
                case "REQUEST_SNAPSHOT":
                    Bukkit.getScheduler().runTask(BedWars.plugin, new Runnable() {
                        @Override
                        public void run() {
                            List<String> snapshot = new ArrayList<String>();
                            for (IArena arena : Arena.getArenas()) {
                                snapshot.add(formatUpdateMessage(arena));
                            }
                            for (ReJoin rejoin : new ArrayList<ReJoin>(ReJoin.getReJoinList())) {
                                if (rejoin != null && rejoin.isLeaseActive()) {
                                    snapshot.add(formatRejoinCreateMessage(rejoin));
                                }
                            }
                            sendMessagesAsync(snapshot);
                        }
                    });
                    break;
                case "PLD":
                    handlePreload(json);
                    break;
                case "PLD_CANCEL":
                    handlePreloadCancel(json);
                    break;
                case "PING":
                case "HEARTBEAT":
                    JsonObject pong = new JsonObject();
                    pong.addProperty("type", "PONG");
                    pong.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
                    sendMessage(pong.toString());
                    break;
                case "Q":
                    handlePlayerQuery(json);
                    break;
                default:
                    break;
            }
        }

        private void handlePreload(JsonObject json) {
            if (!hasStringFields(json, "uuid", "arena_identifier", "lang_iso", "target")) {
                invalid("PLD is missing fields");
                return;
            }
            final String uuid = bounded(json, "uuid", 64);
            final String arenaIdentifier = bounded(json, "arena_identifier", 256);
            final String language = bounded(json, "lang_iso", 32);
            final String target = bounded(json, "target", 128);
            final String joinMode = bounded(json, "join_mode", 16).toUpperCase(Locale.ROOT);
            final String requestId = bounded(json, "request_id", 128);
            final String reservationId = bounded(json, "reservation_id", 128);
            if (isBlank(uuid) || isBlank(arenaIdentifier) || isBlank(language)
                    || (!joinMode.isEmpty() && !"AUTO".equals(joinMode)
                    && !"PLAYER".equals(joinMode) && !"SPECTATOR".equals(joinMode)
                    && !"REJOIN".equals(joinMode))) {
                invalid("PLD has invalid fields");
                return;
            }

            final UUID requestedUuid;
            try {
                requestedUuid = UUID.fromString(uuid);
            } catch (IllegalArgumentException exception) {
                invalid("PLD contains an invalid UUID");
                return;
            }

            Bukkit.getScheduler().runTask(BedWars.plugin, new Runnable() {
                @Override
                public void run() {
                    IArena arena = Arena.getArenaByIdentifier(arenaIdentifier);
                    boolean rejoin = "REJOIN".equals(joinMode);
                    ReJoin lease = rejoin ? ReJoin.getPlayer(requestedUuid) : null;
                    boolean spectator = "SPECTATOR".equals(joinMode)
                            || ("AUTO".equals(joinMode) && arena != null
                            && arena.getStatus() == GameState.playing);
                    boolean accepted = arena != null && (rejoin
                            ? arena.getStatus() == GameState.playing
                            && lease != null && lease.getArena() == arena
                            && (isBlank(reservationId) || reservationId.equals(lease.getReservationId()))
                            && lease.canReJoin()
                            : spectator
                            ? arena.getStatus() == GameState.playing && arena.isAllowSpectate()
                            : (arena.getStatus() == GameState.waiting
                            || arena.getStatus() == GameState.starting)
                            && hasPreloadCapacity(arena, requestedUuid));

                    if (accepted) {
                        LoadedUser previous = LoadedUser.getPreLoaded(requestedUuid);
                        if (previous != null) previous.destroy("重复预加载");
                        new LoadedUser(uuid, arenaIdentifier, language, target, requestId,
                                joinMode, reservationId);
                        accepted = LoadedUser.isPreLoaded(requestedUuid);
                    }

                    if (!isBlank(requestId)) {
                        JsonObject ready = new JsonObject();
                        ready.addProperty("type", "PLD_READY");
                        ready.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
                        ready.addProperty("request_id", requestId);
                        ready.addProperty("uuid", uuid);
                        ready.addProperty("accepted", accepted);
                        if (!accepted) ready.addProperty("reason", "arena_unavailable");
                        sendMessagesAsync(Collections.singletonList(ready.toString()));
                    }
                }
            });
        }

        private void handlePreloadCancel(JsonObject json) {
            if (!hasStringFields(json, "uuid")) {
                invalid("PLD_CANCEL is missing uuid");
                return;
            }
            final String requestId = bounded(json, "request_id", 128);
            final UUID uuid;
            try {
                uuid = UUID.fromString(bounded(json, "uuid", 64));
            } catch (IllegalArgumentException exception) {
                invalid("PLD_CANCEL contains an invalid UUID");
                return;
            }
            Bukkit.getScheduler().runTask(BedWars.plugin, new Runnable() {
                @Override
                public void run() {
                    LoadedUser user = LoadedUser.getPreLoaded(uuid);
                    if (user != null) user.destroyIfRequest(requestId, "大厅取消预加载");
                }
            });
        }

        private void handlePlayerQuery(JsonObject json) {
            if (!hasStringFields(json, "name", "requester")) {
                invalid("Q is missing fields");
                return;
            }
            final String playerName = bounded(json, "name", 64);
            final String requester = bounded(json, "requester", 128);
            Bukkit.getScheduler().runTask(BedWars.plugin, new Runnable() {
                @Override
                public void run() {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player == null || !player.isOnline()) return;
                    IArena arena = Arena.getArenaByPlayer(player);
                    if (arena == null) return;
                    JsonObject response = new JsonObject();
                    response.addProperty("type", "Q");
                    response.addProperty("name", player.getName());
                    response.addProperty("requester", requester);
                    response.addProperty("server_name", configuredServerId());
                    response.addProperty("arena_id", arena.getWorldName());
                    sendMessagesAsync(Collections.singletonList(response.toString()));
                }
            });
        }

        private void sendMessagesAsync(final List<String> messages) {
            if (messages == null || messages.isEmpty() || !isOpen()) return;
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, new Runnable() {
                @Override
                public void run() {
                    for (String message : messages) {
                        if (message == null || message.trim().isEmpty()) continue;
                        if (!sendMessage(message)) return;
                    }
                }
            });
        }

        private synchronized boolean sendMessage(String message) {
            if (!isOpen()) {
                disable();
                return false;
            }
            if (message == null || message.trim().isEmpty()) return false;
            writer.println(message);
            if (writer.checkError()) {
                disable();
                return false;
            }
            return true;
        }

        private synchronized void disable() {
            if (!open) return;
            open = false;
            sockets.remove(endpoint, this);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private void invalid(String reason) {
            invalidMessages++;
            BedWars.plugin.getLogger().log(Level.WARNING,
                    "Rejected lobby socket data from " + endpoint + ": " + reason
                            + " (" + invalidMessages + "/3)");
            if (invalidMessages >= 3) disable();
        }
    }

    static String readLimitedMessage(Reader reader, int maximumLength) throws IOException {
        StringBuilder message = new StringBuilder(Math.min(maximumLength, 1024));
        int character;
        while ((character = reader.read()) != -1) {
            if (character == '\n') break;
            if (character == '\r') continue;
            if (message.length() >= maximumLength) {
                throw new IOException("incoming message exceeds " + maximumLength + " characters");
            }
            message.append((char) character);
        }
        if (character == -1 && message.length() == 0) return null;
        return message.toString();
    }

    private static boolean hasPreloadCapacity(IArena arena, UUID requestedUuid) {
        if (arena == null || arena.getMaxPlayers() <= 0 || isBlank(arena.getWorldName())) return false;
        int pending = 0;
        for (LoadedUser user : LoadedUser.getLoaded().values()) {
            if (user == null || user.getUuid() == null || user.getUuid().equals(requestedUuid)) continue;
            if (arena.getWorldName().equalsIgnoreCase(user.getArenaIdentifier())) pending++;
        }
        return arena.getPlayers().size() + pending + 1 <= arena.getMaxPlayers();
    }

    private static boolean hasStringFields(JsonObject json, String... fields) {
        for (String field : fields) {
            if (json == null || !json.has(field) || json.get(field).isJsonNull()
                    || !json.get(field).isJsonPrimitive()) return false;
        }
        return true;
    }

    private static String value(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()
                || !json.get(key).isJsonPrimitive()) return "";
        try {
            return json.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String bounded(JsonObject json, String key, int maximumLength) {
        String value = value(json, key).trim();
        if (value.length() > maximumLength || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) return "";
        return value;
    }

    private static int intValue(JsonObject json, String key, int fallback) {
        try {
            return json != null && json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    /** Close all active lobby sockets during plugin shutdown. */
    public static void disable() {
        connecting.clear();
        for (RemoteLobby remote : new ArrayList<RemoteLobby>(sockets.values())) {
            remote.disable();
        }
        sockets.clear();
    }
}
