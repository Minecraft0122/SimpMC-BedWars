package com.andrei1058.bedwars.shop.main;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseBatchTest {

    @Test
    void spendsOnlyCompletePriceUnits() {
        assertEquals(8, PurchaseBatch.affordablePurchases(34, 4));
        assertEquals(0, PurchaseBatch.affordablePurchases(3, 4));
        assertEquals(1, PurchaseBatch.affordablePurchases(0, 0));
    }

    @Test
    void combinesPurchasedItemsIntoLegalStacks() {
        int[] stacks = PurchaseBatch.stackAmounts(16, 5, 64);

        assertEquals(2, stacks.length);
        assertEquals(80, Arrays.stream(stacks).sum());
        assertEquals(64, stacks[0]);
        assertEquals(16, stacks[1]);
    }
}
