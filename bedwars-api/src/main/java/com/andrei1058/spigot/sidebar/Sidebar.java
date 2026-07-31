package com.andrei1058.spigot.sidebar;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
    private final Map<String, PlayerTab> tabs = new HashMap<>();
    private final Map<String, String> tabTeamNames = new HashMap<>();
    private int nextTabTeamId;
    private SidebarLine healthLine = new SidebarLine();
    private boolean healthEnabled = false;
    private boolean healthInTab = false;

    public Sidebar(@NotNull SidebarLine title, @NotNull Collection<SidebarLine> lines,
                   @NotNull Collection<PlaceholderProvider> placeholders) {
        this.title = title;
        this.lines.addAll(lines);
        this.placeholders.addAll(placeholders);
    }

    public void add(@NotNull Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Scoreboard currentScoreboard = player.getScoreboard();
        Scoreboard managedScoreboard = scoreboards.get(playerId);
        Scoreboard scoreboard = manager.getNewScoreboard();
        // Build off-screen first. Binding a different scoreboard instance makes
        // CraftBukkit send one complete snapshot, including team CREATE data.
        render(scoreboard);
        tabs.values().forEach(tab -> applyTab(scoreboard, tab));
        renderHealth(scoreboard);
        player.setScoreboard(scoreboard);
        if (shouldCapturePreviousScoreboard(managedScoreboard, currentScoreboard)) {
            // Another plugin may have replaced our board since the previous
            // replay. Restore that latest external board when BedWars releases
            // ownership, not the stale board captured on first entry.
            previousScoreboards.put(playerId, currentScoreboard);
        }
        scoreboards.put(playerId, scoreboard);
    }

    public void remove(@NotNull Player player) {
        Scoreboard scoreboard = scoreboards.remove(player.getUniqueId());
        Scoreboard previous = previousScoreboards.remove(player.getUniqueId());
        if (scoreboard == null) {
            return;
        }
        if (player.getScoreboard() == scoreboard) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            player.setScoreboard(previous == null && manager != null ? manager.getMainScoreboard() : previous);
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
        renderAll();
    }

    public void playerTabRefreshAnimation() {
        scoreboards.values().forEach(scoreboard -> tabs.values().forEach(tab -> applyTab(scoreboard, tab)));
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
        tabs.values().forEach(tab -> {
            detachTab(tab);
            PlayerListNameState.release(tab.getPlayer());
        });
        tabs.clear();
        scoreboards.values().forEach(Sidebar::removeTabTeams);
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
        if (previous == null) {
            PlayerListNameState.acquire(player);
        } else {
            previous.setUpdateCallback(ignored -> {
            });
            if (previous.getPlayer() != player) {
                PlayerListNameState.release(previous.getPlayer());
                PlayerListNameState.acquire(player);
            }
        }
        applyTabToAll(tab);
        return tab;
    }

    public void removeTab(@NotNull String identifier) {
        PlayerTab tab = tabs.remove(identifier);
        if (tab == null) {
            return;
        }
        detachTab(tab);
        PlayerListNameState.release(tab.getPlayer());
        String teamName = teamName(identifier);
        scoreboards.values().forEach(scoreboard -> {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        });
    }

    private void renderAll() {
        if (scoreboards.isEmpty()) {
            return;
        }
        scoreboards.values().forEach(this::render);
    }

    private void render(@NotNull Scoreboard scoreboard) {
        removeLineTeams(scoreboard);
        unregisterObjective(scoreboard, SIDEBAR_OBJECTIVE);

        String renderedTitle = renderText(title, placeholders);
        if (lines.isEmpty() && renderedTitle.isBlank()) {
            return;
        }

        Objective objective = scoreboard.registerNewObjective(
                SIDEBAR_OBJECTIVE,
                Criteria.DUMMY,
                component(renderedTitle)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        int visibleLines = Math.min(lines.size(), LINE_ENTRIES.length);
        for (int index = 0; index < visibleLines; index++) {
            SidebarLine line = lines.get(index);
            String entry = LINE_ENTRIES[index];
            Team team = scoreboard.registerNewTeam(LINE_TEAM_PREFIX + index);
            team.addEntry(entry);
            team.prefix(component(renderText(line, placeholders)));

            Score score = objective.getScore(entry);
            score.setScore(visibleLines - index);
            if (line instanceof ScoredLine scoredLine && scoredLine.getScore() != null) {
                String fixedScore = replacePlaceholders(scoredLine.getScore(), placeholders);
                score.numberFormat(NumberFormat.fixed(component(fixedScore)));
            }
        }
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
        scoreboards.values().forEach(scoreboard -> applyTab(scoreboard, tab));
    }

    void applyTab(@NotNull Scoreboard scoreboard, @NotNull PlayerTab tab) {
        Team team = scoreboard.getTeam(teamName(tab.getIdentifier()));
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName(tab.getIdentifier()));
        }

        Component prefix = component(renderText(tab.getPrefix(), tab.getPlaceholders()));
        if (!team.prefix().equals(prefix)) team.prefix(prefix);

        Component suffix = component(renderText(tab.getSuffix(), tab.getPlaceholders()));
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

        // The client ignores scoreboard team formatting when playerListName is
        // a non-null custom component. Keep the default profile name active so
        // this viewer's team prefix, suffix and color render both TAB and the
        // overhead name tag.
        PlayerListNameState.enforceScoreboardFormatting(tab.getPlayer());
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
