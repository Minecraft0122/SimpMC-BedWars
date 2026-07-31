/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collection;
import java.util.List;

/** 单竞技场组的读取、迁移与校验规则。 */
public final class ArenaGroupPolicy {

    public static final String DEFAULT_GROUP = "Default";
    public static final String GROUP_PATH = "group";
    public static final String LEGACY_GROUPS_PATH = "groups";

    private ArenaGroupPolicy() {
    }

    /**
     * 读取竞技场组。2.13.x 的列表格式优先，以便迁移时保留原主组。
     */
    public static String read(YamlConfiguration config) {
        if (config.contains(LEGACY_GROUPS_PATH, true)) {
            if (config.isList(LEGACY_GROUPS_PATH)) {
                for (String group : config.getStringList(LEGACY_GROUPS_PATH)) {
                    if (group != null && !group.isBlank()) return normalize(group);
                }
            } else if (config.isString(LEGACY_GROUPS_PATH)) {
                return normalize(config.getString(LEGACY_GROUPS_PATH));
            }
        }
        if (config.contains(GROUP_PATH, true) && config.isString(GROUP_PATH)) {
            return normalize(config.getString(GROUP_PATH));
        }
        return DEFAULT_GROUP;
    }

    public static String normalize(String group) {
        return group == null || group.isBlank() ? DEFAULT_GROUP : group.trim();
    }

    /** Default 是内置组；其他名称必须存在于主配置中。 */
    public static String resolveConfigured(String requested, Collection<String> configured) {
        String normalized = normalize(requested);
        if (DEFAULT_GROUP.equalsIgnoreCase(normalized)) return DEFAULT_GROUP;
        if (configured != null) {
            for (String group : configured) {
                if (group != null && group.equalsIgnoreCase(normalized)) return group.trim();
            }
        }
        return DEFAULT_GROUP;
    }

    public static boolean matches(String actual, String expected) {
        return actual != null && expected != null && normalize(actual).equalsIgnoreCase(expected.trim());
    }

    /** 已发布多组 API 的二进制兼容桥，只采用第一项。 */
    public static String first(List<String> groups) {
        if (groups == null) return DEFAULT_GROUP;
        for (String group : groups) {
            if (group != null && !group.isBlank()) return normalize(group);
        }
        return DEFAULT_GROUP;
    }
}
