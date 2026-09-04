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
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Player preload lease held while VC/Bungee moves a player to this node. */
public final class LoadedUser {

    private static final long waitMillis = effectiveTimeoutMillis();

    private static long effectiveTimeoutMillis() {
        long configured = 5_000L;
        int dispatchSeconds = 8;
        if (BedWars.config != null) {
            configured = BedWars.config.getYml().getLong(
                    ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_BWP_TIME_OUT, configured);
            dispatchSeconds = BedWars.config.getYml().getInt(
                    ConfigPath.GENERAL_CONFIGURATION_BUNGEE_DISPATCH_TIMEOUT_SECONDS, dispatchSeconds);
        }
        return PreloadTimeoutPolicy.effectiveTimeoutMillis(configured, dispatchSeconds);
    }

    private final UUID uuid;
    private final String partyOwnerOrSpectateTarget;
    private final long toleranceTime;
    private final String arenaIdentifier;
    private final String requestId;
    private final String joinMode;
    private final String rejoinReservationId;
    private final Language language;

    private static final ConcurrentHashMap<UUID, LoadedUser> loaded =
            new ConcurrentHashMap<UUID, LoadedUser>();

    public LoadedUser(String uuid, String arenaIdentifier, String langIso,
                      String partyOwnerOrSpectateTarget) {
        this(uuid, arenaIdentifier, langIso, partyOwnerOrSpectateTarget, "", "AUTO", "");
    }

    public LoadedUser(String uuid, String arenaIdentifier, String langIso,
                      String partyOwnerOrSpectateTarget, String requestId) {
        this(uuid, arenaIdentifier, langIso, partyOwnerOrSpectateTarget, requestId, "AUTO", "");
    }

    public LoadedUser(String uuid, String arenaIdentifier, String langIso,
                      String partyOwnerOrSpectateTarget, String requestId,
                      String joinMode, String rejoinReservationId) {
        if (Bukkit.getWorld(arenaIdentifier) == null) {
            this.uuid = null;
            this.arenaIdentifier = null;
            this.partyOwnerOrSpectateTarget = null;
            this.requestId = "";
            this.joinMode = "AUTO";
            this.rejoinReservationId = "";
            this.toleranceTime = 0L;
            this.language = null;
            return;
        }
        this.uuid = UUID.fromString(uuid);
        this.arenaIdentifier = arenaIdentifier;
        this.requestId = requestId == null ? "" : requestId.trim();
        this.joinMode = PreloadJoinPolicy.normalize(joinMode);
        this.rejoinReservationId = rejoinReservationId == null ? "" : rejoinReservationId.trim();
        this.partyOwnerOrSpectateTarget = partyOwnerOrSpectateTarget == null
                || partyOwnerOrSpectateTarget.trim().isEmpty()
                ? null : partyOwnerOrSpectateTarget;
        this.toleranceTime = System.currentTimeMillis() + waitMillis;
        this.language = Language.getLang(langIso);
        loaded.put(this.uuid, this);
    }

    public static boolean isPreLoaded(UUID uuid) {
        return uuid != null && loaded.containsKey(uuid);
    }

    public boolean isTimedOut() {
        return System.currentTimeMillis() > toleranceTime;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getArenaIdentifier() {
        return arenaIdentifier;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getJoinMode() {
        return joinMode;
    }

    public boolean expectsRejoin(boolean hasLocalReservation) {
        return PreloadJoinPolicy.expectsRejoin(joinMode, hasLocalReservation);
    }

    public boolean matchesRejoinReservation(String activeReservationId) {
        return PreloadJoinPolicy.matchesReservation(rejoinReservationId, activeReservationId);
    }

    public boolean acceptsArenaStatus(GameState status) {
        return PreloadJoinPolicy.acceptsArenaState(joinMode, status == null ? null : status.toString());
    }

    public void destroy(String reason) {
        if (uuid == null) return;
        BedWars.debug("Destroyed PreLoaded User: " + uuid + " Reason: " + reason
                + ". Tolerance: " + waitMillis);
        loaded.remove(uuid, this);
    }

    /** Remove only when the cancellation belongs to this request. */
    public boolean destroyIfRequest(String cancellationRequestId, String reason) {
        if (!PreloadRequestPolicy.matches(requestId, cancellationRequestId)) return false;
        destroy(reason);
        return true;
    }

    public Language getLanguage() {
        return language;
    }

    public static LoadedUser getPreLoaded(UUID uuid) {
        return uuid == null ? null : loaded.get(uuid);
    }

    public String getPartyOwnerOrSpectateTarget() {
        return partyOwnerOrSpectateTarget;
    }

    public static ConcurrentHashMap<UUID, LoadedUser> getLoaded() {
        return loaded;
    }
}
