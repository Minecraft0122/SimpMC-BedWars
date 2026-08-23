/*
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.api.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Nameable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * Central Adventure conversion helpers for legacy configuration text.
 *
 * <p>Configuration files intentionally continue to use Minecraft's legacy
 * section and ampersand formats for backwards compatibility. The deprecated
 * Bukkit and Bungee text APIs are not used at the boundary: text is converted
 * once and then passed through Paper's native Adventure API.</p>
 */
public final class AdventureText {

    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private AdventureText() {
    }

    /** Deserialize text containing either Minecraft {@code §} or config {@code &} codes. */
    @NotNull
    public static Component section(@Nullable String text) {
        return AMPERSAND.deserialize(normalize(text));
    }

    /** Deserialize text containing configuration-style {@code &} codes. */
    @NotNull
    public static Component ampersand(@Nullable String text) {
        return AMPERSAND.deserialize(normalize(text));
    }

    /** Serialize a component for a legacy configuration or protocol field. */
    @NotNull
    public static String section(@Nullable Component component) {
        return SECTION.serialize(component == null ? Component.empty() : component);
    }

    /** Return the visible text without formatting codes. */
    @NotNull
    public static String plain(@Nullable Component component) {
        return PLAIN.serialize(component == null ? Component.empty() : component);
    }

    /** Send legacy section-formatted text through the native Audience API. */
    public static void send(@NotNull Audience audience, @Nullable String text) {
        audience.sendMessage(section(text));
    }

    /** Send an already-composed Adventure component through the native Audience API. */
    public static void send(@NotNull Audience audience, @Nullable Component component) {
        audience.sendMessage(component == null ? Component.empty() : component);
    }

    /** Parse a legacy list for ItemMeta.lore(List&lt;Component&gt;). */
    @NotNull
    public static List<Component> lore(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        return lines.stream().map(AdventureText::section).toList();
    }

    public static void displayName(@NotNull ItemMeta meta, @Nullable String text) {
        meta.displayName(section(text));
    }

    public static void lore(@NotNull ItemMeta meta, @Nullable List<String> lines) {
        meta.lore(lore(lines));
    }

    public static void customName(@NotNull Nameable nameable, @Nullable String text) {
        nameable.customName(text == null ? null : section(text));
    }

    @NotNull
    public static String displayName(@NotNull Player player) {
        return section(player.displayName());
    }

    @NotNull
    public static String plainDisplayName(@NotNull Player player) {
        return plain(player.displayName());
    }

    @NotNull
    public static String customName(@NotNull Nameable nameable) {
        return plain(nameable.customName());
    }

    /** Convert Minecraft ticks into Adventure title durations. */
    @NotNull
    public static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    private static String normalize(@Nullable String text) {
        return (text == null ? "" : text).replace('\u00a7', '&');
    }
}
