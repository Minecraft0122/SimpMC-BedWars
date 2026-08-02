package com.andrei1058.spigot.sidebar;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SidebarManager {

    private static final SidebarManager INSTANCE = new SidebarManager();

    private final Map<Player, String> headerFooterCache = new WeakHashMap<>();
    private final Map<UUID, Deque<DisplayNameOwner>> displayNameOwners = new HashMap<>();

    private SidebarManager() {
    }

    public static SidebarManager init() {
        PlayerListDisplayNamePackets.verifyCompatibility();
        return INSTANCE;
    }

    void claimDisplayNameOwnership(@NotNull Sidebar sidebar, @NotNull Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        Deque<DisplayNameOwner> owners = displayNameOwners.computeIfAbsent(viewerId,
                ignored -> new ArrayDeque<>());
        owners.removeIf(owner -> !owner.viewer().isOnline());
        DisplayNameOwner current = owners.peekLast();
        if (current != null && current.sidebar() == sidebar) {
            // Refresh the CraftPlayer reference after a reconnect without
            // disturbing the ownership order.
            owners.removeLast();
            owners.addLast(new DisplayNameOwner(sidebar, viewer));
            return;
        }

        if (current != null) {
            current.sidebar().suspendDisplayNameOwnership(current.viewer());
        }
        removeOwner(owners, sidebar);
        owners.addLast(new DisplayNameOwner(sidebar, viewer));
    }

    boolean ownsDisplayNames(@NotNull Sidebar sidebar, @NotNull Player viewer) {
        Deque<DisplayNameOwner> owners = displayNameOwners.get(viewer.getUniqueId());
        DisplayNameOwner owner = owners == null ? null : owners.peekLast();
        return owner != null && owner.sidebar() == sidebar;
    }

    boolean hasDisplayNameOwnership(@NotNull Sidebar sidebar, @NotNull Player viewer) {
        Deque<DisplayNameOwner> owners = displayNameOwners.get(viewer.getUniqueId());
        if (owners == null) return false;
        return owners.stream().anyMatch(owner -> owner.sidebar() == sidebar);
    }

    void unlinkPreviousScoreboard(@NotNull Sidebar sidebar, @NotNull Player viewer,
                                  @NotNull Scoreboard managedScoreboard,
                                  @Nullable Scoreboard replacementScoreboard) {
        Deque<DisplayNameOwner> owners = displayNameOwners.get(viewer.getUniqueId());
        if (owners == null) return;

        boolean found = false;
        for (DisplayNameOwner owner : owners) {
            if (found) {
                owner.sidebar().replacePreviousScoreboard(
                        viewer.getUniqueId(), managedScoreboard, replacementScoreboard);
                return;
            }
            found = owner.sidebar() == sidebar;
        }
    }

    boolean releaseDisplayNameOwnership(@NotNull Sidebar sidebar, @NotNull Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        Deque<DisplayNameOwner> owners = displayNameOwners.get(viewerId);
        if (owners == null || owners.isEmpty()) return false;

        DisplayNameOwner current = owners.peekLast();
        boolean wasCurrentOwner = current != null && current.sidebar() == sidebar;
        if (!removeOwner(owners, sidebar)) return false;

        owners.removeIf(owner -> !owner.viewer().isOnline());
        DisplayNameOwner resumed = wasCurrentOwner ? owners.peekLast() : null;
        if (owners.isEmpty()) displayNameOwners.remove(viewerId);
        if (resumed != null) {
            resumed.sidebar().resumeDisplayNameOwnership(resumed.viewer());
        }
        return wasCurrentOwner;
    }

    private static boolean removeOwner(@NotNull Deque<DisplayNameOwner> owners, @NotNull Sidebar sidebar) {
        Iterator<DisplayNameOwner> iterator = owners.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().sidebar() != sidebar) continue;
            iterator.remove();
            return true;
        }
        return false;
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

    private record DisplayNameOwner(@NotNull Sidebar sidebar, @NotNull Player viewer) {
    }
}
