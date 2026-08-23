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

package com.andrei1058.bedwars.support.vault;

import com.andrei1058.bedwars.api.util.AdventureText;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class WithChat implements Chat {

    private final net.milkbowl.vault.chat.Chat chat;

    public WithChat(net.milkbowl.vault.chat.Chat chat) {
        this.chat = Objects.requireNonNull(chat, "chat");
    }

    @Override
    public String getPrefix(Player p) {
        return AdventureText.section(AdventureText.ampersand(chat.getPlayerPrefix(p)));
    }

    @Override
    public String getSuffix(Player p) {
        return AdventureText.section(AdventureText.ampersand(chat.getPlayerSuffix(p)));
    }
}
