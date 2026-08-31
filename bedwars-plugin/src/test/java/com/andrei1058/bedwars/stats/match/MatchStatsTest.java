package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchStatsTest {

    @Test
    void aggregatesPlayerCountersAndComputesTotalKillDeathRatio() {
        UUID playerUuid = UUID.randomUUID();
        MatchPlayerStats player = new MatchPlayerStats(playerUuid, "Alice", "red");

        player.recordKill(false);
        player.recordKill(false);
        player.recordKill(true);
        player.recordDeath();
        player.recordDeath();
        player.recordBedBreak();
        player.addIllegalTeamVl(2);
        player.addKillBoostingVl(3);
        player.recordReconnect();
        player.recordDisconnect();
        player.setOutcome(MatchPlayerOutcome.WIN);

        MatchPlayerSnapshot snapshot = player.snapshot();
        assertEquals(2, snapshot.kills());
        assertEquals(1, snapshot.finalKills());
        assertEquals(3, snapshot.totalKills());
        assertEquals(2, snapshot.deaths());
        assertEquals(1.5, snapshot.kdRatio().orElseThrow());
        assertEquals(1, snapshot.bedsDestroyed());
        assertEquals(2, snapshot.illegalTeamVl());
        assertEquals(3, snapshot.killBoostingVl());
        assertEquals(5, snapshot.totalVl());
        assertEquals(1, snapshot.reconnects());
        assertEquals(1, snapshot.disconnects());
        assertEquals(MatchPlayerOutcome.WIN, snapshot.outcome());
    }

    @Test
    void representsZeroDeathRatioAsEmpty() {
        MatchPlayerStats player = new MatchPlayerStats(UUID.randomUUID(), null, null);

        assertFalse(player.snapshot().kdRatio().isPresent());
        player.recordKill(true);
        assertFalse(player.snapshot().kdRatio().isPresent());
    }

    @Test
    void duplicateRegistrationKeepsCountersAndUpdatesIdentity() {
        MatchStats stats = new MatchStats();
        UUID playerUuid = UUID.randomUUID();

        MatchPlayerStats first = stats.registerPlayer(playerUuid, "OldName", "red");
        first.recordKill(false);
        MatchPlayerStats second = stats.registerPlayer(playerUuid, "NewName", "blue");

        assertEquals(first, second);
        MatchPlayerSnapshot snapshot = stats.snapshot().player(playerUuid);
        assertEquals("NewName", snapshot.playerName());
        assertEquals("blue", snapshot.teamId());
        assertEquals(1, snapshot.kills());
        assertEquals(1, stats.snapshot().players().size());
    }

    @Test
    void snapshotsAreDetachedAndPlayerOrderIsStable() {
        MatchStats stats = new MatchStats();
        UUID firstUuid = UUID.randomUUID();
        UUID secondUuid = UUID.randomUUID();
        stats.registerPlayer(firstUuid, "First", "red");
        stats.registerPlayer(secondUuid, "Second", "blue");

        MatchStatsSnapshot snapshot = stats.snapshot();
        assertEquals(List.of(firstUuid, secondUuid), snapshot.players().stream()
                .map(MatchPlayerSnapshot::playerUuid).toList());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.players().clear());

        stats.getPlayer(firstUuid).orElseThrow().recordKill(false);
        assertEquals(0, snapshot.player(firstUuid).totalKills());
        assertEquals(1, stats.snapshot().player(firstUuid).totalKills());
        assertEquals(2, stats.snapshot().players().size());
    }

    @Test
    void rejectsNegativeViolationAndNullOutcome() {
        MatchPlayerStats player = new MatchPlayerStats(UUID.randomUUID(), "Alice", "red");

        assertThrows(IllegalArgumentException.class, () -> player.addIllegalTeamVl(-1));
        assertThrows(IllegalArgumentException.class, () -> player.addKillBoostingVl(-1));
        assertThrows(NullPointerException.class, () -> player.setOutcome(null));
        assertTrue(player.snapshot().totalVl() == 0);
    }

    @Test
    void appliesSignedExclusionEvidenceWithoutChangingRawVl() {
        MatchPlayerStats player = new MatchPlayerStats(UUID.randomUUID(), "Alice", "red");

        player.addIllegalTeamVl(12);
        player.adjustEvidence(-5);

        MatchPlayerSnapshot snapshot = player.snapshot();
        assertEquals(12, snapshot.rawVl());
        assertEquals(-5, snapshot.evidenceAdjustment());
        assertEquals(7, snapshot.totalVl());

        player.adjustEvidence(-100);
        assertEquals(12, player.snapshot().rawVl());
        assertEquals(0, player.snapshot().totalVl());
    }
}
