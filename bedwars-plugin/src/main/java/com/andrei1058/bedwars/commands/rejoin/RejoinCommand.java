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

package com.andrei1058.bedwars.commands.rejoin;

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.ReJoin;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.configuration.Sounds;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

public class RejoinCommand extends BukkitCommand {

    public RejoinCommand(String name) {
        super(name);
    }

    @Override
    public boolean execute(CommandSender s, String st, String[] args) {
        if (!(s instanceof Player p)) {
            AdventureText.send(s, "此命令只能由玩家使用！");
            return true;
        }

        if (!Permissions.hasCommandPermission(p, "rejoin", Permissions.PERMISSION_REJOIN)) {
            AdventureText.send(p, Language.getMsg(p, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
            return true;
        }

        ReJoin rj = ReJoin.getPlayer(p);

        if (rj == null) {
            AdventureText.send(p, Language.getMsg(p, Messages.REJOIN_NO_ARENA));
            Sounds.playSound("rejoin-denied", p);
            return true;
        }

        if (!rj.reJoin(p)) {
            AdventureText.send(p, Language.getMsg(p, Messages.REJOIN_DENIED));
            Sounds.playSound("rejoin-denied", p);
            return true;
        }
        return true;
    }
}
