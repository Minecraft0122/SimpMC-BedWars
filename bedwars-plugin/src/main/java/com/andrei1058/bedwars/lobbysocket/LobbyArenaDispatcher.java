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
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Main-thread coordinator for cross-server arena joins. One party is reserved
 * as a unit and every member is connected only after the arena node confirms
 * that its {@code LoadedUser} entry exists.
 */
public final class LobbyArenaDispatcher implements Listener, AutoCloseable {

    /**
     * Keep a reservation briefly after Connect is sent. The child server's
     * next status heartbeat can lag behind the proxy handoff, so releasing the
     * slot immediately would allow another party to overbook the arena.
     */
    private static final long HANDOFF_RESERVATION_TICKS = 20L * 15L;

    private final BedWars plugin;
    private final LobbySocketServer socketServer;
    private final ArenaDirectory directory;
    private final Map<String, PendingBatch> batches = new LinkedHashMap<>();
    private final Map<String, PendingRequest> requests = new HashMap<>();
    private final Map<UUID, String> playerBatches = new HashMap<>();
    /** Reconnect leases published by the arena that still owns the match. */
    private final Map<UUID, LobbySocketServer.RejoinMessage> rejoinReservations = new HashMap<>();
    private boolean closed;

    public LobbyArenaDispatcher(BedWars plugin, LobbySocketServer socketServer) {
        this.plugin = plugin;
        this.socketServer = socketServer;
        this.directory = socketServer.directory();
        socketServer.setReadyHandler(this::onReadyFromSocket);
        socketServer.setRejoinHandlers(this::onRejoinFromSocket, this::onRejoinRemoveFromSocket);
    }

    /**
     * Dispatch a player or the player's online party. The selector may be
     * blank/random, a group name, or a map/runtime arena name. Prefixes
     * {@code group:} and {@code arena:} remove ambiguity when needed.
     */
    public boolean dispatch(Player requester, String selector) {
        return dispatch(requester, selector, false);
    }

    /** Dispatch a single player to an arena that is already in progress as a spectator. */
    public boolean dispatchSpectator(Player requester, String selector) {
        return dispatch(requester, selector, true);
    }

