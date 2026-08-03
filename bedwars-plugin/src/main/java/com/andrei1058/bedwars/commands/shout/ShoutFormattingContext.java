package com.andrei1058.bedwars.commands.shout;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Marks the nested PlaceholderAPI pass that formats one shout message.
 * The context is bound to the current chat thread and is always restored,
 * including when a placeholder expansion fails.
 */
public final class ShoutFormattingContext {

    private static final ThreadLocal<UUID> SHOUTING_PLAYER = new ThreadLocal<>();

    private ShoutFormattingContext() {
    }

    public static <T> T format(@NotNull Player player, @NotNull Supplier<T> formatter) {
        return format(player.getUniqueId(), formatter);
    }

    public static boolean isFormatting(@NotNull Player player) {
        return isFormatting(player.getUniqueId());
    }

    static <T> T format(@NotNull UUID playerId, @NotNull Supplier<T> formatter) {
        UUID previousPlayer = SHOUTING_PLAYER.get();
        SHOUTING_PLAYER.set(playerId);
        try {
            return formatter.get();
        } finally {
            if (previousPlayer == null) {
                SHOUTING_PLAYER.remove();
            } else {
                SHOUTING_PLAYER.set(previousPlayer);
            }
        }
    }

    static boolean isFormatting(@NotNull UUID playerId) {
        return playerId.equals(SHOUTING_PLAYER.get());
    }
}
