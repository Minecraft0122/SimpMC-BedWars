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

package com.andrei1058.bedwars.support.citizens;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinNPC {
    private static boolean citizensSupport = false;
    private static final String HOLOGRAM_METADATA = "bw1058-join-npc-hologram";

    /* Legacy extension surface retained for compatibility; new holograms use TextDisplay below. */
    @Deprecated
    public static HashMap<ArmorStand, List<String>> npcs_holos = new HashMap<>();
    /** Text holograms without colors and placeholders translated, used for refresh. */
    public static HashMap<TextDisplay, List<String>> npcs_text_displays = new HashMap<>();
    /* Here are stored all the NPCs*/
    public static HashMap<Integer, String> npcs = new HashMap<>();


    /**
     * Check if Citizens is loaded correctly
     */
    public static boolean isCitizensSupport() {
        return citizensSupport;
    }

    /**
     * Set Citizens support
     */
    public static void setCitizensSupport(boolean citizensSupport) {
        JoinNPC.citizensSupport = citizensSupport;
        MainCommand bw = MainCommand.getInstance();
        if (bw == null) return;
        if (citizensSupport) {
            //add npc subCommand
            if (bw.isRegistered()) {
                boolean registered = false;
                for (SubCommand sc : bw.getSubCommands()) {
                    if (sc.getSubCommandName().equalsIgnoreCase("npc")) {
                        registered = true;
                        break;
                    }
                }
                if (!registered) {
                    new com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.NPC(bw, "npc");
                }
            }
        } else {
            //remove npc subCommand
            if (bw.isRegistered()) {
                bw.getSubCommands().removeIf(sc -> sc.getSubCommandName().equalsIgnoreCase("npc"));
            }
        }
    }

    /**
     * Spawn a join-NPC
     *
     * @param group Arena Group
     * @param l     Location where to be spawned
     * @param name  Display name
     * @param skin  A player name to get his skin
     */
    @Nullable
    public static NPC spawnNPC(Location l, String name, String group, String skin, NPC spawnExisting) {
        if (!isCitizensSupport()) return null;
        NPC npc;
        if (spawnExisting == null) {
            npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "");
        } else {
            npc = spawnExisting;
        }
        if (!npc.isSpawned()) {
            npc.spawn(l);
        }
        if (npc.getEntity() instanceof SkinnableEntity) ((SkinnableEntity) npc.getEntity()).setSkinName(skin);
        npc.setProtected(true);
        npc.setName("");
        String separator = "\\\\n";
        String[] nume = name.split(separator);
        removeNearbyHolograms(l);
        if (nume.length >= 2) {
            TextDisplay a = createTextDisplay(l.clone().add(0, 0.05, 0));
            a.text(AdventureText.ampersand(nume[0].replace("{players}", String.valueOf(Arena.getPlayers(group))).replace('\u00a7', '&')));
            npcs.put(npc.getId(), group);
            TextDisplay a2 = createTextDisplay(l.clone().subtract(0, 0.25, 0));
            a2.text(AdventureText.ampersand(nume[1].replace("{players}", String.valueOf(Arena.getPlayers(group))).replace('\u00a7', '&')));
            npcs_text_displays.put(a, Arrays.asList(group, nume[0]));
            npcs_text_displays.put(a2, Arrays.asList(group, nume[1]));
        } else if (nume.length == 1) {
            npcs.put(npc.getId(), group);
            TextDisplay a2 = createTextDisplay(l.clone().subtract(0, 0.25, 0));
            a2.text(AdventureText.ampersand(nume[0].replace("{players}", String.valueOf(Arena.getPlayers(group))).replace('\u00a7', '&')));
            npcs_text_displays.put(a2, Arrays.asList(group, nume[0]));
        }
        npc.teleport(l, PlayerTeleportEvent.TeleportCause.PLUGIN);
        npc.setName("");
        return npc;
    }

    /**
     * Spawn all the CmdJoin-NPCs
     */
    public static void spawnNPCs() {
        if (!isCitizensSupport()) return;
        if (BedWars.config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE) != null) {
            for (String s : BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE)) {
                String[] data = s.split(",");
                if (data.length < 10) continue;
                if (!Misc.isNumber(data[0])) continue;
                if (!Misc.isNumber(data[1])) continue;
                if (!Misc.isNumber(data[2])) continue;
                if (!Misc.isNumber(data[3])) continue;
                if (!Misc.isNumber(data[4])) continue;
                if (Misc.isNumber(data[5])) continue;
                if (Misc.isNumber(data[6])) continue;
                if (Misc.isNumber(data[7])) continue;
                if (Misc.isNumber(data[8])) continue;
                if (!Misc.isNumber(data[9])) continue;
                Location l = new Location(Bukkit.getWorld(data[5]), Double.parseDouble(data[0]), Double.parseDouble(data[1]), Double.parseDouble(data[2]), Float.parseFloat(data[3]),
                        Float.parseFloat(data[4]));
                String skin = data[6], name = data[7], group = data[8];
                int id = Integer.parseInt(data[9]);
                net.citizensnpcs.api.npc.NPC npc = CitizensAPI.getNPCRegistry().getById(id);
                if (npc == null) {
                    BedWars.plugin.getLogger().severe("Invalid npc id: " + id);
                    continue;
                }
                spawnNPC(l, name, group, skin, npc);
            }

        }
    }

    /**
     * Update CmdJoin NPCs for a group
     *
     * @param group arena group
     */
    public static void updateNPCs(String group) {
        String x = String.valueOf(Arena.getPlayers(group));
        for (Map.Entry<TextDisplay, List<String>> e : npcs_text_displays.entrySet()) {
            if (e.getValue().get(0).equalsIgnoreCase(group) && e.getKey() != null && !e.getKey().isDead()) {
                e.getKey().text(AdventureText.ampersand(
                        e.getValue().get(1).replace("{players}", x).replace('\u00a7', '&')));
            }
        }
        for (Map.Entry<ArmorStand, List<String>> e : npcs_holos.entrySet()) {
            if (e.getValue().get(0).equalsIgnoreCase(group)) {
                if (e.getKey() != null) {
                    if (!e.getKey().isDead()) {
                        e.getKey().customName(AdventureText.ampersand(e.getValue().get(1).replace("{players}", x).replace('\u00a7', '&')));
                    }
                }

            }
        }
    }

    public static void removeNearbyHolograms(Entity npcEntity) {
        if (npcEntity != null) removeNearbyHolograms(npcEntity.getLocation());
    }

    private static void removeNearbyHolograms(Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 3, 1)) {
            if (entity.getType() == EntityType.ARMOR_STAND
                    || (entity instanceof TextDisplay && entity.hasMetadata(HOLOGRAM_METADATA))) {
                entity.remove();
            }
        }
        npcs_holos.keySet().removeIf(Entity::isDead);
        npcs_text_displays.keySet().removeIf(Entity::isDead);
    }

    private static TextDisplay createTextDisplay(Location location) {
        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class);
        display.setBillboard(Display.Billboard.CENTER);
        display.setDefaultBackground(false);
        display.setSeeThrough(true);
        display.setShadowed(true);
        display.setPersistent(false);
        display.setLineWidth(200);
        display.setMetadata(HOLOGRAM_METADATA, new FixedMetadataValue(BedWars.plugin, true));
        return display;
    }
}
