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

package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;


public class SignsConfig extends ConfigManager {

    public SignsConfig(Plugin plugin, String name, String dir) {
        super(plugin, name, dir);
        YamlConfiguration yml = getYml();
        yml.options().header("SimpMC-BedWars 竞技场告示牌配置。\n材质必须使用 Paper 1.21.11 的 Bukkit Material 名称。");
        yml.addDefault("format", Arrays.asList("&a[arena]", "", "&2[on]&9/&2[max] &7([type])", "[status]"));
        yml.addDefault(ConfigPath.SIGNS_STATUS_BLOCK_WAITING_MATERIAL, "GREEN_CONCRETE");
        yml.addDefault(ConfigPath.SIGNS_STATUS_BLOCK_STARTING_MATERIAL, "YELLOW_CONCRETE");
        yml.addDefault(ConfigPath.SIGNS_STATUS_BLOCK_PLAYING_MATERIAL, "RED_CONCRETE");
        yml.addDefault(ConfigPath.SIGNS_STATUS_BLOCK_RESTARTING_MATERIAL, "RED_CONCRETE");
        yml.options().copyDefaults(true);
        setComments("format", "竞技场告示牌的四行显示格式。", "可用占位符包括 [arena]、[on]、[max]、[type]、[status]。");
        setComments(ConfigPath.SIGNS_STATUS_BLOCK_WAITING_MATERIAL, "不同竞技场状态对应的告示牌背板材质。");
        ChineseConfigDocumentation.signs(this);
        updateToLatestVersion(4, config -> {
            List<String> format = new ArrayList<>(config.getStringList("format"));
            List<String> defaults = Arrays.asList("&a[arena]", "", "&2[on]&9/&2[max] &7([type])", "[status]");
            while (format.size() < defaults.size()) {
                format.add(defaults.get(format.size()));
            }
            config.set("format", format);
        });
    }
}
