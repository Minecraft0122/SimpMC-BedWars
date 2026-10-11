package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.stats.*;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.language.MatchQueryMessages;
import com.andrei1058.bedwars.language.MatchQueryMessages.Key;
import com.andrei1058.bedwars.language.MatchQueryMessages.Snapshot;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** 对局、历史和正式累计共用的异步查询入口，保留旧 /bw stats 菜单。 */
public final class CmdMatchQuery extends SubCommand {
    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));
    private static final Set<CommandSender> PENDING = ConcurrentHashMap.newKeySet();

    public CmdMatchQuery(ParentCommand parent, String name) {
        super(parent, name);
        showInList(true);
        String description = switch (name) {
            case "match" -> "查看当前对局，或按编号、UUID 查询对局。";
            case "history" -> "分页查看玩家的对局历史。";
            default -> "查看玩家所有已完成对局的累计战绩。";
        };
        setDisplayInfo(MainCommand.createTC("§6 ▪ §7/" + parent.getName() + " " + name,
                "/" + parent.getName() + " " + name, "§f" + description));
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        BedWars plugin = BedWars.plugin;
        Snapshot messages = MatchQueryMessages.forSender(sender);
        MatchHistory history = plugin.getMatchHistory();
        if (history == null || !history.isEnabled()) {
            AdventureText.send(sender, messages.text(Key.DISABLED));
            return true;
        }
        if (!PENDING.add(sender)) {
            AdventureText.send(sender, messages.text(Key.PENDING));
            return true;
        }
        try {
            CompletableFuture<List<String>> response = switch (getSubCommandName()) {
                case "match" -> match(args, sender, history, messages);
                case "history" -> history(args, sender, history, messages);
                default -> totals(args, sender, history, messages);
            };
            response.whenComplete((lines, error) -> {
                // 数据库任务已经完成，即使停服取消了回复任务也不能遗留请求锁。
                PENDING.remove(sender);
                if (!plugin.isEnabled()) return;
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                    if (sender instanceof Player player && !player.isOnline()) return;
                    if (error != null) {
                        Throwable cause = error;
                        while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) cause = cause.getCause();
                        if (cause instanceof IllegalArgumentException) {
                            AdventureText.send(sender, cause.getMessage());
                        } else {
                            plugin.getLogger().log(Level.WARNING, "查询对局战绩失败", error);
                            AdventureText.send(sender, messages.text(Key.FAILED));
                        }
                    } else {
                        lines.forEach(line -> AdventureText.send(sender, line));
                    }
                    });
                } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) {
                    // 停服可能发生在 isEnabled 检查与提交回复之间。
                }
            });
        } catch (IllegalArgumentException exception) {
            PENDING.remove(sender);
            AdventureText.send(sender, exception.getMessage());
        } catch (RuntimeException exception) {
            PENDING.remove(sender);
            throw exception;
        }
        return true;
    }

    private CompletableFuture<List<String>> match(String[] args, CommandSender sender,
                                                   MatchHistory history, Snapshot messages) {
        if (args.length > 1) throw new IllegalArgumentException(messages.text(Key.MATCH_USAGE));
        CompletableFuture<Optional<MatchInfo>> query;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) throw new IllegalArgumentException(messages.text(Key.CONSOLE_MATCH_ID));
            Optional<MatchInfo> current = history.getCurrentMatch(Arena.getArenaByPlayer(player));
            if (current.isEmpty()) return CompletableFuture.completedFuture(List.of(messages.text(Key.NO_CURRENT_MATCH)));
            query = CompletableFuture.completedFuture(current);
        } else {
            query = findMatch(history, args[0], messages);
        }
        return query.thenCompose(found -> {
            if (found.isEmpty()) return CompletableFuture.completedFuture(List.of(messages.text(Key.MATCH_NOT_FOUND)));
            MatchInfo info = found.get();
            if (info.matchNumber() == 0) {
                List<String> lines = matchHeader(info, messages);
                lines.add(messages.text(Key.NUMBER_PENDING));
                return CompletableFuture.completedFuture(List.copyOf(lines));
            }
            return history.getMatchPlayers(info.matchUuid()).thenApply(players -> {
                List<String> lines = matchHeader(info, messages);
                if ("RUNNING".equals(info.status())) lines.add(messages.text(Key.RUNNING_SNAPSHOT));
                for (MatchPlayerResult player : players) {
                    String name = player.playerName() == null ? player.playerUuid().toString() : player.playerName();
                    lines.add(messages.text(Key.PLAYER_ROW, "{player}", name,
                            "{outcome}", outcome(player.outcome(), messages),
                            "{counters}", counters(player.kills(), player.finalKills(), player.deaths(),
                                    player.bedsDestroyed(), player.kdRatio(), messages)));
                }
                if (players.isEmpty()) lines.add(messages.text(Key.NO_PLAYERS));
                return List.copyOf(lines);
            });
        });
    }

    static CompletableFuture<Optional<MatchInfo>> findMatch(MatchHistory history, String identifier) {
        return findMatch(history, identifier, MatchQueryMessages.defaults(true));
    }

    private static CompletableFuture<Optional<MatchInfo>> findMatch(MatchHistory history,
                                                                     String identifier, Snapshot messages) {
        try {
            if (identifier.matches("[0-9]+")) {
                long number = Long.parseLong(identifier);
                if (number <= 0) throw new IllegalArgumentException();
                return history.findMatch(number);
            }
            return history.findMatch(parseUuid(identifier));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(messages.text(Key.INVALID_IDENTIFIER));
        }
    }

    private CompletableFuture<List<String>> totals(String[] args, CommandSender sender,
                                                    MatchHistory history, Snapshot messages) {
        if (args.length > 1) throw new IllegalArgumentException(messages.text(Key.RECORD_USAGE));
        return playerId(args.length == 0 ? null : args[0], sender, history, messages)
                .thenCompose(history::getPlayerTotals).thenApply(total -> List.of(
                messages.text(Key.TOTALS_HEADER, "{player}", total.playerUuid().toString()),
                messages.text(Key.TOTALS_MATCHES, "{count}", String.valueOf(total.matchesPlayed())),
                counters(total.kills(), total.finalKills(), total.deaths(), total.bedsDestroyed(), total.kdRatio(), messages),
                messages.text(Key.KD_EXPLANATION)));
    }

    CompletableFuture<List<String>> history(String[] args, CommandSender sender, MatchHistory history) {
        return history(args, sender, history, MatchQueryMessages.defaults(true));
    }

    CompletableFuture<List<String>> history(String[] args, CommandSender sender,
                                            MatchHistory history, Snapshot messages) {
        if (args.length > 2) throw new IllegalArgumentException(messages.text(Key.HISTORY_USAGE));
        String target = args.length == 0 ? null : args[0];
        String pageText = args.length == 2 ? args[1] : "1";
        int offset = pageOffset(pageText, messages);
        int page = offset / PAGE_SIZE + 1;
        if ("all".equalsIgnoreCase(target)) {
            return history.getMatches(PAGE_SIZE, offset).thenApply(matches ->
                    historyLines(matches, messages.text(Key.HISTORY_ALL), "all", page, true, messages));
        }
        return playerId(target, sender, history, messages).thenCompose(uuid ->
                history.getPlayerMatches(uuid, PAGE_SIZE, offset)
                        .thenApply(matches -> historyLines(matches, uuid.toString(), uuid.toString(), page, false, messages)));
    }

    private static List<String> historyLines(List<MatchInfo> matches, String displayTarget,
                                             String commandTarget, int page, boolean oldestAtTop, Snapshot messages) {
            List<String> lines = new ArrayList<>();
            lines.add(messages.text(Key.HISTORY_HEADER, "{target}", displayTarget, "{page}", String.valueOf(page)));
            List<MatchInfo> displayMatches = new ArrayList<>(matches);
            if (oldestAtTop) Collections.reverse(displayMatches);
            for (MatchInfo info : displayMatches) {
                lines.add(messages.text(Key.HISTORY_ROW, "{number}", String.valueOf(info.matchNumber()),
                        "{arena}", info.arenaName(), "{status}", status(info.status(), messages),
                        "{time}", TIME.format(info.startedAt())));
            }
            if (matches.isEmpty()) lines.add(messages.text(Key.HISTORY_EMPTY));
            lines.add(messages.text(Key.HISTORY_NEXT, "{target}", commandTarget,
                    "{page}", String.valueOf(page + 1)));
            return List.copyOf(lines);
    }

    static int pageOffset(String text) {
        return pageOffset(text, MatchQueryMessages.defaults(true));
    }

    private static int pageOffset(String text, Snapshot messages) {
        try {
            int page = Integer.parseInt(text);
            if (page < 1) throw new NumberFormatException();
            return Math.multiplyExact(page - 1, PAGE_SIZE);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new IllegalArgumentException(messages.text(Key.INVALID_PAGE));
        }
    }

    static CompletableFuture<UUID> playerId(String value, CommandSender sender, MatchHistory history) {
        return playerId(value, sender, history, MatchQueryMessages.defaults(true));
    }

    private static CompletableFuture<UUID> playerId(String value, CommandSender sender,
                                                     MatchHistory history, Snapshot messages) {
        if (value == null) {
            if (sender instanceof Player player) return CompletableFuture.completedFuture(player.getUniqueId());
            throw new IllegalArgumentException(messages.text(Key.CONSOLE_HISTORY_TARGET));
        }
        try {
            return CompletableFuture.completedFuture(parseUuid(value));
        } catch (IllegalArgumentException exception) {
            if (value.contains("-")) throw new IllegalArgumentException(messages.text(Key.INVALID_PLAYER_UUID));
        }
        Player player = Bukkit.getPlayerExact(value);
        if (player != null) return CompletableFuture.completedFuture(player.getUniqueId());
        return history.findPlayerUuid(value).thenApply(found -> found.orElseThrow(() ->
                new IllegalArgumentException(messages.text(Key.PLAYER_NOT_FOUND))));
    }

    private static UUID parseUuid(String value) {
        UUID uuid = UUID.fromString(value);
        if (!uuid.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
        return uuid;
    }

    private static List<String> matchHeader(MatchInfo info, Snapshot messages) {
        List<String> lines = new ArrayList<>();
        lines.add(messages.text(Key.MATCH_HEADER, "{number}", info.matchNumber() == 0
                ? messages.text(Key.MATCH_NUMBER_PENDING) : String.valueOf(info.matchNumber()),
                "{status}", status(info.status(), messages)));
        lines.add(messages.text(Key.MATCH_UUID, "{uuid}", info.matchUuid().toString()));
        lines.add(messages.text(Key.MATCH_MAP_GROUP, "{map}", info.arenaName(), "{group}", info.arenaGroup()));
        lines.add(messages.text(Key.MATCH_TIMES, "{start}", TIME.format(info.startedAt()),
                "{end}", info.endedAt() == null ? messages.text(Key.MATCH_NOT_ENDED) : TIME.format(info.endedAt())));
        return lines;
    }

    private static String counters(long kills, long finalKills, long deaths, long beds, double kd, Snapshot messages) {
        return messages.text(Key.COUNTERS, "{kills}", String.valueOf(kills),
                "{finalKills}", String.valueOf(finalKills), "{beds}", String.valueOf(beds),
                "{deaths}", String.valueOf(deaths), "{kd}", String.format(Locale.ROOT, "%.4f", kd));
    }

    private static String status(String status, Snapshot messages) {
        return switch (status) {
            case "FINISHED" -> messages.text(Key.STATUS_FINISHED);
            case "ABORTED" -> messages.text(Key.STATUS_ABORTED);
            default -> messages.text(Key.STATUS_RUNNING);
        };
    }

    private static String outcome(String value, Snapshot messages) {
        if (value == null) return messages.text(Key.OUTCOME_UNSETTLED);
        return switch (value) {
            case "WIN" -> messages.text(Key.OUTCOME_WIN);
            case "LOSS" -> messages.text(Key.OUTCOME_LOSS);
            case "ABANDONED" -> messages.text(Key.OUTCOME_ABANDONED);
            case "DISCONNECTED" -> messages.text(Key.OUTCOME_DISCONNECTED);
            default -> messages.text(Key.OUTCOME_UNSETTLED);
        };
    }

    @Override
    public List<String> getTabComplete() {
        return "history".equals(getSubCommandName()) ? List.of("all") : List.of();
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (args.length > 1) return "history".equals(getSubCommandName()) && args.length == 2 ? List.of("1", "2", "3") : List.of();
        List<String> options = new ArrayList<>(getTabComplete());
        if (!"match".equals(getSubCommandName())) Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    @Override
    public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) {
        return hasPermission(sender);
    }
}
