package com.andrei1058.bedwars.upgrades;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpgradesManagerTest {

    @Test
    void loadsNumericUpgradeTiersInPurchaseOrder() {
        assertEquals(List.of("tier-1", "tier-2", "tier-4", "tier-10"),
                UpgradesManager.orderedTierNames(List.of(
                        "display-item", "tier-10", "tier-2", "tier-x", "tier-4", "tier-1")));
    }
}
