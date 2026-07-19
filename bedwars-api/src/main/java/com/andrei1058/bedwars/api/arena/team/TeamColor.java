/*
 * BedWars1058 - a Bed Wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.api.arena.team;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Colors supported by a Bed Wars team.
 *
 * <p>Block, item, dye and armor mappings are kept together so a team cannot
 * accidentally receive two different Minecraft colors. Minecraft has no
 * {@code ChatColor.CYAN}; cyan therefore uses the visually matching
 * {@link ChatColor#AQUA} for text only.</p>
 */
public enum TeamColor {

    RED(ChatColor.RED, DyeColor.RED, 14, Color.RED,
            Material.RED_BED, Material.RED_STAINED_GLASS, Material.RED_STAINED_GLASS_PANE,
            Material.RED_TERRACOTTA, Material.RED_WOOL),
    BLUE(ChatColor.BLUE, DyeColor.BLUE, 11, Color.BLUE,
            Material.BLUE_BED, Material.BLUE_STAINED_GLASS, Material.BLUE_STAINED_GLASS_PANE,
            Material.BLUE_TERRACOTTA, Material.BLUE_WOOL),
    GREEN(ChatColor.GREEN, DyeColor.LIME, 5, Color.LIME,
            Material.LIME_BED, Material.LIME_STAINED_GLASS, Material.LIME_STAINED_GLASS_PANE,
            Material.LIME_TERRACOTTA, Material.LIME_WOOL),
    YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, 4, Color.YELLOW,
            Material.YELLOW_BED, Material.YELLOW_STAINED_GLASS, Material.YELLOW_STAINED_GLASS_PANE,
            Material.YELLOW_TERRACOTTA, Material.YELLOW_WOOL),
    CYAN(ChatColor.AQUA, DyeColor.CYAN, 9, Color.TEAL,
            Material.CYAN_BED, Material.CYAN_STAINED_GLASS, Material.CYAN_STAINED_GLASS_PANE,
            Material.CYAN_TERRACOTTA, Material.CYAN_WOOL),
    WHITE(ChatColor.WHITE, DyeColor.WHITE, 0, Color.WHITE,
            Material.WHITE_BED, Material.WHITE_STAINED_GLASS, Material.WHITE_STAINED_GLASS_PANE,
            Material.WHITE_TERRACOTTA, Material.WHITE_WOOL),
    PINK(ChatColor.LIGHT_PURPLE, DyeColor.PINK, 6, Color.FUCHSIA,
            Material.PINK_BED, Material.PINK_STAINED_GLASS, Material.PINK_STAINED_GLASS_PANE,
            Material.PINK_TERRACOTTA, Material.PINK_WOOL),
    GRAY(ChatColor.GRAY, DyeColor.LIGHT_GRAY, 8, Color.GRAY,
            Material.LIGHT_GRAY_BED, Material.LIGHT_GRAY_STAINED_GLASS, Material.LIGHT_GRAY_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_TERRACOTTA, Material.LIGHT_GRAY_WOOL),
    DARK_GREEN(ChatColor.DARK_GREEN, DyeColor.GREEN, 13, Color.GREEN,
            Material.GREEN_BED, Material.GREEN_STAINED_GLASS, Material.GREEN_STAINED_GLASS_PANE,
            Material.GREEN_TERRACOTTA, Material.GREEN_WOOL),
    DARK_GRAY(ChatColor.DARK_GRAY, DyeColor.GRAY, 7, Color.fromRGB(74, 74, 74),
            Material.GRAY_BED, Material.GRAY_STAINED_GLASS, Material.GRAY_STAINED_GLASS_PANE,
            Material.GRAY_TERRACOTTA, Material.GRAY_WOOL),

    /**
     * Compatibility alias for configurations and add-ons compiled before 2.10.11.
     * New code and newly saved configurations must use {@link #CYAN}.
     */
    @Deprecated
    AQUA(ChatColor.AQUA, DyeColor.CYAN, 9, Color.TEAL,
            Material.CYAN_BED, Material.CYAN_STAINED_GLASS, Material.CYAN_STAINED_GLASS_PANE,
            Material.CYAN_TERRACOTTA, Material.CYAN_WOOL);

    private static final TeamColor[] SELECTABLE_VALUES = {
            RED, BLUE, GREEN, YELLOW, CYAN, WHITE, PINK, GRAY, DARK_GREEN, DARK_GRAY
    };

    private final ChatColor chatColor;
    private final DyeColor dyeColor;
    private final byte legacyItemColor;
    private final Color bukkitColor;
    private final Material bedMaterial;
    private final Material glassMaterial;
    private final Material glassPaneMaterial;
    private final Material terracottaMaterial;
    private final Material woolMaterial;

    TeamColor(ChatColor chatColor, DyeColor dyeColor, int legacyItemColor, Color bukkitColor,
              Material bedMaterial, Material glassMaterial, Material glassPaneMaterial,
              Material terracottaMaterial, Material woolMaterial) {
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
        this.legacyItemColor = (byte) legacyItemColor;
        this.bukkitColor = bukkitColor;
        this.bedMaterial = bedMaterial;
        this.glassMaterial = glassMaterial;
        this.glassPaneMaterial = glassPaneMaterial;
        this.terracottaMaterial = terracottaMaterial;
        this.woolMaterial = woolMaterial;
    }

    /**
     * Parse a configured team color and normalize the legacy AQUA name to CYAN.
     *
     * @param name configured color name
     * @return canonical team color
     * @throws IllegalArgumentException if the name is not supported
     */
    public static @NotNull TeamColor fromName(@NotNull String name) {
        TeamColor parsed = TeamColor.valueOf(name.trim().toUpperCase(Locale.ROOT));
        return parsed == AQUA ? CYAN : parsed;
    }

    /**
     * Return colors that may be selected for a new team. The deprecated AQUA
     * compatibility alias is intentionally omitted.
     */
    public static TeamColor[] selectableValues() {
        return SELECTABLE_VALUES.clone();
    }

    /**
     * Get chat color by configured team color name.
     */
    public static ChatColor getChatColor(@NotNull String teamColor) {
        return fromName(teamColor).chat();
    }

    /**
     * @return chat color for this team color
     */
    public ChatColor chat() {
        return chatColor;
    }

    /**
     * @return chat color for this team color
     */
    @Deprecated
    public static ChatColor getChatColor(@NotNull TeamColor teamColor) {
        return teamColor.chat();
    }

    /**
     * @return dye color for the configured team color
     */
    @Deprecated
    public static DyeColor getDyeColor(@NotNull String teamColor) {
        return fromName(teamColor).dye();
    }

    /**
     * @return equivalent Minecraft dye color
     */
    public DyeColor dye() {
        return dyeColor;
    }

    /**
     * @return legacy wool color data value for Minecraft 1.12 and older
     */
    @Deprecated
    public static byte itemColor(@NotNull TeamColor teamColor) {
        return teamColor.itemByte();
    }

    /**
     * @return legacy wool color data value for Minecraft 1.12 and older
     */
    public byte itemByte() {
        return legacyItemColor;
    }

    /**
     * Get the supported team color name represented by a modern wool material.
     *
     * @return English color name, or an empty string when unsupported
     */
    public static @NotNull String enName(@NotNull String material) {
        return switch (material.toUpperCase(Locale.ROOT)) {
            case "PINK_WOOL" -> "Pink";
            case "RED_WOOL" -> "Red";
            case "LIGHT_GRAY_WOOL" -> "Gray";
            case "BLUE_WOOL" -> "Blue";
            case "WHITE_WOOL" -> "White";
            case "CYAN_WOOL" -> "Cyan";
            case "LIME_WOOL" -> "Green";
            case "YELLOW_WOOL" -> "Yellow";
            case "GRAY_WOOL" -> "Dark_Gray";
            case "GREEN_WOOL" -> "Dark_Green";
            default -> "";
        };
    }

    /**
     * Get the supported team color name represented by legacy wool data.
     *
     * @return English color name, or an empty string when unsupported
     */
    public static @NotNull String enName(byte color) {
        return switch (color) {
            case 6 -> "Pink";
            case 14 -> "Red";
            case 9 -> "Cyan";
            case 5 -> "Green";
            case 4 -> "Yellow";
            case 11 -> "Blue";
            case 0 -> "White";
            case 8 -> "Gray";
            case 7 -> "Dark_Gray";
            case 13 -> "Dark_Green";
            default -> "";
        };
    }

    /**
     * @return equivalent Bukkit color for leather armor
     */
    @Deprecated
    public static Color getColor(@NotNull TeamColor teamColor) {
        return teamColor.bukkitColor();
    }

    /**
     * @return equivalent Bukkit color for leather armor
     */
    public Color bukkitColor() {
        return bukkitColor;
    }

    @Deprecated
    public static Material getBedBlock(@NotNull TeamColor teamColor) {
        return teamColor.bedMaterial();
    }

    public Material bedMaterial() {
        return bedMaterial;
    }

    @Deprecated
    public static Material getGlass(@NotNull TeamColor teamColor) {
        return teamColor.glassMaterial();
    }

    public Material glassMaterial() {
        return glassMaterial;
    }

    @Deprecated
    public static Material getGlassPane(@NotNull TeamColor teamColor) {
        return teamColor.glassPaneMaterial();
    }

    public Material glassPaneMaterial() {
        return glassPaneMaterial;
    }

    @Deprecated
    public static Material getGlazedTerracotta(@NotNull TeamColor teamColor) {
        return teamColor.glazedTerracottaMaterial();
    }

    public Material glazedTerracottaMaterial() {
        return terracottaMaterial;
    }

    @Deprecated
    public static Material getWool(@NotNull TeamColor teamColor) {
        return teamColor.woolMaterial();
    }

    public Material woolMaterial() {
        return woolMaterial;
    }
}
