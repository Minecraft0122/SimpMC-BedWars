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

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static com.andrei1058.bedwars.BedWars.mainCmd;

public final class SetMinInTeam extends SubCommand {

    public SetMinInTeam(ParentCommand parent, String name) {
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
            player.sendMessage("§c ▪ §7你当前不在竞技场设置会话中！");
            return true;
        }

        int minimum;
        try {
            minimum = args.length == 0 ? 0 : Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            minimum = 0;
        }
        int maximum = Math.max(1, session.getConfig().getYml().getInt("maxInTeam", 1));
        if (minimum < 1 || minimum > maximum) {
            player.sendMessage("§c▪ §7用法：/" + mainCmd + " setMinInTeam <1-" + maximum + ">");
            return true;
        }

        session.getConfig().set("minInTeam", minimum);
        player.sendMessage("§6 ▪ §7已设置正常开局时每队最少人数为 §e" + minimum + "§7！");
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return List.of("1", "2", "3", "4");
    }

    @Override
    public boolean canSee(CommandSender sender, BedWars api) {
        return sender instanceof Player player
                && SetupSession.isInSetupSession(player.getUniqueId())
                && hasPermission(sender);
    }
}
