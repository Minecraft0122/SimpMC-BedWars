package com.andrei1058.bedwars.shop.listeners;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class RecallScrollChannelStore<T> {

    private final Map<UUID, T> channels = new LinkedHashMap<>();

    boolean start(UUID playerId, T channel) {
        return channels.putIfAbsent(playerId, channel) == null;
    }

    boolean isActive(UUID playerId) {
        return channels.containsKey(playerId);
    }

    T remove(UUID playerId) {
        return channels.remove(playerId);
    }

    Collection<T> values() {
        return channels.values();
    }

    boolean isEmpty() {
        return channels.isEmpty();
    }
}
