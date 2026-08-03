package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LastHitTest {

    @Test
    void updatesTheDamagerAndTimeAsOneRecord() {
        UUID victim = UUID.randomUUID();
        LastHit hit = LastHit.record(victim, null, 10L);

        assertEquals(10L, hit.getTime());

        LastHit updated = LastHit.record(victim, null, 20L);
        assertSame(hit, updated);
        assertEquals(20L, updated.getTime());

        updated.remove();
    }
}
