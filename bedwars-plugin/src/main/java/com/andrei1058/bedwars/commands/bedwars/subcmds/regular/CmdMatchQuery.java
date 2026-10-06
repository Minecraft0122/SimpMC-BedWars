package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.stats.*;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
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
        MatchHistory history = plugin.getMatchHistory();
        if (history == null || !history.isEnabled()) {
            AdventureText.send(sender, "§c对局统计未启用，请联系管理员检查统计设置与数据库连接。");
            return true;
        }
        if (!PENDING.add(sender)) {
            AdventureText.send(sender, "§e上一条战绩查询仍在处理中，请稍候。");
            return true;
        }
        try {
            CompletableFuture<List<String>> response = switch (getSubCommandName()) {
                case "match" -> match(args, sender, history);
                case "history" -> history(args, sender, history);
                default -> totals(args, sender, history);
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
                            AdventureText.send(sender, "§e" + cause.getMessage());
                        } else {
                            plugin.getLogger().log(Level.WARNING, "查询对局战绩失败", error);
                            AdventureText.send(sender, "§c暂时无法读取战绩，请稍后重试；详细原因已写入服务器日志。");
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
            AdventureText.send(sender, "§e" + exception.getMessage());
        } catch (RuntimeException exception) {
            PENDING.remove(sender);
            throw exception;
        }
        return true;
    }

    private CompletableFuture<List<String>> match(String[] args, CommandSender sender, MatchHistory history) {
        if (args.length > 1) throw new IllegalArgumentException("用法：/bw match [对局编号或 UUID]");
        CompletableFuture<Optional<MatchInfo>> query;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) throw new IllegalArgumentException("控制台请指定对局编号或 UUID。");
            Optional<MatchInfo> current = history.getCurrentMatch(Arena.getArenaByPlayer(player));
            if (current.isEmpty()) return CompletableFuture.completedFuture(List.of("§e你当前没有已开始的对局。"));
            query = CompletableFuture.completedFuture(current);
        } else {
            query = findMatch(history, args[0]);
        }
        return query.thenCompose(found -> {
            if (found.isEmpty()) return CompletableFuture.completedFuture(List.of("§e没有找到这场对局。"));
            MatchInfo info = found.get();
            if (info.matchNumber() == 0) {
                List<String> lines = matchHeader(info);
                lines.add("§e对局刚开始，编号和首次战绩正在保存，请稍后查询。");
                return CompletableFuture.completedFuture(List.copyOf(lines));
            }
            return history.getMatchPlayers(info.matchUuid()).thenApply(players -> {
                List<String> lines = matchHeader(info);
                if ("RUNNING".equals(info.status())) lines.add("§7以下为最近一次保存的战绩，结束后会保存最终结果。");
                for (MatchPlayerResult player : players) {
                    String name = player.playerName() == null ? player.playerUuid().toString() : player.playerName();
                    lines.add("§f" + name + " §7[" + outcome(player.outcome()) + "] "
                            + counters(player.kills(), player.finalKills(), player.deaths(), player.bedsDestroyed(), player.kdRatio()));
                }
                if (players.isEmpty()) lines.add("§7暂无已保存的参赛玩家战绩。");
                return List.copyOf(lines);
            });
        });
    }

    static CompletableFuture<Optional<MatchInfo>> findMatch(MatchHistory history, String identifier) {
        try {
            if (identifier.matches("[0-9]+")) {
                long number = Long.parseLong(identifier);
                if (number <= 0) throw new IllegalArgumentException();
                return history.findMatch(number);
            }
            return history.findMatch(parseUuid(identifier));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("请输入有效的正整数对局编号或完整 UUID。");
        }
    }

    private CompletableFuture<List<String>> totals(String[] args, CommandSender sender, MatchHistory history) {
        if (args.length > 1) throw new IllegalArgumentException("用法：/bw record [玩家名或 UUID]（支持离线玩家）");
        return playerId(args.length == 0 ? null : args[0], sender, history)
                .thenCompose(history::getPlayerTotals).thenApply(total -> List.of(
                "§6玩家累计战绩 §7" + total.playerUuid(),
                "§7已完成对局：§f" + total.matchesPlayed(),
                counters(total.kills(), total.finalKills(), total.deaths(), total.bedsDestroyed(), total.kdRatio()),
                "§7K/D = 普通击杀 ÷ 全部死亡；死亡为 0 时等于普通击杀。"));
    }

    CompletableFuture<List<String>> history(String[] args, CommandSender sender, MatchHistory history) {
        if (args.length > 2) throw new IllegalArgumentException("用法：/bw history [玩家名或 UUID] [页码]；/bw history all [页码]");
        String target = args.length == 0 ? null : args[0];
        String pageText = args.length == 2 ? args[1] : "1";
        int offset = pageOffset(pageText);
        int page = offset / PAGE_SIZE + 1;
        if ("all".equalsIgnoreCase(target)) {
            return history.getMatches(PAGE_SIZE, offset).thenApply(matches -> historyLines(matches, "全部记录", page));
        }
        return playerId(target, sender, history).thenCompose(uuid ->
                history.getPlayerMatches(uuid, PAGE_SIZE, offset)
                        .thenApply(matches -> historyLines(matches, uuid.toString(), page)));
    }

    private static List<String> historyLines(List<MatchInfo> matches, String target, int page) {
            List<String> lines = new ArrayList<>();
            lines.add("§6对局历史 §7" + target + " · 第 " + page + " 页");
            for (MatchInfo info : matches) {
                lines.add("§e#" + info.matchNumber() + " §f" + info.arenaName() + " §7"
                        + status(info.status()) + " · " + TIME.format(info.startedAt()));
            }
            if (matches.isEmpty()) lines.add("§7这一页没有对局记录。");
            lines.add("§7使用 /bw match <编号> 查看单局战绩；/bw history "
                    + ("全部记录".equals(target) ? "all" : target) + " " + (page + 1) + " 查看下一页。");
            return List.copyOf(lines);
    }

    static int pageOffset(String text) {
        try {
            int page = Integer.parseInt(text);
            if (page < 1) throw new NumberFormatException();
            return Math.multiplyExact(page - 1, PAGE_SIZE);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new IllegalArgumentException("页码必须是有效的正整数。");
        }
    }

    static CompletableFuture<UUID> playerId(String value, CommandSender sender, MatchHistory history) {
        if (value == null) {
            if (sender instanceof Player player) return CompletableFuture.completedFuture(player.getUniqueId());
            throw new IllegalArgumentException("控制台请指定玩家名或 UUID，或使用 /bw history all。");
        }
        try {
            return CompletableFuture.completedFuture(parseUuid(value));
        } catch (IllegalArgumentException exception) {
            if (value.contains("-")) throw new IllegalArgumentException("请输入完整的玩家 UUID。");
        }
        Player player = Bukkit.getPlayerExact(value);
        if (player != null) return CompletableFuture.completedFuture(player.getUniqueId());
        return history.findPlayerUuid(value).thenApply(found -> found.orElseThrow(() ->
                new IllegalArgumentException("历史中未找到该玩家名，请检查名字或使用完整玩家 UUID。")));
    }

    private static UUID parseUuid(String value) {
        UUID uuid = UUID.fromString(value);
        if (!uuid.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
        return uuid;
    }

    private static List<String> matchHeader(MatchInfo info) {
        List<String> lines = new ArrayList<>();
        lines.add("§6对局 #" + (info.matchNumber() == 0 ? "待分配" : info.matchNumber()) + " §7" + status(info.status()));
        lines.add("§7UUID：§f" + info.matchUuid());
        lines.add("§7地图：§f" + info.arenaName() + " §7模式：§f" + info.arenaGroup());
        lines.add("§7开始：§f" + TIME.format(info.startedAt()) + " §7结束：§f"
                + (info.endedAt() == null ? "尚未结束" : TIME.format(info.endedAt())) + " §7（北京时间）");
        return lines;
    }

    private static String counters(long kills, long finalKills, long deaths, long beds, double kd) {
        return "§7击杀 §f" + kills + " §7最终击杀 §f" + finalKills + " §7破坏床 §f" + beds
                + " §7死亡 §f" + deaths + " §7K/D §f" + String.format(Locale.ROOT, "%.4f", kd);
    }

    private static String status(String status) {
        return switch (status) {
            case "FINISHED" -> "已完成";
            case "ABORTED" -> "已中止";
            default -> "进行中";
        };
    }

    private static String outcome(String value) {
        if (value == null) return "未结算";
        return switch (value) {
            case "WIN" -> "胜利";
            case "LOSS" -> "失败";
            case "ABANDONED" -> "中途离开";
            case "DISCONNECTED" -> "掉线";
            default -> "未结算";
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
