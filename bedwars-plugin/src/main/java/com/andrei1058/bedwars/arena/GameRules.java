package com.andrei1058.bedwars.arena;

import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;

import java.util.Objects;

public final class GameRules {

    /** Equivalent to the vanilla command {@code /time set noon}. */
    static final long VANILLA_NOON_TIME = 6000L;
    private static final long TICKS_PER_DAY = 24000L;

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

    /**
     * Locator Bar reveals player positions and is never part of BedWars gameplay.
     */
    public static void disableLocatorBar(World world) {
        if (world != null) setTyped(world, org.bukkit.GameRules.LOCATOR_BAR, false);
    }

    /**
     * Set the requested fixed time once and disable vanilla time progression.
     */
    public static void enforceFixedTime(World world) {
        if (world == null) return;
        setTyped(world, org.bukkit.GameRules.ADVANCE_TIME, false);
        if (world.getTime() != VANILLA_NOON_TIME) world.setTime(VANILLA_NOON_TIME);
    }

    /**
     * Keep a BedWars world at the brightest vanilla daytime state.
     * Weather and daylight progression are disabled once; later attempts to
     * change them are rejected by the corresponding event listeners.
     */
    public static void enforceBrightNoon(World world) {
        if (world == null) return;
        setTyped(world, org.bukkit.GameRules.ADVANCE_WEATHER, false);
        enforceFixedTime(world);
        if (world.hasStorm()) world.setStorm(false);
        if (world.isThundering()) world.setThundering(false);
    }

    /**
     * Calculate a skip that lands exactly on the fixed BedWars time.
     * Near {@link Long#MAX_VALUE}, use the equivalent previous-day offset so
     * Paper can add the skip to the full-time value without overflowing.
     */
    public static long skipAmountToFixedTime(long currentFullTime) {
        long currentDayTime = Math.floorMod(currentFullTime, TICKS_PER_DAY);
        long forwardSkip = Math.floorMod(VANILLA_NOON_TIME - currentDayTime, TICKS_PER_DAY);
        return forwardSkip > 0L && currentFullTime > Long.MAX_VALUE - forwardSkip
                ? forwardSkip - TICKS_PER_DAY
                : forwardSkip;
    }

    /**
     * Disable vanilla random block ticks such as leaf decay, crop growth and
     * grass or mushroom spread. Player-driven block mechanics remain enabled.
     */
    public static void disableNaturalBlockTicks(World world) {
        if (world != null) setTyped(world, org.bukkit.GameRules.RANDOM_TICK_SPEED, 0);
    }

    /**
     * Apply the non-negotiable BedWars environment when a world or arena is initialized.
     */
    public static void enforceArenaEnvironment(World world) {
        if (world == null) return;
        setTyped(world, org.bukkit.GameRules.SPAWN_MOBS, false);
        disableNaturalBlockTicks(world);
        enforceBrightNoon(world);
        disableFireSpread(world);
        disableLocatorBar(world);
    }

    /**
     * Minecraft 1.21.11 replaced doFireTick with an integer radius rule.
     * A zero radius prevents arena fire from spreading around players.
     */
    public static void disableFireSpread(World world) {
        if (world != null) setTyped(world, org.bukkit.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
    }

    private static GameRule<?> get(String name) {
        String registryKey = toRegistryKey(name);
        return registryKey.isEmpty() ? null : Registry.GAME_RULE.get(NamespacedKey.minecraft(registryKey));
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
        return switch (key.toString()) {
            // Paper 1.21.11 renamed these registry keys. Arena files keep
            // accepting their long-standing Bukkit names for compatibility.
            case "do_daylight_cycle" -> "advance_time";
            case "do_weather_cycle" -> "advance_weather";
            case "do_mob_spawning" -> "spawn_mobs";
            case "announce_advancements" -> "show_advancement_messages";
            case "do_insomnia" -> "spawn_phantoms";
            case "do_immediate_respawn" -> "immediate_respawn";
            default -> key.toString();
        };
    }
}
