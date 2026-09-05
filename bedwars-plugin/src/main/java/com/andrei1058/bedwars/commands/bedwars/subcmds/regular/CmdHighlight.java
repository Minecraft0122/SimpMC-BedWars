/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.arena.feature.TeammateHighlightManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

/** Toggle the global glow flag on the viewer's active teammates. */
public final class CmdHighlight extends SubCommand {

    private final TeammateHighlightManager highlights = TeammateHighlightManager.getInstance();

    public CmdHighlight(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(15);
        showInList(false);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (!(sender instanceof Player player)) return false;

        IArena arena = Arena.getArenaByPlayer(player);
        TeammateHighlightManager.ToggleResult result = highlights.toggle(player, arena);
        switch (result.outcome()) {
            case NOT_IN_GAME -> AdventureText.send(player, getMsg(player, Messages.COMMAND_HIGHLIGHT_NOT_IN_GAME));
            case NO_TEAMMATES -> AdventureText.send(player, getMsg(player, Messages.COMMAND_HIGHLIGHT_NO_TEAMMATES));
            case DISABLED -> AdventureText.send(player, getMsg(player, Messages.COMMAND_HIGHLIGHT_DISABLED));
            case ENABLED -> {
                String names = result.teammates().stream()
                        .map(Player::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .collect(Collectors.joining("、"));
                AdventureText.send(player, getMsg(player, Messages.COMMAND_HIGHLIGHT_ENABLED)
                        .replace("{players}", names));
            }
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return List.of();
    }

    @Override
    public boolean canSee(CommandSender sender, BedWars api) {
        if (sender instanceof ConsoleCommandSender || !(sender instanceof Player player)) return false;
        if (SetupSession.isInSetupSession(player.getUniqueId())) return false;
        IArena arena = Arena.getArenaByPlayer(player);
        return arena != null && arena.isPlayer(player)
                && arena.getStatus() == com.andrei1058.bedwars.api.arena.GameState.playing
                && !arena.isSpectator(player) && !arena.isReSpawning(player.getUniqueId())
                && hasPermission(sender);
    }
}
