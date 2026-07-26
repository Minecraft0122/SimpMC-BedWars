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

package com.andrei1058.bedwars.api.command;

import com.andrei1058.bedwars.api.BedWars;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class SubCommand {

    private static final Set<String> PLAYER_COMMANDS = Set.of(
            "help", "cmds", "join", "leave", "lang", "teleporter", "gui", "stats",
            "team", "invite", "upgradesmenu", "party");
    private static final Set<String> PERMISSION_FREE_COMMANDS = Set.of(
            "help", "cmds", "join", "leave", "lang", "teleporter", "gui", "stats",
            "team", "invite", "upgradesmenu", "party", "arenalist");

    // SubCommand name
    private String name;
    // Show the subCommand in the main command list
    private boolean show = false;
    // SubCommand parent
    private ParentCommand parent;
    // Command order in main command list
    private int priority = 20;
    // Display name/ info in subCommands list
    private TextComponent displayInfo;
    // True if this is an arena setup SubCommand
    private boolean arenaSetupCommand = false;
    // Legacy/official permissions accepted in addition to the stable per-command permission.
    private final List<String> permissions = new ArrayList<>();
    // Stable per-command permission used by permission plugins.
    private final String commandPermission;

    /**
     * Create a sub-command for a bedWars command
     * Make sure you return true or it will say command not found
     *
     * @param name   sub-command name
     * @param parent parent command
     */
    public SubCommand(ParentCommand parent, String name) {
        this.name = name;
        this.parent = parent;
        this.commandPermission = "bw.command." + name.toLowerCase(Locale.ROOT);
        parent.addSubCommand(this);
    }

    /**
     * Add your sub-command code under this method
     */
    public abstract boolean execute(String[] args, CommandSender s);

    /**
     * Get sub-command name
     */
    public String getSubCommandName() {
        return name;
    }

    /**
     * Display the sub-command under the main command
     * Don't forget to set the displayInfo
     * Ops only will see it cuz players receive a commands list from messages file
     */
    public void showInList(boolean value) {
        this.show = value;
    }

    /**
     * This is the command information in the subCommands list of the target parent
     */
    public void setDisplayInfo(TextComponent displayInfo) {
        this.displayInfo = displayInfo;
    }

    /**
     * This is the command priority in the sub-commands list
     * You may use this method if you set showInList true
     * Commands with a minor number will be displayed first
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Get parent command
     */
    public ParentCommand getParent() {
        return parent;
    }

    /**
     * Get show priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get command description for subCommands list
     */
    public TextComponent getDisplayInfo() {
        return displayInfo;
    }

    /**
     * True if it is an arena setup sub-command
     */
    public void setArenaSetupCommand(boolean arenaSetupCommand) {
        this.arenaSetupCommand = arenaSetupCommand;
    }

    /**
     * Check if it is an arena setup command
     */
    public boolean isArenaSetupCommand() {
        return arenaSetupCommand;
    }

    /**
     * Check if is displayed on the list
     */
    public boolean isShow() {
        return show;
    }

    /**
     * Set permission for sub-command
     */
    public void setPermission(String permission) {
        permissions.clear();
        addPermission(permission);
    }

    /**
     * Add a compatible permission alias without replacing the primary permission.
     */
    public void addPermission(String permission) {
        if (permission != null && !permission.isBlank() && !permissions.contains(permission)) {
            permissions.add(permission);
        }
    }

    /**
     * Get the stable permission assigned to this exact sub-command.
     */
    public String getCommandPermission() {
        return commandPermission;
    }

    /**
     * Check if player has permission to use the command
     */
    public boolean hasPermission(CommandSender p) {
        return isPermissionFreeCommand(name)
                || p.hasPermission("bw.*") || p.hasPermission("bw.command.*")
                || p.hasPermission(commandPermission)
                || (isPlayerCommand(name) && p.hasPermission("bw.player"))
                || permissions.stream().anyMatch(p::hasPermission);
    }

    /**
     * Whether a command is part of the ordinary-player permission bundle.
     */
    public static boolean isPlayerCommand(String name) {
        return name != null && PLAYER_COMMANDS.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Whether the official permission table exposes the command without a permission.
     */
    public static boolean isPermissionFreeCommand(String name) {
        return name != null && PERMISSION_FREE_COMMANDS.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Check if a sender can see/ use the sub cmd
     *
     * @param api BedWars api instance
     */
    public boolean canSee(CommandSender sender, BedWars api) {
        if (sender instanceof ConsoleCommandSender) return false;
        if (isArenaSetupCommand() && api.isInSetupSession(((Player) sender).getUniqueId())) return hasPermission(sender);
        if (!isArenaSetupCommand() && api.isInSetupSession(((Player) sender).getUniqueId())) return false;
        return !isArenaSetupCommand() && hasPermission(sender);
    }

    /**
     * Manage sub-command tab complete
     */
    public abstract List<String> getTabComplete();

    /**
     * Get sender-aware tab suggestions. Existing extensions that only
     * implement {@link #getTabComplete()} remain source and binary compatible.
     */
    public List<String> getTabComplete(CommandSender sender) {
        return getTabComplete();
    }
}
