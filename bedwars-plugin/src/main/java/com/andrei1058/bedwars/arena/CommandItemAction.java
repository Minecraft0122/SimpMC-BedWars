package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores actions whose meaning must not depend on the player's current world
 * or on a configurable command string. This is especially important for the
 * three visually identical return-bed items.
 */
public final class CommandItemAction {

    private static final String TARGET_TAG = "return_item_target";

    private CommandItemAction() {
    }

    public enum Target {
        PROXY_LOBBY,
        ARENA_LOBBY
    }

    /**
     * Mark configured leave items while preserving their legacy RUNCOMMAND tag.
     */
    public static ItemStack tagReturnItem(@NotNull ItemStack itemStack, @Nullable String itemId,
                                          @Nullable String command, @Nullable String mainCommand,
                                          @NotNull Target target) {
        if (!isLeaveItemDefinition(itemId, command, mainCommand)) return itemStack;
        return BedWars.nms.setTag(itemStack, TARGET_TAG, target.name());
    }

    @Nullable
    public static Target readTarget(@Nullable ItemStack itemStack) {
        if (itemStack == null) return null;
        return parseTarget(BedWars.nms.getTag(itemStack, TARGET_TAG));
    }

    static boolean isLeaveItemDefinition(@Nullable String itemId, @Nullable String command,
                                         @Nullable String mainCommand) {
        if (itemId != null && itemId.equalsIgnoreCase("leave")) return true;
        return command != null && mainCommand != null
                && command.trim().equalsIgnoreCase(mainCommand.trim() + " leave");
    }

    @Nullable
    static Target parseTarget(@Nullable String storedTarget) {
        if (storedTarget == null || storedTarget.isBlank()) return null;
        try {
            return Target.valueOf(storedTarget.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
