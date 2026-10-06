package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.command.CommandSender;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 不依赖悬浮提示的完整帮助，控制台与玩家均可读取用法和说明。 */
public final class CmdHelp extends SubCommand {
    public CmdHelp(ParentCommand parent, String name) {
        super(parent, name);
        showInList(true);
        setPriority(0);
        setDisplayInfo(MainCommand.createTC("§6 ▪ §7/" + parent.getName() + " help",
                "/" + parent.getName() + " help", "§f列出所有有权限的命令、说明和用法。"));
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        AdventureText.send(sender, "§6SimpMC-BedWars 命令帮助 §7<> 必填；[] 可选；标记“游戏内”的命令需要玩家执行。");
        getParent().getSubCommands().stream().filter(command -> command.hasPermission(sender))
                .sorted(Comparator.comparingInt(SubCommand::getPriority))
                .forEach(command -> {
                    HelpEntry entry = describe(command.getSubCommandName());
                    String context = command.isArenaSetupCommand() ? " §8（游戏内·竞技场设置会话）"
                            : entry.playerOnly() ? " §8（游戏内）" : "";
                    AdventureText.send(sender, "§e/" + getParent().getName() + " " + command.getSubCommandName()
                            + (entry.arguments().isEmpty() ? "" : " " + entry.arguments()) + " §7— " + entry.description() + context);
                });
        AdventureText.send(sender, "§e/party [help|invite <玩家>|accept <玩家>|leave|disband|remove <玩家>|promote <玩家>|list] §7— 管理固定小队（游戏内，需启用内置 party 命令）。");
        AdventureText.send(sender, "§e/leave §7— /bw leave 的快捷入口（游戏内）。");
        if (Permissions.hasShoutPermission(sender)) AdventureText.send(sender,
                "§e/shout <消息> §7— 对局全体喊话；别名 /hh、/h，也可使用 !消息（游戏内）。");
        if (Permissions.hasCommandPermission(sender, "rejoin", Permissions.PERMISSION_REJOIN)) AdventureText.send(sender,
                "§e/rejoin §7— 在允许的时间窗口内重新加入掉线对局（游戏内）。");
        return true;
    }

    static HelpEntry describe(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "help" -> entry("", "列出所有有权限的命令及完整用法。", false);
            case "cmds" -> entry("", "查看玩家命令；设置会话内查看地图设置进度。", true);
            case "join" -> entry("<竞技场|分组|random>", "加入指定或随机竞技场。", true);
            case "leave" -> entry("", "离开对局；大厅内返回配置的代理大厅。", true);
            case "message", "lang" -> entry("[语言代码]", "列出语言或切换语言，例如 zh_cn。", true);
            case "teleporter" -> entry("", "打开旁观传送菜单。", true);
            case "gui" -> entry("[分组+分组]", "打开竞技场选择菜单。", true);
            case "stats" -> entry("", "打开旧版累计战绩菜单。", true);
            case "match" -> entry("[对局编号|UUID]", "查询当前或指定对局详情。控制台必须指定编号。", false);
            case "history" -> entry("[玩家名|UUID|all] [页码]", "查询玩家（包括离线）历史；all 分页列出全部对局。", false);
            case "record" -> entry("[玩家名|UUID]", "查询玩家（包括离线）的已完成对局累计战绩。", false);
            case "team" -> entry("[invite <玩家>|accept <玩家>|decline <玩家>|leave|squad|help]", "管理开局前同队邀请；无参数打开组队菜单。", true);
            case "invite" -> entry("[<玩家>|accept <邀请者>|decline <邀请者>]", "邀请其他玩家加入当前等待竞技场。", true);
            case "start", "forcestart" -> entry("[debug]", "启动当前竞技场倒计时；debug 仅 OP，可单队测试。", true);
            case "setlobby" -> entry("", "将当前位置设为本服大厅。", true);
            case "setuparena" -> entry("<世界名>", "开始创建或编辑竞技场。", true);
            case "arenalist" -> entry("[页码]", "列出已加载竞技场和状态。", true);
            case "delarena" -> entry("<世界名>", "删除竞技场及其配置。", true);
            case "enablearena" -> entry("<世界名>", "启用竞技场。", true);
            case "disablearena" -> entry("<世界名>", "停用竞技场并移出玩家。", true);
            case "clonearena" -> entry("<源世界> <新竞技场名>", "克隆竞技场。", true);
            case "arenagroup" -> entry("<list|create <分组>|show <竞技场>|set <竞技场> <分组>|remove <分组>>", "查看或管理竞技场分组。", true);
            case "build" -> entry("", "切换大厅建造权限。", true);
            case "level" -> entry("<setLevel|giveXp> <玩家> <数量>", "设置玩家等级或增加经验。", false);
            case "reload" -> entry("", "重载语言文件。", false);
            case "npc" -> entry("<add <皮肤> <分组> <显示名>|remove>", "创建或删除加入游戏 NPC（需要 Citizens）。", true);
            case "tp" -> entry("<玩家>", "进入目标玩家的对局旁观。", true);
            case "upgradesmenu" -> entry("", "打开本队升级菜单。", true);
            case "autocreateteams" -> entry("", "按地图岛屿颜色自动检测并创建队伍。", true);
            case "setwaitingspawn" -> entry("", "把当前位置设为等待大厅出生点。", true);
            case "setspectspawn" -> entry("", "把当前位置设为旁观出生点。", true);
            case "createteam" -> entry("<队伍名> <颜色>", "创建地图队伍。", true);
            case "listteams" -> entry("", "列出地图已配置的队伍。", true);
            case "waitingpos" -> entry("<1|2>", "设置开局时移除的等待区域两个角点。", true);
            case "removeteam" -> entry("<队伍名>", "删除地图队伍。", true);
            case "setmaxinteam" -> entry("<人数>", "设置每支队伍容量。", true);
            case "setminplayers", "setmininteam" -> entry("<人数>", "设置全场最低开局人数；setMinInTeam 为兼容别名。", true);
            case "setmaxbuildheight" -> entry("<高度>", "设置地图最高建造高度。", true);
            case "setspawn" -> entry("<队伍名>", "设置队伍出生点。", true);
            case "setbed" -> entry("", "设置正看着的队伍床。", true);
            case "setshop" -> entry("[队伍名]", "设置队伍商店 NPC 位置。", true);
            case "setupgrade" -> entry("[队伍名]", "设置队伍升级 NPC 位置。", true);
            case "addgenerator" -> entry("[Iron|Gold|Emerald|Diamond|upgrade] [队伍名]", "添加资源点；辅助模式可自动识别。", true);
            case "removegenerator" -> entry("", "删除附近资源生成点。", true);
            case "settype" -> entry("[分组]", "设置地图分组。", true);
            case "save" -> entry("", "校验并保存竞技场，退出设置会话。", true);
            case "setkilldrops" -> entry("<队伍名>", "设置击杀后物品掉落位置。", true);
            default -> entry("", "由附属插件提供，请查看该插件的命令说明。", false);
        };
    }

    private static HelpEntry entry(String arguments, String description, boolean playerOnly) {
        return new HelpEntry(arguments, description, playerOnly);
    }

    record HelpEntry(String arguments, String description, boolean playerOnly) { }

    @Override public List<String> getTabComplete() { return List.of(); }
    @Override public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) { return hasPermission(sender); }
}
