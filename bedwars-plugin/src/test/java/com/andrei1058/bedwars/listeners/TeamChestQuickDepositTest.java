package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamChestQuickDepositTest {

    @Test
    void reportsCompleteAndPartialTransfers() {
        assertEquals(32, TeamChestQuickDeposit.transferredAmount(32, 0));
        assertEquals(20, TeamChestQuickDeposit.transferredAmount(32, 12));
    }

    @Test
    void clampsUnexpectedInventoryResults() {
        assertEquals(0, TeamChestQuickDeposit.transferredAmount(16, 32));
        assertEquals(0, TeamChestQuickDeposit.transferredAmount(0, 0));
    }
}
