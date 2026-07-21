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

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.server.SetupType;
import com.andrei1058.bedwars.arena.GeneratorStructureLocator;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Explicit assisted-setup trigger for locating global diamond and emerald generators.
 */
public class AutoDetectGenerators extends SubCommand {

    public AutoDetectGenerators(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (!(sender instanceof Player player) || args.length != 0) return false;
        SetupSession session = SetupSession.getSession(player.getUniqueId());
        if (session == null) return false;
        if (session.getSetupType() != SetupType.ASSISTED) {
            player.sendMessage(session.getPrefix() + ChatColor.RED + "此指令仅在引导式快速设置中可用。");
            return true;
        }

        player.sendMessage(session.getPrefix() + ChatColor.YELLOW
                + "正在扫描钻石/绿宝石生成器结构，期间可能出现短暂卡顿……");
        GeneratorStructureLocator.ScanResult result = session.autoDetectGlobalGenerators();
        player.sendMessage(session.getPrefix() + ChatColor.GREEN + "扫描完成：钻石 "
                + result.diamondGenerators().size() + " 个，绿宝石 "
                + result.emeraldGenerators().size() + " 个。");
        if (result.diamondGenerators().isEmpty() || result.emeraldGenerators().isEmpty()) {
            player.sendMessage(session.getPrefix() + ChatColor.YELLOW
                    + "未识别到的类型不会写入；请先设置队伍出生点并检查 3×3×3 结构，"
                    + "或使用 /bw addGenerator 手动设置。");
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return List.of();
    }

    @Override
    public boolean canSee(CommandSender sender, com.andrei1058.bedwars.api.BedWars api) {
        if (!(sender instanceof Player player)) return false;
        SetupSession session = SetupSession.getSession(player.getUniqueId());
        return session != null && session.getSetupType() == SetupType.ASSISTED && hasPermission(sender);
    }
}
