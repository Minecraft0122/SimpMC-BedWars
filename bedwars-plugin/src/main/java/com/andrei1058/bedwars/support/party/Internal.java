/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.support.party;

import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

/**
 * Built-in party implementation used when no cross-server party provider is
 * installed. Party membership is held by UUID, so it survives arena changes
 * and only changes when a player leaves, is removed, disbands, or disconnects.
 */
public class Internal implements Party {
    private static final List<InternalParty> parties = new ArrayList<>();

    @Override
    public synchronized boolean hasParty(Player player) {
        return player != null && findParty(player.getUniqueId()) != null;
    }

    @Override
    public synchronized int partySize(Player player) {
        InternalParty party = player == null ? null : findParty(player.getUniqueId());
        return party == null ? 0 : party.members.size();
    }

    @Override
    public synchronized boolean isOwner(Player player) {
        InternalParty party = player == null ? null : findParty(player.getUniqueId());
        return party != null && party.owner.equals(player.getUniqueId());
    }

    @Override
    @NotNull
    public synchronized List<Player> getMembers(Player player) {
        InternalParty party = player == null ? null : findParty(player.getUniqueId());
        if (party == null) return new ArrayList<>();
        List<Player> online = new ArrayList<>();
        for (UUID member : party.members) {
            Player onlinePlayer = Bukkit.getPlayer(member);
            if (onlinePlayer != null && onlinePlayer.isOnline()) online.add(onlinePlayer);
        }
        return online;
    }

    @Override
    public synchronized void createParty(Player owner, Player... members) {
        if (owner == null) return;
        InternalParty party = findParty(owner.getUniqueId());
        if (party == null) {
            party = new InternalParty(owner.getUniqueId());
            parties.add(party);
        }
        if (members != null) {
            for (Player member : members) {
                if (member != null && findParty(member.getUniqueId()) == null) addMember(party, member);
            }
        }
    }

    @Override
    public synchronized void addMember(Player owner, Player member) {
        if (owner == null || member == null || owner.getUniqueId().equals(member.getUniqueId())) return;
        InternalParty party = findParty(owner.getUniqueId());
        if (party == null) {
            createParty(owner, member);
            return;
        }
        if (findParty(member.getUniqueId()) == null) addMember(party, member);
    }

    @Override
    public synchronized void removeFromParty(Player member) {
        if (member == null) return;
        InternalParty party = findParty(member.getUniqueId());
        if (party == null) return;

        UUID removed = member.getUniqueId();
        party.members.remove(removed);
        if (party.members.isEmpty()) {
            parties.remove(party);
            return;
        }
        if (party.owner.equals(removed)) party.owner = party.members.iterator().next();
        notifyMembers(party, Messages.COMMAND_PARTY_LEAVE_SUCCESS,
                "{playername}", member.getName(), "{player}", member.getDisplayName());
    }

    @Override
    public synchronized void disband(Player owner) {
        InternalParty party = owner == null ? null : findParty(owner.getUniqueId());
        if (party == null || !party.owner.equals(owner.getUniqueId())) return;
        notifyMembers(party, Messages.COMMAND_PARTY_DISBAND_SUCCESS, null, null, null, null);
        parties.remove(party);
    }

    @Override
    public synchronized boolean isMember(Player owner, Player check) {
        if (owner == null || check == null) return false;
        InternalParty party = findParty(owner.getUniqueId());
        return party != null && party.members.contains(check.getUniqueId());
    }

    @Override
    public synchronized void removePlayer(Player owner, Player target) {
        InternalParty party = owner == null ? null : findParty(owner.getUniqueId());
        if (party == null || !party.owner.equals(owner.getUniqueId()) || target == null
                || !party.members.remove(target.getUniqueId())) return;
        notifyMembers(party, Messages.COMMAND_PARTY_REMOVE_SUCCESS,
                "{player}", target.getName(), null, null);
        if (party.members.isEmpty()) parties.remove(party);
        else if (party.owner.equals(target.getUniqueId())) party.owner = party.members.iterator().next();
    }

    @Override
    @Nullable
    public synchronized Player getOwner(Player member) {
        InternalParty party = member == null ? null : findParty(member.getUniqueId());
        return party == null ? null : Bukkit.getPlayer(party.owner);
    }

    @Override
    public synchronized void promote(@NotNull Player owner, @NotNull Player target) {
        InternalParty party = findParty(owner.getUniqueId());
        if (party == null || !party.owner.equals(owner.getUniqueId())
                || !party.members.contains(target.getUniqueId())) return;
        party.owner = target.getUniqueId();
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    /** Snapshot used by assignment and pure logic tests. */
    @NotNull
    @Contract(pure = true)
    public static synchronized List<InternalParty> getParites() {
        return Collections.unmodifiableList(new ArrayList<>(parties));
    }

    private static synchronized InternalParty findParty(UUID player) {
        for (InternalParty party : parties) {
            if (party.members.contains(player)) return party;
        }
        return null;
    }

    private static void addMember(InternalParty party, Player member) {
        party.members.add(member.getUniqueId());
    }

    private static void notifyMembers(InternalParty party, String messageKey,
                                      String replacement1, String value1,
                                      String replacement2, String value2) {
        for (UUID member : party.members) {
            Player player = Bukkit.getPlayer(member);
            if (player == null || !player.isOnline()) continue;
            String message = getMsg(player, messageKey);
            if (replacement1 != null) message = message.replace(replacement1, value1);
            if (replacement2 != null) message = message.replace(replacement2, value2);
            player.sendMessage(message);
        }
    }

    static final class InternalParty {
        private final List<UUID> members = new ArrayList<>();
        private UUID owner;

        private InternalParty(UUID owner) {
            this.owner = owner;
            this.members.add(owner);
        }

        public Player getOwner() {
            return Bukkit.getPlayer(owner);
        }
    }
}
