/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2023 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.team.PreGameTeamSelectionManager;
import com.andrei1058.spigot.sidebar.PlayerTab;
import com.andrei1058.spigot.sidebar.Sidebar;
import com.andrei1058.spigot.sidebar.SidebarLine;
import com.andrei1058.spigot.sidebar.SidebarLineAnimated;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.andrei1058.bedwars.BedWars.*;

public class BwTabList {

    private static final Comparator<Player> PLAYER_NAME_ORDER = Comparator
            .comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Player::getName)
            .thenComparing(Player::getUniqueId);
    private static final Map<UUID, PlayerListOrderState> managedPlayerListOrders = new HashMap<>();
    private static boolean playerListOrderUpdateScheduled;

    // Player list container. Used to manipulate deployed player tab: lines ecc.
    // Key is player uuid.
    private final HashMap<UUID, PlayerTab> deployedPerPlayerTabList = new HashMap<>();
    private final BwSidebar sidebar;

    public BwTabList(BwSidebar sidebar) {
        this.sidebar = sidebar;
    }

    /**
     * Triggered when sidebar context changes.
     * Arena/ game state change.
     */
    void handlePlayerList() {

        handleHealthIcon();
        requestPlayerListOrderUpdate();
        boolean fullFormatting = !this.isTabFormattingDisabled();
        if (!fullFormatting && sidebar.getArena() == null) {
            clearDeployedTabs();
            return;
        }

        LinkedHashMap<UUID, Player> desiredPlayers = new LinkedHashMap<>();
        if (null == sidebar.getArena()) {
            // if tab formatting is enabled in lobby world
            if (config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY) &&
                    !config.getLobbyWorldName().trim().isEmpty()) {

                World lobby = Bukkit.getWorld(config.getLobbyWorldName());
                if (null == lobby) {
                    clearDeployedTabs();
                    return;
                }
                lobby.getPlayers().forEach(inLobby -> desiredPlayers.put(inLobby.getUniqueId(), inLobby));
            }
            // sometimes due to timing issues player is not listed yet in lobby players
            desiredPlayers.put(sidebar.getPlayer().getUniqueId(), sidebar.getPlayer());
            synchronizeTabs(desiredPlayers, true);
            return;
        }

        sidebar.getArena().getPlayers().forEach(playing -> desiredPlayers.put(playing.getUniqueId(), playing));
        sidebar.getArena().getSpectators().forEach(spectating -> desiredPlayers.put(spectating.getUniqueId(), spectating));
        synchronizeTabs(desiredPlayers, fullFormatting);
    }

    public void handleHealthIcon() {
        if (null == sidebar.getHandle()) {
            return;
        }

        if (null == sidebar.getArena()) {
            sidebar.getHandle().hidePlayersHealth();
            return;
        } else if (sidebar.getArena().getStatus() != GameState.playing) {
            sidebar.getHandle().hidePlayersHealth();
            return;
        }

        List<String> animation = Language.getList(sidebar.getPlayer(), Messages.FORMATTING_SCOREBOARD_HEALTH);
        if (animation.isEmpty()) return;
        SidebarLine line;
        if (animation.size() > 1) {
            String[] lines = new String[animation.size()];
            for (int i = 0; i < animation.size(); i++) {
                lines[i] = animation.get(i);
            }
            line = new SidebarLineAnimated(lines);
        } else {
            final String text = animation.get(0);
            line = new SidebarLine() {
                @NotNull
                @Override
                public String getLine() {
                    return text;
                }
            };
        }

        if (config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_ENABLE)) {
            sidebar.getHandle().showPlayersHealth(line, config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (null != sidebar.getArena() && null != sidebar.getHandle()) {
                sidebar.getArena().getPlayers().forEach(player -> {
                    if (SidebarHealthPolicy.shouldDisplay(sidebar.getArena().getStatus(), false)) {
                        sidebar.getHandle().setPlayerHealth(player, (int) Math.ceil(player.getHealth()));
                    } else {
                        sidebar.getHandle().clearPlayerHealth(player);
                    }
                });
                sidebar.getArena().getSpectators().forEach(sidebar.getHandle()::clearPlayerHealth);
            }
        }, 10L);
    }

    /**
     * @return true if tab formatting is disabled for current sidebar/ arena stage
     */
    public boolean isTabFormattingDisabled() {
        if (null == sidebar.getArena()) {

            if (getServerType() == ServerType.SHARED) {
                if (config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY) &&
                        !config.getLobbyWorldName().trim().isEmpty()) {

                    World lobby = Bukkit.getWorld(config.getLobbyWorldName());
                    return null == lobby || !sidebar.getPlayer().getWorld().getName().equals(lobby.getName());
                }
            }

            return !config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY);
        }
        GameState status = sidebar.getArena().getStatus();

        // if tab formatting is disabled in game
        if (status == GameState.playing) {
            return !config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_PLAYING);
        }

        // if tab formatting is disabled in starting
        if (status == GameState.starting) {
            return !config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING);
        }

        // if tab formatting is disabled in waiting
        if (status == GameState.waiting) {
            return !config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING);
        }

        // if tab formatting is disabled in restarting
        return status != GameState.restarting || !config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_RESTARTING);
    }

    /**
     * Handle given player in sidebar owner tab list.
     * Will remove existing tab and give a new one based on game conditions list like spectator, team red, etc.
     * Will handle invisibility potion as well.
     */
    public void giveUpdateTabFormat(@NotNull Player player, boolean skipStateCheck, @Nullable Boolean spectator) {
        // if sidebar was not created
        if (sidebar.getHandle() == null) {
            return;
        }
        requestPlayerListOrderUpdate();

        // unique tab list name
        String playerTabId = player.getUniqueId().toString();

        if (!skipStateCheck) {
            if (this.isTabFormattingDisabled()) {
                giveUpdateTeamColor(player, spectator);
                return;
            }
        }
        SidebarLine prefix;
        SidebarLine suffix;
        IArena arena = sidebar.getArena();
        Sidebar handle = sidebar.getHandle();

        if (null == arena) {
            prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_LOBBY_PREFIX, player, null);
            suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_LOBBY_SUFFIX, player, null);

            PlayerTab tab = handle.playerTabCreate(
                    playerTabId, player, prefix, suffix, PlayerTab.PushingRule.NEVER,
                    this.sidebar.getPlaceholders(player)
            );
            deployedPerPlayerTabList.put(player.getUniqueId(), tab);
            return;
        }

        // in-game tab has a special treatment
        if (arena.isSpectator(player) || (spectator != null && spectator)) {
            handle.clearPlayerHealth(player);

            // if has been eliminated from a team
            ITeam exTeam = arena.getExTeam(player.getUniqueId());

            // when player leaves but decides to join to spectate later
            if (null != exTeam) {

                HashMap<String, String> replacements = getTeamReplacements(exTeam);

                if (arena.getStatus() == GameState.restarting && null != arena.getWinner()) {
                    if (arena.getWinner().equals(exTeam)) {
                        prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_PREFIX, player, replacements);
                        suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_RESTARTING_WIN2_SUFFIX, player, replacements);
                    } else {
                        prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_RESTARTING_ELM_PREFIX, player, replacements);
                        suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_RESTARTING_ELM_SUFFIX, player, replacements);
                    }
                } else {
                    prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_PLAYING_ELM_PREFIX, player, replacements);
                    suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_PLAYING_ELM_SUFFIX, player, replacements);
                }

                PlayerTab tab = handle.playerTabCreate(
                        playerTabId,
                        player, prefix, suffix, PlayerTab.PushingRule.NEVER,
                        this.sidebar.getPlaceholders(player), getPlayerListColor(exTeam),
                        PlayerTab.NameTagVisibility.ALWAYS, PlayerTab.PlayerListMode.SPECTATOR
                );
                deployedPerPlayerTabList.put(player.getUniqueId(), tab);
                return;
            }

            switch (arena.getStatus()) {
                case waiting:
                    prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_WAITING_PREFIX_SPEC, player, null);
                    suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_WAITING_SUFFIX_SPEC, player, null);
                    break;
                case starting:
                    prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_STARTING_PREFIX_SPEC, player, null);
                    suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_STARTING_SUFFIX_SPEC, player, null);
                    break;
                case playing:
                    prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_PLAYING_SPEC_PREFIX, player, null);
                    suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_PLAYING_SPEC_SUFFIX, player, null);
                    break;
                case restarting:
                    prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_RESTARTING_SPEC_PREFIX, player, null);
                    suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_RESTARTING_SPEC_SUFFIX, player, null);
                    break;
                default:
                    throw new RuntimeException("Unhandled game state..");
            }

            PlayerTab tab = handle.playerTabCreate(
                    playerTabId,
                    player, prefix, suffix, PlayerTab.PushingRule.NEVER,
                    this.sidebar.getPlaceholders(player), ChatColor.WHITE,
                    PlayerTab.NameTagVisibility.ALWAYS, PlayerTab.PlayerListMode.SPECTATOR
            );
            deployedPerPlayerTabList.put(player.getUniqueId(), tab);
            return;
        }

        // this is reached only by alive players
        GameState status = arena.getStatus();
        if (status != GameState.playing) {

            ITeam team = resolvePlayerListTeam(arena, player);

            switch (status) {
                case waiting:
                    prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_WAITING_PREFIX, player, null);
                    suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_WAITING_SUFFIX, player, null);
                    break;
                case starting:
                    prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_STARTING_PREFIX, player, null);
                    suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_STARTING_SUFFIX, player, null);
                    break;
                case restarting:
                    HashMap<String, String> replacements = getTeamReplacements(team);

                    prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_PREFIX, player, replacements);
                    suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_RESTARTING_WIN1_SUFFIX, player, replacements);
                    break;
                default:
                    throw new IllegalStateException("Unhandled game status!");
            }
            PlayerTab t = handle.playerTabCreate(
                    playerTabId, player, prefix, suffix, PlayerTab.PushingRule.NEVER,
                    this.sidebar.getPlaceholders(player), getPlayerListColor(team)
            );
            deployedPerPlayerTabList.put(player.getUniqueId(), t);
            return;
        }

        // if status is playing and player is alive

        ITeam team = resolvePlayerListTeam(arena, player);
        // tab list of playing state
        HashMap<String, String> replacements = getTeamReplacements(team);

        prefix = getPlayerRowText(Messages.FORMATTING_SB_TAB_PLAYING_PREFIX, player, replacements);
        suffix = getPlayerRowText(Messages.FORMATTING_SB_TAB_PLAYING_SUFFIX, player, replacements);

        PlayerTab teamTab = handle.playerTabCreate(
                playerTabId,
                player, prefix, suffix, PlayerTab.PushingRule.PUSH_OTHER_TEAMS,
                this.sidebar.getPlaceholders(player), getPlayerListColor(team),
                player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                        ? PlayerTab.NameTagVisibility.NEVER
                        : PlayerTab.NameTagVisibility.ALWAYS
        );
        deployedPerPlayerTabList.put(player.getUniqueId(), teamTab);
    }

    /** Recreate an existing row so Sidebar sends a forced one-entry update. */
    void replayPlayerListEntry(@NotNull Player player) {
        if (!deployedPerPlayerTabList.containsKey(player.getUniqueId())) return;
        giveUpdateTabFormat(player, false, null);
    }

    /** Remove one target from this viewer's deployed TAB state. */
    void removePlayerListEntry(@NotNull UUID playerId) {
        removeDeployedTab(playerId);
    }

    private void giveUpdateTeamColor(@NotNull Player player, @Nullable Boolean spectatorOverride) {
        IArena arena = sidebar.getArena();
        Sidebar handle = sidebar.getHandle();
        if (arena == null || handle == null) {
            removeDeployedTab(player.getUniqueId());
            return;
        }

        ITeam team = resolvePlayerListTeam(arena, player);
        boolean spectator = arena.isSpectator(player) || Boolean.TRUE.equals(spectatorOverride);
        PlayerTab.PlayerListMode playerListMode = resolveMinimalPlayerListMode(team, spectator);
        if (playerListMode == null) {
            removeDeployedTab(player.getUniqueId());
            return;
        }
        boolean spectatorRow = playerListMode == PlayerTab.PlayerListMode.SPECTATOR;

        PlayerTab tab = handle.playerTabCreate(
                player.getUniqueId().toString(), player, new SidebarLine(), new SidebarLine(),
                spectatorRow ? PlayerTab.PushingRule.NEVER : PlayerTab.PushingRule.PUSH_OTHER_TEAMS,
                sidebar.getPlaceholders(player),
                getPlayerListColor(team), player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                        ? PlayerTab.NameTagVisibility.NEVER
                        : PlayerTab.NameTagVisibility.ALWAYS,
                playerListMode
        );
        deployedPerPlayerTabList.put(player.getUniqueId(), tab);
    }

    static @Nullable PlayerTab.PlayerListMode resolveMinimalPlayerListMode(
            @Nullable ITeam team, boolean spectator) {
        if (spectator) return PlayerTab.PlayerListMode.SPECTATOR;
        return team == null ? null : PlayerTab.PlayerListMode.ACTUAL;
    }

    static @Nullable ITeam resolvePlayerListTeam(@NotNull IArena arena, @NotNull Player player) {
        GameState state = arena.getStatus();
        ITeam selectedTeam = state == GameState.waiting || state == GameState.starting
                ? PreGameTeamSelectionManager.getInstance().getSelection(arena, player)
                : null;
        return resolvePlayerListTeam(arena, player, selectedTeam);
    }

    static @Nullable ITeam resolvePlayerListTeam(@NotNull IArena arena, @NotNull Player player,
                                                  @Nullable ITeam selectedTeam) {
        ITeam currentTeam = arena.getTeam(player);
        if (currentTeam != null) return currentTeam;
        GameState state = arena.getStatus();
        if ((state == GameState.waiting || state == GameState.starting)
                && selectedTeam != null) return selectedTeam;
        return arena.getExTeam(player.getUniqueId());
    }

    private void synchronizeTabs(@NotNull Map<UUID, Player> desiredPlayers, boolean fullFormatting) {
        Set<UUID> onlinePlayers = new HashSet<>();
        desiredPlayers.forEach((uuid, player) -> {
            if (player.isOnline()) onlinePlayers.add(uuid);
        });
        List<UUID> stale = deployedPerPlayerTabList.keySet().stream()
                .filter(uuid -> !onlinePlayers.contains(uuid))
                .toList();
        stale.forEach(this::removeDeployedTab);
        desiredPlayers.values().stream()
                .filter(Player::isOnline)
                .forEach(player -> {
                    if (fullFormatting) {
                        giveUpdateTabFormat(player, true, null);
                    } else {
                        giveUpdateTeamColor(player, null);
                    }
                });
    }

    private void clearDeployedTabs() {
        new ArrayList<>(deployedPerPlayerTabList.keySet()).forEach(this::removeDeployedTab);
    }

    private void removeDeployedTab(@NotNull UUID playerId) {
        PlayerTab playerTab = deployedPerPlayerTabList.remove(playerId);
        if (playerTab != null && sidebar.getHandle() != null) {
            sidebar.getHandle().removeTab(playerTab.getIdentifier());
        }
    }

    @NotNull
    private SidebarLine getPlayerRowText(String path, Player targetPlayer,
                                         @Nullable HashMap<String, String> replacements) {
        List<String> strings = Language.getList(sidebar.getPlayer(), path);
        if (strings.isEmpty()) {
            return new SidebarLine() {
                @NotNull
                @Override
                public String getLine() {
                    return "";
                }
            };
        }

        strings = new ArrayList<>();
        for (String string : Language.getList(sidebar.getPlayer(), path)) {
            String parsed = removePlayerRowTeamMarkers(string)
                    .replace("{vPrefix}", BedWars.getChatSupport().getPrefix(targetPlayer))
                    .replace("{vSuffix}", BedWars.getChatSupport().getSuffix(targetPlayer));

            if (null != replacements) {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    parsed = parsed.replace(entry.getKey(), entry.getValue());
                }
            }

            strings.add(parsed);
        }

        if (strings.size() == 1) {
            final String line = strings.get(0);
            return new SidebarLine() {
                @NotNull
                @Override
                public String getLine() {
                    return line;
                }
            };
        }

        final String[] lines = new String[strings.size()];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = strings.get(i);
        }
        return new SidebarLineAnimated(lines);
    }

    static @NotNull String removePlayerRowTeamMarkers(@NotNull String template) {
        return template.replace("{teamName}", "").replace("{teamLetter}", "");
    }

    @NotNull HashMap<String, String> getTeamReplacements(@Nullable ITeam team) {
        HashMap<String, String> replacements = new HashMap<>();
        String displayName = null == team ? "" : team.getDisplayName(Language.getPlayerLanguage(sidebar.getPlayer()));
        replacements.put("{teamName}", displayName);
        replacements.put("{teamLetter}", null == team || displayName.isEmpty() ? "" : team.getColor().chat() + (displayName.substring(0, 1)));
        replacements.put("{teamColor}", null == team ? "" : team.getColor().chat().toString());

        return replacements;
    }

    static ChatColor getPlayerListColor(@Nullable ITeam targetTeam) {
        if (targetTeam == null) return ChatColor.WHITE;
        return targetTeam.getColor().chat();
    }

    /**
     * Returns the arena roster grouped from red to violet. Active and eliminated
     * members of the same team stay together and are ordered by player name;
     * unassigned spectators are placed last.
     */
    static List<Player> orderedArenaPlayers(@NotNull IArena arena) {
        List<ITeam> teams = new ArrayList<>(arena.getTeams());
        teams.sort(TabTeamOrder.COMPARATOR);
        LinkedHashMap<UUID, List<Player>> teamMembers = new LinkedHashMap<>();
        for (ITeam team : teams) {
            teamMembers.put(team.getIdentity(), new ArrayList<>());
        }

        Set<UUID> seenPlayers = new HashSet<>();
        List<Player> unassigned = new ArrayList<>();
        for (Player player : arena.getPlayers()) {
            if (!seenPlayers.add(player.getUniqueId())) continue;
            ITeam team = resolvePlayerListTeam(arena, player);
            List<Player> members = team == null ? null : teamMembers.get(team.getIdentity());
            if (members == null) {
                unassigned.add(player);
            } else {
                members.add(player);
            }
        }
        for (Player player : arena.getSpectators()) {
            if (!seenPlayers.add(player.getUniqueId())) continue;
            ITeam formerTeam = resolvePlayerListTeam(arena, player);
            List<Player> members = formerTeam == null ? null : teamMembers.get(formerTeam.getIdentity());
            if (members == null) {
                unassigned.add(player);
            } else {
                members.add(player);
            }
        }

        List<Player> ordered = new ArrayList<>(seenPlayers.size());
        appendSortedGroups(ordered, teamMembers.values());
        unassigned.sort(PLAYER_NAME_ORDER);
        ordered.addAll(unassigned);
        return ordered;
    }

    private static void appendSortedGroups(List<Player> destination, Collection<List<Player>> groups) {
        for (List<Player> group : groups) {
            group.sort(PLAYER_NAME_ORDER);
            destination.addAll(group);
        }
    }

    /**
     * Coalesce the many sidebar updates produced by a join/team/death event into
     * one player-list refresh on the next tick.
     */
    private static void requestPlayerListOrderUpdate() {
        if (playerListOrderUpdateScheduled) return;
        if (plugin == null || !plugin.isEnabled()) {
            applyPlayerListOrder(List.of());
            return;
        }
        playerListOrderUpdateScheduled = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            playerListOrderUpdateScheduled = false;
            applyServerPlayerListOrder();
        });
    }

    private static void applyServerPlayerListOrder() {
        LinkedHashMap<UUID, Player> arenaPlayers = new LinkedHashMap<>();
        for (Player player : orderedActiveArenaPlayers(Arena.getArenas())) {
            arenaPlayers.put(player.getUniqueId(), player);
        }

        List<Player> lobbyPlayers = new ArrayList<>();
        if (config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (Arena.getArenaByPlayer(player) != null) continue;
                if (getServerType() == ServerType.SHARED && !isInConfiguredLobbyWorld(player)) continue;
                lobbyPlayers.add(player);
            }
        }
        lobbyPlayers.sort(PLAYER_NAME_ORDER);

        List<Player> orderedPlayers = new ArrayList<>(lobbyPlayers.size() + arenaPlayers.size());
        orderedPlayers.addAll(lobbyPlayers);
        orderedPlayers.addAll(arenaPlayers.values());
        applyPlayerListOrder(orderedPlayers);
    }

    /**
     * Builds the global arena section without consulting cosmetic TAB switches.
     * Every arena keeps a minimal scoreboard Team context, so team grouping and
     * spectrum ordering must remain active even when full prefixes are disabled.
     */
    static @NotNull List<Player> orderedActiveArenaPlayers(
            @NotNull Collection<? extends IArena> activeArenas) {
        List<IArena> arenas = new ArrayList<>(activeArenas);
        arenas.sort(Comparator.comparing(IArena::getArenaName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(IArena::getArenaName));

        LinkedHashMap<UUID, Player> players = new LinkedHashMap<>();
        for (IArena arena : arenas) {
            for (Player player : orderedArenaPlayers(arena)) {
                if (player.isOnline()) players.putIfAbsent(player.getUniqueId(), player);
            }
        }
        return new ArrayList<>(players.values());
    }

    static void applyPlayerListOrder(@NotNull Collection<Player> orderedPlayers) {
        LinkedHashMap<UUID, Player> desiredPlayers = new LinkedHashMap<>();
        orderedPlayers.forEach(player -> desiredPlayers.putIfAbsent(player.getUniqueId(), player));

        Iterator<Map.Entry<UUID, PlayerListOrderState>> staleStates = managedPlayerListOrders.entrySet().iterator();
        while (staleStates.hasNext()) {
            Map.Entry<UUID, PlayerListOrderState> entry = staleStates.next();
            if (desiredPlayers.containsKey(entry.getKey())) continue;
            entry.getValue().restoreIfUnchanged();
            staleStates.remove();
        }

        // The 1.21 client sorts this field descending, so the first desired
        // player must receive the largest value.
        int order = desiredPlayers.size();
        for (Player player : desiredPlayers.values()) {
            PlayerListOrderState state = managedPlayerListOrders.get(player.getUniqueId());
            if (state == null || state.player != player) {
                state = new PlayerListOrderState(player);
                managedPlayerListOrders.put(player.getUniqueId(), state);
            }
            state.apply(order--);
        }
    }

    static void restorePlayerListOrder() {
        playerListOrderUpdateScheduled = false;
        applyPlayerListOrder(List.of());
    }

    private static boolean isInConfiguredLobbyWorld(@NotNull Player player) {
        String lobbyWorldName = config.getLobbyWorldName().trim();
        return !lobbyWorldName.isEmpty() && player.getWorld().getName().equals(lobbyWorldName);
    }

    private static final class PlayerListOrderState {
        private final Player player;
        private final int originalOrder;
        private int appliedOrder;

        private PlayerListOrderState(Player player) {
            this.player = player;
            this.originalOrder = player.getPlayerListOrder();
            this.appliedOrder = originalOrder;
        }

        private void apply(int order) {
            if (player.getPlayerListOrder() != order) player.setPlayerListOrder(order);
            appliedOrder = order;
        }

        private void restoreIfUnchanged() {
            if (player.isOnline() && player.getPlayerListOrder() == appliedOrder
                    && appliedOrder != originalOrder) {
                player.setPlayerListOrder(originalOrder);
            }
        }
    }

    /**
     * Clear tab lines from instance.
     */
    public void onSidebarRemoval() {
        requestPlayerListOrderUpdate();
        sidebar.getHandle().clearLines();
        deployedPerPlayerTabList.clear();
        sidebar.getHandle().removeTabs();
    }
}
