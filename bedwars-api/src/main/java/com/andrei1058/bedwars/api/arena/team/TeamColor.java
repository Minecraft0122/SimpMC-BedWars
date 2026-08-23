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
import org.jetbrains.annotations.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

/**
 * Colors supported by a Bed Wars team.
 *
 * <p>Block, item, dye and armor mappings are kept together so a team cannot
 * accidentally receive two different Minecraft colors. Minecraft has no
 * {@code ChatColor.CYAN}; cyan therefore uses the visually matching
 * {@link NamedTextColor#AQUA} for text only.</p>
 */
public enum TeamColor {

    RED(NamedTextColor.RED, DyeColor.RED, 14, Color.RED,
            Material.RED_BED, Material.RED_STAINED_GLASS, Material.RED_STAINED_GLASS_PANE,
            Material.RED_TERRACOTTA, Material.RED_WOOL),
    BLUE(NamedTextColor.BLUE, DyeColor.BLUE, 11, Color.BLUE,
            Material.BLUE_BED, Material.BLUE_STAINED_GLASS, Material.BLUE_STAINED_GLASS_PANE,
            Material.BLUE_TERRACOTTA, Material.BLUE_WOOL),
    GREEN(NamedTextColor.GREEN, DyeColor.LIME, 5, Color.LIME,
            Material.LIME_BED, Material.LIME_STAINED_GLASS, Material.LIME_STAINED_GLASS_PANE,
            Material.LIME_TERRACOTTA, Material.LIME_WOOL),
    YELLOW(NamedTextColor.YELLOW, DyeColor.YELLOW, 4, Color.YELLOW,
            Material.YELLOW_BED, Material.YELLOW_STAINED_GLASS, Material.YELLOW_STAINED_GLASS_PANE,
            Material.YELLOW_TERRACOTTA, Material.YELLOW_WOOL),
    CYAN(NamedTextColor.AQUA, DyeColor.CYAN, 9, Color.TEAL,
            Material.CYAN_BED, Material.CYAN_STAINED_GLASS, Material.CYAN_STAINED_GLASS_PANE,
            Material.CYAN_TERRACOTTA, Material.CYAN_WOOL),
    WHITE(NamedTextColor.WHITE, DyeColor.WHITE, 0, Color.WHITE,
            Material.WHITE_BED, Material.WHITE_STAINED_GLASS, Material.WHITE_STAINED_GLASS_PANE,
            Material.WHITE_TERRACOTTA, Material.WHITE_WOOL),
    PINK(NamedTextColor.LIGHT_PURPLE, DyeColor.PINK, 6, Color.FUCHSIA,
            Material.PINK_BED, Material.PINK_STAINED_GLASS, Material.PINK_STAINED_GLASS_PANE,
            Material.PINK_TERRACOTTA, Material.PINK_WOOL),
    GRAY(NamedTextColor.GRAY, DyeColor.LIGHT_GRAY, 8, Color.GRAY,
            Material.LIGHT_GRAY_BED, Material.LIGHT_GRAY_STAINED_GLASS, Material.LIGHT_GRAY_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_TERRACOTTA, Material.LIGHT_GRAY_WOOL),
    DARK_GREEN(NamedTextColor.DARK_GREEN, DyeColor.GREEN, 13, Color.GREEN,
            Material.GREEN_BED, Material.GREEN_STAINED_GLASS, Material.GREEN_STAINED_GLASS_PANE,
            Material.GREEN_TERRACOTTA, Material.GREEN_WOOL),
    DARK_GRAY(NamedTextColor.DARK_GRAY, DyeColor.GRAY, 7, Color.fromRGB(74, 74, 74),
            Material.GRAY_BED, Material.GRAY_STAINED_GLASS, Material.GRAY_STAINED_GLASS_PANE,
            Material.GRAY_TERRACOTTA, Material.GRAY_WOOL),

