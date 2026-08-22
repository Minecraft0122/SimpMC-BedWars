package com.andrei1058.spigot.sidebar;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class PlayerTab {

    public enum PushingRule {
        NEVER,
        PUSH_OTHER_TEAMS
    }

    public enum NameTagVisibility {
        ALWAYS,
        NEVER
    }

    /**
     * Controls only the game-mode style advertised for this player-list row.
     * The target player's real Bukkit game mode is never changed.
     */
    public enum PlayerListMode {
        ACTUAL,
        SPECTATOR
    }

    private final String identifier;
    private final Player player;
    private final SidebarLine prefix;
    private final SidebarLine suffix;
    private final PushingRule pushingRule;
    /**
     * Optional scoreboard collision group. Rows keep their own identifiers
     * for TAB rendering, while members of the same game team can share one
     * scoreboard team for collision rules.
     */
    private final String collisionGroup;
    private final ConcurrentLinkedQueue<PlaceholderProvider> placeholders = new ConcurrentLinkedQueue<>();
    private NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;
    private PlayerListMode playerListMode = PlayerListMode.ACTUAL;
    private ChatColor color = ChatColor.WHITE;
    private Consumer<PlayerTab> updateCallback = tab -> {
    };

    public PlayerTab(@NotNull String identifier, @NotNull Player player) {
        this(identifier, player, new SidebarLine(), new SidebarLine(), PushingRule.NEVER, new ConcurrentLinkedQueue<>());
    }

    public PlayerTab(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                     @NotNull SidebarLine suffix, @NotNull PushingRule pushingRule,
                     @NotNull Collection<PlaceholderProvider> placeholders) {
        this(identifier, player, prefix, suffix, pushingRule, placeholders,
                ChatColor.WHITE, NameTagVisibility.ALWAYS);
    }

    public PlayerTab(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                     @NotNull SidebarLine suffix, @NotNull PushingRule pushingRule,
                     @NotNull Collection<PlaceholderProvider> placeholders, @NotNull ChatColor color,
                     @NotNull NameTagVisibility nameTagVisibility) {
        this(identifier, player, prefix, suffix, pushingRule, placeholders, color,
                nameTagVisibility, PlayerListMode.ACTUAL);
    }

    public PlayerTab(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                     @NotNull SidebarLine suffix, @NotNull PushingRule pushingRule,
                     @NotNull Collection<PlaceholderProvider> placeholders, @NotNull ChatColor color,
                     @NotNull NameTagVisibility nameTagVisibility,
                     @NotNull PlayerListMode playerListMode) {
        this(identifier, player, prefix, suffix, pushingRule, placeholders, color,
                nameTagVisibility, playerListMode, null);
    }

    /**
     * Creates a row with an optional shared collision group. The group is
     * deliberately separate from the row identifier because a TAB entry may
     * have unique display formatting while its in-game team must be shared.
     */
    public PlayerTab(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                     @NotNull SidebarLine suffix, @NotNull PushingRule pushingRule,
                     @NotNull Collection<PlaceholderProvider> placeholders, @NotNull ChatColor color,
                     @NotNull NameTagVisibility nameTagVisibility,
                     @NotNull PlayerListMode playerListMode,
                     @Nullable String collisionGroup) {
        this.identifier = identifier;
        this.player = player;
        this.prefix = prefix;
        this.suffix = suffix;
        this.pushingRule = pushingRule;
        this.placeholders.addAll(placeholders);
        this.color = color;
        this.nameTagVisibility = nameTagVisibility;
        this.playerListMode = playerListMode;
        this.collisionGroup = collisionGroup;
    }

    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the shared collision group, or {@code null} for a row that must
     * retain its private scoreboard team.
     */
    @Nullable
    public String getCollisionGroup() {
        return collisionGroup;
    }

    public void setNameTagVisibility(@NotNull NameTagVisibility nameTagVisibility) {
        this.nameTagVisibility = nameTagVisibility;
        updateCallback.accept(this);
    }

    @NotNull
    public NameTagVisibility getNameTagVisibility() {
        return nameTagVisibility;
    }

    public void setPlayerListMode(@NotNull PlayerListMode playerListMode) {
        if (this.playerListMode == playerListMode) return;
        this.playerListMode = playerListMode;
        updateCallback.accept(this);
    }

    @NotNull
    public PlayerListMode getPlayerListMode() {
        return playerListMode;
    }

    public void setColor(@NotNull ChatColor color) {
        if (this.color == color) return;
        this.color = color;
        updateCallback.accept(this);
    }

    @NotNull
    public ChatColor getColor() {
        return color;
    }

    @NotNull
    SidebarLine getPrefix() {
        return prefix;
    }

    @NotNull
    SidebarLine getSuffix() {
        return suffix;
    }

    @NotNull
    PushingRule getPushingRule() {
        return pushingRule;
    }

    @NotNull
    ConcurrentLinkedQueue<PlaceholderProvider> getPlaceholders() {
        return placeholders;
    }

    void setUpdateCallback(@NotNull Consumer<PlayerTab> updateCallback) {
        this.updateCallback = updateCallback;
    }
}
