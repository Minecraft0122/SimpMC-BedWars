package com.andrei1058.bedwars.arena;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReJoinNoticeTest {

    @Test
    public void replacesPlayerAndTimeoutPlaceholders() {
        assertEquals("Alex can rejoin in 60 seconds", ReJoin.rejoinNotice(
                "{player} can rejoin in {time} seconds", "Alex", 60));
    }

    @Test
    public void clampsInvalidTimeoutToOneSecond() {
        assertEquals("Alex can rejoin in 1 seconds", ReJoin.rejoinNotice(
                "{player} can rejoin in {time} seconds", "Alex", 0));
    }
}
