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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ArenaSocket {

    public static List<String> lobbies = new ArrayList<>();
    private static final ConcurrentHashMap<String, RemoteLobby> sockets = new ConcurrentHashMap<>();
    private static final Set<String> connecting = ConcurrentHashMap.newKeySet();

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

        for (String lobby : lobbies) {
            String[] l = lobby.split(":");

            if (l.length != 2) continue;
            if (!Misc.isNumber(l[1])) continue;

            if (sockets.containsKey(lobby)) {
                sockets.get(lobby).sendMessage(message);
            } else if (connecting.add(lobby)) {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(l[0], Integer.parseInt(l[1])), 2_000);
                    RemoteLobby rl = new RemoteLobby(socket, lobby);
                    if (rl.out != null && rl.in != null) {
                        sockets.put(lobby, rl);
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
        JsonObject js = new JsonObject();
        js.addProperty("type", "UPDATE");
        js.addProperty("server_name", BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID));
        js.addProperty("arena_name", a.getArenaName());
        js.addProperty("arena_identifier", a.getWorldName());
        js.addProperty("arena_status", a.getStatus().toString().toUpperCase());
        js.addProperty("arena_current_players", a.getPlayers().size());
        js.addProperty("arena_max_players", a.getMaxPlayers());
        js.addProperty("arena_max_in_team", a.getMaxInTeam());
        js.addProperty("arena_group", a.getGroup().toUpperCase());
        js.addProperty("spectate", a.isAllowSpectate());
        return js.toString();
    }

    private static class RemoteLobby {
        private Socket socket;
        private PrintWriter out;
        private Scanner in;
        private String lobby;
        private volatile boolean compute = true;

        private RemoteLobby(Socket socket, String lobby) {
            this.socket = socket;
            this.lobby = lobby;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException ignored) {
                out = null;
                return;
            }

            try {
                in = new Scanner(socket.getInputStream());
            } catch (IOException ignored) {
                return;
            }

            BedWars.debug("RemoteLobby created: " + lobby + " " + socket.toString());
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> {
                while (compute) {
                    if (in.hasNext()) {
                        String msg = in.next();
                        BedWars.debug(msg);
                        if (msg.isEmpty()) continue;
                        final JsonObject json;
                        try {
                            json = new JsonParser().parse(msg).getAsJsonObject();
                        } catch (JsonSyntaxException e) {
                            BedWars.plugin.getLogger().log(Level.WARNING, "Received bad data from: " + socket.getInetAddress().toString());
                            continue;
                        }
                        if (json == null) continue;
                        if (!json.has("type")) continue;
                        switch (json.get("type").getAsString().toUpperCase()) {
                            //pre load data
                            //pld,worldIdentifier,uuidUser,languageIso,uuidPartyOwner
                            case "PLD":
                                String uuid = json.get("uuid").getAsString();
                                String arenaIdentifier = json.get("arena_identifier").getAsString();
                                String language = json.get("lang_iso").getAsString();
                                String target = json.get("target").getAsString();
                                Bukkit.getScheduler().runTask(BedWars.plugin,
                                        () -> new LoadedUser(uuid, arenaIdentifier, language, target));
                                break;
                            case "Q":
                                String playerName = json.get("name").getAsString();
                                String requester = json.get("requester").getAsString();
                                Bukkit.getScheduler().runTask(BedWars.plugin,
                                        () -> handlePlayerQuery(playerName, requester));
                                break;
                        }
                    } else {
                        disable();
                    }
                }
            });
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
            BedWars.debug("Disabling socket: " + socket.toString());
            sockets.remove(lobby);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
