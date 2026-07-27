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

package com.andrei1058.bedwars.arena;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 竞技场多分组的规范化、兼容读取和成员判断。 */
public final class ArenaGroupMembership {

    public static final String DEFAULT_GROUP = "Default";
    public static final String GROUPS_PATH = "groups";
    public static final String LEGACY_GROUP_PATH = "group";

    private ArenaGroupMembership() {
    }

    /** 读取新版列表；没有列表时兼容旧版单个 group 字段。 */
    public static List<String> read(YamlConfiguration config) {
        if (config.contains(GROUPS_PATH, true) && config.isList(GROUPS_PATH)) {
            return normalize(config.getStringList(GROUPS_PATH));
        }
        if (config.contains(GROUPS_PATH, true) && config.isString(GROUPS_PATH)) {
            return normalize(List.of(config.getString(GROUPS_PATH, DEFAULT_GROUP)));
        }
        if (config.contains(LEGACY_GROUP_PATH, true) && config.isString(LEGACY_GROUP_PATH)) {
            return normalize(List.of(config.getString(LEGACY_GROUP_PATH, DEFAULT_GROUP)));
        }
        return List.of(DEFAULT_GROUP);
    }

    /** 去除空值并按大小写不敏感的方式去重，同时保持配置顺序。 */
    public static List<String> normalize(Collection<String> groups) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (groups != null) {
            for (String group : groups) {
                if (group == null || group.isBlank()) continue;
                String trimmed = group.trim();
                unique.putIfAbsent(key(trimmed), trimmed);
            }
        }
        if (unique.isEmpty()) unique.put(key(DEFAULT_GROUP), DEFAULT_GROUP);
        return new ArrayList<>(unique.values());
    }

    /**
     * 只保留主配置中存在的分组，并按主配置的大小写规范名称。
     * Default 是内置分组，不必写入 arenaGroups。
     */
    public static List<String> resolveConfigured(Collection<String> requested, Collection<String> configured) {
        Map<String, String> allowed = new LinkedHashMap<>();
        allowed.put(key(DEFAULT_GROUP), DEFAULT_GROUP);
        if (configured != null) {
            for (String group : configured) {
                if (group == null || group.isBlank()) continue;
                String trimmed = group.trim();
                allowed.putIfAbsent(key(trimmed), trimmed);
            }
        }

        List<String> resolved = new ArrayList<>();
        for (String group : normalize(requested)) {
            String canonical = allowed.get(key(group));
            if (canonical != null && !contains(resolved, canonical)) resolved.add(canonical);
        }
        return resolved.isEmpty() ? List.of(DEFAULT_GROUP) : resolved;
    }

    /** 把指定分组设为主组，并保留其余不重复的成员组。 */
    public static List<String> withPrimary(Collection<String> current, String primary) {
        List<String> reordered = new ArrayList<>();
        reordered.add(primary);
        if (current != null) {
            for (String group : current) {
                if (!DEFAULT_GROUP.equalsIgnoreCase(primary)
                        && DEFAULT_GROUP.equalsIgnoreCase(group)) continue;
                reordered.add(group);
            }
        }
        return normalize(reordered);
    }

    public static boolean contains(Collection<String> groups, String expected) {
        if (groups == null || expected == null) return false;
        String expectedKey = key(expected.trim());
        return groups.stream().filter(value -> value != null)
                .anyMatch(value -> key(value.trim()).equals(expectedKey));
    }

    /** 检查成员组是否命中以加号组合的任意查询组。 */
    public static boolean matchesAny(Collection<String> memberships, String groupQuery) {
        if (groupQuery == null || groupQuery.isBlank()) return false;
        List<String> requested = normalize(Arrays.asList(groupQuery.split("\\+")));
        return requested.stream().anyMatch(group -> contains(memberships, group));
    }

    private static String key(String group) {
        return group.toLowerCase(Locale.ROOT);
    }
}
