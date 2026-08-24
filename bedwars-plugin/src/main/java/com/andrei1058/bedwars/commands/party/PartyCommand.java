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

package com.andrei1058.bedwars.commands.party;

import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.party.Party;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.configuration.Permissions;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

import static com.andrei1058.bedwars.BedWars.getParty;
import static com.andrei1058.bedwars.api.language.Language.getList;
import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class PartyCommand extends BukkitCommand {

    public PartyCommand(String name) {
        super(name);
    }

    //owner, target
    private static HashMap<UUID, UUID> partySessionRequest = new HashMap<>();

    /** Remove pending invitations involving a player who is leaving the server. */
    public static void clearPlayer(UUID playerId) {
        if (playerId == null) return;
        partySessionRequest.entrySet().removeIf(entry -> entry.getKey().equals(playerId)
                || entry.getValue().equals(playerId));
    }

    @Override
    public boolean execute(CommandSender s, String c, String[] args) {
        if (s instanceof ConsoleCommandSender) return true;
        Player p = (Player) s;
        if (!Permissions.hasCommandPermission(p, "party")) {
            AdventureText.send(p, getMsg(p, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendPartyCmds(p);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "invite":
                if (args.length == 1) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INVITE_USAGE));
                    return true;
                }
                if (getParty().hasParty(p) && !getParty().isOwner(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INSUFFICIENT_PERMISSIONS));
                    return true;
                }
                if (Bukkit.getPlayer(args[1]) != null && Bukkit.getPlayer(args[1]).isOnline()) {
                    if (p == Bukkit.getPlayer(args[1])) {
                        AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INVITE_DENIED_CANNOT_INVITE_YOURSELF));
                        return true;
                    }
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INVITE_SENT).replace("{playername}", p.getName()).replace("{player}", args[1]));
                    Component tc = AdventureText.section(getMsg(p, Messages.COMMAND_PARTY_INVITE_SENT_TARGET_RECEIVE_MSG)
                            .replace("{player}", p.getName()))
                            .clickEvent(ClickEvent.runCommand("/party accept " + p.getName()));
                    Bukkit.getPlayer(args[1]).sendMessage(tc);
                    if (partySessionRequest.containsKey(p.getUniqueId())) {
                        partySessionRequest.replace(p.getUniqueId(), Bukkit.getPlayer(args[1]).getUniqueId());
                    } else {
                        partySessionRequest.put(p.getUniqueId(), Bukkit.getPlayer(args[1]).getUniqueId());
                    }
                } else {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INVITE_DENIED_PLAYER_OFFLINE).replace("{player}", args[1]));
                }
                break;
            case "accept":
                if (args.length < 2) {
                    return true;
                }
                if (getParty().hasParty(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_ACCEPT_DENIED_ALREADY_IN_PARTY));
                    return true;
                }
                if (Bukkit.getPlayer(args[1]) == null || !Bukkit.getPlayer(args[1]).isOnline()) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INVITE_DENIED_PLAYER_OFFLINE).replace("{player}", args[1]));
                    return true;
                }
                if (!partySessionRequest.containsKey(Bukkit.getPlayer(args[1]).getUniqueId())) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_ACCEPT_DENIED_NO_INVITE));
                    return true;
                }
                if (partySessionRequest.get(Bukkit.getPlayer(args[1]).getUniqueId()).equals(p.getUniqueId())) {
                    partySessionRequest.remove(Bukkit.getPlayer(args[1]).getUniqueId());
                    if (getParty().hasParty(Bukkit.getPlayer(args[1]))) {
                        getParty().addMember(Bukkit.getPlayer(args[1]), p);
                        for (Player on : getParty().getMembers(Bukkit.getPlayer(args[1]))) {
                            AdventureText.send(on, getMsg(p, Messages.COMMAND_PARTY_ACCEPT_SUCCESS).replace("{playername}", p.getName()).replace("{player}", AdventureText.displayName(p)));
                        }
                    } else {
                        getParty().createParty(Bukkit.getPlayer(args[1]), p);
                        for (Player on : getParty().getMembers(Bukkit.getPlayer(args[1]))) {
                            AdventureText.send(on, getMsg(p, Messages.COMMAND_PARTY_ACCEPT_SUCCESS).replace("{playername}", p.getName()).replace("{player}", AdventureText.displayName(p)));
                        }
                    }
                } else {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_ACCEPT_DENIED_NO_INVITE));
                }
                break;
            case "leave":
                if (!getParty().hasParty(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_GENERAL_DENIED_NOT_IN_PARTY));
                    return true;
                }
                if (getParty().isOwner(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_LEAVE_DENIED_IS_OWNER_NEEDS_DISBAND));
                    return true;
                }
                getParty().removeFromParty(p);
                break;
            case "disband":
                if (!getParty().hasParty(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_GENERAL_DENIED_NOT_IN_PARTY));
                    return true;
                }
                if (!getParty().isOwner(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INSUFFICIENT_PERMISSIONS));
                    return true;
                }
                getParty().disband(p);
                break;
            case "remove":
                if (args.length == 1) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_REMOVE_USAGE));
                    return true;
                }
                if (getParty().hasParty(p) && !getParty().isOwner(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INSUFFICIENT_PERMISSIONS));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_REMOVE_DENIED_TARGET_NOT_PARTY_MEMBER).replace("{player}", args[1]));
                    return true;
                }
                if (!isPartyMember(getParty(), p, target)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_REMOVE_DENIED_TARGET_NOT_PARTY_MEMBER).replace("{player}", args[1]));
                    return true;
                }
                getParty().removePlayer(p, target);
                break;
            case "promote":
                if (!getParty().hasParty(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_GENERAL_DENIED_NOT_IN_PARTY));
                    return true;
                } else if (!getParty().isOwner(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INSUFFICIENT_PERMISSIONS));
                    return true;
                }
                if (args.length == 1){
                    this.sendPartyCmds(p);
                    return true;
                }
                Player target1 = Bukkit.getPlayer(args[1]);
                if (!isPartyMember(getParty(), p, target1)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_REMOVE_DENIED_TARGET_NOT_PARTY_MEMBER).replace("{player}", args[1]));
                    return true;
                }
                getParty().promote(p, target1);
                for (Player p1 : getParty().getMembers(p)) {
                    if (p1.equals(p)) {
                        AdventureText.send(p1, getMsg(p1, Messages.COMMAND_PARTY_PROMOTE_SUCCESS).replace("{player}", args[1]));
                    } else if (p1.equals(target1)) {
                        AdventureText.send(p1, getMsg(p1, Messages.COMMAND_PARTY_PROMOTE_OWNER));
                    } else {
                        AdventureText.send(p1, getMsg(p1, Messages.COMMAND_PARTY_PROMOTE_NEW_OWNER).replace("{player}", args[1]));
                    }
                }
                break;
            case "info" :
            case "list":
                if (!getParty().hasParty(p)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_GENERAL_DENIED_NOT_IN_PARTY));
                    return true;
                }
                Player owner = getParty().getOwner(p);
                AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INFO_OWNER).replace("{owner}", owner.getName()));
                AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INFO_PLAYERS));
                for (Player p1 : getParty().getMembers(owner)) {
                    AdventureText.send(p, getMsg(p, Messages.COMMAND_PARTY_INFO_PLAYER).replace("{player}", p1.getName()));
                }
                break;
            default:
                sendPartyCmds(p);
                break;
        }
        return false;
    }

    static boolean isPartyMember(Party party, Player owner, Player target) {
        return target != null && party != null && party.isMember(owner, target);
    }

    private void sendPartyCmds(Player p) {
        for (String s : getList(p, Messages.COMMAND_PARTY_HELP)) {
            AdventureText.send(p, s);
        }
    }
}
