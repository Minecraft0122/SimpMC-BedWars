/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.lobbysocket;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class ArenaSocket {

    static final int MAX_INCOMING_MESSAGE_LENGTH = 65_536;
    public static List<String> lobbies = java.util.Collections.synchronizedList(new ArrayList<>());
    private static final ConcurrentHashMap<String, RemoteLobby> sockets = new ConcurrentHashMap<>();
    private static final Set<String> connecting = ConcurrentHashMap.newKeySet();
    private static final String NODE_INSTANCE_ID = UUID.randomUUID().toString();
    private static final AtomicLong messageSequence = new AtomicLong();

    /**
     * Send arena data to the lobbies.
     */
    public static void sendMessage(String message) {
        if (message == null) return;
        if (message.isEmpty()) return;
        if (BedWars.isShuttingDown()) return;

        // Socket connect/write operations must never stall the server tick.
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> sendMessage(message));
            return;
        }

        List<String> configuredLobbies;
        synchronized (lobbies) {
            configuredLobbies = List.copyOf(lobbies);
        }
        for (String lobby : configuredLobbies) {
            if (lobby == null || lobby.isBlank()) continue;
            String[] l = lobby.split(":", 2);

            if (l.length != 2) continue;
            if (!Misc.isNumber(l[1])) continue;

            RemoteLobby connected = sockets.get(lobby);
            if (connected != null) {
                connected.sendMessage(message);
            } else if (connecting.add(lobby)) {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(l[0], Integer.parseInt(l[1])), 2_000);
                    RemoteLobby rl = new RemoteLobby(socket, lobby);
                    if (rl.out != null && rl.in != null) {
                        sockets.put(lobby, rl);
                        rl.sendMessage(formatHelloMessage());
                        rl.sendMessage(message);
                    } else {
                        socket.close();
                    }
                } catch (IOException ignored) {
                } finally {
                    connecting.remove(lobby);
                }
            }
        }
    }

    /**
     * Format message before sending it to lobbies.
     */
    public static String formatUpdateMessage(IArena a) {
        if (a == null) return "";
        if (a.getWorldName() == null) return "";
        String arenaName = a.getArenaName() == null || a.getArenaName().isBlank()
                ? a.getWorldName() : a.getArenaName();
        String group = a.getGroup() == null || a.getGroup().isBlank() ? "Default" : a.getGroup();
        String status = a.getStatus() == null ? "RESTARTING" : a.getStatus().toString();
        JsonObject js = new JsonObject();
        js.addProperty("type", "UPDATE");
        js.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        js.addProperty("message_id", UUID.randomUUID().toString());
        js.addProperty("node_instance_id", NODE_INSTANCE_ID);
        js.addProperty("sequence", messageSequence.incrementAndGet());
        js.addProperty("server_name", BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID));
        String proxyServer = BedWars.config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_PROXY_SERVER, "");
        if (proxyServer == null || proxyServer.isBlank()) {
            proxyServer = BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID);
        }
        js.addProperty("proxy_server", proxyServer);
        js.addProperty("arena_name", arenaName);
        js.addProperty("arena_identifier", a.getWorldName());
        js.addProperty("arena_status", status.toUpperCase(Locale.ROOT));
        js.addProperty("arena_current_players", a.getPlayers().size());
        js.addProperty("arena_max_players", a.getMaxPlayers());
        js.addProperty("arena_max_in_team", a.getMaxInTeam());
        js.addProperty("arena_group", group.toUpperCase(Locale.ROOT));
        JsonArray arenaGroups = new JsonArray();
        arenaGroups.add(group.toUpperCase(Locale.ROOT));
        js.add("arena_groups", arenaGroups);
        js.addProperty("spectate", a.isAllowSpectate());
        return js.toString();
    }

    /** Notify lobbies that one runtime copy is no longer dispatchable. */
    public static String formatRemoveMessage(String arenaIdentifier) {
        if (arenaIdentifier == null || arenaIdentifier.isBlank()) return "";
        JsonObject json = new JsonObject();
        json.addProperty("type", "REMOVE");
        json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        json.addProperty("node_instance_id", NODE_INSTANCE_ID);
        json.addProperty("sequence", messageSequence.incrementAndGet());
        json.addProperty("server_name", BedWars.config.getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID));
        json.addProperty("arena_identifier", arenaIdentifier);
        return json.toString();
    }

    /** Initial handshake sent before the first arena snapshot on a connection. */
    public static String formatHelloMessage() {
        JsonObject js = new JsonObject();
        js.addProperty("type", "HELLO");
        js.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        js.addProperty("role", BungeeProtocol.ARENA_ROLE);
        js.addProperty("node_instance_id", NODE_INSTANCE_ID);
        js.addProperty("server_id", BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID, "bw1"));
        String proxyServer = BedWars.config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_PROXY_SERVER, "");
        if (proxyServer == null || proxyServer.isBlank()) proxyServer = BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID, "bw1");
        js.addProperty("proxy_server", proxyServer);
        js.addProperty("secret", BedWars.config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_SOCKET_SECRET, ""));
        return js.toString();
    }

    private static class RemoteLobby {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String lobby;
        private volatile boolean compute = true;
        private volatile boolean authenticated;
        private int invalidMessages;

        private RemoteLobby(Socket socket, String lobby) {
            this.socket = socket;
            this.lobby = lobby;
            try {
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException ignored) {
                out = null;
                return;
            }

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                return;
            }

            BedWars.debug("RemoteLobby created: " + lobby + " " + socket.toString());
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> {
                while (compute) {
                    try {
                        String msg = readLimitedMessage(in, MAX_INCOMING_MESSAGE_LENGTH);
                        if (msg == null) {
                            disable();
                            break;
                        }
                        BedWars.debug(msg);
                        if (msg.isEmpty()) continue;
                        final JsonObject json;
                        try {
                            json = new JsonParser().parse(msg).getAsJsonObject();
                        } catch (JsonSyntaxException e) {
                            warnInvalidMessage("malformed JSON");
                            continue;
                        }
                        if (json == null) continue;
                        if (!json.has("type")) continue;
                        String type = json.get("type").getAsString().toUpperCase(Locale.ROOT);
                        if (!authenticated) {
                            if (type.equals("WELCOME")) {
                                if (!acceptWelcome(json)) disable();
                            } else if (type.equals("REJECT")) {
                                warnInvalidMessage("lobby rejected HELLO");
                                disable();
                            } else {
                                warnInvalidMessage("expected WELCOME before control messages");
                                disable();
                            }
                            continue;
                        }
                        switch (type) {
                            case "REQUEST_SNAPSHOT":
                                Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                                    for (IArena arena : Arena.getArenas()) {
                                        sendMessage(formatUpdateMessage(arena));
                                    }
                                });
                                break;
                            //pre load data
                            //pld,worldIdentifier,uuidUser,languageIso,uuidPartyOwner
                            case "PLD":
                                if (!hasStringFields(json, "uuid", "arena_identifier", "lang_iso", "target")) {
                                    warnInvalidMessage("PLD message is missing fields");
                                    continue;
                                }
                                String uuid = json.get("uuid").getAsString();
                                try {
                                    UUID.fromString(uuid);
                                } catch (IllegalArgumentException exception) {
                                    warnInvalidMessage("PLD message contains an invalid UUID");
                                    continue;
                                }
                                String arenaIdentifier = json.get("arena_identifier").getAsString();
                                String language = json.get("lang_iso").getAsString();
                                String target = json.get("target").getAsString();
                                String joinMode = json.has("join_mode") && json.get("join_mode").isJsonPrimitive()
                                        ? json.get("join_mode").getAsString().trim().toUpperCase(Locale.ROOT) : "AUTO";
                                if (!joinMode.equals("AUTO") && !joinMode.equals("PLAYER")
                                        && !joinMode.equals("SPECTATOR")) {
                                    warnInvalidMessage("PLD message contains an invalid join mode");
                                    continue;
                                }
                                String requestId = json.has("request_id") && json.get("request_id").isJsonPrimitive()
                                        ? json.get("request_id").getAsString() : "";
                                Bukkit.getScheduler().runTask(BedWars.plugin,
                                        () -> {
                                            IArena arena = Arena.getArenaByIdentifier(arenaIdentifier);
                                            boolean spectator = joinMode.equals("SPECTATOR")
                                                    || (joinMode.equals("AUTO") && arena != null
                                                    && arena.getStatus() == com.andrei1058.bedwars.api.arena.GameState.playing);
                                            boolean accepted = arena != null
                                                    && (spectator
                                                    ? arena.getStatus() == com.andrei1058.bedwars.api.arena.GameState.playing
                                                    && arena.isAllowSpectate()
                                                    : (arena.getStatus() == com.andrei1058.bedwars.api.arena.GameState.waiting
                                                    || arena.getStatus() == com.andrei1058.bedwars.api.arena.GameState.starting)
                                                    && hasPreloadCapacity(arena, UUID.fromString(uuid)));
                                            if (accepted) {
                                                LoadedUser previous = LoadedUser.getPreLoaded(UUID.fromString(uuid));
                                                if (previous != null) previous.destroy("重复预加载");
                                                new LoadedUser(uuid, arenaIdentifier, language, target, requestId);
                                                accepted = LoadedUser.isPreLoaded(UUID.fromString(uuid));
                                            }
                                            if (!requestId.isBlank()) {
                                                JsonObject ready = new JsonObject();
                                                ready.addProperty("type", "PLD_READY");
                                                ready.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
                                                ready.addProperty("request_id", requestId);
                                                ready.addProperty("uuid", uuid);
                                                ready.addProperty("accepted", accepted);
                                                if (!accepted) ready.addProperty("reason", "arena_unavailable");
                                                sendMessage(ready.toString());
                                            }
                                });
                                break;
                            case "PLD_CANCEL":
                                if (!hasStringFields(json, "uuid")) {
                                    warnInvalidMessage("PLD_CANCEL message is missing fields");
                                    continue;
                                }
                                String cancellationRequestId = json.has("request_id")
                                        && json.get("request_id").isJsonPrimitive()
                                        ? json.get("request_id").getAsString() : "";
                                try {
                                    UUID cancelled = UUID.fromString(json.get("uuid").getAsString());
                                    Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                                        LoadedUser user = LoadedUser.getPreLoaded(cancelled);
                                        if (user != null) {
                                            user.destroyIfRequest(cancellationRequestId, "大厅取消预加载");
                                        }
                                    });
                                } catch (IllegalArgumentException exception) {
                                    warnInvalidMessage("PLD_CANCEL message contains an invalid UUID");
                                }
                                break;
                            case "Q":
                                if (!hasStringFields(json, "name", "requester")) {
                                    warnInvalidMessage("Q message is missing fields");
                                    continue;
                                }
                                String playerName = json.get("name").getAsString();
                                String requester = json.get("requester").getAsString();
                                Bukkit.getScheduler().runTask(BedWars.plugin,
                                        () -> handlePlayerQuery(playerName, requester));
                                break;
                        }
                    } catch (IOException exception) {
                        if (compute) warnInvalidMessage(exception.getMessage());
                        disable();
                        break;
                    } catch (RuntimeException exception) {
                        warnInvalidMessage("Malformed JSON payload");
                    }
                }
            });
        }

        /**
         * Confirm that the configured endpoint speaks the lobby protocol before
         * accepting PLD/PLD_CANCEL control messages. This is not a replacement
         * for the shared secret, but it prevents accidental connections to an
         * unrelated service from being treated as an authenticated lobby.
         */
        private boolean acceptWelcome(JsonObject json) {
            int protocol = json.has("protocol_version") && json.get("protocol_version").isJsonPrimitive()
                    ? json.get("protocol_version").getAsInt() : 0;
            String sessionId = json.has("session_id") && json.get("session_id").isJsonPrimitive()
                    ? json.get("session_id").getAsString().trim() : "";
            if (protocol < 1 || protocol > BungeeProtocol.CURRENT_VERSION || sessionId.isBlank()) {
                warnInvalidMessage("WELCOME 缺少有效协议版本或会话标识");
                return false;
            }
            try {
                UUID.fromString(sessionId);
            } catch (IllegalArgumentException exception) {
                warnInvalidMessage("WELCOME 的 session_id 无效");
                return false;
            }
            authenticated = true;
            return true;
        }

        /**
         * Send a message to the given host with target port.
         *
         * @return true if message was sent successfully.
         */
        @SuppressWarnings("UnusedReturnValue")
        private synchronized boolean sendMessage(String message) {
            if (socket == null) {
                disable();
                return false;
            }
            if (!socket.isConnected()) {
                disable();
                return false;
            }
            if (socket.isClosed()) {
                disable();
                return false;
            }
            if (out == null) {
                disable();
                return false;
            }
            if (in == null) {
                disable();
                return false;
            }
            if (out.checkError()) {
                disable();
                return false;
            }
            out.println(message);
            return true;
        }

        private synchronized void disable() {
            compute = false;
            BedWars.debug("Disabling socket: " + socket);
            sockets.remove(lobby, this);
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                BedWars.plugin.getLogger().log(Level.WARNING,
                        "Could not close lobby socket " + lobby, e);
            }
        }

        private void warnInvalidMessage(String reason) {
            invalidMessages++;
            BedWars.plugin.getLogger().log(Level.WARNING,
                    "Rejected lobby socket data from " + socket.getRemoteSocketAddress() + ": " + reason
                            + " (" + invalidMessages + "/3)");
            if (invalidMessages >= 3) disable();
        }

        private void handlePlayerQuery(String playerName, String requester) {
            Player player = Bukkit.getPlayer(playerName);
            if (player == null || !player.isOnline()) {
                return;
            }

            IArena arena = Arena.getArenaByPlayer(player);
            if (arena == null) {
                return;
            }

            JsonObject response = new JsonObject();
            response.addProperty("type", "Q");
            response.addProperty("name", player.getName());
            response.addProperty("requester", requester);
            response.addProperty("server_name", BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID));
            response.addProperty("arena_id", arena.getWorldName());
            sendMessage(response.toString());
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
        if (character == -1 && message.isEmpty()) return null;
        return message.toString();
    }

    /**
     * Re-check capacity on the child node. The lobby reservation is local to
     * one lobby process, while several lobbies may legitimately target this
     * same child; counting pending preloads closes that cross-lobby race.
     */
    private static boolean hasPreloadCapacity(IArena arena, UUID requestedUuid) {
        if (arena == null || arena.getMaxPlayers() <= 0 || arena.getWorldName() == null) return false;
        int pending = 0;
        for (LoadedUser user : LoadedUser.getLoaded().values()) {
            if (user == null || user.getUuid() == null || user.getUuid().equals(requestedUuid)) continue;
            if (arena.getWorldName().equalsIgnoreCase(user.getArenaIdentifier())) pending++;
        }
        return arena.getPlayers().size() + pending + 1 <= arena.getMaxPlayers();
    }

    private static boolean hasStringFields(JsonObject json, String... fields) {
        for (String field : fields) {
            if (!json.has(field) || json.get(field).isJsonNull() || !json.get(field).isJsonPrimitive()) return false;
        }
        return true;
    }

    /**
     * Close active sockets.
     */
    public static void disable() {
        connecting.clear();
        for (RemoteLobby rl : new ArrayList<>(sockets.values())) {
            rl.disable();
        }
    }
}
