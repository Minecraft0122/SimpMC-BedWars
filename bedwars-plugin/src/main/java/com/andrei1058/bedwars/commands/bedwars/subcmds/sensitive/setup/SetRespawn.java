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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.andrei1058.bedwars.BedWars.mainCmd;
import static com.andrei1058.bedwars.commands.Misc.createArmorStand;
import static com.andrei1058.bedwars.commands.Misc.removeArmorStand;

public class SetRespawn extends SubCommand {

    public SetRespawn(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player player = (Player) sender;
        SetupSession session = SetupSession.getSession(player.getUniqueId());
        if (session == null) return false;

        if (args.length < 1) {
            player.sendMessage(session.getPrefix() + ChatColor.RED + "用法：/" + mainCmd + " setRespawn <队伍>");
            displayMissingTeams(player, session);
            return true;
        }

        String team = args[0];
        String teamRoot = "Team." + team + '.';
        if (session.getConfig().getYml().getConfigurationSection(teamRoot.substring(0, teamRoot.length() - 1)) == null) {
            player.sendMessage(session.getPrefix() + ChatColor.RED + "找不到目标队伍：" + team);
            session.displayAvailableTeams();
            return true;
        }

        Location spawn = session.getConfig().getArenaLoc(teamRoot + "Spawn");
        if (spawn == null) {
            player.sendMessage(session.getPrefix() + ChatColor.RED + "请先设置该队伍的首次出生点。");
            return true;
        }

        Location target = ConfigManager.toArenaBlockCenter(player.getLocation());
        Location bed = session.getConfig().getArenaLoc(teamRoot + "Bed");
        if (ConfigManager.areBothLocationsNearBed(spawn, target, bed)) {
            player.sendMessage(session.getPrefix() + ChatColor.RED
                    + "首次出生点已经靠近床，复活点必须设置在距离床至少 4 格的位置。");
            return true;
        }

        String respawnPath = teamRoot + ConfigPath.ARENA_TEAM_RESPAWN;
        Location previous = session.getConfig().getArenaLoc(respawnPath);
        if (previous != null) {
            removeArmorStand("复活点", previous, session.getConfig().getString(respawnPath));
        }
        session.getConfig().getYml().set(respawnPath, ConfigManager.serializeArenaLocation(target));
        session.getConfig().setComments(respawnPath, "队伍成员死亡倒计时结束后的复活位置；与首次出生点分开配置。");
        session.getConfig().save();

        String displayTeam = session.getTeamColor(team) + team;
        createArmorStand(displayTeam + " " + ChatColor.GOLD + "复活点已设置", target,
                ConfigManager.serializeArenaLocation(target));
        player.sendMessage(session.getPrefix() + "已设置复活点：" + displayTeam);
        displayMissingTeams(player, session);
        return true;
    }

    private void displayMissingTeams(Player player, SetupSession session) {
        if (session.getConfig().getYml().getConfigurationSection("Team") == null) return;
        for (String team : Objects.requireNonNull(session.getConfig().getYml().getConfigurationSection("Team")).getKeys(false)) {
            if (session.getConfig().getYml().get("Team." + team + '.' + ConfigPath.ARENA_TEAM_RESPAWN) != null) continue;
            player.spigot().sendMessage(Misc.msgHoverClick(
                    session.getPrefix() + "为队伍设置复活点：" + session.getTeamColor(team) + team + " §7（点击设置）",
                    ChatColor.WHITE + "设置 " + session.getTeamColor(team) + team + " 的独立复活点",
                    "/" + mainCmd + " setRespawn " + team,
                    ClickEvent.Action.RUN_COMMAND));
        }
    }

    @Override
    public List<String> getTabComplete() {
        return new ArrayList<>();
    }

    @Override
    public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player player = (Player) sender;
        return SetupSession.isInSetupSession(player.getUniqueId()) && hasPermission(sender);
    }
}
