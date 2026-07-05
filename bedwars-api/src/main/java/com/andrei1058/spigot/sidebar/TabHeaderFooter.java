package com.andrei1058.spigot.sidebar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TabHeaderFooter {

    private final List<SidebarLine> header;
    private final List<SidebarLine> footer;
    private final ConcurrentLinkedQueue<PlaceholderProvider> placeholders;

    public TabHeaderFooter(@NotNull Collection<SidebarLine> header, @NotNull Collection<SidebarLine> footer,
                           @NotNull ConcurrentLinkedQueue<PlaceholderProvider> placeholders) {
        this.header = new ArrayList<>(header);
        this.footer = new ArrayList<>(footer);
        this.placeholders = placeholders;
    }

    @NotNull
    public List<SidebarLine> getHeader() {
        return header;
    }

    @NotNull
    public List<SidebarLine> getFooter() {
        return footer;
    }

    @NotNull
    public ConcurrentLinkedQueue<PlaceholderProvider> getPlaceholders() {
        return placeholders;
    }
}
