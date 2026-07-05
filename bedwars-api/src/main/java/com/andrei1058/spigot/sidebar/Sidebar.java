package com.andrei1058.spigot.sidebar;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Sidebar {

    private SidebarLine title;
    private final List<SidebarLine> lines = new ArrayList<>();
    private final ConcurrentLinkedQueue<PlaceholderProvider> placeholders = new ConcurrentLinkedQueue<>();

    public Sidebar(@NotNull SidebarLine title, @NotNull Collection<SidebarLine> lines,
                   @NotNull Collection<PlaceholderProvider> placeholders) {
        this.title = title;
        this.lines.addAll(lines);
        this.placeholders.addAll(placeholders);
    }

    public void add(@NotNull Player player) {
    }

    public void remove(@NotNull Player player) {
    }

    public void clearLines() {
        lines.clear();
    }

    @NotNull
    public Collection<PlaceholderProvider> getPlaceholders() {
        return placeholders;
    }

    public void removePlaceholder(@NotNull String placeholder) {
        placeholders.removeIf(provider -> provider.getPlaceholder().equals(placeholder));
    }

    public void addPlaceholder(@NotNull PlaceholderProvider placeholderProvider) {
        placeholders.add(placeholderProvider);
    }

    public void setTitle(@NotNull SidebarLine title) {
        this.title = title;
    }

    @NotNull
    public SidebarLine getTitle() {
        return title;
    }

    public void addLine(@NotNull SidebarLine line) {
        lines.add(line);
    }

    @NotNull
    public List<SidebarLine> getLines() {
        return lines;
    }

    public void refreshTitle() {
    }

    public void refreshPlaceholders() {
    }

    public void playerTabRefreshAnimation() {
    }

    public void playerHealthRefreshAnimation() {
    }

    public void setPlayerHealth(@NotNull Player player, int health) {
    }

    public void removeTabs() {
    }

    public void hidePlayersHealth() {
    }

    public void showPlayersHealth(@NotNull SidebarLine line, boolean inTab) {
    }

    @NotNull
    public PlayerTab playerTabCreate(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                                     @NotNull SidebarLine suffix, @NotNull PlayerTab.PushingRule pushingRule,
                                     @NotNull ConcurrentLinkedQueue<PlaceholderProvider> placeholders) {
        return new PlayerTab(identifier, player);
    }

    public void removeTab(@NotNull String identifier) {
    }
}
