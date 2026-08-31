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

final class PreloadRequestPolicy {

    private PreloadRequestPolicy() {
    }

    /**
     * Legacy messages did not carry a request token. Keep those messages
     * compatible while preventing an old token from deleting a newer preload.
     */
    static boolean matches(String activeRequestId, String cancellationRequestId) {
        String active = activeRequestId == null ? "" : activeRequestId.trim();
        String cancellation = cancellationRequestId == null ? "" : cancellationRequestId.trim();
        return active.isBlank() || cancellation.isBlank() || active.equals(cancellation);
    }
}
