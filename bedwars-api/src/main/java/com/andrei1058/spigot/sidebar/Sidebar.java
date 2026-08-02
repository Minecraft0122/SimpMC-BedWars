package com.andrei1058.spigot.sidebar;

import io.papermc.paper.scoreboard.numbers.FixedFormat;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Sidebar {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final String SIDEBAR_OBJECTIVE = "bw_sidebar";
    private static final String HEALTH_BELOW_OBJECTIVE = "bw_health";
    private static final String HEALTH_TAB_OBJECTIVE = "bw_health_tab";
    private static final String LINE_TEAM_PREFIX = "bw_l_";
    private static final String TAB_TEAM_PREFIX = "bw_t_";
    private static final String[] LINE_ENTRIES = {
            ChatColor.BLACK.toString(),
            ChatColor.DARK_BLUE.toString(),
            ChatColor.DARK_GREEN.toString(),
            ChatColor.DARK_AQUA.toString(),
            ChatColor.DARK_RED.toString(),
            ChatColor.DARK_PURPLE.toString(),
            ChatColor.GOLD.toString(),
            ChatColor.GRAY.toString(),
            ChatColor.DARK_GRAY.toString(),
            ChatColor.BLUE.toString(),
            ChatColor.GREEN.toString(),
            ChatColor.AQUA.toString(),
            ChatColor.RED.toString(),
            ChatColor.LIGHT_PURPLE.toString(),
            ChatColor.YELLOW.toString()
    };

    private SidebarLine title;
    private final List<SidebarLine> lines = new ArrayList<>();
    private final ConcurrentLinkedQueue<PlaceholderProvider> placeholders = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    private final Map<UUID, Player> viewers = new HashMap<>();
    private final Map<UUID, Set<UUID>> clearedPlayerListNames = new HashMap<>();
    private final Map<UUID, Map<UUID, Player>> pendingPlayerListRestores = new HashMap<>();
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
    private final Map<String, PlayerTab> tabs = new HashMap<>();
    private final Map<String, String> tabTeamNames = new HashMap<>();
    private int nextTabTeamId;
    private SidebarLine healthLine = new SidebarLine();
    private boolean healthEnabled = false;
    private boolean healthInTab = false;
    private final PlayerListDisplayNameRenderer displayNameRenderer;

    public Sidebar(@NotNull SidebarLine title, @NotNull Collection<SidebarLine> lines,
                   @NotNull Collection<PlaceholderProvider> placeholders) {
        this(title, lines, placeholders, PlayerListDisplayNamePackets.instance());
    }

    Sidebar(@NotNull SidebarLine title, @NotNull Collection<SidebarLine> lines,
            @NotNull Collection<PlaceholderProvider> placeholders,
            @NotNull PlayerListDisplayNameRenderer displayNameRenderer) {
        this.title = title;
        this.lines.addAll(lines);
        this.placeholders.addAll(placeholders);
        this.displayNameRenderer = displayNameRenderer;
    }

    public void add(@NotNull Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Scoreboard currentScoreboard = player.getScoreboard();
        Scoreboard managedScoreboard = scoreboards.get(playerId);
        SidebarManager sidebarManager = SidebarManager.getInstance();
        if (managedScoreboard != null
                && sidebarManager.hasDisplayNameOwnership(this, player)
                && !sidebarManager.ownsDisplayNames(this, player)) {
            // Re-attaching a suspended layer must first remove its old place
            // from the ownership chain, otherwise previous scoreboards form a
            // cycle (A -> B -> A).
            Scoreboard oldPrevious = previousScoreboards.remove(playerId);
            sidebarManager.unlinkPreviousScoreboard(this, player, managedScoreboard, oldPrevious);
            sidebarManager.releaseDisplayNameOwnership(this, player);
            scoreboards.remove(playerId);
            viewers.remove(playerId);
            managedScoreboard = null;
        }
        Scoreboard scoreboard = manager.getNewScoreboard();
        List<RenderedPlayerTab> renderedTabs = renderPlayerTabs(tabs.values());
        // Build off-screen first. Binding a different scoreboard instance makes
        // CraftBukkit send one complete snapshot, including team CREATE data.
        render(scoreboard);
        renderedTabs.forEach(tab -> applyTab(scoreboard, tab));
        renderHealth(scoreboard);
        player.setScoreboard(scoreboard);
        if (shouldCapturePreviousScoreboard(managedScoreboard, currentScoreboard)) {
            // Another plugin may have replaced our board since the previous
            // replay. Restore that latest external board when BedWars releases
            // ownership, not the stale board captured on first entry.
            previousScoreboards.put(playerId, currentScoreboard);
        }
        scoreboards.put(playerId, scoreboard);
        viewers.put(playerId, player);
        clearedPlayerListNames.computeIfAbsent(playerId, ignored -> new HashSet<>());
        sidebarManager.claimDisplayNameOwnership(this, player);
        retryPendingPlayerListRestores(player);
        renderPlayerListNames(player, renderedTabs, true);
    }

    public void remove(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        Scoreboard scoreboard = scoreboards.remove(playerId);
        Player viewer = viewers.remove(playerId);
        Scoreboard previous = previousScoreboards.remove(playerId);
        SidebarManager sidebarManager = SidebarManager.getInstance();
        if (scoreboard == null) {
            sidebarManager.releaseDisplayNameOwnership(this, player);
            discardViewerDisplayNameState(playerId);
            return;
        }
        Player ownershipViewer = viewer == null ? player : viewer;
        boolean ownedDisplayNames = viewer != null
                && sidebarManager.ownsDisplayNames(this, viewer);
        if (ownedDisplayNames && viewer.isOnline()) {
            retryPendingPlayerListRestores(viewer);
            restorePlayerListNames(viewer, tabs.values());
        }
        if (player.getScoreboard() == scoreboard) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            player.setScoreboard(previous == null && manager != null ? manager.getMainScoreboard() : previous);
        }
        sidebarManager.unlinkPreviousScoreboard(this, ownershipViewer, scoreboard, previous);
        sidebarManager.releaseDisplayNameOwnership(this, ownershipViewer);
        if (viewer != null) {
            if (!viewer.isOnline() || !pendingPlayerListRestores.containsKey(playerId)) {
                discardViewerDisplayNameState(playerId);
            }
        } else {
            discardViewerDisplayNameState(playerId);
        }
    }

    void replacePreviousScoreboard(@NotNull UUID viewerId, @NotNull Scoreboard expected,
                                   @Nullable Scoreboard replacement) {
        if (previousScoreboards.get(viewerId) != expected) return;
        if (replacement == null) {
            previousScoreboards.remove(viewerId);
        } else {
            previousScoreboards.put(viewerId, replacement);
        }
    }

    public void clearLines() {
        lines.clear();
        renderAll();
    }

    @NotNull
    public Collection<PlaceholderProvider> getPlaceholders() {
        return placeholders;
    }

    public void removePlaceholder(@NotNull String placeholder) {
        placeholders.removeIf(provider -> provider.getPlaceholder().equals(placeholder));
        renderAll();
    }

    public void addPlaceholder(@NotNull PlaceholderProvider placeholderProvider) {
        placeholders.add(placeholderProvider);
        renderAll();
    }

    public void setTitle(@NotNull SidebarLine title) {
        this.title = title;
        refreshTitle();
    }

    public void setContent(@NotNull SidebarLine title, @NotNull Collection<SidebarLine> lines,
                           @NotNull Collection<PlaceholderProvider> placeholders) {
        this.title = title;
        this.lines.clear();
        this.lines.addAll(lines);
        this.placeholders.clear();
        this.placeholders.addAll(placeholders);
        renderAll();
    }

    @NotNull
    public SidebarLine getTitle() {
        return title;
    }

    public void addLine(@NotNull SidebarLine line) {
        lines.add(line);
        renderAll();
    }

    @NotNull
    public List<SidebarLine> getLines() {
        return lines;
    }

    public void refreshTitle() {
        scoreboards.values().forEach(scoreboard -> {
            Objective objective = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
            if (objective != null) {
                objective.displayName(component(renderText(title, placeholders)));
            }
        });
    }

    public void refreshPlaceholders() {
        if (scoreboards.isEmpty()) return;
        RenderedSidebar rendered = renderSidebar();
        scoreboards.values().forEach(scoreboard -> refreshRenderedContent(scoreboard, rendered));
    }

    public void playerTabRefreshAnimation() {
        List<RenderedPlayerTab> renderedTabs = renderPlayerTabs(tabs.values());
        List<Player> detachedViewers = new ArrayList<>();
        scoreboards.forEach((viewerId, scoreboard) -> {
            Player viewer = viewers.get(viewerId);
            if (viewer == null || !viewer.isOnline()) return;
            boolean ownsDisplayNames = SidebarManager.getInstance().ownsDisplayNames(this, viewer);
            if (ownsDisplayNames
                    && shouldReattachScoreboard(true, scoreboard, viewer.getScoreboard())) {
                detachedViewers.add(viewer);
                return;
            }
            renderedTabs.forEach(tab -> applyTab(scoreboard, tab));
            retryPendingPlayerListRestores(viewer);
            // A visibility change or another plugin can resend ADD_PLAYER or
            // UPDATE_DISPLAY_NAME after our event update. One batched replay
            // per viewer keeps production clients authoritative without a
            // viewer-by-target packet explosion.
            if (ownsDisplayNames) {
                renderPlayerListNames(viewer, renderedTabs, true);
            }
        });
        detachedViewers.forEach(this::add);
    }

    public void playerHealthRefreshAnimation() {
        scoreboards.values().forEach(this::renderHealthTitle);
    }

    public void setPlayerHealth(@NotNull Player player, int health) {
        scoreboards.values().forEach(scoreboard -> {
            Objective belowName = scoreboard.getObjective(HEALTH_BELOW_OBJECTIVE);
            if (belowName != null) {
                belowName.getScore(player.getName()).setScore(health);
            }
            Objective tab = scoreboard.getObjective(HEALTH_TAB_OBJECTIVE);
            if (tab != null) {
                tab.getScore(player.getName()).setScore(health);
            }
        });
    }

    /** Remove a player's stale health scores without disabling health for everyone else. */
    public void clearPlayerHealth(@NotNull Player player) {
        scoreboards.values().forEach(scoreboard -> {
            Objective belowName = scoreboard.getObjective(HEALTH_BELOW_OBJECTIVE);
            if (belowName != null) belowName.getScore(player.getName()).resetScore();
            Objective tab = scoreboard.getObjective(HEALTH_TAB_OBJECTIVE);
            if (tab != null) tab.getScore(player.getName()).resetScore();
        });
    }

    public void removeTabs() {
        viewers.values().stream()
                .filter(Player::isOnline)
                .filter(viewer -> SidebarManager.getInstance().ownsDisplayNames(this, viewer))
                .forEach(viewer -> restorePlayerListNames(viewer, tabs.values()));
        tabs.values().forEach(tab -> {
            detachTab(tab);
        });
        tabs.clear();
        scoreboards.values().forEach(Sidebar::removeTabTeams);
        tabTeamNames.clear();
        nextTabTeamId = 0;
    }

    public void hidePlayersHealth() {
        healthEnabled = false;
        scoreboards.values().forEach(scoreboard -> {
            unregisterObjective(scoreboard, HEALTH_BELOW_OBJECTIVE);
            unregisterObjective(scoreboard, HEALTH_TAB_OBJECTIVE);
        });
    }

    public void showPlayersHealth(@NotNull SidebarLine line, boolean inTab) {
        healthLine = line;
        healthInTab = inTab;
        healthEnabled = true;
        scoreboards.values().forEach(this::renderHealth);
    }

    @NotNull
    public PlayerTab playerTabCreate(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                                     @NotNull SidebarLine suffix, @NotNull PlayerTab.PushingRule pushingRule,
                                     @NotNull ConcurrentLinkedQueue<PlaceholderProvider> placeholders) {
        return playerTabCreate(identifier, player, prefix, suffix, pushingRule, placeholders, ChatColor.WHITE);
    }

    @NotNull
    public PlayerTab playerTabCreate(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                                     @NotNull SidebarLine suffix, @NotNull PlayerTab.PushingRule pushingRule,
                                     @NotNull ConcurrentLinkedQueue<PlaceholderProvider> placeholders,
                                     @NotNull ChatColor color) {
        return playerTabCreate(identifier, player, prefix, suffix, pushingRule, placeholders, color,
                PlayerTab.NameTagVisibility.ALWAYS);
    }

    @NotNull
    public PlayerTab playerTabCreate(@NotNull String identifier, @NotNull Player player, @NotNull SidebarLine prefix,
                                     @NotNull SidebarLine suffix, @NotNull PlayerTab.PushingRule pushingRule,
                                     @NotNull ConcurrentLinkedQueue<PlaceholderProvider> placeholders,
                                     @NotNull ChatColor color,
                                     @NotNull PlayerTab.NameTagVisibility nameTagVisibility) {
        PlayerTab tab = new PlayerTab(identifier, player, prefix, suffix, pushingRule, placeholders,
                color, nameTagVisibility);
        tab.setUpdateCallback(this::applyTabToAll);
        PlayerTab previous = tabs.put(identifier, tab);
        boolean forceDisplayName = previous != null;
        if (previous != null) {
            previous.setUpdateCallback(ignored -> {
            });
            if (!previous.getPlayer().getName().equals(player.getName())) {
                removePlayerFromTabTeam(identifier, previous.getPlayer().getName());
            }
            if (!previous.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                restoreOrRenderRemainingTab(previous);
            }
        }
        applyTabToAll(tab, forceDisplayName);
        return tab;
    }

    public void removeTab(@NotNull String identifier) {
        PlayerTab tab = tabs.remove(identifier);
        if (tab == null) {
            return;
        }
        detachTab(tab);
        String teamName = tabTeamNames.remove(identifier);
        if (teamName != null) {
            scoreboards.values().forEach(scoreboard -> {
                Team team = scoreboard.getTeam(teamName);
                if (team != null) {
                    team.unregister();
                }
            });
        }
        restoreOrRenderRemainingTab(tab);
    }

    private void removePlayerFromTabTeam(@NotNull String identifier, @NotNull String playerName) {
        String teamName = tabTeamNames.get(identifier);
        if (teamName == null) return;
        scoreboards.values().forEach(scoreboard -> {
            Team team = scoreboard.getTeam(teamName);
            if (team != null && team.hasEntry(playerName)) {
                team.removeEntry(playerName);
            }
        });
    }

    private void renderAll() {
        if (scoreboards.isEmpty()) {
            return;
        }
        RenderedSidebar rendered = renderSidebar();
        scoreboards.values().forEach(scoreboard -> render(scoreboard, rendered));
    }

    private void render(@NotNull Scoreboard scoreboard) {
        render(scoreboard, renderSidebar());
    }

    private void render(@NotNull Scoreboard scoreboard, @NotNull RenderedSidebar rendered) {
        removeLineTeams(scoreboard);
        unregisterObjective(scoreboard, SIDEBAR_OBJECTIVE);

        if (rendered.empty()) {
            return;
        }

        Objective objective = scoreboard.registerNewObjective(
                SIDEBAR_OBJECTIVE,
                Criteria.DUMMY,
                rendered.title()
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        int visibleLines = rendered.lines().size();
        for (int index = 0; index < visibleLines; index++) {
            RenderedSidebarLine line = rendered.lines().get(index);
            Team team = scoreboard.registerNewTeam(LINE_TEAM_PREFIX + index);
            team.addEntry(line.entry());
            team.prefix(line.text());

            Score score = objective.getScore(line.entry());
            score.setScore(visibleLines - index);
            if (line.numberFormat() != null) score.numberFormat(line.numberFormat());
        }
    }

    /**
     * Placeholder refreshes do not change the sidebar shape. Reuse the
     * existing objective and line teams so a final kill only sends the values
     * that actually changed. If another plugin removed part of our structure,
     * rebuild once to restore a complete, authoritative model.
     */
    private void refreshRenderedContent(@NotNull Scoreboard scoreboard,
                                        @NotNull RenderedSidebar rendered) {
        Objective objective = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
        if (!hasCompleteStructure(scoreboard, objective, rendered)) {
            render(scoreboard, rendered);
            return;
        }
        if (rendered.empty()) return;

        if (!Objects.equals(objective.displayName(), rendered.title())) {
            objective.displayName(rendered.title());
        }
        for (int index = 0; index < rendered.lines().size(); index++) {
            RenderedSidebarLine line = rendered.lines().get(index);
            Team team = scoreboard.getTeam(LINE_TEAM_PREFIX + index);
            if (!Objects.equals(team.prefix(), line.text())) {
                team.prefix(line.text());
            }

            Score score = objective.getScore(line.entry());
            if (!sameNumberFormat(score.numberFormat(), line.numberFormat())) {
                score.numberFormat(line.numberFormat());
            }
        }
    }

    private static boolean hasCompleteStructure(@NotNull Scoreboard scoreboard,
                                                @Nullable Objective objective,
                                                @NotNull RenderedSidebar rendered) {
        if (rendered.empty()) {
            if (objective != null) return false;
            for (int index = 0; index < LINE_ENTRIES.length; index++) {
                if (scoreboard.getTeam(LINE_TEAM_PREFIX + index) != null) return false;
            }
            return true;
        }
        if (objective == null || objective.getDisplaySlot() != DisplaySlot.SIDEBAR) return false;

        int visibleLines = rendered.lines().size();
        for (int index = 0; index < LINE_ENTRIES.length; index++) {
            Team team = scoreboard.getTeam(LINE_TEAM_PREFIX + index);
            if (index >= visibleLines) {
                if (team != null) return false;
                continue;
            }
            RenderedSidebarLine line = rendered.lines().get(index);
            if (team == null || !team.hasEntry(line.entry())) return false;
            Score score = objective.getScore(line.entry());
            if (!score.isScoreSet() || score.getScore() != visibleLines - index) return false;
        }
        return true;
    }

    private @NotNull RenderedSidebar renderSidebar() {
        String renderedTitle = renderText(title, placeholders);
        int visibleLines = Math.min(lines.size(), LINE_ENTRIES.length);
        List<RenderedSidebarLine> renderedLines = new ArrayList<>(visibleLines);
        for (int index = 0; index < visibleLines; index++) {
            SidebarLine line = lines.get(index);
            NumberFormat numberFormat = null;
            if (line instanceof ScoredLine scoredLine && scoredLine.getScore() != null) {
                String fixedScore = replacePlaceholders(scoredLine.getScore(), placeholders);
                numberFormat = NumberFormat.fixed(component(fixedScore));
            }
            renderedLines.add(new RenderedSidebarLine(
                    LINE_ENTRIES[index], component(renderText(line, placeholders)), numberFormat));
        }
        return new RenderedSidebar(
                component(renderedTitle), renderedLines,
                renderedLines.isEmpty() && renderedTitle.isBlank());
    }

    private static boolean sameNumberFormat(@Nullable NumberFormat current,
                                            @Nullable NumberFormat expected) {
        if (current == expected) return true;
        if (current == null || expected == null) return false;
        if (current instanceof FixedFormat currentFixed
                && expected instanceof FixedFormat expectedFixed) {
            return currentFixed.component().equals(expectedFixed.component());
        }
        return current.equals(expected);
    }

    private record RenderedSidebar(@NotNull Component title,
                                   @NotNull List<RenderedSidebarLine> lines,
                                   boolean empty) {
    }

    private record RenderedSidebarLine(@NotNull String entry, @NotNull Component text,
                                       @Nullable NumberFormat numberFormat) {
    }

    private void renderHealth(@NotNull Scoreboard scoreboard) {
        if (!healthEnabled) {
            return;
        }
        Objective belowName = getOrCreateObjective(scoreboard, HEALTH_BELOW_OBJECTIVE, DisplaySlot.BELOW_NAME);
        belowName.numberFormat(NumberFormat.noStyle());

        if (healthInTab) {
            Objective tab = getOrCreateObjective(scoreboard, HEALTH_TAB_OBJECTIVE, DisplaySlot.PLAYER_LIST);
            tab.numberFormat(NumberFormat.noStyle());
        } else {
            unregisterObjective(scoreboard, HEALTH_TAB_OBJECTIVE);
        }
        renderHealthTitle(scoreboard);
    }

    private void renderHealthTitle(@NotNull Scoreboard scoreboard) {
        if (!healthEnabled) {
            return;
        }
        Component title = component(renderText(healthLine, placeholders));
        Objective belowName = scoreboard.getObjective(HEALTH_BELOW_OBJECTIVE);
        if (belowName != null) {
            belowName.displayName(title);
        }
        Objective tab = scoreboard.getObjective(HEALTH_TAB_OBJECTIVE);
        if (tab != null) {
            tab.displayName(title);
        }
    }

    private Objective getOrCreateObjective(@NotNull Scoreboard scoreboard, @NotNull String name, @NotNull DisplaySlot slot) {
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(name, Criteria.DUMMY, component(""));
        }
        objective.setDisplaySlot(slot);
        return objective;
    }

    private void applyTabToAll(@NotNull PlayerTab tab) {
        applyTabToAll(tab, false);
    }

    private void applyTabToAll(@NotNull PlayerTab tab, boolean forceDisplayName) {
        RenderedPlayerTab renderedTab = renderPlayerTab(tab);
        List<Player> detachedViewers = new ArrayList<>();
        scoreboards.forEach((viewerId, scoreboard) -> {
            Player viewer = viewers.get(viewerId);
            if (viewer != null && viewer.isOnline()) {
                boolean ownsDisplayNames = SidebarManager.getInstance().ownsDisplayNames(this, viewer);
                if (ownsDisplayNames
                        && shouldReattachScoreboard(true, scoreboard, viewer.getScoreboard())) {
                    detachedViewers.add(viewer);
                    return;
                }
                applyTab(scoreboard, renderedTab);
                if (ownsDisplayNames) {
                    renderPlayerListNames(viewer, List.of(renderedTab), forceDisplayName);
                }
            }
        });
        detachedViewers.forEach(this::add);
    }

    void applyTab(@NotNull Scoreboard scoreboard, @NotNull PlayerTab tab) {
        applyTab(scoreboard, renderPlayerTab(tab));
    }

    private void applyTab(@NotNull Scoreboard scoreboard, @NotNull RenderedPlayerTab renderedTab) {
        PlayerTab tab = renderedTab.tab();
        Team team = scoreboard.getTeam(teamName(tab.getIdentifier()));
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName(tab.getIdentifier()));
        }

        Component prefix = component(renderedTab.prefix());
        if (!team.prefix().equals(prefix)) team.prefix(prefix);

        Component suffix = component(renderedTab.suffix());
        if (!team.suffix().equals(suffix)) team.suffix(suffix);

        if (team.getColor() != tab.getColor()) team.setColor(tab.getColor());

        Team.OptionStatus visibility = tab.getNameTagVisibility() == PlayerTab.NameTagVisibility.NEVER
                ? Team.OptionStatus.NEVER
                : Team.OptionStatus.ALWAYS;
        if (team.getOption(Team.Option.NAME_TAG_VISIBILITY) != visibility) {
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, visibility);
        }
        if (!team.hasEntry(tab.getPlayer().getName())) {
            team.addEntry(tab.getPlayer().getName());
        }

    }

    void renderPlayerListName(@NotNull Player viewer, @NotNull PlayerTab tab) {
        renderPlayerListNames(viewer, List.of(renderPlayerTab(tab)), false);
    }

    void forcePlayerListNameRefresh(@NotNull Player viewer) {
        Scoreboard managedScoreboard = scoreboards.get(viewer.getUniqueId());
        if (managedScoreboard != null
                && SidebarManager.getInstance().ownsDisplayNames(this, viewer)
                && shouldReattachScoreboard(true, managedScoreboard, viewer.getScoreboard())) {
            add(viewer);
            return;
        }
        retryPendingPlayerListRestores(viewer);
        renderPlayerListNames(viewer, renderPlayerTabs(tabs.values()), true);
    }

    void suspendDisplayNameOwnership(@NotNull Player viewer) {
        if (!viewer.isOnline()) {
            discardViewerDisplayNameState(viewer.getUniqueId());
            return;
        }
        retryPendingPlayerListRestores(viewer);
        restorePlayerListNames(viewer, tabs.values());
    }

    void resumeDisplayNameOwnership(@NotNull Player viewer) {
        if (!viewer.isOnline() || !SidebarManager.getInstance().ownsDisplayNames(this, viewer)) return;
        Scoreboard managedScoreboard = scoreboards.get(viewer.getUniqueId());
        if (managedScoreboard != null
                && shouldReattachScoreboard(true, managedScoreboard, viewer.getScoreboard())) {
            add(viewer);
            return;
        }
        retryPendingPlayerListRestores(viewer);
        renderPlayerListNames(viewer, renderPlayerTabs(tabs.values()), true);
    }

    private void renderPlayerListNames(@NotNull Player viewer, @NotNull Collection<RenderedPlayerTab> playerTabs,
                                       boolean force) {
        Set<UUID> viewerCache = clearedPlayerListNames.computeIfAbsent(
                viewer.getUniqueId(), ignored -> new HashSet<>());
        List<Player> updates = new ArrayList<>();
        for (RenderedPlayerTab renderedTab : playerTabs) {
            PlayerTab tab = renderedTab.tab();
            UUID targetId = tab.getPlayer().getUniqueId();
            if (!tab.getPlayer().isOnline()) {
                releaseCachedPlayerListName(viewer.getUniqueId(), targetId);
                continue;
            }
            if (!force && viewerCache.contains(targetId)) continue;
            updates.add(tab.getPlayer());
        }
        if (updates.isEmpty()) return;
        if (displayNameRenderer.clear(viewer, updates)) {
            updates.forEach(update -> {
                UUID targetId = update.getUniqueId();
                viewerCache.add(targetId);
                removePendingPlayerListRestore(viewer.getUniqueId(), targetId);
            });
        }
    }

    void restorePlayerListName(@NotNull Player viewer, @NotNull PlayerTab tab) {
        restorePlayerListNames(viewer, List.of(tab));
    }

    private void restorePlayerListNames(@NotNull Player viewer, @NotNull Collection<PlayerTab> playerTabs) {
        List<Player> targets = playerTabs.stream().map(PlayerTab::getPlayer).toList();
        restorePlayerListTargets(viewer, targets);
    }

    private void retryPendingPlayerListRestores(@NotNull Player viewer) {
        Map<UUID, Player> pending = pendingPlayerListRestores.get(viewer.getUniqueId());
        if (pending == null || pending.isEmpty()) return;
        restorePlayerListTargets(viewer, new ArrayList<>(pending.values()));
    }

    private void restorePlayerListTargets(@NotNull Player viewer, @NotNull Collection<Player> candidates) {
        UUID viewerId = viewer.getUniqueId();
        Set<UUID> viewerCache = clearedPlayerListNames.get(viewerId);
        if (viewerCache == null) return;
        Map<UUID, Player> targets = new LinkedHashMap<>();
        for (Player target : candidates) {
            UUID targetId = target.getUniqueId();
            if (!viewerCache.contains(targetId)) continue;
            if (target.isOnline()) {
                targets.putIfAbsent(targetId, target);
            } else {
                // Paper has already removed an offline player's PlayerInfo row,
                // so there is nothing left to restore on the client. Still
                // release our per-viewer cache entry to avoid retaining every
                // player that has ever passed through a long-running lobby.
                releaseCachedPlayerListName(viewerId, targetId);
            }
        }
        if (targets.isEmpty()) return;
        if (displayNameRenderer.restore(viewer, targets.values())) {
            targets.keySet().forEach(targetId -> releaseCachedPlayerListName(viewerId, targetId));
            return;
        }
        pendingPlayerListRestores.computeIfAbsent(viewerId, ignored -> new LinkedHashMap<>())
                .putAll(targets);
    }

    private void releaseCachedPlayerListName(@NotNull UUID viewerId, @NotNull UUID targetId) {
        Set<UUID> viewerCache = clearedPlayerListNames.get(viewerId);
        if (viewerCache != null) viewerCache.remove(targetId);
        removePendingPlayerListRestore(viewerId, targetId);
    }

    private void removePendingPlayerListRestore(@NotNull UUID viewerId, @NotNull UUID targetId) {
        Map<UUID, Player> pending = pendingPlayerListRestores.get(viewerId);
        if (pending == null) return;
        pending.remove(targetId);
        if (pending.isEmpty()) pendingPlayerListRestores.remove(viewerId);
    }

    private void discardViewerDisplayNameState(@NotNull UUID viewerId) {
        clearedPlayerListNames.remove(viewerId);
        pendingPlayerListRestores.remove(viewerId);
    }

    private void restoreOrRenderRemainingTab(@NotNull PlayerTab removedTab) {
        PlayerTab remaining = tabs.values().stream()
                .filter(tab -> tab.getPlayer().getUniqueId().equals(removedTab.getPlayer().getUniqueId()))
                .findFirst()
                .orElse(null);
        if (remaining != null) {
            applyTabToAll(remaining, true);
            return;
        }
        viewers.values().stream()
                .filter(Player::isOnline)
                .filter(viewer -> SidebarManager.getInstance().ownsDisplayNames(this, viewer))
                .forEach(viewer -> restorePlayerListNames(viewer, List.of(removedTab)));
    }

    private static @NotNull List<RenderedPlayerTab> renderPlayerTabs(
            @NotNull Collection<PlayerTab> playerTabs) {
        return playerTabs.stream().map(Sidebar::renderPlayerTab).toList();
    }

    private static @NotNull RenderedPlayerTab renderPlayerTab(@NotNull PlayerTab tab) {
        String prefix = renderText(tab.getPrefix(), tab.getPlaceholders());
        String suffix = renderText(tab.getSuffix(), tab.getPlaceholders());
        return new RenderedPlayerTab(tab, prefix, suffix);
    }

    private record RenderedPlayerTab(@NotNull PlayerTab tab, @NotNull String prefix,
                                     @NotNull String suffix) {
    }

    private static void unregisterObjective(@NotNull Scoreboard scoreboard, @NotNull String name) {
        Objective objective = scoreboard.getObjective(name);
        if (objective != null) {
            objective.unregister();
        }
    }

    private static void removeLineTeams(@NotNull Scoreboard scoreboard) {
        for (int i = 0; i < LINE_ENTRIES.length; i++) {
            Team team = scoreboard.getTeam(LINE_TEAM_PREFIX + i);
            if (team != null) {
                team.unregister();
            }
        }
    }

    private static void removeTabTeams(@NotNull Scoreboard scoreboard) {
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith(TAB_TEAM_PREFIX)) {
                team.unregister();
            }
        }
    }

    String teamName(@NotNull String identifier) {
        // A per-sidebar monotonic id is stable and cannot collide. String.hashCode based names
        // could merge two unrelated players into one scoreboard team and corrupt client state.
        return tabTeamNames.computeIfAbsent(identifier,
                ignored -> TAB_TEAM_PREFIX + Integer.toString(nextTabTeamId++, Character.MAX_RADIX));
    }

    static boolean shouldCapturePreviousScoreboard(Scoreboard managedScoreboard,
                                                    @NotNull Scoreboard currentScoreboard) {
        return managedScoreboard != currentScoreboard;
    }

    static boolean shouldReattachScoreboard(boolean ownsDisplayNames, @Nullable Scoreboard managedScoreboard,
                                             @NotNull Scoreboard currentScoreboard) {
        return ownsDisplayNames && managedScoreboard != null && managedScoreboard != currentScoreboard;
    }

    private static void detachTab(@NotNull PlayerTab tab) {
        tab.setUpdateCallback(ignored -> {
        });
    }

    static String renderText(@NotNull SidebarLine line, @NotNull Collection<PlaceholderProvider> placeholders) {
        return replacePlaceholders(line.getLine(), placeholders);
    }

    static String replacePlaceholders(String text, @NotNull Collection<PlaceholderProvider> placeholders) {
        String result = text == null ? "" : text;
        for (PlaceholderProvider placeholder : placeholders) {
            String value = placeholder.getValue();
            result = result.replace(placeholder.getPlaceholder(), value == null ? "" : value);
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    static Component component(String legacyText) {
        return LEGACY.deserialize(legacyText == null ? "" : legacyText);
    }
}
