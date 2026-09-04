/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.lobbysocket;

import java.util.Locale;

/** Pure join-mode and reconnect-token rules shared by login listeners. */
final class PreloadJoinPolicy {

    private PreloadJoinPolicy() {
    }

    static String normalize(String joinMode) {
        if (joinMode == null || joinMode.trim().isEmpty()) return "AUTO";
        return joinMode.trim().toUpperCase(Locale.ROOT);
    }

    static boolean expectsRejoin(String joinMode, boolean hasLocalReservation) {
        String normalized = normalize(joinMode);
        return "REJOIN".equals(normalized)
                || ("AUTO".equals(normalized) && hasLocalReservation);
    }

    static boolean acceptsArenaState(String joinMode, String arenaState) {
        String normalizedMode = normalize(joinMode);
        String normalizedState = arenaState == null ? "" : arenaState.trim().toUpperCase(Locale.ROOT);
        if ("PLAYER".equals(normalizedMode)) {
            return "WAITING".equals(normalizedState) || "STARTING".equals(normalizedState);
        }
        if ("SPECTATOR".equals(normalizedMode) || "REJOIN".equals(normalizedMode)) {
            return "PLAYING".equals(normalizedState);
        }
        return "WAITING".equals(normalizedState) || "STARTING".equals(normalizedState)
                || "PLAYING".equals(normalizedState);
    }

    static boolean matchesReservation(String requestedReservationId, String activeReservationId) {
        String requested = requestedReservationId == null ? "" : requestedReservationId.trim();
        String active = activeReservationId == null ? "" : activeReservationId.trim();
        return requested.isEmpty() || requested.equals(active);
    }
}
