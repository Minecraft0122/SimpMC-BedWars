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

package com.andrei1058.bedwars.commands.bedwars;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.commands.bedwars.subcmds.regular.*;
import com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.Level;
import com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.Reload;
import com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.*;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.support.citizens.JoinNPC;
import com.andrei1058.bedwars.api.util.AdventureText;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class MainCommand extends BukkitCommand implements ParentCommand {

    /* SubCommands ArenaList */
    private static List<SubCommand> subCommandList = new ArrayList<>();
    /* MainCommand instance*/
    private static MainCommand instance;
    /* Dot char */
    @SuppressWarnings("WeakerAccess")
    public static char dot = 254;

    public MainCommand(String name) {
        super(name);
        setAliases(Arrays.asList("bedwars", "simpmcbedwars", "simpmcbw"));
        instance = this;
        new CmdJoin(this, "join");
        new CmdLeave(this, "leave");
        new CmdLang(this, "lang");
        new CmdTeleporter(this, "teleporter");
        // A dedicated BUNGEE lobby has no local Arena instances, but it still
        // exposes the selector GUI backed by the remote arena directory.
        if (getServerType() != ServerType.BUNGEE || BedWars.isBungeeLobby()) {
            new CmdGUI(this, "gui");
        }
        new CmdStats(this, "stats");
        new CmdTeam(this, "team");
        new CmdInvite(this, "invite");
        new CmdHighlight(this, "highlight");
        new CmdStart(this, "forceStart");
        new CmdStart(this, "start");
        if (BedWars.getServerType() != ServerType.BUNGEE) {
            new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.SetLobby(this, "setLobby"); //priority 1
        }
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.SetupArena(this, "setupArena"); //priority 2
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.ArenaList(this, "arenaList"); //priority 3
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.DelArena(this, "delArena"); //priority 4
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.EnableArena(this, "enableArena"); //priority 5
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.DisableArena(this, "disableArena"); //priority 6
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.CloneArena(this, "cloneArena"); //priority 7
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.ArenaGroup(this, "arenaGroup"); //priority 8
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.Build(this, "build"); //priority 9
        new Level(this, "level");
        new Reload(this, "reload"); //priority 11
        new CmdList(this, "cmds"); //priority 20

        /* Arena setup commands (in world) */
        new AutoCreateTeams(this, "autoCreateTeams");
        new SetWaitingSpawn(this, "setWaitingSpawn");
        new SetSpectatorPos(this, "setSpectSpawn");
        new CreateTeam(this, "createTeam");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.ListTeams(this, "listTeams");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.WaitingPos(this, "waitingPos");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.RemoveTeam(this, "removeTeam");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.SetMaxInTeam(this, "setMaxInTeam");
        new SetMinPlayers(this, "setMinPlayers");
        new SetMinInTeam(this, "setMinInTeam");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.SetBuildHeight(this, "setMaxBuildHeight");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.SetSpawn(this, "setSpawn");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.SetBed(this, "setBed");
        new SetShop(this, "setShop");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.SetUpgrade(this, "setUpgrade");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.AddGenerator(this, "addGenerator");
        new RemoveGenerator(this, "removeGenerator");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.SetType(this, "setType");
        new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.Save(this, "save");
        if (JoinNPC.isCitizensSupport() && BedWars.getServerType() != ServerType.BUNGEE) {
            new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.NPC(this, "npc");
        }
        new CmdTpStaff(this, "tp");
        new CmdUpgrades(this, "upgradesmenu");
        new SetKillDropsLoc(this, "setKillDrops");
    }

    @Override
    public boolean execute(CommandSender s, String st, String[] args) {

        if (args.length == 0) {
            if (!Permissions.hasCommandPermission(s, "help")) {
                if (s instanceof Player player) {
                    AdventureText.send(player, getMsg(player, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
                } else {
                    AdventureText.send(s, Language.getDefaultLanguage().m(Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
                }
                return true;
            }
            /* Set op commands*/
            if ((s.isOp() || s.hasPermission(BedWars.mainCmd + ".*"))) {
                if (s instanceof Player) {
                    if (SetupSession.isInSetupSession(((Player) s).getUniqueId())) {
                        Bukkit.dispatchCommand(s, getName() + " cmds");
                    } else {
                        AdventureText.send(s, "");
                        AdventureText.send(s, "§8§l" + dot + " §6" + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion() + " §7- §c管理员命令");
                        AdventureText.send(s, "");
                        sendSubCommands((Player) s);
                    }
                } else {
                    AdventureText.send(s, "§f   bw safemode §eenable/disable §7（启用/禁用安全模式）");
                }
            } else {
                if (s instanceof ConsoleCommandSender) {
                    AdventureText.send(s, "§f当前没有可用的控制台命令。");
                    return true;
                }
                /* Send player commands */
                Bukkit.dispatchCommand(s, mainCmd + " cmds");
            }
            return true;
        }

        boolean commandFound = false;
        for (SubCommand sb : getSubCommands()) {
            if (sb.getSubCommandName().equalsIgnoreCase(args[0])) {
                if (sb.hasPermission(s)) {
                    commandFound = sb.execute(Arrays.copyOfRange(args, 1, args.length), s);
                }
            }
        }

        if (!commandFound) {
            if (s instanceof Player) {
                AdventureText.send(s, getMsg((Player) s, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
            } else {
                AdventureText.send(s, Language.getDefaultLanguage().m(Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
            }
        }
        return true;
    }

    public static boolean isArenaGroup(String var) {
        if (var == null) return false;
        if (var.equalsIgnoreCase("default")) return true;
        return config.getYml().getStringList("arenaGroups").stream()
                .anyMatch(group -> group.equalsIgnoreCase(var.trim()));
    }

    public static Component createTC(String text, String suggest, String shot_text) {
        return AdventureText.section(text)
                .clickEvent(ClickEvent.suggestCommand(suggest))
                .hoverEvent(HoverEvent.showText(AdventureText.section(shot_text)));
    }

    @Override
    public void addSubCommand(SubCommand subCommand) {
        subCommandList.add(subCommand);
    }

    @Override
    public void sendSubCommands(Player p) {
        for (int i = 0; i <= 20; i++) {
            for (SubCommand sb : getSubCommands()) {
                if (sb.getPriority() == i && sb.isShow() && sb.canSee(p, BedWars.getAPI())) {
                    AdventureText.send(p, sb.getDisplayInfo());
                }
            }
        }
    }

    public List<String> tabComplete(CommandSender s, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            for (SubCommand sb : getSubCommands()) {
                if (sb.canSee(s, BedWars.getAPI())) sub.add(sb.getSubCommandName());
            }
            return sub;
        } else if (args.length >= 2 && hasSubCommand(args[0])) {
            SubCommand subCommand = getSubCommand(args[0]);
            if (subCommand != null && subCommand.canSee(s, BedWars.getAPI())) {
                return subCommand.getTabComplete(s, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return null;
    }


    @Override
    public List<SubCommand> getSubCommands() {
        return subCommandList;
    }

    /**
     * Get instance
     */
    public static MainCommand getInstance() {
        return instance;
    }

    /**
     * Check if lobby location is set, else send a error message to the player
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isLobbySet(Player p) {
        if (BedWars.getServerType() == ServerType.BUNGEE) return true;
        if (config.getLobbyWorldName().isEmpty()) {
            if (p != null) {
                AdventureText.send(p, "§c▪ §7请先设置大厅位置！");
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean hasSubCommand(String name) {
        for (SubCommand sc : getSubCommands()) {
            if (sc.getSubCommandName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get sub-command by name
     */
    @SuppressWarnings("WeakerAccess")
    public SubCommand getSubCommand(String name) {
        for (SubCommand sc : getSubCommands()) {
            if (sc.getSubCommandName().equalsIgnoreCase(name)) {
                return sc;
            }
        }
        return null;
    }

    /**
     * Get a dot symbol
     */
    public static char getDot() {
        return dot;
    }
}
