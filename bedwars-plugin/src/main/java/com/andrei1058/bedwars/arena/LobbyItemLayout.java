package com.andrei1058.bedwars.arena;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves configured lobby-item slots without depending on YAML iteration side effects. */
final class LobbyItemLayout {

    private LobbyItemLayout() {
    }

    static Result resolve(List<Item> configuredItems) {
        List<Item> ordered = new ArrayList<>(configuredItems);
        ordered.sort((first, second) -> Integer.compare(first.returnPriority(), second.returnPriority()));

        Map<Integer, Item> bySlot = new LinkedHashMap<>();
        List<Conflict> conflicts = new ArrayList<>();
        for (Item item : ordered) {
            Item replaced = bySlot.put(item.slot(), item);
            if (replaced != null) conflicts.add(new Conflict(item.slot(), replaced.id(), item.id()));
        }
        return new Result(List.copyOf(bySlot.values()), List.copyOf(conflicts));
    }

    record Item(String id, int slot, int returnPriority) {
    }

    record Conflict(int slot, String replacedId, String selectedId) {
    }

    record Result(List<Item> items, List<Conflict> conflicts) {
    }
}
