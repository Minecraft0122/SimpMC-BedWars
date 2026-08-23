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

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.maprestore.internal.WorldNameValidator;
import net.kyori.adventure.text.event.ClickEvent;
import org.apache.commons.io.FileUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.andrei1058.bedwars.BedWars.plugin;

public class CloneArena extends SubCommand {
    public CloneArena(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(7);
        showInList(true);
        setPermission(Permissions.PERMISSION_CLONE);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " §6<世界名> <新名称>", "§f克隆现有竞技场。",
                "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.SUGGEST_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        if (!MainCommand.isLobbySet(p)) return true;
        if (args.length != 2) {
            AdventureText.send(p, "§c▪ §7用法：§o/" + getParent().getName() + " " + getSubCommandName() + " <地图名> <新竞技场名>");
            return true;
        }
        if (!WorldNameValidator.isSafe(args[0]) || !WorldNameValidator.isSafe(args[1])) {
            AdventureText.send(p, ChatColor.RED + "竞技场世界名称不能包含路径分隔符、冒号或控制字符。");
            return true;
        }
        if (!BedWars.getAPI().getRestoreAdapter().isWorld(args[0])) {
            AdventureText.send(p, "§c▪ §7" + args[0] + " 不存在！");
            return true;
        }
        File yml1 = new File(plugin.getDataFolder(), "/Arenas/" + args[0] + ".yml"), yml2 = new File(plugin.getDataFolder(), "/Arenas/" + args[1] + ".yml");
        if (!yml1.exists()) {
            AdventureText.send(p, "§c▪ §7" + args[0] + " 不存在！");
            return true;
        }
        if (BedWars.getAPI().getRestoreAdapter().isWorld(args[1]) && yml2.exists()) {
            AdventureText.send(p, "§c▪ §7" + args[1] + " 已存在！");
            return true;
        }
        if (args[1].contains("+")) {
            AdventureText.send(p, "§c▪ §7" + args[1] + " 不能包含符号：" + ChatColor.RED + "+");
            return true;
        }
        if (Arena.getArenaByName(args[0]) != null) {
            AdventureText.send(p, "§c▪ §7请先禁用 " + args[0] + "！");
            return true;
        }
        BedWars.getAPI().getRestoreAdapter().cloneArena(args[0], args[1]);
        if (yml1.exists()) {
            try {
                FileUtils.copyFile(yml1, yml2, true);
            } catch (IOException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "无法复制竞技场配置 " + yml1 + " 到 " + yml2, e);
                AdventureText.send(p, "§c▪ §7复制地图配置时发生错误，请查看控制台。");
            }
        }
        AdventureText.send(p, "§6 ▪ §7克隆完成。");
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        List<String> tab = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "/Arenas");
        if (dir.exists()) {
            File[] fls = dir.listFiles();
            for (File fl : Objects.requireNonNull(fls)) {
                if (fl.isFile()) {
                    if (fl.getName().contains(".yml")) {
                        tab.add(fl.getName().replace(".yml", ""));
                    }
                }
            }
        }
        return tab;
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (Arena.isInArena(p)) return false;

        if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        return hasPermission(s);
    }
}
