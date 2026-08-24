package com.andrei1058.bedwars.arena;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Fallback text for administrator-defined lobby command items. */
final class LobbyItemText {

    private static final List<String> RETURN_LORE = List.of("&f右键返回主大厅！");

    private LobbyItemText() {
    }

    @NotNull
    static String fallbackName(@Nullable String id, @Nullable String command, @Nullable String mainCommand) {
        if (CommandItemAction.isLeaveItemDefinition(id, command, mainCommand)) return "&e回到主大厅";
        return "&f" + (id == null || id.isBlank() ? "大厅物品" : id);
    }

    @NotNull
    static List<String> fallbackLore(@Nullable String id, @Nullable String command, @Nullable String mainCommand) {
        return CommandItemAction.isLeaveItemDefinition(id, command, mainCommand) ? RETURN_LORE : List.of();
    }

    static boolean isGeneratedName(@Nullable String value, @NotNull String path) {
        return ("&cName not set at: &f" + path).equals(value);
    }

    static boolean isGeneratedLore(List<String> lines, @NotNull String path) {
        return List.of("&cLore not set at:", " &f" + path).equals(lines);
    }
}
