package com.andrei1058.bedwars.api;

import org.bukkit.GameRule;

/** Verifies the exact Paper 1.21.11 game-rule registry contract used by the plugin. */
public final class PaperGameRulesRuntimeProbe {

    private PaperGameRulesRuntimeProbe() {
    }

    public static void main(String[] args) {
        bootstrapMinecraftRegistries();
        requireRule(org.bukkit.GameRules.ADVANCE_TIME, "advance_time", Boolean.class);
        requireRule(org.bukkit.GameRules.ADVANCE_WEATHER, "advance_weather", Boolean.class);
        requireRule(org.bukkit.GameRules.SPAWN_MOBS, "spawn_mobs", Boolean.class);
        requireRule(org.bukkit.GameRules.RANDOM_TICK_SPEED, "random_tick_speed", Integer.class);
        requireRule(org.bukkit.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER,
                "fire_spread_radius_around_player", Integer.class);
        requireRule(org.bukkit.GameRules.LOCATOR_BAR, "locator_bar", Boolean.class);
        requireRule(org.bukkit.GameRules.SHOW_ADVANCEMENT_MESSAGES,
                "show_advancement_messages", Boolean.class);
        requireRule(org.bukkit.GameRules.SPAWN_PHANTOMS, "spawn_phantoms", Boolean.class);
        requireRule(org.bukkit.GameRules.IMMEDIATE_RESPAWN, "immediate_respawn", Boolean.class);
    }

    private static void requireRule(GameRule<?> rule, String expectedKey, Class<?> expectedType) {
        if (!rule.getKey().getKey().equals(expectedKey) || !rule.getType().equals(expectedType)) {
            throw new IllegalStateException("Unexpected Paper game rule contract for " + expectedKey);
        }
    }

    private static void bootstrapMinecraftRegistries() {
        try {
            Class.forName("net.minecraft.SharedConstants").getMethod("tryDetectVersion").invoke(null);
            Class.forName("net.minecraft.server.Bootstrap").getMethod("bootStrap").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to bootstrap Paper 1.21.11 runtime", exception);
        }
    }
}
