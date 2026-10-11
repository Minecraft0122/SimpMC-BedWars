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

package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class CmdLang extends SubCommand {

    public CmdLang(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(18);
        showInList(false);
        setDisplayInfo(com.andrei1058.bedwars.commands.bedwars.MainCommand.createTC("§6 ▪ §7/" + MainCommand.getInstance().getName() + " " + getSubCommandName(), "/" + getParent().getName() + " " + getSubCommandName(), "§f切换你的语言。"));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (!(s instanceof Player p)) {
            AdventureText.send(s, Language.getDefaultLanguage().m(Messages.COMMAND_LANG_PLAYER_ONLY));
            return true;
        }
        if (args.length == 0) {
            sendLanguageList(p);
            return true;
        }
        if (args.length != 1) {
            AdventureText.send(p, getMsg(p, Messages.COMMAND_LANG_USAGE));
            return true;
        }
        if (Arena.getArenaByPlayer(p) != null) {
            AdventureText.send(p, getMsg(p, Messages.COMMAND_LANG_USAGE_DENIED));
            return true;
        }
        if (!Language.isLanguageExist(args[0])) {
            AdventureText.send(p, getMsg(p, Messages.COMMAND_LANG_SELECTED_NOT_EXIST));
            return true;
        }
        if (Language.getPlayerLanguage(p).getIso().equalsIgnoreCase(args[0])
                || Language.setPlayerLanguage(p.getUniqueId(), args[0])) {
            AdventureText.send(p, getMsg(p, Messages.COMMAND_LANG_SELECTED_SUCCESSFULLY));
        } else {
            AdventureText.send(p, getMsg(p, Messages.COMMAND_LANG_CHANGE_CANCELLED));
        }
        return true;
    }

    private static void sendLanguageList(Player player) {
        AdventureText.send(player, getMsg(player, Messages.COMMAND_LANG_LIST_HEADER));
        for (Language language : Language.getLanguages()) {
            AdventureText.send(player, getMsg(player, Messages.COMMAND_LANG_LIST_FORMAT)
                    .replace("{iso}", language.getIso()).replace("{name}", language.getLangName()));
        }
        AdventureText.send(player, getMsg(player, Messages.COMMAND_LANG_USAGE));
    }

    @Override
    public List<String> getTabComplete() {
        List<String> tab = new ArrayList<>();
        for (Language lang : Language.getLanguages()) {
            tab.add(lang.getIso());
        }
        return tab;
    }

    @Override
    public boolean canSee(CommandSender s, BedWars api) {
        return s instanceof Player && hasPermission(s);
    }
}
