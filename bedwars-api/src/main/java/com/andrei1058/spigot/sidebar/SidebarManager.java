package com.andrei1058.spigot.sidebar;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SidebarManager {

    private static final SidebarManager INSTANCE = new SidebarManager();

    private final Map<Player, String> headerFooterCache = new WeakHashMap<>();

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
        String header = renderLines(headerFooter.getHeader(), headerFooter.getPlaceholders());
        String footer = renderLines(headerFooter.getFooter(), headerFooter.getPlaceholders());
        String cacheKey = header + '\u0000' + footer;
        if (cacheKey.equals(headerFooterCache.get(player))) {
            return;
        }
        player.setPlayerListHeaderFooter(header, footer);
        headerFooterCache.put(player, cacheKey);
    }

    public void clearHeaderFooterCache(@NotNull Player player) {
        headerFooterCache.remove(player);
    }

    public void clearHeaderFooter(@NotNull Player player) {
        player.setPlayerListHeaderFooter("", "");
        clearHeaderFooterCache(player);
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
