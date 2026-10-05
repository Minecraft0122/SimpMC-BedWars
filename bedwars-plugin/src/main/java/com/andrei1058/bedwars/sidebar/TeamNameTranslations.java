package com.andrei1058.bedwars.sidebar;

import java.util.Map;

/** Canonical Minecraft team-color names used by the optional Chinese TAB mode. */
final class TeamNameTranslations {
    private static final Map<String, String> ZH = Map.ofEntries(
            Map.entry("Red", "红"), Map.entry("Blue", "蓝"), Map.entry("Green", "绿"),
            Map.entry("Yellow", "黄"), Map.entry("Cyan", "青"), Map.entry("White", "白"),
            Map.entry("Pink", "粉"), Map.entry("Gray", "灰"), Map.entry("Dark_Green", "深绿"),
            Map.entry("Dark_Gray", "深灰"), Map.entry("Aqua", "青"));

    private TeamNameTranslations() {}

    static String chinese(String name) {
        if (name == null) return "";
        return ZH.getOrDefault(name, name);
    }
}
