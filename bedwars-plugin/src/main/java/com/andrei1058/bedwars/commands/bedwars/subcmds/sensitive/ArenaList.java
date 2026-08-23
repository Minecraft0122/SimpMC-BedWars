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

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.andrei1058.bedwars.BedWars.plugin;

public class ArenaList extends SubCommand {

    private static final int ARENAS_PER_PAGE = 10;

    public ArenaList(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(3);
        showInList(true);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + com.andrei1058.bedwars.commands.bedwars.MainCommand.getInstance().getName() + " " + getSubCommandName() + ((getArenas().size() == 0) ? " §c（已设置 0 个）" : " §a（已设置 " + getArenas().size() + " 个）"),
                "§f显示可用竞技场", "/" + MainCommand.getInstance().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;


        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 0) {
                    page = 1;
                }
            } catch (Exception ignored) {
            }
        }
        int start = (page - 1) * ARENAS_PER_PAGE;
        List<IArena> arenas = new ArrayList<>(Arena.getArenas());
        if (arenas.size() <= start){
            page = 1;
            start = 0;
        }

        AdventureText.send(p, color(" \n&1|| &3" + plugin.getName() + "&7 已加载的竞技场：\n "));

        if (arenas.isEmpty()) {
            AdventureText.send(p, ChatColor.RED + "没有可显示的竞技场。");
            return true;
        }

        int limit = Math.min(arenas.size(), start + ARENAS_PER_PAGE);

        arenas.subList(start, limit).forEach(arena -> {
            String gameState = arena.getDisplayStatus(Language.getPlayerLanguage(p));
            String msg = color(
                    "ID：&e" + arena.getWorldName() +
                            " &f组：&e" + arena.getDisplayGroup(p) +
                            " &f人数：&e" + (arena.getPlayers().size() + arena.getSpectators().size()) +
                            " &f状态：" + gameState +
                            " &f世界已加载：&e" + (Bukkit.getWorld(arena.getWorldName()) != null ? "是" : "否")
            );


            AdventureText.send(p, msg);
        });

        AdventureText.send(p, " ");

        if (arenas.size() > ARENAS_PER_PAGE * page) {
            AdventureText.send(p, ChatColor.GRAY + "输入 /" + ChatColor.GREEN + MainCommand.getInstance().getName() + " arenaList " + ++page + ChatColor.GRAY + " 查看下一页。");
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }

    @NotNull
    private java.util.List<String> getArenas() {
        ArrayList<String> arene = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "/Arenas");
        if (dir.exists()) {
            for (File f : Objects.requireNonNull(dir.listFiles())) {
                if (f.isFile()) {
                    if (f.getName().contains(".yml")) {
                        arene.add(f.getName().replace(".yml", ""));
                    }
                }
            }
        }
        return arene;
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {

        if (s instanceof Player) {
            Player p = (Player) s;
            if (Arena.isInArena(p)) return false;

            if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        }

        return hasPermission(s);
    }

    private static String color(String msg) {
        return AdventureText.section(AdventureText.ampersand(msg));
    }
}
