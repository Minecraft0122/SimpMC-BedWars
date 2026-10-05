package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.sidebar.BwTabList;
import com.andrei1058.bedwars.sidebar.SidebarService;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public final class CmdTab extends SubCommand {
    public CmdTab(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(17);
        setDisplayInfo(MainCommand.createTC("§6 ▪ §7/bw tab zh", "/bw tab zh", "§f将 TAB 中的队伍名称切换为中文。"));
    }

    @Override public boolean execute(String[] args, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender || !(sender instanceof Player player)) return false;
        if (args.length != 1 || !args[0].equalsIgnoreCase("zh")) {
            AdventureText.send(player, "§c用法：/bw tab zh");
            return true;
        }
        BwTabList.setChineseTeamNames(player.getUniqueId(), true);
        SidebarService service = SidebarService.getInstance();
        if (service != null) service.refreshTabList();
        AdventureText.send(player, "§a已将你看到的 TAB 队伍名称切换为中文。");
        return true;
    }

    @Override public boolean canSee(CommandSender sender, BedWars api) { return sender instanceof Player && hasPermission(sender); }

    @Override public List<String> getTabComplete() { return List.of("zh"); }
}
