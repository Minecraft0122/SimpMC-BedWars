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

package com.andrei1058.bedwars.commands;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.util.AdventureText;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class Misc {

    /**
     * This is used to spawn text markers during the setup
     * so the player knows what he set
     *
     * @since api v6
     */
    public static void createArmorStand(String name, @NotNull Location location, String configLoc) {
        TextDisplay display = location.getWorld().spawn(
                location.getBlock().getLocation().add(0.5, 2, 0.5), TextDisplay.class
        );
        display.text(AdventureText.section(name));
        AdventureText.customName(display, name);
        display.setCustomNameVisible(false);
        display.setBillboard(Display.Billboard.CENTER);
        display.setDefaultBackground(false);
        display.setSeeThrough(true);
        display.setShadowed(true);
        display.setPersistent(false);
        display.setMetadata("bw1058-setup", new FixedMetadataValue(BedWars.plugin, "hologram"));
        if (configLoc != null) {
            display.setMetadata("bw1058-loc", new FixedMetadataValue(BedWars.plugin, configLoc));
        }
    }

    /**
     * Remove a setup text marker or a legacy armor stand
     */
    public static void removeArmorStand(String contains, @NotNull Location location, String configLoc) {
        for (Entity e : location.getWorld().getNearbyEntities(location, 1, 3, 1)) {
            if (e.hasMetadata("bw1058-setup")) {
                if (e.hasMetadata("bw1058-loc")) {
                    if (configLoc == null || e.getMetadata("bw1058-loc").get(0).asString().equalsIgnoreCase(configLoc)) {
                        if (contains != null){
                            if (!contains.isEmpty()){
                                String customName = e instanceof org.bukkit.Nameable nameable ? AdventureText.customName(nameable) : null;
                                if (customName != null && customName.toLowerCase(Locale.ROOT).contains(contains.toLowerCase(Locale.ROOT))){
                                    e.remove();
                                    return;
                                }
                            }
                        }
                        e.remove();
                    }
                } else {
                    e.remove();
                }
                continue;
            }
            if (e.getType() == EntityType.ARMOR_STAND) {
                if (!((ArmorStand) e).isVisible()) {
                    if (contains != null && e instanceof org.bukkit.Nameable nameable
                            && AdventureText.customName(nameable).toLowerCase(Locale.ROOT).contains(contains.toLowerCase(Locale.ROOT))) {
                        e.remove();
                    }
                }
            }
        }
    }

}