    /**
     * Compatibility alias for configurations and add-ons compiled before 2.10.11.
     * New code and newly saved configurations must use {@link #CYAN}.
     */
    @Deprecated
    AQUA(NamedTextColor.AQUA, DyeColor.CYAN, 9, Color.TEAL,
            Material.CYAN_BED, Material.CYAN_STAINED_GLASS, Material.CYAN_STAINED_GLASS_PANE,
            Material.CYAN_TERRACOTTA, Material.CYAN_WOOL);

    private static final TeamColor[] SELECTABLE_VALUES = {
            RED, BLUE, GREEN, YELLOW, CYAN, WHITE, PINK, GRAY, DARK_GREEN, DARK_GRAY
    };

    private final NamedTextColor textColor;
    private final DyeColor dyeColor;
    private final byte legacyItemColor;
    private final Color bukkitColor;
    private final Material bedMaterial;
    private final Material glassMaterial;
    private final Material glassPaneMaterial;
    private final Material terracottaMaterial;
    private final Material woolMaterial;

    TeamColor(NamedTextColor textColor, DyeColor dyeColor, int legacyItemColor, Color bukkitColor,
              Material bedMaterial, Material glassMaterial, Material glassPaneMaterial,
              Material terracottaMaterial, Material woolMaterial) {
        this.textColor = textColor;
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
        if (textColor == NamedTextColor.BLACK) return ChatColor.BLACK;
        if (textColor == NamedTextColor.DARK_BLUE) return ChatColor.DARK_BLUE;
        if (textColor == NamedTextColor.DARK_GREEN) return ChatColor.DARK_GREEN;
        if (textColor == NamedTextColor.DARK_AQUA) return ChatColor.DARK_AQUA;
        if (textColor == NamedTextColor.DARK_RED) return ChatColor.DARK_RED;
        if (textColor == NamedTextColor.DARK_PURPLE) return ChatColor.DARK_PURPLE;
        if (textColor == NamedTextColor.GOLD) return ChatColor.GOLD;
        if (textColor == NamedTextColor.GRAY) return ChatColor.GRAY;
        if (textColor == NamedTextColor.DARK_GRAY) return ChatColor.DARK_GRAY;
        if (textColor == NamedTextColor.BLUE) return ChatColor.BLUE;
        if (textColor == NamedTextColor.GREEN) return ChatColor.GREEN;
        if (textColor == NamedTextColor.AQUA) return ChatColor.AQUA;
        if (textColor == NamedTextColor.RED) return ChatColor.RED;
        if (textColor == NamedTextColor.LIGHT_PURPLE) return ChatColor.LIGHT_PURPLE;
        if (textColor == NamedTextColor.YELLOW) return ChatColor.YELLOW;
        return ChatColor.WHITE;
    }

    /** @return the modern Adventure color for this team. */
    @NotNull
    public TextColor textColor() {
        return textColor;
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
        try {
            TeamColor color = fromWool(Material.valueOf(material.toUpperCase(Locale.ROOT)));
            return color == null ? "" : color.setupName();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    /**
     * Resolve a modern wool block to exactly one canonical team color.
     * Similar names are deliberately not matched by prefixes or substrings:
     * lime/green and light-gray/gray represent different teams.
     *
     * @param material block material to inspect
     * @return matching team color, or {@code null} for unsupported/non-wool materials
     */
    public static @Nullable TeamColor fromWool(@NotNull Material material) {
        return switch (material) {
            case RED_WOOL -> RED;
            case BLUE_WOOL -> BLUE;
            case LIME_WOOL -> GREEN;
            case YELLOW_WOOL -> YELLOW;
            case CYAN_WOOL -> CYAN;
            case WHITE_WOOL -> WHITE;
            case PINK_WOOL -> PINK;
            case LIGHT_GRAY_WOOL -> GRAY;
            case GREEN_WOOL -> DARK_GREEN;
            case GRAY_WOOL -> DARK_GRAY;
            default -> null;
        };
    }

    /**
     * Name used for teams created by the assisted arena scanner.
     */
    public @NotNull String setupName() {
        StringBuilder result = new StringBuilder();
        for (String part : name().toLowerCase(Locale.ROOT).split("_")) {
            if (!result.isEmpty()) result.append('_');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
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
