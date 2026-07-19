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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.api.language.Language.getScoreboard;

public class SidebarService implements ISidebarService {

    private static final int MIN_GENERAL_REFRESH_INTERVAL = 20;
    private static final int MIN_TITLE_REFRESH_INTERVAL = 4;

    private static SidebarService instance;

    private final SidebarManager sidebarHandler;
    private final HashMap<UUID, BwSidebar> sidebars = new HashMap<>();

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
        if (sidebarHandler == null) return;
        BwSidebar sidebar = sidebars.getOrDefault(player.getUniqueId(), null);
        boolean sidebarEnabled = arena == null
                ? config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR)
                    && BedWars.getServerType() != ServerType.SHARED
                : config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_GAME_SIDEBAR);
        boolean lobbyTabEnabled = arena == null && shouldKeepLobbyTabContext(
                config.getBoolean(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_ENABLE),
                config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY));

        // check if we might need to remove the existing sidebar
        if (null != sidebar) {
            if (null == arena) {
                if (!sidebarEnabled && !lobbyTabEnabled) {
                    this.remove(sidebar);
                    return;
                }
            } else {
                // if sidebar is disabled in game
                if (!sidebarEnabled) {
                    this.remove(sidebar);
                    return;
                }
            }
        }

        if (!sidebarEnabled && !lobbyTabEnabled) return;

        // set sidebar lines based on game state or lobby
        List<String> lines = null;
        List<String> title;
        if (null == arena) {
            if (sidebarEnabled) {
                lines = Language.getList(player, Messages.SCOREBOARD_LOBBY);
            } else if (lobbyTabEnabled) {
                lines = Collections.emptyList();
            }
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
            if (arena == null && lobbyTabEnabled) {
                boolean newlyAdded = false;
                if (sidebar == null) {
                    sidebar = new BwSidebar(player);
                    newlyAdded = true;

                    PlayerSidebarInitEvent event = new PlayerSidebarInitEvent(player, sidebar);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) return;
                }
                sidebar.setContent(Collections.emptyList(), Collections.emptyList(), null);
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

    /**
     * Kill a sidebar lifecycle.
     */
    public void remove(@NotNull BwSidebar sidebar) {
        this.sidebars.remove(sidebar.getPlayer().getUniqueId());
        sidebar.remove();
    }

    public void remove(@NotNull Player player) {
        BwSidebar sidebar = this.sidebars.remove(player.getUniqueId());
        if (null != sidebar) {
            sidebar.remove();
        }
    }

    /** Remove scoreboards that still reference an arena being destroyed. */
    public void removeArena(@NotNull IArena arena) {
        List<BwSidebar> stale = sidebars.values().stream()
                .filter(sidebar -> sidebar.getArena() == arena)
                .toList();
        stale.forEach(this::remove);
    }

    public static SidebarService getInstance() {
        return instance;
    }

    static boolean shouldKeepLobbyTabContext(boolean headerFooterEnabled, boolean playerListFormattingEnabled) {
        return headerFooterEnabled || playerListFormattingEnabled;
    }

    protected SidebarManager getSidebarHandler() {
        return sidebarHandler;
    }

    public void refreshTitles() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> v.getHandle().refreshTitle());
    }

    public void refreshPlaceholders() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> v.getHandle().refreshPlaceholders());
    }

    public void refreshPlaceholders(IArena arena) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> {
            if (v.getArena() != null)
                if (v.getArena().equals(arena)) {
                    v.getHandle().refreshPlaceholders();
                }
        });
    }

    public void refreshTabList() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> v.getHandle().playerTabRefreshAnimation());
    }

    public void refreshTabHeaderFooter() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> {
            if (null != v && null != v.getHeaderFooter()) {
                this.sidebarHandler.sendHeaderFooter(v.getPlayer(), v.getHeaderFooter());
            }
        });
    }

    public void refreshHealth() {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> {
            if (null != v.getArena() && Arena.getArenas().contains(v.getArena())) {
                v.getHandle().playerHealthRefreshAnimation();
                for (Player player : v.getArena().getPlayers()) {
                    v.getHandle().setPlayerHealth(player, (int) Math.ceil(player.getHealth()));
                }
            }
        });
    }

    @Override
    public @Nullable ISidebar getSidebar(@NotNull Player player) {
        return this.sidebars.getOrDefault(player.getUniqueId(), null);
    }

    public void refreshHealth(IArena arena, Player player, int health) {
        if (sidebarHandler == null || sidebars.isEmpty()) return;
        this.sidebars.forEach((k, v) -> {
            if (null != v.getArena() && v.getArena().equals(arena)) {
                v.getHandle().setPlayerHealth(player, health);
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
        this.sidebars.forEach((k, v) -> {
            if (null != v.getArena() && v.getArena().equals(arena)) {
                if (!v.getPlayer().equals(player)) {
                    v.giveUpdateTabFormat(player, false, spectator);
                }
            }
        });
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
