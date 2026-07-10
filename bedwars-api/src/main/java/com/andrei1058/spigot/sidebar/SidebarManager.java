package com.andrei1058.spigot.sidebar;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SidebarManager {

    private static final SidebarManager INSTANCE = new SidebarManager();

    private SidebarManager() {
    }

    public static SidebarManager init() {
        return INSTANCE;
    }

    @NotNull
    public static SidebarManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    public Sidebar createSidebar(@NotNull SidebarLine title, @NotNull Collection<SidebarLine> lines,
                                 @NotNull ConcurrentLinkedQueue<PlaceholderProvider> placeholders) {
        return new Sidebar(title, lines, placeholders);
    }

    public void sendHeaderFooter(@NotNull Player player, @NotNull TabHeaderFooter headerFooter) {
        player.setPlayerListHeaderFooter(
                renderLines(headerFooter.getHeader(), headerFooter.getPlaceholders()),
                renderLines(headerFooter.getFooter(), headerFooter.getPlaceholders())
        );
    }

    private static String renderLines(@NotNull List<SidebarLine> lines,
                                      @NotNull Collection<PlaceholderProvider> placeholders) {
        List<String> rendered = new ArrayList<>(lines.size());
        for (SidebarLine line : lines) {
            rendered.add(Sidebar.renderText(line, placeholders));
        }
        return String.join("\n", rendered);
    }
}
