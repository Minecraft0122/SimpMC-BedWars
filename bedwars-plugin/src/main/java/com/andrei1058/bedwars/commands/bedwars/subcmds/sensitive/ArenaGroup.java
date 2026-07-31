/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
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

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.ArenaGroupPolicy;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.ArenaConfig;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.maprestore.internal.WorldNameValidator;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.BedWars.plugin;

public class ArenaGroup extends SubCommand {

    public ArenaGroup(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(8);
        showInList(true);
        setPermission(Permissions.PERMISSION_ARENA_GROUP);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName()
                        + " §8- §e点击查看详情", "§f管理竞技场组。",
                "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player player = (Player) sender;
        if (!MainCommand.isLobbySet(player)) return true;
        if (args.length == 0) {
            sendArenaGroupCmdList(player);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> createGroup(player, args);
            case "remove" -> removeGroup(player, args);
            case "list" -> listGroups(player);
            case "set" -> setArenaGroup(player, args);
            case "show" -> showArenaGroup(player, args);
            default -> {
                sendArenaGroupCmdList(player);
                yield true;
            }
        };
    }

    private boolean createGroup(Player player, String[] args) {
        if (args.length < 2) {
            sendArenaGroupCmdList(player);
            return true;
        }
        String requested = args[1].trim();
        if (requested.isEmpty() || requested.contains("+")) {
            player.sendMessage("§c▪ §7竞技场组名称不能为空，也不能包含：" + ChatColor.RED + "+");
            return true;
        }
        if (findConfiguredGroup(requested) != null) {
            player.sendMessage("§c▪ §7该竞技场组已存在！");
            return true;
        }

        List<String> groups = configuredGroups();
        groups.add(requested);
        config.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS, groups);
        player.sendMessage("§6 ▪ §7竞技场组已创建：§f" + requested);
        return true;
    }

    private boolean removeGroup(Player player, String[] args) {
        if (args.length < 2) {
            sendArenaGroupCmdList(player);
            return true;
        }
        String group = findConfiguredGroup(args[1]);
        if (group == null || group.equalsIgnoreCase(ArenaGroupPolicy.DEFAULT_GROUP)) {
            player.sendMessage("§c▪ §7该竞技场组不存在或不能删除！");
            return true;
        }

        List<String> groups = configuredGroups();
        groups.removeIf(value -> value.equalsIgnoreCase(group));
        config.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS, groups);
        int changed = resetGroupForAllArenas(group);
        player.sendMessage("§6 ▪ §7竞技场组已删除：§f" + group + "§7；已将 " + changed + " 张地图重置到 Default。");
        return true;
    }

    private boolean listGroups(Player player) {
        player.sendMessage("§7可用的竞技场组：");
        player.sendMessage("§6 ▪ §f默认组（Default）");
        for (String group : configuredGroups()) {
            player.sendMessage("§6 ▪ §f" + group);
        }
        return true;
    }

    private boolean setArenaGroup(Player player, String[] args) {
        if (args.length < 3) {
            sendArenaGroupCmdList(player);
            return true;
        }
        ArenaConfig arenaConfig = arenaConfig(player, args[1]);
        if (arenaConfig == null) return true;

        String group = findConfiguredGroup(args[2]);
        if (group == null) {
            player.sendMessage("§c▪ §7不存在该竞技场组：" + args[2]);
            return true;
        }

        saveGroup(args[1], arenaConfig, group);
        player.sendMessage("§6 ▪ §7竞技场 §f" + args[1] + " §7的分组已设为：§f" + group);
        return true;
    }

    private boolean showArenaGroup(Player player, String[] args) {
        if (args.length < 2) {
            sendArenaGroupCmdList(player);
            return true;
        }
        ArenaConfig arenaConfig = arenaConfig(player, args[1]);
        if (arenaConfig == null) return true;
        player.sendMessage("§6 ▪ §7竞技场 §f" + args[1] + " §7的分组：§f"
                + ArenaGroupPolicy.read(arenaConfig.getYml()));
        return true;
    }

    private ArenaConfig arenaConfig(Player player, String arenaName) {
        if (!WorldNameValidator.isSafe(arenaName)) {
            player.sendMessage(ChatColor.RED + "竞技场世界名称不能包含路径分隔符、冒号或控制字符。");
            return null;
        }
        File file = new File(plugin.getDataFolder(), "Arenas/" + arenaName + ".yml");
        if (!file.isFile()) {
            player.sendMessage("§c▪ §7竞技场 " + arenaName + " 不存在！");
            return null;
        }
        return new ArenaConfig(BedWars.plugin, arenaName, new File(plugin.getDataFolder(), "Arenas").getPath());
    }

    private void saveGroup(String arenaName, ArenaConfig arenaConfig, String group) {
        String normalized = ArenaGroupPolicy.normalize(group);
        arenaConfig.getYml().set(ArenaGroupPolicy.GROUP_PATH, normalized);
        arenaConfig.getYml().set(ArenaGroupPolicy.LEGACY_GROUPS_PATH, null);
        arenaConfig.save();
        IArena liveArena = Arena.getArenaByName(arenaName);
        if (liveArena != null) liveArena.setGroup(normalized);
    }

    private int resetGroupForAllArenas(String removedGroup) {
        File directory = new File(plugin.getDataFolder(), "Arenas");
        File[] files = directory.listFiles(file -> file.isFile() && file.getName().endsWith(".yml"));
        if (files == null) return 0;

        int changed = 0;
        for (File file : files) {
            String arenaName = file.getName().substring(0, file.getName().length() - 4);
            if (!WorldNameValidator.isSafe(arenaName)) continue;
            try {
                ArenaConfig arenaConfig = new ArenaConfig(BedWars.plugin, arenaName, directory.getPath());
                String group = ArenaGroupPolicy.read(arenaConfig.getYml());
                if (!ArenaGroupPolicy.matches(group, removedGroup)) continue;
                saveGroup(arenaName, arenaConfig, ArenaGroupPolicy.DEFAULT_GROUP);
                changed++;
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING,
                        "清理竞技场 " + arenaName + " 的已删除分组 " + removedGroup + " 时失败。", exception);
            }
        }
        return changed;
    }

    private List<String> configuredGroups() {
        return new ArrayList<>(config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS));
    }

    private String findConfiguredGroup(String requested) {
        if (requested == null) return null;
        if (ArenaGroupPolicy.DEFAULT_GROUP.equalsIgnoreCase(requested.trim())) {
            return ArenaGroupPolicy.DEFAULT_GROUP;
        }
        return configuredGroups().stream()
                .filter(group -> group.equalsIgnoreCase(requested.trim()))
                .findFirst().orElse(null);
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("create", "remove", "list", "set", "show");
    }

    private void sendArenaGroupCmdList(Player player) {
        sendUsage(player, "create <groupName>", "创建竞技场组。", "create");
        sendUsage(player, "list", "查看可用的竞技场组。", "list");
        sendUsage(player, "remove <groupName>", "删除竞技场组，并把使用它的地图重置到 Default。", "remove");
        sendUsage(player, "set <arenaName> <groupName>", "设置竞技场唯一的分组。", "set");
        sendUsage(player, "show <arenaName>", "查看竞技场分组。", "show");
    }

    private void sendUsage(Player player, String syntax, String description, String action) {
        player.spigot().sendMessage(Misc.msgHoverClick(
                "§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " §o" + syntax,
                description,
                "/" + getParent().getName() + " " + getSubCommandName() + " " + action,
                action.equals("list") ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
    }

    @Override
    public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player player = (Player) sender;
        if (Arena.isInArena(player)) return false;
        if (SetupSession.isInSetupSession(player.getUniqueId())) return false;
        return hasPermission(sender);
    }

}
