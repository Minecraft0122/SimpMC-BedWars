package com.andrei1058.spigot.sidebar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Sidebar {

    private static final String SIDEBAR_OBJECTIVE = "bw_sidebar";
    private static final String HEALTH_BELOW_OBJECTIVE = "bw_health";
    private static final String HEALTH_TAB_OBJECTIVE = "bw_health_tab";
    private static final String LINE_TEAM_PREFIX = "bw_line_";
    private static final String TAB_TEAM_PREFIX = "t";
    private static final int MAX_LINES = 15;
    private static final String[] LINE_ENTRIES = {
            "\u00a70", "\u00a71", "\u00a72", "\u00a73", "\u00a74",
            "\u00a75", "\u00a76", "\u00a77", "\u00a78", "\u00a79",
            "\u00a7a", "\u00a7b", "\u00a7c", "\u00a7d", "\u00a7e"
    };

    private SidebarLine title;
    private final List<SidebarLine> lines = new ArrayList<>();
    private final ConcurrentLinkedQueue<PlaceholderProvider> placeholders = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
    private final Map<String, PlayerTab> tabs = new LinkedHashMap<>();
    private final Map<String, String> tabTeamNames = new HashMap<>();
    private SidebarLine healthLine = new SidebarLine();
    private boolean healthEnabled;
    private boolean healthInTab;

    public Sidebar(@NotNull SidebarLine title, @NotNull Collection<SidebarLine> lines,
                   @NotNull Collection<PlaceholderProvider> placeholders) {
        this.title = title;
        this.lines.addAll(lines);
        this.placeholders.addAll(placeholders);
    }

    public void add(@NotNull Player player) {
        SidebarManager.runSync(() -> addNow(player));
    }

    private void addNow(@NotNull Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Scoreboard scoreboard = scoreboards.get(playerId);
        if (scoreboard == null) {
            scoreboard = manager.getNewScoreboard();
            previousScoreboards.put(playerId, player.getScoreboard());
            scoreboards.put(playerId, scoreboard);
        } else if (player.getScoreboard() != scoreboard) {
            previousScoreboards.put(playerId, player.getScoreboard());
        }

        applySidebar(scoreboard, renderSidebar());
        for (PlayerTab tab : tabs.values()) {
            applyTab(scoreboard, renderTab(tab));
        }
        renderHealth(scoreboard, renderText(healthLine, placeholders));
        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        }
    }

    public void remove(@NotNull Player player) {
        SidebarManager.runSync(() -> removeNow(player));
    }

    private void removeNow(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        Scoreboard scoreboard = scoreboards.remove(playerId);
        Scoreboard previous = previousScoreboards.remove(playerId);
        SidebarManager.getInstance().clearHeaderFooter(player);
        if (scoreboard == null || player.getScoreboard() != scoreboard) {
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        player.setScoreboard(previous == null && manager != null ? manager.getMainScoreboard() : previous);
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
    }

    public void addPlaceholder(@NotNull PlaceholderProvider placeholderProvider) {
        placeholders.add(placeholderProvider);
    }

    public void setTitle(@NotNull SidebarLine title) {
        this.title = title;
        refreshTitle();
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
        SidebarManager.runSync(() -> {
            String renderedTitle = trimLegacy(renderText(title, placeholders), 32);
            for (Scoreboard scoreboard : scoreboards.values()) {
                Objective objective = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
                if (objective != null && !objective.getDisplayName().equals(renderedTitle)) {
                    objective.setDisplayName(renderedTitle);
                }
            }
        });
    }

    public void refreshPlaceholders() {
        renderAll();
    }

    public void playerTabRefreshAnimation() {
        SidebarManager.runSync(() -> {
            for (PlayerTab tab : tabs.values()) {
                RenderedTab renderedTab = renderTab(tab);
                for (Scoreboard scoreboard : scoreboards.values()) {
                    applyTab(scoreboard, renderedTab);
                }
            }
        });
    }

    public void playerHealthRefreshAnimation() {
        SidebarManager.runSync(() -> {
            String renderedTitle = renderText(healthLine, placeholders);
            for (Scoreboard scoreboard : scoreboards.values()) {
                renderHealthTitle(scoreboard, renderedTitle);
            }
        });
    }

    public void setPlayerHealth(@NotNull Player player, int health) {
        final int displayedHealth = Math.max(0, health);
        SidebarManager.runSync(() -> {
            for (Scoreboard scoreboard : scoreboards.values()) {
                Objective belowName = scoreboard.getObjective(HEALTH_BELOW_OBJECTIVE);
                if (belowName != null) {
                    belowName.getScore(player.getName()).setScore(displayedHealth);
                }
                Objective tab = scoreboard.getObjective(HEALTH_TAB_OBJECTIVE);
                if (tab != null) {
                    tab.getScore(player.getName()).setScore(displayedHealth);
                }
            }
        });
    }

    public void removeTabs() {
        for (PlayerTab tab : tabs.values()) {
            tab.setUpdateCallback(ignored -> {
            });
        }
        tabs.clear();
        tabTeamNames.clear();
        SidebarManager.runSync(() -> {
            for (Scoreboard scoreboard : scoreboards.values()) {
                removeTabTeams(scoreboard);
            }
        });
    }

    public void hidePlayersHealth() {
        healthEnabled = false;
        SidebarManager.runSync(() -> {
            for (Scoreboard scoreboard : scoreboards.values()) {
                unregisterObjective(scoreboard, HEALTH_BELOW_OBJECTIVE);
                unregisterObjective(scoreboard, HEALTH_TAB_OBJECTIVE);
            }
        });
    }

    public void showPlayersHealth(@NotNull SidebarLine line, boolean inTab) {
        healthLine = line;
        healthInTab = inTab;
        healthEnabled = true;
        SidebarManager.runSync(() -> {
            String renderedTitle = renderText(healthLine, placeholders);
            for (Scoreboard scoreboard : scoreboards.values()) {
                renderHealth(scoreboard, renderedTitle);
            }
        });
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
        removeTab(identifier);
        PlayerTab tab = new PlayerTab(identifier, player, prefix, suffix, pushingRule, placeholders, color);
        tab.setUpdateCallback(this::applyTabToAll);
        tabs.put(identifier, tab);
        tabTeamNames.put(identifier, allocateTabTeamName(identifier));
        applyTabToAll(tab);
        return tab;
    }

    public void removeTab(@NotNull String identifier) {
        PlayerTab tab = tabs.remove(identifier);
        String teamName = tabTeamNames.remove(identifier);
        if (tab != null) {
            tab.setUpdateCallback(ignored -> {
            });
        }
        if (teamName == null) {
            return;
        }
        SidebarManager.runSync(() -> {
            for (Scoreboard scoreboard : scoreboards.values()) {
                Team team = scoreboard.getTeam(teamName);
                if (team != null) {
                    team.unregister();
                }
            }
        });
    }

    private void renderAll() {
        SidebarManager.runSync(() -> {
            RenderedSidebar rendered = renderSidebar();
            for (Scoreboard scoreboard : scoreboards.values()) {
                applySidebar(scoreboard, rendered);
            }
        });
    }

    private void applyTabToAll(@NotNull PlayerTab tab) {
        SidebarManager.runSync(() -> {
            RenderedTab renderedTab = renderTab(tab);
            for (Scoreboard scoreboard : scoreboards.values()) {
                applyTab(scoreboard, renderedTab);
            }
        });
    }

    @NotNull
    private RenderedSidebar renderSidebar() {
        List<String> renderedLines = new ArrayList<>();
        int visibleLines = Math.min(lines.size(), MAX_LINES);
        for (int index = 0; index < visibleLines; index++) {
            renderedLines.add(renderText(lines.get(index), placeholders));
        }
        return new RenderedSidebar(trimLegacy(renderText(title, placeholders), 32), renderedLines);
    }

    private void applySidebar(@NotNull Scoreboard scoreboard, @NotNull RenderedSidebar rendered) {
        Objective objective = scoreboard.getObjective(SIDEBAR_OBJECTIVE);
        if (rendered.lines.isEmpty() && rendered.title.isEmpty()) {
            if (objective != null) {
                objective.unregister();
            }
            removeLineTeams(scoreboard);
            return;
        }
        if (objective == null) {
            objective = scoreboard.registerNewObjective(SIDEBAR_OBJECTIVE, "dummy");
        }
        if (!objective.getDisplayName().equals(rendered.title)) {
            objective.setDisplayName(rendered.title);
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int visibleLines = rendered.lines.size();
        for (int index = 0; index < MAX_LINES; index++) {
            String entry = LINE_ENTRIES[index];
            Team team = scoreboard.getTeam(LINE_TEAM_PREFIX + index);
            if (index >= visibleLines) {
                if (team != null) {
                    team.unregister();
                }
                scoreboard.resetScores(entry);
                continue;
            }

            if (team == null) {
                team = scoreboard.registerNewTeam(LINE_TEAM_PREFIX + index);
            }
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
            String[] parts = splitLine(rendered.lines.get(index));
            if (!team.getPrefix().equals(parts[0])) {
                team.setPrefix(parts[0]);
            }
            if (!team.getSuffix().equals(parts[1])) {
                team.setSuffix(parts[1]);
            }

            Score score = objective.getScore(entry);
            int scoreValue = visibleLines - index;
            if (!score.isScoreSet() || score.getScore() != scoreValue) {
                score.setScore(scoreValue);
            }
        }
    }

    private void renderHealth(@NotNull Scoreboard scoreboard, @NotNull String renderedTitle) {
        if (!healthEnabled) {
            return;
        }
        Objective belowName = getOrCreateObjective(scoreboard, HEALTH_BELOW_OBJECTIVE);
        belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
        if (healthInTab) {
            Objective tab = getOrCreateObjective(scoreboard, HEALTH_TAB_OBJECTIVE);
            tab.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        } else {
            unregisterObjective(scoreboard, HEALTH_TAB_OBJECTIVE);
        }
        renderHealthTitle(scoreboard, renderedTitle);
    }

    private void renderHealthTitle(@NotNull Scoreboard scoreboard, @NotNull String renderedTitle) {
        if (!healthEnabled) {
            return;
        }
        String title = trimLegacy(renderedTitle, 32);
        Objective belowName = scoreboard.getObjective(HEALTH_BELOW_OBJECTIVE);
        if (belowName != null && !belowName.getDisplayName().equals(title)) {
            belowName.setDisplayName(title);
        }
        Objective tab = scoreboard.getObjective(HEALTH_TAB_OBJECTIVE);
        if (tab != null && !tab.getDisplayName().equals(title)) {
            tab.setDisplayName(title);
        }
    }

    @NotNull
    private Objective getOrCreateObjective(@NotNull Scoreboard scoreboard, @NotNull String name) {
        Objective objective = scoreboard.getObjective(name);
        return objective == null ? scoreboard.registerNewObjective(name, "dummy") : objective;
    }

    @NotNull
    private RenderedTab renderTab(@NotNull PlayerTab tab) {
        String teamName = tabTeamNames.get(tab.getIdentifier());
        if (teamName == null) {
            teamName = allocateTabTeamName(tab.getIdentifier());
            tabTeamNames.put(tab.getIdentifier(), teamName);
        }
        String color = tab.getColor().toString();
        int prefixLength = Math.max(0, 16 - color.length());
        String prefix = trimLegacy(renderText(tab.getPrefix(), tab.getPlaceholders()), prefixLength) + color;
        return new RenderedTab(teamName, tab.getPlayer().getName(),
                prefix,
                trimLegacy(renderText(tab.getSuffix(), tab.getPlaceholders()), 16),
                tab.getNameTagVisibility());
    }

    private void applyTab(@NotNull Scoreboard scoreboard, @NotNull RenderedTab tab) {
        Team team = scoreboard.getTeam(tab.teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(tab.teamName);
        }
        if (!team.getPrefix().equals(tab.prefix)) {
            team.setPrefix(tab.prefix);
        }
        if (!team.getSuffix().equals(tab.suffix)) {
            team.setSuffix(tab.suffix);
        }
        NameTagVisibility visibility = tab.nameTagVisibility == PlayerTab.NameTagVisibility.NEVER
                ? NameTagVisibility.NEVER
                : NameTagVisibility.ALWAYS;
        if (team.getNameTagVisibility() != visibility) {
            team.setNameTagVisibility(visibility);
        }
        team.setAllowFriendlyFire(true);
        team.setCanSeeFriendlyInvisibles(false);
        if (!team.hasEntry(tab.playerName)) {
            team.addEntry(tab.playerName);
        }
    }

    @NotNull
    private String allocateTabTeamName(@NotNull String identifier) {
        String base = trimPlain(TAB_TEAM_PREFIX + identifier, 16);
        if (base.isEmpty()) {
            base = TAB_TEAM_PREFIX;
        }
        if (!tabTeamNames.containsValue(base)) {
            return base;
        }
        int index = 1;
        while (true) {
            String suffix = Integer.toHexString(index++);
            String candidate = trimPlain(base, 16 - suffix.length()) + suffix;
            if (!tabTeamNames.containsValue(candidate)) {
                return candidate;
            }
        }
    }

    private static void unregisterObjective(@NotNull Scoreboard scoreboard, @NotNull String name) {
        Objective objective = scoreboard.getObjective(name);
        if (objective != null) {
            objective.unregister();
        }
    }

    private static void removeLineTeams(@NotNull Scoreboard scoreboard) {
        for (int index = 0; index < MAX_LINES; index++) {
            Team team = scoreboard.getTeam(LINE_TEAM_PREFIX + index);
            if (team != null) {
                team.unregister();
            }
            scoreboard.resetScores(LINE_ENTRIES[index]);
        }
    }

    private static void removeTabTeams(@NotNull Scoreboard scoreboard) {
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith(TAB_TEAM_PREFIX)
                    && !team.getName().startsWith(LINE_TEAM_PREFIX)) {
                team.unregister();
            }
        }
    }

    @NotNull
    static String renderText(@NotNull SidebarLine line,
                             @NotNull Collection<PlaceholderProvider> placeholders) {
        return replacePlaceholders(line.getLine(), placeholders);
    }

    @NotNull
    static String replacePlaceholders(String text,
                                      @NotNull Collection<PlaceholderProvider> placeholders) {
        String result = text == null ? "" : text;
        for (PlaceholderProvider placeholder : placeholders) {
            if (!result.contains(placeholder.getPlaceholder())) {
                continue;
            }
            String value = placeholder.getValue();
            result = result.replace(placeholder.getPlaceholder(), value == null ? "" : value);
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    @NotNull
    private static String[] splitLine(@NotNull String text) {
        String prefix = trimLegacy(text, 16);
        if (prefix.length() >= text.length()) {
            return new String[]{prefix, ""};
        }
        String remainder = text.substring(prefix.length());
        String suffix = trimLegacy(ChatColor.getLastColors(prefix) + remainder, 16);
        return new String[]{prefix, suffix};
    }

    @NotNull
    private static String trimLegacy(String text, int maxLength) {
        String result = trimPlain(text == null ? "" : text, maxLength);
        if (!result.isEmpty() && result.charAt(result.length() - 1) == ChatColor.COLOR_CHAR) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @NotNull
    private static String trimPlain(String text, int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private static final class RenderedSidebar {
        private final String title;
        private final List<String> lines;

        private RenderedSidebar(String title, List<String> lines) {
            this.title = title;
            this.lines = lines;
        }
    }

    private static final class RenderedTab {
        private final String teamName;
        private final String playerName;
        private final String prefix;
        private final String suffix;
        private final PlayerTab.NameTagVisibility nameTagVisibility;

        private RenderedTab(String teamName, String playerName, String prefix, String suffix,
                            PlayerTab.NameTagVisibility nameTagVisibility) {
            this.teamName = teamName;
            this.playerName = playerName;
            this.prefix = prefix;
            this.suffix = suffix;
            this.nameTagVisibility = nameTagVisibility;
        }
    }
}
