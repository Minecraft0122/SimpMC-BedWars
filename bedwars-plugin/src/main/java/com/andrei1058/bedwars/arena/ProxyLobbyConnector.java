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

import com.andrei1058.bedwars.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 管理用于返回代理大厅的 BungeeCord 兼容出站通道。
 * 返回操作只立即发送 {@code Connect}，不会向玩家暴露代理信息或故障诊断。
 */
public final class ProxyLobbyConnector {

    public static final String CHANNEL = "BungeeCord";

    private final BedWars plugin;

    public ProxyLobbyConnector(@NotNull BedWars plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void close() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    /** 静默地把玩家直接发送到配置的代理大厅。 */
    public boolean connect(@NotNull Player player) {
        return Misc.connectToProxyLobby(player);
    }
}
