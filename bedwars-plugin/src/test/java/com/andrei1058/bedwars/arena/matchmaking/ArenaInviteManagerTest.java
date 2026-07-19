package com.andrei1058.bedwars.arena.matchmaking;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaInviteManagerTest {

    @Test
    void invitationExpiresAfterThirtySeconds() {
        ArenaInviteManager manager = ArenaInviteManager.getInstance();
        UUID inviter = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        manager.create(inviter, target, "acropolis", 1_000L);

        assertTrue(manager.find(target, inviter, 30_999L).isPresent());
        assertTrue(manager.find(target, inviter, 31_000L).isEmpty());
        manager.clearPlayer(target);
    }

    @Test
    void clearingAPlayerRemovesSentAndReceivedInvitations() {
        ArenaInviteManager manager = ArenaInviteManager.getInstance();
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        manager.create(player, other, "one", 0L);
        manager.create(other, player, "two", 0L);

        manager.clearPlayer(player);

        assertEquals(0, manager.findAll(player, 1L).size());
        assertEquals(0, manager.findAll(other, 1L).size());
    }
}
