package com.andrei1058.bedwars.arena;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure configuration validation for the two mandatory team resources. */
final class RequiredTeamGeneratorValidator {

    private RequiredTeamGeneratorValidator() {
    }

    static Map<String, List<String>> findMissing(@NotNull ConfigurationSection configuration,
                                                  @NotNull Collection<String> teams) {
        Map<String, List<String>> missing = new LinkedHashMap<>();
        for (String team : teams) {
            List<String> types = new ArrayList<>(2);
            if (!hasLocation(configuration.get("Team." + team + ".Iron"))) types.add("铁");
            if (!hasLocation(configuration.get("Team." + team + ".Gold"))) types.add("金");
            if (!types.isEmpty()) missing.put(team, List.copyOf(types));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(missing));
    }

    private static boolean hasLocation(Object value) {
        if (value instanceof String location) return !location.isBlank();
        if (value instanceof Collection<?> locations) {
            return locations.stream().anyMatch(location -> location instanceof String text && !text.isBlank());
        }
        return false;
    }
}
