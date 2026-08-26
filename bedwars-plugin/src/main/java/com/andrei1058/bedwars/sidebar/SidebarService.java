package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.sidebar.PlayerSidebarInitEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.api.sidebar.ISidebar;
import com.andrei1058.bedwars.api.sidebar.ISidebarService;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.metrics.MetricsManager;
import com.andrei1058.bedwars.sidebar.thread.*;
import com.andrei1058.spigot.sidebar.SidebarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.api.language.Language.getScoreboard;

public class SidebarService implements ISidebarService {

    private static final int MIN_GENERAL_REFRESH_INTERVAL = 20;
    private static final int MIN_TITLE_REFRESH_INTERVAL = 4;
    private static final int TAB_REFRESH_VIEWERS_PER_TICK = 4;
    private static final long JOIN_INITIALIZATION_DELAY_TICKS = 5L;

    private static SidebarService instance;

    private final SidebarManager sidebarHandler;
    private final HashMap<UUID, BwSidebar> sidebars = new HashMap<>();
    private final HashMap<UUID, BukkitTask> delayedSidebarTasks = new HashMap<>();
    private final HashSet<UUID> pendingWorldResynchronizations = new HashSet<>();
    private final ArrayDeque<BwSidebar> pendingTabRefreshes = new ArrayDeque<>();
    private final Map<IArena, LinkedHashMap<UUID, Player>> pendingEliminationRefreshes =
            new IdentityHashMap<>();
    private BukkitTask tabRefreshBatchTask;

