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

package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.support.citizens.JoinNPC;
import com.google.common.base.Joiner;
import net.citizensnpcs.api.CitizensAPI;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.andrei1058.bedwars.BedWars.mainCmd;

public class NPC extends SubCommand {

    //main usage
    private final List<Component> MAIN_USAGE = Arrays.asList(Misc.msgHoverClick("§f\n§c▪ §7用法：§e/" + mainCmd + " " + getSubCommandName() + " add", "§f使用此命令创建加入游戏的 NPC。\n§f点击查看命令格式。", "/"+getParent().getName()+" "+getSubCommandName()+" add", ClickEvent.Action.RUN_COMMAND),
            Misc.msgHoverClick("§c▪ §7用法：§e/" + mainCmd + " " + getSubCommandName() + " remove", "§f站在 NPC 面前以将其删除。", "/"+getParent().getName()+" "+getSubCommandName()+" remove", ClickEvent.Action.SUGGEST_COMMAND));
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private final List<Component> ADD_USAGE = Arrays.asList(Misc.msgHoverClick("f\n§c▪ §7用法：§e§o/" + getParent().getName() + " " + getSubCommandName() + " add <皮肤> <竞技场组> <§7第一行§9\\n§7第二行§e>\n§7可使用 §e{players} §7显示该竞技场组的玩家数量。", "点击填入命令。", "/"+getParent().getName()+" "+getSubCommandName()+" add", ClickEvent.Action.SUGGEST_COMMAND));

    public NPC(ParentCommand parent, String name) {
        super(parent, name);
        showInList(true);
        setPriority(12);
        setPermission(Permissions.PERMISSION_NPC);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + "         §8   - §e创建加入游戏的 NPC", "§f创建加入游戏的 NPC。\n§f点击查看更多详情。",
                "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        if (!JoinNPC.isCitizensSupport()) return false;
        Player p = (Player) s;
        if (args.length < 1) {
            for (Component bc : MAIN_USAGE){
                AdventureText.send(p, bc);
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 4) {
                for (Component bc : ADD_USAGE){
                    AdventureText.send(p, bc);
                }
                return true;
            }

            List<String> npcs;
            if (BedWars.config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE) != null) {
                npcs = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE);
            } else {
                npcs = new ArrayList<>();
            }
            String name = Joiner.on(" ").join(args).replace(args[0] + " " + args[1] + " " + args[2] + " ", "");
            net.citizensnpcs.api.npc.NPC npc = JoinNPC.spawnNPC(p.getLocation(), name, args[2], args[1], null);
            assert npc != null;
            npcs.add(BedWars.config.stringLocationConfigFormat(p.getLocation()) + "," + args[1] + "," + name + "," + args[2] + "," + npc.getId());
            String NPC_SET = "§a§c▪ §bNPC：%name% §b已设置！";
            AdventureText.send(p, NPC_SET.replace("%name%", AdventureText.section(AdventureText.ampersand(name.replace("\\\\n", " ")))));
            AdventureText.send(p, AdventureText.section("§a§c▪ §b目标竞技场组：§6" + args[2]));
            BedWars.config.set(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE, npcs);

        } else if (args[0].equalsIgnoreCase("remove")) {

            List<Entity> e = p.getNearbyEntities(4, 4, 4);
            String NO_NPCS = "§c▪ §b附近没有 NPC。";
            if (e.isEmpty()) {
                AdventureText.send(p, NO_NPCS);
                return true;
            }
            if (BedWars.config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE) == null) {
                String NO_SET = "§c▪ §b尚未设置任何 NPC！";
                AdventureText.send(p, NO_SET);
                return true;
            }
            net.citizensnpcs.api.npc.NPC npc = getTarget(p);
            if (npc == null) {
                AdventureText.send(p, NO_NPCS);
                return true;
            }
            List<String> locations = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE);
            for (Integer id : JoinNPC.npcs.keySet()) {
                if (id == npc.getId()) {
                    for (String loc : BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE)) {
                        if (loc.split(",")[4].equalsIgnoreCase(String.valueOf(npc.getId()))) {
                            locations.remove(loc);
                        }
                    }
                }
            }
            JoinNPC.npcs.remove(npc.getId());
            for (Entity e2 : npc.getEntity().getNearbyEntities(0, 3, 0)) {
                if (e2.getType() == EntityType.ARMOR_STAND) {
                    e2.remove();
                }
            }
            BedWars.config.set(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE, locations);
            npc.destroy();
            String NPC_REMOVED = "§c▪ §b目标 NPC 已删除！";
            AdventureText.send(p, NPC_REMOVED);
        } else {
            for (Component bc : MAIN_USAGE){
                AdventureText.send(p, bc);
            }
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("remove", "add");
    }


    /**
     * Create an armor-stand hologram
     */
    @NotNull
    public static ArmorStand createArmorStand(Location loc) {
        ArmorStand a = loc.getWorld().spawn(loc, ArmorStand.class);
        a.setGravity(false);
        a.setVisible(false);
        a.setCustomNameVisible(false);
        a.setMarker(true);
        return a;
    }

    /**
     * Get target NPC
     */
    @Nullable
    @SuppressWarnings("WeakerAccess")
    public static net.citizensnpcs.api.npc.NPC getTarget(Player player) {

        BlockIterator iterator = new BlockIterator(player.getWorld(), player.getLocation().toVector(), player.getEyeLocation().getDirection(), 0, 100);
        while (iterator.hasNext()) {
            Block item = iterator.next();
            for (Entity entity : player.getNearbyEntities(100, 100, 100)) {
                int acc = 2;
                for (int x = -acc; x < acc; x++) {
                    for (int z = -acc; z < acc; z++) {
                        for (int y = -acc; y < acc; y++) {
                            if (entity.getLocation().getBlock().getRelative(x, y, z).equals(item)) {
                                if (entity.hasMetadata("NPC")) {
                                    net.citizensnpcs.api.npc.NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
                                    if (npc != null) return npc;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (Arena.isInArena(p)) return false;

        if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        return hasPermission(s);
    }
}
