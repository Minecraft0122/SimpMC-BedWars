package com.andrei1058.spigot.sidebar;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SidebarManager {

    private static final SidebarManager INSTANCE = new SidebarManager();

    private SidebarManager() {
    }

    public static SidebarManager init() {
        return null;
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
    }
}
