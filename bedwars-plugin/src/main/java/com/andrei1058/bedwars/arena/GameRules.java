package com.andrei1058.bedwars.arena;

import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;

public final class GameRules {

    static final long BEDWARS_DAY_TIME = 1000L;

    private GameRules() {
    }

    static void set(World world, String name, String value) {
        if (world == null || name == null || value == null) return;
        GameRule<?> rule = get(name);
        if (rule == null) return;

        Class<?> type = rule.getType();
        if (Boolean.class.equals(type)) {
            setTyped(world, rule, Boolean.parseBoolean(value));
        } else if (Integer.class.equals(type)) {
            try {
                setTyped(world, rule, Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    static void setBoolean(World world, String name, boolean value) {
        if (world == null || name == null) return;
        GameRule<?> rule = get(name);
        if (rule != null && Boolean.class.equals(rule.getType())) {
            setTyped(world, rule, value);
        }
    }

    /**
     * Locator Bar reveals player positions and is never part of BedWars gameplay.
     */
    public static void disableLocatorBar(World world) {
        setBoolean(world, "locatorBar", false);
    }

    /**
     * Keep setup and live arena worlds at the fixed BedWars daytime.
     */
    public static void enforceDaytime(World world) {
        if (world == null) return;
        setBoolean(world, "doDaylightCycle", false);
        world.setTime(BEDWARS_DAY_TIME);
    }

    /**
     * Minecraft 1.21.11 replaced doFireTick with an integer radius rule.
     * A zero radius prevents arena fire from spreading around players.
     */
    public static void disableFireSpread(World world) {
        set(world, "fireSpreadRadiusAroundPlayer", "0");
    }

    private static GameRule<?> get(String name) {
        return Registry.GAME_RULE.get(NamespacedKey.minecraft(toRegistryKey(name)));
    }

    @SuppressWarnings("unchecked")
    private static <T> void setTyped(World world, GameRule<?> rule, Object value) {
        world.setGameRule((GameRule<T>) rule, (T) value);
    }

    static String toRegistryKey(String name) {
        String raw = name.trim();
        int namespace = raw.indexOf(':');
        if (namespace >= 0) {
            raw = raw.substring(namespace + 1);
        }

        StringBuilder key = new StringBuilder(raw.length() + 4);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && key.charAt(key.length() - 1) != '_') {
                    key.append('_');
                }
                key.append(Character.toLowerCase(c));
            } else if (c == '-' || c == '.' || c == ' ') {
                key.append('_');
            } else {
                key.append(Character.toLowerCase(c));
            }
        }
        return key.toString();
    }
}
