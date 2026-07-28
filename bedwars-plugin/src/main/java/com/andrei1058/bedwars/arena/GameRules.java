package com.andrei1058.bedwars.arena;

import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;

import java.util.Objects;

public final class GameRules {

    static final long BEDWARS_DAY_TIME = 6000L;

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
        if (world.getTime() != BEDWARS_DAY_TIME) world.setTime(BEDWARS_DAY_TIME);
    }

    /**
     * Disable vanilla random block ticks such as leaf decay, crop growth and
     * grass or mushroom spread. Player-driven block mechanics remain enabled.
     */
    public static void disableNaturalBlockTicks(World world) {
        set(world, "randomTickSpeed", "0");
    }

    /**
     * Re-apply the non-negotiable BedWars world environment. This method is
     * intentionally idempotent so it can also be used by the periodic guard.
     */
    public static void enforceArenaEnvironment(World world) {
        if (world == null) return;
        setBoolean(world, "doMobSpawning", false);
        setBoolean(world, "doWeatherCycle", false);
        disableNaturalBlockTicks(world);
        enforceDaytime(world);
        disableFireSpread(world);
        disableLocatorBar(world);
        if (world.hasStorm()) world.setStorm(false);
        if (world.isThundering()) world.setThundering(false);
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
        GameRule<T> typedRule = (GameRule<T>) rule;
        T typedValue = (T) value;
        if (!Objects.equals(world.getGameRuleValue(typedRule), typedValue)) {
            world.setGameRule(typedRule, typedValue);
        }
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