    public static boolean init(JavaPlugin plugin) {
        if (null == instance) {
            instance = new SidebarService();
            if (instance.sidebarHandler == null) {
                return false;
            }

            var log = plugin.getLogger();

            int playerListRefreshInterval = config.getInt(ConfigPath.SB_CONFIG_SIDEBAR_LIST_REFRESH);
            int effectivePlayerListRefreshInterval = playerListRefreshInterval;
            if (playerListRefreshInterval < 1) {
                log.info("Scoreboard names list refresh is disabled. (It is set to " + playerListRefreshInterval + ").");
            } else {
                effectivePlayerListRefreshInterval = refreshInterval(playerListRefreshInterval, MIN_GENERAL_REFRESH_INTERVAL);
                Bukkit.getScheduler().runTaskTimer(plugin, new RefreshPlayerListTask(), 1L, effectivePlayerListRefreshInterval);
            }
            final int playerListMetric = effectivePlayerListRefreshInterval;
            MetricsManager.appendPie("sb_list_refresh_interval", () -> String.valueOf(playerListMetric));

            int placeholdersRefreshInterval = config.getInt(ConfigPath.SB_CONFIG_SIDEBAR_PLACEHOLDERS_REFRESH_INTERVAL);
            int effectivePlaceholdersRefreshInterval = placeholdersRefreshInterval;
            if (placeholdersRefreshInterval < 1) {
                log.info("Scoreboard placeholders refresh is disabled. (It is set to " + placeholdersRefreshInterval + ").");
            } else {
                effectivePlaceholdersRefreshInterval = refreshInterval(placeholdersRefreshInterval, MIN_GENERAL_REFRESH_INTERVAL);
                Bukkit.getScheduler().runTaskTimer(plugin, new RefreshPlaceholdersTask(), 1L, effectivePlaceholdersRefreshInterval);
            }
            final int placeholdersMetric = effectivePlaceholdersRefreshInterval;
            MetricsManager.appendPie("sb_placeholder_refresh_interval", () -> String.valueOf(placeholdersMetric));

            int titleRefreshInterval = config.getInt(ConfigPath.SB_CONFIG_SIDEBAR_TITLE_REFRESH_INTERVAL);
            int effectiveTitleRefreshInterval = titleRefreshInterval;
            if (titleRefreshInterval < 1) {
                log.info("Scoreboard title refresh is disabled. (It is set to " + titleRefreshInterval + ").");
            } else {
                effectiveTitleRefreshInterval = refreshInterval(titleRefreshInterval, MIN_TITLE_REFRESH_INTERVAL);
                Bukkit.getScheduler().runTaskTimer(plugin, new RefreshTitleTask(), 1L, effectiveTitleRefreshInterval);
            }
            final int titleMetric = effectiveTitleRefreshInterval;
            MetricsManager.appendPie("sb_title_refresh_interval", () -> String.valueOf(titleMetric));

            int healthAnimationInterval = config.getInt(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_REFRESH);
            int effectiveHealthAnimationInterval = healthAnimationInterval;
            if (healthAnimationInterval < 1) {
                log.info("Scoreboard health animation refresh is disabled. (It is set to " + healthAnimationInterval + ").");
            } else {
                effectiveHealthAnimationInterval = refreshInterval(healthAnimationInterval, MIN_GENERAL_REFRESH_INTERVAL);
                Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new RefreshLifeTask(), 1L, effectiveHealthAnimationInterval);
            }
            final int healthMetric = effectiveHealthAnimationInterval;
            MetricsManager.appendPie("sb_health_refresh_interval", () -> String.valueOf(healthMetric));

            int tabHeaderFooterRefreshInterval = config.getInt(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_REFRESH_INTERVAL);
            int effectiveTabHeaderFooterRefreshInterval = tabHeaderFooterRefreshInterval;
            if (tabHeaderFooterRefreshInterval < 1 || !config.getBoolean(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_ENABLE)) {
                log.info("Scoreboard Tab header-footer refresh is disabled.");
            } else {
                effectiveTabHeaderFooterRefreshInterval = refreshInterval(tabHeaderFooterRefreshInterval, MIN_GENERAL_REFRESH_INTERVAL);
                Bukkit.getScheduler().runTaskTimer(plugin, new RefreshTabHeaderFooterTask(), 1L, effectiveTabHeaderFooterRefreshInterval);
            }
            final int headerFooterMetric = effectiveTabHeaderFooterRefreshInterval;
            MetricsManager.appendPie("sb_header_footer_refresh_interval", () -> String.valueOf(headerFooterMetric));

            var lobbySidebar = config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR) &&
                    BedWars.getServerType() == ServerType.MULTIARENA;
            MetricsManager.appendPie("sb_lobby_enable", () -> String.valueOf(lobbySidebar));
            var gameSidebar = config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_GAME_SIDEBAR);
            MetricsManager.appendPie("sb_game_enable", () -> String.valueOf(gameSidebar));

            BedWars.registerEvents(new ScoreboardListener());
        }
        return instance.sidebarHandler != null;
    }

    private SidebarService() {
        sidebarHandler = SidebarManager.init();
    }

    private static int refreshInterval(int configuredInterval, int minimumInterval) {
        return Math.max(configuredInterval, minimumInterval);
    }

    public void giveSidebar(@NotNull Player player, @Nullable IArena arena, boolean delay) {
        if (sidebarHandler == null || !player.isOnline()) return;
        // Callers can outlive an arena transition (for example the delayed
        // lobby cleanup). Always render the player's current registry state.
        arena = Arena.getArenaByPlayer(player);
        UUID playerId = player.getUniqueId();
        if (delay) {
            IArena expectedArena = arena;
            cancelDelayedSidebar(playerId);
            BukkitTask task = Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
                delayedSidebarTasks.remove(playerId);
                if (!player.isOnline() || Arena.getArenaByPlayer(player) != expectedArena) {
                    return;
                }
                giveSidebar(player, expectedArena, false);
            }, JOIN_INITIALIZATION_DELAY_TICKS);
            delayedSidebarTasks.put(playerId, task);
            return;
        }
        cancelDelayedSidebar(playerId);
        BwSidebar sidebar = sidebars.getOrDefault(playerId, null);
        boolean sidebarEnabled = arena == null
                ? config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR)
                    && BedWars.getServerType() != ServerType.SHARED
                : config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_GAME_SIDEBAR);
        boolean lobbyTabEnabled = arena == null && shouldKeepLobbyTabContext(
                config.getBoolean(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_ENABLE),
                config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY));
        // Team color and overhead name-tag ownership are gameplay state, not a
        // cosmetic sidebar option. Every arena player therefore needs a TAB
        // context even when all full-format/header/sidebar switches are off.
        boolean arenaTabEnabled = shouldKeepArenaTabContext(arena);
        boolean tabContextEnabled = lobbyTabEnabled || arenaTabEnabled;

        // check if we might need to remove the existing sidebar
        if (null != sidebar) {
            if (null == arena) {
                if (!sidebarEnabled && !lobbyTabEnabled) {
                    this.remove(sidebar);
                    return;
                }
            } else {
                if (!sidebarEnabled && !arenaTabEnabled) {
                    this.remove(sidebar);
                    return;
                }
            }
        }

        if (!sidebarEnabled && !tabContextEnabled) return;

        // set sidebar lines based on game state or lobby
        List<String> lines = null;
        List<String> title;
        if (null == arena) {
            if (sidebarEnabled) {
                lines = Language.getList(player, Messages.SCOREBOARD_LOBBY);
            } else if (lobbyTabEnabled) {
                lines = Collections.emptyList();
            }
        } else if (!sidebarEnabled) {
            lines = Collections.emptyList();
        } else {
            if (arena.getStatus() == GameState.waiting) {
                if (arena.isSpectator(player)) {
                    lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".waiting.spectator", Messages.SCOREBOARD_DEFAULT_WAITING_SPEC);
                } else {
                    lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".waiting.player", Messages.SCOREBOARD_DEFAULT_WAITING);
                }
            } else if (arena.getStatus() == GameState.starting) {
                if (arena.isSpectator(player)) {
                    lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".starting.spectator", Messages.SCOREBOARD_DEFAULT_STARTING_SPEC);
                } else {
                    lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".starting.player", Messages.SCOREBOARD_DEFAULT_STARTING);
                }
            } else if (arena.getStatus() == GameState.playing) {
                if (arena.isSpectator(player)) {
                    ITeam holderExTeam = arena.getExTeam(player.getUniqueId());
                    if (null == holderExTeam) {
                        lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".playing.spectator", Messages.SCOREBOARD_DEFAULT_PLAYING_SPEC);
                    } else {
                        lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".playing.eliminated", Messages.SCOREBOARD_DEFAULT_PLAYING_SPEC_ELIMINATED);
                    }
                } else {
                    lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".playing.alive", Messages.SCOREBOARD_DEFAULT_PLAYING);
                }
            } else if (arena.getStatus() == GameState.restarting) {

                ITeam holderTeam = arena.getTeam(player);
                ITeam holderExTeam = null == holderTeam ? arena.getExTeam(player.getUniqueId()) : null;

                if (null == holderTeam && null == holderExTeam) {
                    lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".restarting.spectator", Messages.SCOREBOARD_DEFAULT_RESTARTING_SPEC);
                } else {
                    if (null == holderTeam && holderExTeam.equals(arena.getWinner())) {
                        lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".restarting.winner-eliminated", Messages.SCOREBOARD_DEFAULT_RESTARTING_WIN2);
                    } else if (null == holderExTeam && holderTeam.equals(arena.getWinner())) {
                        lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".restarting.winner-alive", Messages.SCOREBOARD_DEFAULT_RESTARTING_WIN1);
                    } else {
                        lines = getScoreboard(player, "sidebar." + arena.getGroup() + ".restarting.loser", Messages.SCOREBOARD_DEFAULT_RESTARTING_LOSER);
                    }
                }
            }
        }

        // if we do not have lines we eventually remove the sidebar
        if (null == lines || lines.isEmpty()) {
            if (tabContextEnabled) {
                boolean newlyAdded = false;
                if (sidebar == null) {
                    sidebar = new BwSidebar(player);
                    newlyAdded = true;

                    PlayerSidebarInitEvent event = new PlayerSidebarInitEvent(player, sidebar);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) return;
                }
                sidebar.setContent(Collections.emptyList(), Collections.emptyList(), arena);
                if (newlyAdded) sidebars.put(player.getUniqueId(), sidebar);
                return;
            }
            if (null != sidebar) {
                this.remove(sidebar);
            }
            return;
        }

        // title is the first line from array
        title = new ArrayList<>(Arrays.asList(lines.get(0).split(",")));
        if (lines.size() == 1) {
            lines = new ArrayList<>();
        }
        lines = lines.subList(1, lines.size());

        // at this point we are sure we need a sidebar instance
        boolean newlyAdded = false;
        if (null == sidebar) {
            sidebar = new BwSidebar(player);
            newlyAdded = true;

            PlayerSidebarInitEvent event = new PlayerSidebarInitEvent(player, sidebar);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
        }
        sidebar.setContent(title, lines, arena);

        if (newlyAdded) {
            sidebars.put(player.getUniqueId(), sidebar);
        }
    }

    /** Replay after initial/respawn loading or consume a pending world change. */
    void handleClientLoadedWorld(@NotNull Player player, boolean initialLoad) {
        boolean changedWorld = pendingWorldResynchronizations.remove(player.getUniqueId());
        resynchronizeClientState(player, initialLoad, changedWorld);
    }

    private void resynchronizeClientState(@NotNull Player player, boolean initialLoad,
                                          boolean changedWorld) {
        if (sidebarHandler == null || !player.isOnline()) return;
        if (!requiresClientResynchronization(initialLoad, changedWorld)) {
            return;
        }
        cancelDelayedSidebar(player.getUniqueId());
        IArena currentArena = Arena.getArenaByPlayer(player);
        BwSidebar sidebar = sidebars.get(player.getUniqueId());
        if (shouldCreateSidebarOnClientLoad(initialLoad, changedWorld, sidebar != null)) {
            // The client-load event can precede the asynchronous lobby
            // teleport callback. Create the TAB context now instead of
            // consuming the only initial replay and leaving a vanilla list.
            giveSidebar(player, currentArena, false);
            return;
        }

        if (sidebar.getArena() != currentArena) {
            giveSidebar(player, currentArena, false);
            return;
        }
        sidebar.resynchronizeClientState();
    }

    /**
     * Rebuild after an ordinary cross-world teleport. Paper 1.21.11 does not
     * fire PlayerClientLoadedWorldEvent again for this path, so the next-tick
     * fallback must consume the marker itself. A real client-loaded event can
     * consume it first; the set then makes the fallback a no-op.
     */
    void markClientWorldChange(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        scheduleWorldResynchronization(
                pendingWorldResynchronizations,
                playerId,
                task -> Bukkit.getScheduler().runTask(BedWars.plugin, task),
                () -> resynchronizeClientState(player, false, true)
        );
    }

    static void scheduleWorldResynchronization(@NotNull Set<UUID> pending, @NotNull UUID playerId,
                                               @NotNull Consumer<Runnable> scheduler,
                                               @NotNull Runnable resynchronization) {
        if (!pending.add(playerId)) return;
        scheduler.accept(() -> {
            if (!pending.remove(playerId)) return;
            resynchronization.run();
        });
    }

    /** Replay only the PlayerInfo row rebuilt by Paper's showPlayer path. */
    void handlePlayerShown(@NotNull Player viewer, @NotNull Player target) {
        if (!viewer.isOnline() || !target.isOnline()) return;
        BwSidebar sidebar = sidebars.get(viewer.getUniqueId());
        if (sidebar != null && shouldReplayPlayerShown(
                sidebar.getArena(), target.getUniqueId(), pendingEliminationRefreshes)) {
            sidebar.replayPlayerListEntry(target);
        }
    }

    /**
     * An elimination already owns the target row until its coalesced arena
     * refresh runs. Paper's showPlayer callback would otherwise replay the
     * same row once for every visibility pair in the death path.
     */
    static boolean shouldReplayPlayerShown(
            @Nullable IArena viewerArena, @NotNull UUID targetId,
            @NotNull Map<IArena, ? extends Map<UUID, Player>> pendingRefreshes) {
        if (viewerArena == null) return true;
        Map<UUID, Player> pendingPlayers = pendingRefreshes.get(viewerArena);
        return pendingPlayers == null || !pendingPlayers.containsKey(targetId);
    }

    /**
     * Synchronize a newly connected player after Paper has broadcast its
     * ADD_PLAYER entry. BUNGEE may already have added the player to an arena
     * during PlayerJoinEvent, while multi-arena/shared players can still be in
     * a lobby at this point.
     */
    void synchronizeJoinedPlayer(@NotNull Player joinedPlayer) {
        if (sidebarHandler == null || !joinedPlayer.isOnline()) return;
        IArena joinedArena = Arena.getArenaByPlayer(joinedPlayer);
        if (joinedArena == null) {
            applyLobbyTab(joinedPlayer);
            return;
        }
        UUID joinedId = joinedPlayer.getUniqueId();
        for (BwSidebar sidebar : sidebars.values()) {
            if (sidebar.getArena() != joinedArena
                    || sidebar.getPlayer().getUniqueId().equals(joinedId)) continue;
            sidebar.replayPlayerListEntry(joinedPlayer);
        }
    }

    /** Drop a departing target from every viewer so TAB state cannot grow with player churn. */
    void removePlayerFromTabs(@NotNull Player player) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        UUID playerId = player.getUniqueId();
        sidebars.values().forEach(sidebar -> sidebar.removePlayerListEntry(playerId));
    }

    static boolean requiresClientResynchronization(boolean initialLoad, boolean changedWorld) {
        return initialLoad || changedWorld;
    }

    static boolean shouldCreateSidebarOnClientLoad(boolean initialLoad, boolean changedWorld,
                                                   boolean sidebarExists) {
        return requiresClientResynchronization(initialLoad, changedWorld) && !sidebarExists;
    }

    /**
     * Kill a sidebar lifecycle.
     */
    public void remove(@NotNull BwSidebar sidebar) {
        UUID playerId = sidebar.getPlayer().getUniqueId();
        cancelDelayedSidebar(playerId);
        pendingWorldResynchronizations.remove(playerId);
        this.sidebars.remove(playerId);
        sidebar.remove();
    }

    public void remove(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        cancelDelayedSidebar(playerId);
        pendingWorldResynchronizations.remove(playerId);
        BwSidebar sidebar = this.sidebars.remove(playerId);
        if (null != sidebar) {
            sidebar.remove();
        }
    }

    private void cancelDelayedSidebar(@NotNull UUID playerId) {
        BukkitTask pending = delayedSidebarTasks.remove(playerId);
        if (pending != null) {
            pending.cancel();
        }
    }

    /** Remove scoreboards that still reference an arena being destroyed. */
    public void removeArena(@NotNull IArena arena) {
        pendingEliminationRefreshes.remove(arena);
        List<BwSidebar> stale = sidebars.values().stream()
                .filter(sidebar -> sidebar.getArena() == arena)
                .toList();
        stale.forEach(this::remove);
    }

    /** Restore every client-owned TAB/scoreboard value before plugin unload. */
    void shutdown() {
        delayedSidebarTasks.values().forEach(BukkitTask::cancel);
        delayedSidebarTasks.clear();
        if (tabRefreshBatchTask != null) tabRefreshBatchTask.cancel();
        tabRefreshBatchTask = null;
        pendingTabRefreshes.clear();
        pendingWorldResynchronizations.clear();
        pendingEliminationRefreshes.clear();
        List<BwSidebar> activeSidebars = new ArrayList<>(sidebars.values());
        for (BwSidebar sidebar : activeSidebars) {
            try {
                sidebar.remove();
            } catch (RuntimeException exception) {
                BedWars.plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "关闭插件时无法完整清理 " + sidebar.getPlayer().getName() + " 的 TAB 状态。",
                        exception);
            }
        }
        sidebars.clear();
        BwTabList.restorePlayerListOrder();
    }

    public static SidebarService getInstance() {
        return instance;
    }

    static boolean shouldKeepLobbyTabContext(boolean headerFooterEnabled, boolean playerListFormattingEnabled) {
        return headerFooterEnabled || playerListFormattingEnabled;
    }

    static boolean shouldKeepArenaTabContext(@Nullable IArena arena) {
        return arena != null;
    }

    protected SidebarManager getSidebarHandler() {
        return sidebarHandler;
    }

    public void refreshTitles() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        activeSidebars().forEach(sidebar -> sidebar.getHandle().refreshTitle());
    }

    public void refreshPlaceholders() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        activeSidebars().forEach(sidebar -> sidebar.getHandle().refreshPlaceholders());
    }

    public void refreshPlaceholders(IArena arena) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        activeSidebars().stream()
                .filter(sidebar -> sidebar.getArena() == arena)
                .forEach(sidebar -> sidebar.getHandle().refreshPlaceholders());
    }

    public void refreshTabList() {
        if (sidebarHandler == null || sidebars.isEmpty() || tabRefreshBatchTask != null) return;
        pendingTabRefreshes.clear();
        pendingTabRefreshes.addAll(activeSidebars());
        if (pendingTabRefreshes.isEmpty()) return;
        tabRefreshBatchTask = Bukkit.getScheduler().runTaskTimer(
                BedWars.plugin, this::refreshTabListBatch, 0L, 1L);
    }

    private void refreshTabListBatch() {
        int remaining = TAB_REFRESH_VIEWERS_PER_TICK;
        while (remaining-- > 0) {
            BwSidebar sidebar = pendingTabRefreshes.pollFirst();
            if (sidebar == null) {
                tabRefreshBatchTask.cancel();
                tabRefreshBatchTask = null;
                return;
            }
            Player player = sidebar.getPlayer();
            if (player.isOnline() && sidebars.get(player.getUniqueId()) == sidebar
                    && sidebar.getHandle() != null) {
                sidebar.getHandle().playerTabRefreshAnimation();
            }
        }
    }

    public void refreshTabHeaderFooter() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        activeSidebars().forEach(sidebar -> {
            if (sidebar.getHeaderFooter() != null) {
                this.sidebarHandler.sendHeaderFooter(sidebar.getPlayer(), sidebar.getHeaderFooter());
            }
        });
    }

    public void refreshHealth() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        activeSidebars().forEach(sidebar -> {
            IArena arena = sidebar.getArena();
            if (Arena.isRegistered(arena)) {
                boolean displayHealth = SidebarHealthPolicy.shouldDisplay(arena.getStatus(), false);
                sidebar.getHandle().playerHealthRefreshAnimation();
                for (Player player : arena.getPlayers()) {
                    if (displayHealth) {
                        sidebar.getHandle().setPlayerHealth(player, (int) Math.ceil(player.getHealth()));
                    } else {
                        sidebar.getHandle().clearPlayerHealth(player);
                    }
                }
                arena.getSpectators().forEach(sidebar.getHandle()::clearPlayerHealth);
            }
        });
    }

    private List<BwSidebar> activeSidebars() {
        return sidebars.values().stream()
                .filter(Objects::nonNull)
                .filter(sidebar -> sidebar.getHandle() != null)
                .filter(sidebar -> sidebar.getPlayer().isOnline())
                .filter(sidebar -> sidebars.get(sidebar.getPlayer().getUniqueId()) == sidebar)
                .toList();
    }

    @Override
    public @Nullable ISidebar getSidebar(@NotNull Player player) {
        return this.sidebars.getOrDefault(player.getUniqueId(), null);
    }

    public void refreshHealth(IArena arena, Player player, int health) {
        if (sidebarHandler == null || sidebars.isEmpty() || !Arena.isRegistered(arena)) return;
        activeSidebars().forEach(sidebar -> {
            if (sidebar.getArena() == arena) {
                if (SidebarHealthPolicy.shouldDisplay(arena.getStatus(), arena.isSpectator(player))) {
                    sidebar.getHandle().setPlayerHealth(player, health);
                } else {
                    sidebar.getHandle().clearPlayerHealth(player);
                }
            }
        });
    }

    public void handleReJoin(IArena arena, Player player) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> {
            if (null != v.getArena() && v.getArena().equals(arena)) {
                v.giveUpdateTabFormat(player, false);
            }
        });
    }

    public void handleJoin(IArena arena, Player player, @Nullable Boolean spectator) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        updateArenaPlayerTabs(sidebars.values(), arena, player, spectator);
    }

    /** Immediately replay a changed pre-game team preference to every arena viewer. */
    public void handlePreGameTeamSelection(@NotNull IArena arena,
                                           @NotNull Collection<Player> affectedPlayers) {
        if (sidebarHandler == null || sidebars.isEmpty() || affectedPlayers.isEmpty()) return;
        updatePreGameTeamTabs(sidebars.values(), arena, affectedPlayers);
    }

    static void updatePreGameTeamTabs(@NotNull Collection<? extends ISidebar> sidebars,
                                      @NotNull IArena arena,
                                      @NotNull Collection<Player> affectedPlayers) {
        for (ISidebar sidebar : sidebars) {
            if (sidebar == null || sidebar.getArena() != arena) continue;
            for (Player affected : affectedPlayers) {
                if (affected.isOnline()) sidebar.giveUpdateTabFormat(affected, false, null);
            }
        }
    }

    /**
     * Refresh elimination state after the arena has moved the player out of
     * its active team. Multiple eliminations from the same arena and tick are
     * collapsed into one placeholder pass and one TAB-row update per player.
     */
    public void handleElimination(@NotNull IArena arena, @NotNull Player player) {
        if (sidebarHandler == null) return;
        scheduleArenaEliminationRefresh(
                pendingEliminationRefreshes,
                arena,
                player,
                task -> Bukkit.getScheduler().runTask(BedWars.plugin, task),
                this::flushArenaEliminationRefresh
        );
    }

    static void scheduleArenaEliminationRefresh(
            @NotNull Map<IArena, LinkedHashMap<UUID, Player>> pendingRefreshes,
            @NotNull IArena arena,
            @NotNull Player player,
            @NotNull Consumer<Runnable> scheduler,
            @NotNull BiConsumer<IArena, Collection<Player>> refresh) {
        LinkedHashMap<UUID, Player> players = pendingRefreshes.get(arena);
        boolean scheduleRefresh = players == null;
        if (players == null) {
            players = new LinkedHashMap<>();
            pendingRefreshes.put(arena, players);
        }
        players.put(player.getUniqueId(), player);
        if (scheduleRefresh) {
            scheduler.accept(() -> {
                LinkedHashMap<UUID, Player> queuedPlayers = pendingRefreshes.remove(arena);
                if (queuedPlayers != null && !queuedPlayers.isEmpty()) {
                    refresh.accept(arena, List.copyOf(queuedPlayers.values()));
                }
            });
        }
    }

    private void flushArenaEliminationRefresh(@NotNull IArena arena,
                                               @NotNull Collection<Player> eliminatedPlayers) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;

        List<BwSidebar> arenaSidebars = sidebars.values().stream()
                .filter(sidebar -> sidebar.getArena() == arena)
                .toList();
        refreshEliminationState(
                eliminatedPlayers,
                player -> isCurrentElimination(arena, player, Arena::getArenaByPlayer),
                () -> arenaSidebars.forEach(sidebar -> {
                    if (sidebar.getHandle() != null) sidebar.getHandle().refreshPlaceholders();
                }),
                player -> updateArenaPlayerTabs(arenaSidebars, arena, player, true)
        );
    }

    static boolean isCurrentElimination(@NotNull IArena arena, @NotNull Player player,
                                        @NotNull Function<Player, IArena> arenaLookup) {
        return player.isOnline() && arenaLookup.apply(player) == arena && arena.isSpectator(player);
    }

    static void refreshEliminationState(@NotNull Collection<Player> queuedPlayers,
                                        @NotNull Predicate<Player> currentPlayer,
                                        @NotNull Runnable refreshPlaceholders,
                                        @NotNull Consumer<Player> refreshTabRow) {
        refreshPlaceholders.run();
        queuedPlayers.stream().filter(currentPlayer).forEach(refreshTabRow);
    }

    static void updateArenaPlayerTabs(@NotNull Collection<? extends ISidebar> sidebars,
                                      @NotNull IArena arena, @NotNull Player player,
                                      @Nullable Boolean spectator) {
        for (ISidebar sidebar : sidebars) {
            if (sidebar == null || sidebar.getArena() != arena) continue;
            sidebar.giveUpdateTabFormat(player, false, spectator);
        }
    }

    public void applyLobbyTab(Player player) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> {
            if (null == v.getArena()) {
                if (!v.getPlayer().equals(player)) {
                    v.giveUpdateTabFormat(player, false);
                }
            }
        });
    }

    public void handleInvisibility(ITeam team, Player player, boolean toggle) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> {
            if (null != v.getArena() && v.getArena().equals(team.getArena())) {
                v.handleInvisibilityPotion(player, toggle);
            }
        });
    }
}
