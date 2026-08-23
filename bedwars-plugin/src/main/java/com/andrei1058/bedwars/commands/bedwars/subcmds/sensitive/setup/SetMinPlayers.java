/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.IntStream;

public class SetMinPlayers extends SubCommand {

    public SetMinPlayers(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player player = (Player) sender;
        SetupSession session = SetupSession.getSession(player.getUniqueId());
        if (session == null) {
            AdventureText.send(player, "§c ▪ §7你当前不在竞技场设置会话中！");
            return true;
        }

        int minimum;
        try {
            minimum = args.length == 0 ? 0 : Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            minimum = 0;
        }
        int capacity = arenaCapacity(session);
        if (minimum < 2 || (capacity > 0 && minimum > capacity)) {
            String range = capacity > 0 ? "2 到 " + capacity : "大于或等于 2";
            AdventureText.send(player, "§c▪ §7全场最低开局人数必须是" + range + "的整数。");
            AdventureText.send(player, "§c▪ §7用法：/" + getParent().getName() + " setMinPlayers <整数>");
            return true;
        }

        session.getConfig().set("minPlayers", minimum);
        AdventureText.send(player, "§6 ▪ §7已将全场最低开局人数设置为 §e" + minimum
                + "§7；达到人数且能够分成至少两个不超过每队容量的队伍后开始倒计时。");
        return true;
    }

    private static int arenaCapacity(SetupSession session) {
        ConfigurationSection teams = session.getConfig().getYml().getConfigurationSection("Team");
        if (teams == null) return 0;
        int maxInTeam = Math.max(1, session.getConfig().getYml().getInt("maxInTeam", 1));
        return teams.getKeys(false).size() * maxInTeam;
    }

    @Override
    public List<String> getTabComplete() {
        return IntStream.rangeClosed(2, 16).mapToObj(String::valueOf).toList();
    }

    @Override
    public boolean canSee(CommandSender sender, BedWars api) {
        return sender instanceof Player player
                && SetupSession.isInSetupSession(player.getUniqueId())
                && hasPermission(sender);
    }
}
