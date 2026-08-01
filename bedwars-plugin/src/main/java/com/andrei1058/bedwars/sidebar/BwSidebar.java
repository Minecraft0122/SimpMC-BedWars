package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.stats.DefaultStatistics;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.sidebar.ISidebar;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.stats.StatisticsOrdered;
import com.andrei1058.bedwars.levels.internal.PlayerLevel;
import com.andrei1058.bedwars.stats.PlayerStats;
import com.andrei1058.spigot.sidebar.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.*;

public class BwSidebar implements ISidebar {

    static final int TAB_MIN_WIDTH = 128;
    private static final String TAB_WIDTH_SPACER = " ".repeat(TAB_MIN_WIDTH);

    private static final SidebarLine EMPTY_TITLE = new SidebarLine() {
        @Override
        public @NotNull String getLine() {
            return "";
        }
    };

    private final Player player;
    private IArena arena;
    private GameState renderedArenaState;
    private Sidebar handle;
    private TabHeaderFooter headerFooter;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat nextEventDateFormat;

    private final ConcurrentLinkedQueue<PlaceholderProvider> persistentProviders = new ConcurrentLinkedQueue<>();

    private final BwTabList tabList;

    public @Nullable StatisticsOrdered topStatistics;

    protected BwSidebar(Player player) {
        this.player = player;
        nextEventDateFormat = new SimpleDateFormat(getMsg(player, Messages.FORMATTING_SCOREBOARD_NEXEVENT_TIMER));
        nextEventDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat = new SimpleDateFormat(getMsg(player, Messages.FORMATTING_SCOREBOARD_DATE));
        this.tabList = new BwTabList(this);

        // Persistent placeholders
        String poweredBy = BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_POWERED_BY);
        this.registerPersistentPlaceholder(new PlaceholderProvider("{poweredBy}", () -> poweredBy));
        String serverId = config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID);
        this.registerPersistentPlaceholder(new PlaceholderProvider("{server}", () -> serverId));
        String serverIp = BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP);
        this.registerPersistentPlaceholder(new PlaceholderProvider("{serverIp}", () -> serverIp));
    }

    public void remove() {
        if (handle != null) {
            tabList.onSidebarRemoval();
            handle.remove(player);
        }
        SidebarManager.getInstance().clearHeaderFooter(player);
        headerFooter = null;
    }

    public void setContent(List<String> titleArray, List<String> lineArray, @Nullable IArena arena) {
        GameState nextArenaState = arena == null ? null : arena.getStatus();
        boolean tabContextChanged = handle != null && shouldResynchronizeTabContext(
                this.arena, renderedArenaState, arena, nextArenaState);
        this.arena = arena;
        SidebarLine title = this.normalizeTitle(titleArray);
        List<SidebarLine> lines = this.normalizeLines(lineArray);

        if (null == arena) {
            // clean up
            setTopStatistics(null);
        }

        ConcurrentLinkedQueue<PlaceholderProvider> placeholders = this.getPlaceholders(this.getPlayer());
        placeholders.addAll(this.persistentProviders);

        // if it is the first time setting content we create the handle
        if (null == handle) {
            handle = SidebarService.getInstance().getSidebarHandler().createSidebar(title, lines, placeholders);
            // Populate TAB teams before attaching the scoreboard so the client
            // receives one complete initial snapshot instead of an empty board
            // followed by a burst of incremental team CREATE packets.
            tabList.handlePlayerList();
            handle.add(player);
        } else {
            handle.setContent(title, lines, placeholders);
            tabList.handlePlayerList();
        }
        if (tabContextChanged) {
            handle.add(player);
        }
        renderedArenaState = nextArenaState;
        assignTabHeaderFooter();
    }

    /** Replay the complete managed scoreboard after the client finished login. */
    void resynchronizeClientState() {
        if (handle == null || !player.isOnline()) {
            return;
        }
        handle.add(player);
        SidebarManager.getInstance().clearHeaderFooterCache(player);
        assignTabHeaderFooter();
    }

    public Player getPlayer() {
        return player;
    }

    @SuppressWarnings("ConstantConditions")
    public SidebarLine normalizeTitle(@Nullable List<String> titleArray) {
        if (null == titleArray || titleArray.isEmpty()) {
            return EMPTY_TITLE;
        }
        String[] data = new String[titleArray.size()];
        for (int x = 0; x < titleArray.size(); x++) {
            data[x] = titleArray.get(x);
        }
        return new SidebarLineAnimated(data);
    }

    /**
     * Normalize lines where subject player is sidebar holder.
     */
    @Contract(pure = true)
    public @NotNull LinkedList<SidebarLine> normalizeLines(@NotNull List<String> lineArray) {
        LinkedList<SidebarLine> lines = new LinkedList<>();

        int teamCount = 0;
        Language language = Language.getPlayerLanguage(player);
        String genericTeamFormat = language.m(Messages.FORMATTING_SCOREBOARD_TEAM_GENERIC);
        List<ITeam> displayedTeams = arena == null
                ? Collections.emptyList()
                : SidebarTeamPolicy.displayedTeams(arena);

        StatisticsOrdered.StringParser statParser = null == topStatistics ? null : topStatistics.newParser();

        for (String line : lineArray) {
            // convert old placeholders
            line = line.replace("{server_ip}", "{serverIp}");
            String scoreLine = null;

            // generic team placeholder {team}
            if (null != arena) {
                if (SidebarTeamPolicy.referencesHiddenTeam(line, arena.getTeams(), displayedTeams)) {
                    continue;
                }
                if (line.trim().equals("{team}")) {
                    if (displayedTeams.size() > teamCount) {
                        ITeam team = displayedTeams.get(teamCount++);
                        String teamName = team.getDisplayName(language);
                        String teamLetter = String.valueOf(!teamName.isEmpty() ? teamName.charAt(0) : "");

                        line = genericTeamFormat
                                .replace("{TeamLetter}", teamLetter)
                                .replace("{TeamColor}", team.getColor().chat().toString())
                                .replace("{TeamName}", teamName);

                        if (line.contains("{TeamStatus}")) {
                            line = line.replace("{TeamStatus}", "");
                            scoreLine = "{Team" + team.getName() + "Status}";
                        } else {
                            line = line.replace("{TeamStatus}", "{Team" + team.getName() + "Status}");
                        }
                    } else {
                        // skip line
                        continue;
                    }
                }

                line = line
                        .replace("{map}", arena.getDisplayName())
                        .replace("{map_name}", arena.getArenaName())
                        .replace("{group}", arena.getDisplayGroup(player));

                for (ITeam currentTeam : displayedTeams) {
                    final ChatColor color = currentTeam.getColor().chat();
                    final String teamName = currentTeam.getDisplayName(language);
                    final String teamLetter = String.valueOf(!teamName.isEmpty() ? teamName.charAt(0) : "");

                    // Static team placeholders
                    line = line
                            .replace("{Team" + currentTeam.getName() + "Color}", color.toString())
                            .replace("{Team" + currentTeam.getName() + "Name}", teamName)
                            .replace("{Team" + currentTeam.getName() + "Letter}", teamLetter);


                    boolean isMember = currentTeam.isMember(getPlayer()) || currentTeam.wasMember(getPlayer().getUniqueId());
                    if (isMember) {
                        HashMap<String, String> replacements = tabList.getTeamReplacements(currentTeam);
                        for (Map.Entry<String, String> entry : replacements.entrySet()) {
                            line = line.replace(entry.getKey(), entry.getValue());
                        }
                    }
                }
                if (arena.getWinner() != null) {
                    String winnerDisplayName = arena.getWinner().getDisplayName(Language.getPlayerLanguage(getPlayer()));
                    line = line
                            .replace(
                                    "{winnerTeamName}",
                                    winnerDisplayName
                            ).replace(
                                    "{winnerTeamLetter}",
                                    arena.getWinner().getColor().chat() + (winnerDisplayName.substring(0, 1))
                            ).replace(
                                    "{winnerTeamColor}",
                                    arena.getWinner().getColor().chat().toString()
                            );
                }

                if (null != this.topStatistics && null != statParser) {
                    line = statParser.parseString(line, language, language.m(Messages.MEANING_NOBODY));
                    if (null == line) {
                        continue;
                    }
                }
            }

            // General static placeholders
            line = line
                    .replace("{serverIp}", BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP))
                    .replace("{poweredBy}", BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_POWERED_BY))
                    .replace("{version}", plugin.getDescription().getVersion())
                    .replace("{server}", config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID))
            ;

            // Add the line to the sidebar
            String finalTemp = line;

            String[] divided = finalTemp.split(",");

            SidebarLine sidebarLine;

            if (divided.length > 1) {
                sidebarLine = normalizeTitle(Arrays.asList(divided));
            } else {
                sidebarLine = new BwSidebarLine(finalTemp, scoreLine);
            }

            lines.add(sidebarLine);
        }
        return lines;
    }

    @Override
    public void giveUpdateTabFormat(@NotNull Player player, boolean skipStateCheck, @Nullable Boolean spectator) {
        tabList.giveUpdateTabFormat(player, skipStateCheck, spectator);
    }

    void replayPlayerListEntry(@NotNull Player player) {
        tabList.replayPlayerListEntry(player);
    }

    void removePlayerListEntry(@NotNull UUID playerId) {
        tabList.removePlayerListEntry(playerId);
    }

    @SuppressWarnings("removal")
    @Override
    public boolean isTabFormattingDisabled() {
        return tabList.isTabFormattingDisabled();
    }

    /**
     * Get placeholders for given player.
     *
     * @param player subject.
     * @return placeholders.
     */
    @Contract(pure = true)
    @NotNull ConcurrentLinkedQueue<PlaceholderProvider> getPlaceholders(@NotNull Player player) {
        ConcurrentLinkedQueue<PlaceholderProvider> providers = new ConcurrentLinkedQueue<>();
        providers.add(new PlaceholderProvider("{player}", player::getDisplayName));
        providers.add(new PlaceholderProvider("{money}", () -> String.valueOf(getEconomy().getMoney(player))));
        providers.add(new PlaceholderProvider("{playerName}", player::getCustomName));
        providers.add(new PlaceholderProvider("{date}", () -> dateFormat.format(new Date(System.currentTimeMillis()))));
        // fixme 29/08/2023: disabled for now because this is not a dynamic placeholder. Let's see what's the impact.
//        providers.add(new PlaceholderProvider("{serverIp}", () -> BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP)));
        providers.add(new PlaceholderProvider("{version}", () -> plugin.getDescription().getVersion()));
        PlayerLevel level = PlayerLevel.getLevelByPlayer(player.getUniqueId());
        if (null != level) {
            providers.add(new PlaceholderProvider("{progress}", level::getProgress));
            providers.add(new PlaceholderProvider("{level}", () -> String.valueOf(level.getLevelName())));
            providers.add(new PlaceholderProvider("{levelUnformatted}", () -> String.valueOf(level.getLevel())));
            providers.add(new PlaceholderProvider("{currentXp}", level::getFormattedCurrentXp));
            providers.add(new PlaceholderProvider("{requiredXp}", level::getFormattedRequiredXp));
        }

        if (hasNoArena()) {
            providers.add(new PlaceholderProvider("{on}", () ->
                    String.valueOf(Bukkit.getOnlinePlayers().size()))
            );
            PlayerStats persistentStats = BedWars.getStatsManager().getUnsafe(player.getUniqueId());
            if (null != persistentStats) {
                providers.add(new PlaceholderProvider("{kills}", () ->
                        String.valueOf(persistentStats.getKills()))
                );
                providers.add(new PlaceholderProvider("{finalKills}", () ->
                        String.valueOf(persistentStats.getFinalKills()))
                );
                providers.add(new PlaceholderProvider("{beds}", () ->
                        String.valueOf(persistentStats.getBedsDestroyed()))
                );
                providers.add(new PlaceholderProvider("{deaths}", () ->
                        String.valueOf(persistentStats.getDeaths()))
                );
                providers.add(new PlaceholderProvider("{finalDeaths}", () ->
                        String.valueOf(persistentStats.getFinalDeaths()))
                );
                providers.add(new PlaceholderProvider("{wins}", () ->
                        String.valueOf(persistentStats.getWins()))
                );
                providers.add(new PlaceholderProvider("{losses}", () ->
                        String.valueOf(persistentStats.getLosses()))
                );
                providers.add(new PlaceholderProvider("{gamesPlayed}", () ->
                        String.valueOf(persistentStats.getGamesPlayed()))
                );
            }
        } else {
            providers.add(new PlaceholderProvider("{on}", () -> String.valueOf(arena.getPlayers().size())));
            providers.add(new PlaceholderProvider("{max}", () -> String.valueOf(arena.getMaxPlayers())));
            providers.add(new PlaceholderProvider("{nextEvent}", this::getNextEventName));

            if (arena.isSpectator(player)) {
                Language lang = getPlayerLanguage(player);
                String targetFormat = lang.m(Messages.FORMAT_SPECTATOR_TARGET);

                providers.add(new PlaceholderProvider("{spectatorTarget}", () -> {
                    if (null == player.getSpectatorTarget() || !(player.getSpectatorTarget() instanceof Player)) {
                        return "";
                    }
                    Player target = (Player) player.getSpectatorTarget();
                    ITeam targetTeam = arena.getTeam(target);

                    if (null == targetTeam) {
                        return "";
                    }
                    return targetFormat.replace("{targetTeamColor}", targetTeam.getColor().chat().toString())
                            .replace("{targetDisplayName}", target.getDisplayName())
                            .replace("{targetName}", target.getDisplayName())
                            .replace("{targetTeamName}", targetTeam.getDisplayName(lang));
                }));
            }

            providers.add(new PlaceholderProvider("{time}", () -> {
                GameState status = this.arena.getStatus();
                if (status == GameState.restarting) {
                    return arena.getRestartingTask() == null
                            ? "0"
                            : String.valueOf(Math.max(0, arena.getRestartingTask().getRestarting()));
                }
                if (status == GameState.playing) {
                    return getNextEventTime();
                } else {
                    if (status == GameState.starting) {
                        if (arena.getStartingTask() != null) {
                            return String.valueOf(arena.getStartingTask().getCountdown() + 1);
                        }
                    }
                    return dateFormat.format(new Date(System.currentTimeMillis()));
                }
            }));

            if (null != arena.getStatsHolder()) {

                arena.getStatsHolder().get(player).ifPresent(holder -> {
                    holder.getStatistic(DefaultStatistics.KILLS).ifPresent(st ->
                            providers.add(new PlaceholderProvider("{kills}", () ->
                                    String.valueOf(st.getDisplayValue(null))
                            )));

                    holder.getStatistic(DefaultStatistics.KILLS_FINAL).ifPresent(st ->
                            providers.add(new PlaceholderProvider("{finalKills}", () ->
                                    String.valueOf(st.getDisplayValue(null))
                            )));

                    holder.getStatistic(DefaultStatistics.BEDS_DESTROYED).ifPresent(st ->
                            providers.add(new PlaceholderProvider("{beds}", () ->
                                    String.valueOf(st.getDisplayValue(null))
                            )));

                    holder.getStatistic(DefaultStatistics.DEATHS).ifPresent(st ->
                            providers.add(new PlaceholderProvider("{deaths}", () ->
                                    String.valueOf(st.getDisplayValue(null))
                            )));
                });
            }

            // Dynamic team placeholders
            for (ITeam currentTeam : SidebarTeamPolicy.displayedTeams(arena)) {
                boolean isMember = currentTeam.isMember(player) || currentTeam.wasMember(player.getUniqueId());

                providers.add(new PlaceholderProvider("{Team" + currentTeam.getName() + "Status}", () -> {
                    String result;
                    if (currentTeam.isBedDestroyed()) {
                        if (currentTeam.getSize() > 0) {
                            result = getMsg(getPlayer(), Messages.FORMATTING_SCOREBOARD_BED_DESTROYED)
                                    .replace("{remainingPlayers}", String.valueOf(currentTeam.getSize()));
                        } else {
                            result = getMsg(getPlayer(), Messages.FORMATTING_SCOREBOARD_TEAM_ELIMINATED);
                        }
                    } else {
                        result = getMsg(getPlayer(), Messages.FORMATTING_SCOREBOARD_TEAM_ALIVE);
                    }
                    if (isMember) {
                        result += getMsg(getPlayer(), Messages.FORMATTING_SCOREBOARD_YOUR_TEAM);
                    }
                    return result;
                }));

                if (isMember) {
                    providers.add(new PlaceholderProvider("{teamStatus}", () -> {
                        if (currentTeam.isBedDestroyed()) {
                            if (currentTeam.getSize() > 0) {
                                return getMsg(getPlayer(), Messages.FORMATTING_SCOREBOARD_BED_DESTROYED)
                                        .replace("{remainingPlayers}", String.valueOf(currentTeam.getSize()));
                            }
                            return getMsg(getPlayer(), Messages.FORMATTING_SCOREBOARD_TEAM_ELIMINATED);
                        }
                        return getMsg(getPlayer(), Messages.FORMATTING_SCOREBOARD_TEAM_ALIVE);
                    }));
                }
            }
        }

        return providers;
    }

    @NotNull
    private String getNextEventName() {
        if (!(arena instanceof Arena)) return "-";
        Arena arena = (Arena) this.arena;
        String st = "-";
        switch (arena.getNextEvent()) {
            case EMERALD_GENERATOR_TIER_II:
                st = getMsg(getPlayer(), Messages.NEXT_EVENT_EMERALD_UPGRADE_II);
                break;
            case EMERALD_GENERATOR_TIER_III:
                st = getMsg(getPlayer(), Messages.NEXT_EVENT_EMERALD_UPGRADE_III);
                break;
            case DIAMOND_GENERATOR_TIER_II:
                st = getMsg(getPlayer(), Messages.NEXT_EVENT_DIAMOND_UPGRADE_II);
                break;
            case DIAMOND_GENERATOR_TIER_III:
                st = getMsg(getPlayer(), Messages.NEXT_EVENT_DIAMOND_UPGRADE_III);
                break;
            case GAME_END:
                st = getMsg(getPlayer(), Messages.NEXT_EVENT_GAME_END);
                break;
            case BEDS_DESTROY:
                st = getMsg(getPlayer(), Messages.NEXT_EVENT_BEDS_DESTROY);
                break;
            case ENDER_DRAGON:
                st = getMsg(getPlayer(), Messages.NEXT_EVENT_DRAGON_SPAWN);
                break;
        }

        return st;
    }

    @NotNull
    private String getNextEventTime() {
        if (!(arena instanceof Arena)) return nextEventDateFormat.format((0L));
        Arena arena = (Arena) this.arena;
        long time = 0L;
        switch (arena.getNextEvent()) {
            case EMERALD_GENERATOR_TIER_II:
            case EMERALD_GENERATOR_TIER_III:
                time = (arena.upgradeEmeraldsCount) * 1000L;
                break;
            case DIAMOND_GENERATOR_TIER_II:
            case DIAMOND_GENERATOR_TIER_III:
                time = (arena.upgradeDiamondsCount) * 1000L;
                break;
            case GAME_END:
                time = (arena.getPlayingTask().getGameEndCountdown()) * 1000L;
                break;
            case BEDS_DESTROY:
                time = (arena.getPlayingTask().getBedsDestroyCountdown()) * 1000L;
                break;
            case ENDER_DRAGON:
                time = (arena.getPlayingTask().getDragonSpawnCountdown()) * 1000L;
                break;
        }
        return time == 0 ? "0" : nextEventDateFormat.format(new Date(time));
    }

    private boolean hasNoArena() {
        return null == arena;
    }

    // Keep the original BedWars1058 state-specific TAB style. Player rows are
    // still managed separately by BwTabList.
    private void assignTabHeaderFooter() {
        if (!config.getBoolean(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_ENABLE)) {
            SidebarManager.getInstance().clearHeaderFooter(player);
            this.headerFooter = null;
            return;
        }

        Language language = Language.getPlayerLanguage(player);
        String headerPath;
        String footerPath;

        if (hasNoArena()) {
            headerPath = Messages.FORMATTING_SB_TAB_LOBBY_HEADER;
            footerPath = Messages.FORMATTING_SB_TAB_LOBBY_FOOTER;
        } else if (arena.isSpectator(player)) {
            ITeam formerTeam = arena.getExTeam(player.getUniqueId());
            if (formerTeam == null) {
                switch (arena.getStatus()) {
                    case waiting -> {
                        headerPath = Messages.FORMATTING_SB_TAB_WAITING_HEADER_SPEC;
                        footerPath = Messages.FORMATTING_SB_TAB_WAITING_FOOTER_SPEC;
                    }
                    case starting -> {
                        headerPath = Messages.FORMATTING_SB_TAB_STARTING_HEADER_SPEC;
                        footerPath = Messages.FORMATTING_SB_TAB_STARTING_FOOTER_SPEC;
                    }
                    case playing -> {
                        headerPath = Messages.FORMATTING_SB_TAB_PLAYING_SPEC_HEADER;
                        footerPath = Messages.FORMATTING_SB_TAB_PLAYING_SPEC_FOOTER;
                    }
                    case restarting -> {
                        headerPath = Messages.FORMATTING_SB_TAB_RESTARTING_SPEC_HEADER;
                        footerPath = Messages.FORMATTING_SB_TAB_RESTARTING_SPEC_FOOTER;
                    }
                    default -> throw new IllegalStateException("Unhandled arena status");
                }
            } else if (arena.getStatus() == GameState.restarting) {
                if (arena.getWinner() != null && arena.getWinner().equals(formerTeam)) {
                    headerPath = Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_HEADER;
                    footerPath = Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_FOOTER;
                } else {
                    headerPath = Messages.FORMATTING_SB_TAB_RESTARTING_ELM_HEADER;
                    footerPath = Messages.FORMATTING_SB_TAB_RESTARTING_ELM_FOOTER;
                }
            } else {
                headerPath = Messages.FORMATTING_SB_TAB_PLAYING_ELM_HEADER;
                footerPath = Messages.FORMATTING_SB_TAB_PLAYING_ELM_FOOTER;
            }
        } else {
            switch (arena.getStatus()) {
                case waiting -> {
                    headerPath = Messages.FORMATTING_SB_TAB_WAITING_HEADER;
                    footerPath = Messages.FORMATTING_SB_TAB_WAITING_FOOTER;
                }
                case starting -> {
                    headerPath = Messages.FORMATTING_SB_TAB_STARTING_HEADER;
                    footerPath = Messages.FORMATTING_SB_TAB_STARTING_FOOTER;
                }
                case playing -> {
                    headerPath = Messages.FORMATTING_SB_TAB_PLAYING_HEADER;
                    footerPath = Messages.FORMATTING_SB_TAB_PLAYING_FOOTER;
                }
                case restarting -> {
                    headerPath = Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_HEADER;
                    footerPath = Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_FOOTER;
                }
                default -> throw new IllegalStateException("Unhandled arena status");
            }
        }

        List<String> headerLines = language.l(headerPath);
        if (hasNoArena()) {
            headerLines = selectLobbyHeader(config.getYml().getStringList(ConfigPath.SB_CONFIG_TAB_LOBBY_HEADER),
                    headerLines);
        } else {
            headerLines = ensureTabWidth(headerLines);
        }

        this.headerFooter = new TabHeaderFooter(
                this.normalizeLines(headerLines),
                this.normalizeLines(language.l(footerPath)),
                withPersistentPlaceholders(getPlaceholders(this.getPlayer()))
        );

        SidebarManager.getInstance().sendHeaderFooter(player, headerFooter);
    }

    static List<String> selectLobbyHeader(List<String> configuredHeader, List<String> languageHeader) {
        List<String> selected = configuredHeader == null || configuredHeader.isEmpty()
                ? languageHeader : configuredHeader;
        return ensureTabWidth(selected);
    }

    static List<String> ensureTabWidth(List<String> selected) {
        if (selected == null || selected.isEmpty()) return selected;

        String firstLine = selected.getFirst();
        if (firstLine != null && firstLine.isBlank() && firstLine.length() >= TAB_MIN_WIDTH) {
            return selected;
        }

        List<String> widenedHeader = new ArrayList<>(selected.size() + 1);
        if (firstLine != null && firstLine.isBlank()) {
            widenedHeader.addAll(selected);
            widenedHeader.set(0, TAB_WIDTH_SPACER);
            return widenedHeader;
        }
        widenedHeader.add(TAB_WIDTH_SPACER);
        widenedHeader.addAll(selected);
        return widenedHeader;
    }

    static boolean shouldResynchronizeTabContext(@Nullable IArena previousArena,
                                                 @Nullable GameState previousState,
                                                 @Nullable IArena nextArena,
                                                 @Nullable GameState nextState) {
        return previousArena != nextArena || previousState != nextState;
    }

    @Override
    public boolean registerPersistentPlaceholder(PlaceholderProvider placeholderProvider) {
        this.persistentProviders.add(placeholderProvider);
        return true;
    }

    /**
     * Hide player name tag on head when he drinks an invisibility potion.
     * This is required because not all clients hide it automatically.
     *
     * @param _toggle true when applied, false when expired.
     */
    public void handleInvisibilityPotion(@NotNull Player player, boolean _toggle) {
        if (null == arena) {
            throw new RuntimeException("This can only be used when the player is in arena");
        }
        this.giveUpdateTabFormat(player, false);
    }

    public Sidebar getHandle() {
        return handle;
    }

    public IArena getArena() {
        return arena;
    }

    @Nullable
    public TabHeaderFooter getHeaderFooter() {
        return headerFooter;
    }

    @SuppressWarnings("unused")
    public void setHeaderFooter(@Nullable TabHeaderFooter headerFooter) {
        this.headerFooter = headerFooter;
    }

    public void setTopStatistics(@Nullable StatisticsOrdered topStatistics) {
        this.topStatistics = topStatistics;
    }

    @NotNull
    private ConcurrentLinkedQueue<PlaceholderProvider> withPersistentPlaceholders(@NotNull ConcurrentLinkedQueue<PlaceholderProvider> placeholders) {
        placeholders.addAll(this.persistentProviders);
        return placeholders;
    }
}
