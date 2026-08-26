package com.andrei1058.spigot.sidebar;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

    private final String identifier;
    private final Player player;
    private final SidebarLine prefix;
    private final SidebarLine suffix;
    private final PushingRule pushingRule;
    private final ConcurrentLinkedQueue<PlaceholderProvider> placeholders = new ConcurrentLinkedQueue<>();
    private NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;
    private ChatColor color = ChatColor.WHITE;
    private Consumer<PlayerTab> updateCallback = tab -> {
    };

    public PlayerTab(@NotNull String identifier, @NotNull Player player) {
        this(identifier, player, new SidebarLine(), new SidebarLine(), PushingRule.NEVER,
                new ConcurrentLinkedQueue<>());
    }

    public PlayerTab(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                     @NotNull SidebarLine suffix, @NotNull PushingRule pushingRule,
                     @NotNull Collection<PlaceholderProvider> placeholders) {
        this.identifier = identifier;
        this.player = player;
        this.prefix = prefix;
        this.suffix = suffix;
        this.pushingRule = pushingRule;
        this.placeholders.addAll(placeholders);
    }

    /**
     * Creates a tab row with the scoreboard color used by the player's team.
     * The overload keeps the old constructor source-compatible for add-ons.
     */
    public PlayerTab(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                     @NotNull SidebarLine suffix, @NotNull PushingRule pushingRule,
                     @NotNull Collection<PlaceholderProvider> placeholders, @NotNull ChatColor color) {
        this(identifier, player, prefix, suffix, pushingRule, placeholders);
        this.color = color;
    }

    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public void setNameTagVisibility(@NotNull NameTagVisibility nameTagVisibility) {
        this.nameTagVisibility = nameTagVisibility;
        updateCallback.accept(this);
    }

    @NotNull
    public NameTagVisibility getNameTagVisibility() {
        return nameTagVisibility;
    }

    public void setColor(@NotNull ChatColor color) {
        if (this.color == color) {
            return;
        }
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
    Collection<PlaceholderProvider> getPlaceholders() {
        return placeholders;
    }

    void setUpdateCallback(@NotNull Consumer<PlayerTab> updateCallback) {
        this.updateCallback = updateCallback;
    }
}
