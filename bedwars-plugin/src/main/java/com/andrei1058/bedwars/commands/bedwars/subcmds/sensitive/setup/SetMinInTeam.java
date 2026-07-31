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

import com.andrei1058.bedwars.api.command.ParentCommand;
import org.bukkit.command.CommandSender;

/** @deprecated compatibility alias for {@code /bw setMinPlayers}. */
@Deprecated
public final class SetMinInTeam extends SetMinPlayers {

    public SetMinInTeam(ParentCommand parent, String name) {
        super(parent, name);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        sender.sendMessage("§e旧命令 setMinInTeam 已改为全场最低开局人数；请改用 /"
                + getParent().getName() + " setMinPlayers。");
        return super.execute(args, sender);
    }
}
