package com.andrei1058.bedwars.arena;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure layout calculations for the paginated arena selector. */
public final class ArenaSelectorPagination {

    public static final int DEFAULT_SIZE = 54;
    public static final String DEFAULT_CONTENT_SLOTS =
            "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,"
                    + "27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44";

    private ArenaSelectorPagination() {
    }

    static int normalizeSize(int configuredSize) {
        return configuredSize >= 9 && configuredSize <= 54 && configuredSize % 9 == 0
                ? configuredSize : DEFAULT_SIZE;
    }

    static List<Integer> contentSlots(String configuredSlots, int inventorySize) {
        Set<Integer> reserved = Set.of(previousSlot(inventorySize), indicatorSlot(inventorySize),
                nextSlot(inventorySize));
        LinkedHashSet<Integer> slots = new LinkedHashSet<>();
        if (configuredSlots != null) {
            for (String token : configuredSlots.split(",")) {
                try {
                    int slot = Integer.parseInt(token.trim());
                    if (slot >= 0 && slot < inventorySize && !reserved.contains(slot)) slots.add(slot);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (!slots.isEmpty()) return List.copyOf(slots);

        // A broken or empty custom slot list must not make every arena vanish.
        List<Integer> fallback = new ArrayList<>();
        for (int slot = 0; slot < inventorySize; slot++) {
            if (!reserved.contains(slot)) fallback.add(slot);
        }
        return List.copyOf(fallback);
    }

    static int pageCount(int entryCount, int pageCapacity) {
        if (pageCapacity < 1) return 1;
        return Math.max(1, entryCount / pageCapacity + (entryCount % pageCapacity == 0 ? 0 : 1));
    }

    static int clampPage(int requestedPage, int entryCount, int pageCapacity) {
        return Math.max(0, Math.min(requestedPage, pageCount(entryCount, pageCapacity) - 1));
    }

    static int previousSlot(int inventorySize) {
        return inventorySize - 9;
    }

    static int indicatorSlot(int inventorySize) {
        return inventorySize - 5;
    }

    static int nextSlot(int inventorySize) {
        return inventorySize - 1;
    }
}