    /**
     * Dispatch a reconnecting player to the exact arena node that published
     * the lease. Reconnects must never be sent through normal random matching:
     * the team and match state still live on the original arena process.
     *
     * @return true when a lease was found and either dispatched or already
     *         waiting for confirmation
     */
    public boolean dispatchRejoin(Player player) {
        if (closed || player == null || !player.isOnline()) return false;
        UUID uuid = player.getUniqueId();
        LobbySocketServer.RejoinMessage lease = activeRejoin(uuid);
        if (lease == null) return false;
        if (playerBatches.containsKey(uuid)) {
            AdventureText.send(player, "§e▪ §7正在等待原竞技场确认，请稍候。 ");
            return true;
        }
        if (!socketServer.isSessionOpen(lease.sessionId())) {
            rejoinReservations.remove(uuid, lease);
            return false;
        }

        // Prefer the latest directory snapshot for display/proxy metadata, but
        // allow an RC received just before the first UPDATE: the authenticated
        // socket session is already authoritative for this handoff.
        ArenaNodeSnapshot advertised = directory.snapshot().stream()
                .filter(snapshot -> snapshot.sessionId().equals(lease.sessionId()))
                .filter(snapshot -> snapshot.arenaIdentifier().equalsIgnoreCase(lease.arenaIdentifier()))
                .findFirst().orElse(null);
        ArenaNodeSnapshot node = advertised == null
                ? new ArenaNodeSnapshot(lease.sessionId(), lease.serverId(), lease.proxyServer(),
                "rejoin-" + lease.sessionId(), lease.arenaIdentifier(), lease.arenaIdentifier(),
                "PLAYING", 0, 0, 0, Set.of("DEFAULT"), false, 0,
                System.currentTimeMillis(), true)
                : new ArenaNodeSnapshot(
                advertised.sessionId(), advertised.serverId(), advertised.proxyServer(),
                advertised.nodeInstanceId(), advertised.arenaName(), advertised.arenaIdentifier(),
                advertised.status(), advertised.currentPlayers(), advertised.maxPlayers(),
                advertised.maxInTeam(), advertised.groups(), advertised.spectate(),
                advertised.sequence(), advertised.lastSeenMillis(), advertised.dispatchable());
        String batchId = UUID.randomUUID().toString();
        PendingBatch batch = new PendingBatch(batchId, node, List.of(player), "", false,
                true, lease.reservationId());
        batches.put(batchId, batch);
        String requestId = UUID.randomUUID().toString();
        PendingRequest request = new PendingRequest(requestId, batch, uuid);
        batch.requests.put(requestId, request);
        requests.put(requestId, request);
        playerBatches.put(uuid, batchId);
        sendPreload(request, player);

        long remainingMillis = Math.max(1_000L, lease.expiresAtMillis() - System.currentTimeMillis());
        int configuredSeconds = Math.max(1, BedWars.config.getYml().getInt(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_DISPATCH_TIMEOUT_SECONDS, 8));
        long timeoutTicks = Math.max(1L, Math.min(configuredSeconds * 20L,
                (remainingMillis + 49L) / 50L));
        batch.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> failBatch(batch, "原竞技场确认超时，请稍后重试。"), timeoutTicks);
        AdventureText.send(player, "§a▪ §7正在连接你之前的竞技场……");
        return true;
    }

    /** Whether the lobby currently knows a valid reconnect lease for a UUID. */
    public boolean hasRejoin(UUID uuid) {
        return activeRejoin(uuid) != null;
    }

    private LobbySocketServer.RejoinMessage activeRejoin(UUID uuid) {
        if (uuid == null) return null;
        LobbySocketServer.RejoinMessage lease = rejoinReservations.get(uuid);
        if (lease != null && !isActiveLease(lease, System.currentTimeMillis())) {
            rejoinReservations.remove(uuid, lease);
            return null;
        }
        return lease;
    }

    static boolean isActiveLease(LobbySocketServer.RejoinMessage lease, long nowMillis) {
        return lease != null && lease.uuid() != null && !lease.uuid().isBlank()
                && lease.arenaIdentifier() != null && !lease.arenaIdentifier().isBlank()
                && lease.sessionId() != null && !lease.sessionId().isBlank()
                && lease.expiresAtMillis() > nowMillis;
    }

    static boolean matchesRemoval(LobbySocketServer.RejoinMessage current,
                                   LobbySocketServer.RejoinRemoveMessage removal) {
        if (current == null || removal == null || current.sessionId() == null
                || !current.sessionId().equals(removal.sessionId())) return false;
        return removal.reservationId() == null || removal.reservationId().isBlank()
                || (current.reservationId() != null && current.reservationId().equals(removal.reservationId()));
    }

    private void onRejoinFromSocket(LobbySocketServer.RejoinMessage message) {
        if (closed || message == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closed) return;
            // A create callback can already be queued when the TCP session
            // closes. The matching unregister callback will remove its leases;
            // ignoring a closed session here also prevents an old RC from
            // replacing a replay received on a newer connection.
            if (!socketServer.isSessionOpen(message.sessionId())) return;
            try {
                UUID uuid = UUID.fromString(message.uuid());
                if (message.expiresAtMillis() > System.currentTimeMillis()) {
                    LobbySocketServer.RejoinMessage current = rejoinReservations.get(uuid);
                    // A reconnect replacement publishes a later expiry. If
                    // TCP reconnects reorder old/new RC deliveries, retain
                    // the lease with the newer deadline.
                    boolean sameLease = current != null && message.reservationId() != null
                            && message.reservationId().equals(current.reservationId());
                    boolean currentSessionOpen = current != null
                            && socketServer.isSessionOpen(current.sessionId());
                    if (current == null || message.expiresAtMillis() > current.expiresAtMillis()
                            || (sameLease && !currentSessionOpen)) {
                        rejoinReservations.put(uuid, message);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // The socket listener validates UUIDs before reaching here.
            }
        });
    }

    private void onRejoinRemoveFromSocket(LobbySocketServer.RejoinRemoveMessage message) {
        if (closed || message == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closed) return;
            if (message.uuid() == null || message.uuid().isBlank()) {
                rejoinReservations.entrySet().removeIf(entry ->
                        entry.getValue().sessionId() != null
                                && entry.getValue().sessionId().equals(message.sessionId()));
                return;
            }
            try {
                UUID uuid = UUID.fromString(message.uuid());
                LobbySocketServer.RejoinMessage current = rejoinReservations.get(uuid);
                if (!matchesRemoval(current, message)) return;
                rejoinReservations.remove(uuid, current);
            } catch (IllegalArgumentException ignored) {
                // The socket listener validates UUIDs before reaching here.
            }
        });
    }

    /**
     * Return fresh node snapshots suitable for the lobby selector. The
     * default group has the same wildcard meaning as the local selector.
     */
    public List<ArenaNodeSnapshot> visibleSnapshots(String group, boolean includePlaying) {
        if (closed) return List.of();
        String requestedGroup = group == null ? "" : group.trim();
        boolean wildcard = requestedGroup.isEmpty() || requestedGroup.equalsIgnoreCase("default");
        long now = System.currentTimeMillis();
        long timeout = nodeTimeoutMillis();
        return directory.snapshot().stream()
                .filter(snapshot -> snapshot.dispatchable())
                .filter(snapshot -> now - snapshot.lastSeenMillis() <= timeout)
                .filter(snapshot -> wildcard || snapshot.belongsToGroup(requestedGroup))
                .filter(snapshot -> snapshot.isWaitingOrStarting()
                        || (includePlaying && "PLAYING".equals(snapshot.status()) && snapshot.spectate()))
                .sorted(java.util.Comparator
                        .comparing(ArenaNodeSnapshot::arenaName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ArenaNodeSnapshot::arenaIdentifier, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean dispatch(Player requester, String selector, boolean spectator) {
        if (closed || requester == null || !requester.isOnline()) return false;
        if (Arena.isInArena(requester)) {
            AdventureText.send(requester, "§c▪ §7你已经在一场竞技场游戏中。");
            return true;
        }
        if (BedWars.getParty().hasParty(requester) && !BedWars.getParty().isOwner(requester)) {
            Player owner = BedWars.getParty().getOwner(requester);
            AdventureText.send(requester, owner == null
                    ? "§c▪ §7请让队长发起加入。"
                    : "§c▪ §7请让队长 §e" + owner.getName() + " §7发起加入。 ");
            return true;
        }

        List<Player> members = partyMembers(requester);
        if (members.isEmpty()) members = List.of(requester);
        if (spectator && members.size() > 1) {
            AdventureText.send(requester, "§c▪ §7观战暂不支持整队跨服，请单独选择玩家。 ");
            return true;
        }
        for (Player member : members) {
            if (member == null || !member.isOnline() || Arena.isInArena(member)) {
                AdventureText.send(requester, "§c▪ §7队伍中有玩家当前无法加入竞技场。");
                return true;
            }
            if (playerBatches.containsKey(member.getUniqueId())) {
                AdventureText.send(requester, "§e▪ §7你的队伍已经在等待竞技场确认，请稍候。");
                return true;
            }
        }

        Selector parsed = parseSelector(selector);
        long now = System.currentTimeMillis();
        long timeout = nodeTimeoutMillis();
        ArenaNodeSnapshot node = directory.select(parsed.group, parsed.arena, members.size(), now, timeout, spectator).orElse(null);
        if (node == null || !socketServer.isSessionOpen(node)) {
            AdventureText.send(requester, "§c▪ §7当前没有可用的竞技场，请稍后再试。");
            return true;
        }
        if (!spectator && !directory.reserve(node, members.size())) {
            AdventureText.send(requester, "§e▪ §7该竞技场刚刚被其他队伍预约，请重新尝试。");
            return true;
        }

        String batchId = UUID.randomUUID().toString();
        String partyOwner = members.size() > 1 ? requester.getName() : "";
        PendingBatch batch = new PendingBatch(batchId, node, members, partyOwner, spectator);
        batch.reservationHeld = !spectator;
        batches.put(batchId, batch);
        for (Player member : members) {
            String requestId = UUID.randomUUID().toString();
            PendingRequest request = new PendingRequest(requestId, batch, member.getUniqueId());
            batch.requests.put(requestId, request);
            requests.put(requestId, request);
            playerBatches.put(member.getUniqueId(), batchId);
            sendPreload(request, member);
        }
        int timeoutSeconds = Math.max(1, BedWars.config.getYml().getInt(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_DISPATCH_TIMEOUT_SECONDS, 8));
        batch.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> failBatch(batch, "竞技场节点确认超时，请稍后重试。"), timeoutSeconds * 20L);
        AdventureText.send(requester, "§a▪ §7已为队伍预约 §e" + node.arenaName()
                + " §7，正在连接竞技场……");
        return true;
    }

    private long nodeTimeoutMillis() {
        return Math.max(1_000L, BedWars.config.getYml().getLong(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_NODE_TIMEOUT_SECONDS, 30) * 1_000L);
    }

    public List<String> suggestions() {
        return directory.suggestions();
    }

    private void sendPreload(PendingRequest request, Player player) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("type", "PLD");
        json.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        json.addProperty("request_id", request.requestId);
        json.addProperty("batch_id", request.batch.batchId);
        json.addProperty("uuid", player.getUniqueId().toString());
        json.addProperty("arena_identifier", request.batch.node.arenaIdentifier());
        json.addProperty("lang_iso", Language.getPlayerLanguage(player).getIso());
        json.addProperty("target", request.batch.partyOwner);
        json.addProperty("join_mode", request.batch.rejoin
                ? "REJOIN" : request.batch.spectator ? "SPECTATOR" : "PLAYER");
        if (request.batch.rejoin && !request.batch.reservationId.isBlank()) {
            json.addProperty("reservation_id", request.batch.reservationId);
        }
        socketServer.send(request.batch.node, json.toString()).whenComplete((sent, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (closed || !requests.containsKey(request.requestId)) return;
                    if (error != null || !Boolean.TRUE.equals(sent)) {
                        failBatch(request.batch, "竞技场节点暂时不可用，请稍后重试。");
                    }
                }));
    }

    private void onReadyFromSocket(LobbySocketServer.ReadyMessage ready) {
        if (closed || ready == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> onReady(ready));
    }

    private void onReady(LobbySocketServer.ReadyMessage ready) {
        PendingRequest request = requests.get(ready.requestId());
        if (request == null || !request.batch.node.sessionId().equals(ready.sessionId())) return;
        if (!request.playerUuid.toString().equalsIgnoreCase(ready.uuid())) {
            failBatch(request.batch, "竞技场节点返回的玩家身份无效。");
            return;
        }
        if (!ready.accepted()) {
            failBatch(request.batch, "竞技场节点拒绝了本次加入：" + reasonText(ready.reason()));
            return;
        }
        request.ready = true;
        if (request.batch.requests.values().stream().allMatch(pending -> pending.ready)) {
            completeBatch(request.batch);
        }
    }

    private void completeBatch(PendingBatch batch) {
        if (!batches.containsKey(batch.batchId)) return;
        // Keep the capacity reservation until the child has had time to report
        // the newly connected players. This short TTL closes the status-update
        // gap between the proxy handoff and the next child heartbeat.
        removeBatch(batch, false);
        scheduleHandoffRelease(batch);
        for (Player player : batch.players) {
            if (player == null) continue;
            if (!player.isOnline()) {
                // The player can quit after PLD_READY but before the proxy
                // handoff. Do not leave a child-side LoadedUser until its TTL.
                cancelPreload(batch, player.getUniqueId());
                continue;
            }
            if (!Misc.connectToProxyServer(player, batch.node.proxyServer())) {
                // A failed proxy handoff leaves the remote LoadedUser entry
                // behind. Cancel only that member's preload; other members
                // may already have been accepted by the proxy.
                cancelPreload(batch, player.getUniqueId());
                AdventureText.send(player, "§c▪ §7无法连接竞技场，请稍后重新加入。");
            }
        }
    }

    private void failBatch(PendingBatch batch, String message) {
        if (batch == null || !batches.containsKey(batch.batchId)) return;
        cancelPreloads(batch);
        removeBatch(batch);
        for (Player player : batch.players) {
            if (player != null && player.isOnline()) {
                AdventureText.send(player, "§c▪ §7" + message);
            }
        }
    }

    private void removeBatch(PendingBatch batch) {
        removeBatch(batch, true);
    }

    private void removeBatch(PendingBatch batch, boolean releaseReservation) {
        batches.remove(batch.batchId);
        if (batch.timeoutTask != null) batch.timeoutTask.cancel();
        requests.values().removeIf(request -> request.batch == batch);
        for (Player player : batch.players) playerBatches.remove(player.getUniqueId(), batch.batchId);
        if (releaseReservation) releaseReservation(batch);
    }

    private void scheduleHandoffRelease(PendingBatch batch) {
        if (batch.spectator || !batch.reservationHeld || closed) return;
        batch.reservationReleaseTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> releaseReservation(batch), HANDOFF_RESERVATION_TICKS);
    }

    private void releaseReservation(PendingBatch batch) {
        if (batch == null || batch.spectator || !batch.reservationHeld) return;
        batch.reservationHeld = false;
        if (batch.reservationReleaseTask != null) {
            batch.reservationReleaseTask.cancel();
            batch.reservationReleaseTask = null;
        }
        directory.release(batch.node, batch.players.size());
    }

    private void cancelPreloads(PendingBatch batch) {
        cancelPreloads(batch, false);
    }

    private void cancelPreloads(PendingBatch batch, boolean immediate) {
        for (PendingRequest request : batch.requests.values()) {
            cancelPreload(batch, request.playerUuid, immediate);
        }
    }

    private void cancelPreload(PendingBatch batch, UUID playerUuid) {
        cancelPreload(batch, playerUuid, false);
    }

    private void cancelPreload(PendingBatch batch, UUID playerUuid, boolean immediate) {
        if (batch == null || playerUuid == null) return;
        PendingRequest request = batch.requests.values().stream()
                .filter(candidate -> candidate.playerUuid.equals(playerUuid))
                .findFirst().orElse(null);
        if (request == null) return;
        com.google.gson.JsonObject cancel = new com.google.gson.JsonObject();
        cancel.addProperty("type", "PLD_CANCEL");
        cancel.addProperty("protocol_version", BungeeProtocol.CURRENT_VERSION);
        cancel.addProperty("request_id", request.requestId);
        cancel.addProperty("uuid", request.playerUuid.toString());
        if (immediate) socketServer.sendNow(batch.node, cancel.toString());
        else socketServer.send(batch.node, cancel.toString());
    }

    private void cancelForPlayer(UUID uuid) {
        String batchId = playerBatches.get(uuid);
        if (batchId == null) return;
        PendingBatch batch = batches.get(batchId);
        if (batch != null) failBatch(batch, "队伍成员已离开大厅，本次预约已取消。");
    }

    private List<Player> partyMembers(Player requester) {
        if (!BedWars.getParty().hasParty(requester)) return List.of(requester);
        List<Player> configured = BedWars.getParty().getMembers(requester);
        if (configured == null || configured.isEmpty()) return List.of(requester);
        List<Player> result = new ArrayList<>();
        for (Player player : configured) {
            if (player != null && player.isOnline() && !result.contains(player)) result.add(player);
        }
        if (!result.contains(requester)) result.add(0, requester);
        return result;
    }

    private Selector parseSelector(String selector) {
        if (selector == null || selector.isBlank() || selector.equalsIgnoreCase("random")
                || selector.equalsIgnoreCase("default")) return new Selector("", "");
        String value = selector.trim();
        int separator = value.indexOf(':');
        if (separator > 0) {
            String prefix = value.substring(0, separator).toLowerCase(Locale.ROOT);
            String remainder = value.substring(separator + 1).trim();
            if (prefix.equals("group")) return new Selector(remainder, "");
            if (prefix.equals("arena") || prefix.equals("map")) return new Selector("", remainder);
        }
        for (ArenaNodeSnapshot snapshot : directory.snapshot()) {
            if (snapshot.arenaName().equalsIgnoreCase(value)
                    || snapshot.arenaIdentifier().equalsIgnoreCase(value)) {
                return new Selector("", value);
            }
        }
        return new Selector(value, "");
    }

    private static String reasonText(String reason) {
        if (reason == null || reason.isBlank()) return "地图当前不可用";
        return switch (reason) {
            case "arena_unavailable" -> "地图当前不可用";
            default -> reason;
        };
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event != null && event.getPlayer() != null) cancelForPlayer(event.getPlayer().getUniqueId());
    }

    @Override
    public void close() {
        closed = true;
        // The plugin may be disabled while PLD entries are still waiting on a
        // child node. Remove those entries before dropping local bookkeeping;
        // otherwise a reconnecting player can inherit a stale preload.
        for (PendingBatch batch : new ArrayList<>(batches.values())) {
            // Flush cancellation messages before the socket executor is stopped.
            cancelPreloads(batch, true);
            removeBatch(batch);
        }
        batches.clear();
        requests.clear();
        playerBatches.clear();
        rejoinReservations.clear();
    }

    private record Selector(String group, String arena) {
    }

    private static final class PendingBatch {
        private final String batchId;
        private final ArenaNodeSnapshot node;
        private final List<Player> players;
        private final String partyOwner;
        private final boolean spectator;
        private final boolean rejoin;
        private final String reservationId;
        private final Map<String, PendingRequest> requests = new LinkedHashMap<>();
        private BukkitTask timeoutTask;
        private BukkitTask reservationReleaseTask;
        private boolean reservationHeld;

        private PendingBatch(String batchId, ArenaNodeSnapshot node, List<Player> players, String partyOwner,
                             boolean spectator) {
            this(batchId, node, players, partyOwner, spectator, false, "");
        }

        private PendingBatch(String batchId, ArenaNodeSnapshot node, List<Player> players, String partyOwner,
                             boolean spectator, boolean rejoin, String reservationId) {
            this.batchId = batchId;
            this.node = node;
            this.players = List.copyOf(players);
            this.partyOwner = partyOwner;
            this.spectator = spectator;
            this.rejoin = rejoin;
            this.reservationId = reservationId == null ? "" : reservationId;
        }
    }

    private static final class PendingRequest {
        private final String requestId;
        private final PendingBatch batch;
        private final UUID playerUuid;
        private boolean ready;

        private PendingRequest(String requestId, PendingBatch batch, UUID playerUuid) {
            this.requestId = requestId;
            this.batch = batch;
            this.playerUuid = playerUuid;
        }
    }
}
