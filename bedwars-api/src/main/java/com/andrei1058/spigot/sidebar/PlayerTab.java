package com.andrei1058.spigot.sidebar;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;

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
     * Optional shared collision identity. Rows with the same value are put in
     * one private scoreboard team so the client follows the server team
     * relationship. A null value retains the legacy per-row identity.
     */
    private final String collisionGroup;
    private final ConcurrentLinkedQueue<PlaceholderProvider> placeholders = new ConcurrentLinkedQueue<>();
    private NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;
    private PlayerListMode playerListMode = PlayerListMode.ACTUAL;
    private ChatColor color = ChatColor.WHITE;
    private NamedTextColor playerListColor;
    private boolean italic;
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

    /** 创建 TAB 行；末尾参数用于为同一游戏队伍共享客户端碰撞队伍。 */
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

    @Nullable
    @Deprecated
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

    public void setItalic(boolean italic) {
        if (this.italic == italic) return;
        this.italic = italic;
        updateCallback.accept(this);
    }

    public boolean isItalic() {
        return italic;
    }

    /** Set the list name independently of the scoreboard team/name tag color. */
    public void setPlayerListColor(@NotNull NamedTextColor color) {
        if (java.util.Objects.equals(playerListColor, color)) return;
        playerListColor = color;
        updateCallback.accept(this);
    }

    public @NotNull NamedTextColor getPlayerListColor() {
        return playerListColor == null ? getTextColor() : playerListColor;
    }

    @NotNull
    public ChatColor getColor() {
        return color;
    }

    /**
     * Return the Adventure color used by Paper scoreboard teams.
     *
     * <p>The ChatColor accessor remains for binary/source compatibility with
     * older add-ons; new integrations should use this method.</p>
     */
    @NotNull
    public NamedTextColor getTextColor() {
        return switch (color) {
            case BLACK -> NamedTextColor.BLACK;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
            case GOLD -> NamedTextColor.GOLD;
            case GRAY -> NamedTextColor.GRAY;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case BLUE -> NamedTextColor.BLUE;
            case GREEN -> NamedTextColor.GREEN;
            case AQUA -> NamedTextColor.AQUA;
            case RED -> NamedTextColor.RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case YELLOW -> NamedTextColor.YELLOW;
            case WHITE -> NamedTextColor.WHITE;
            default -> NamedTextColor.WHITE;
        };
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
