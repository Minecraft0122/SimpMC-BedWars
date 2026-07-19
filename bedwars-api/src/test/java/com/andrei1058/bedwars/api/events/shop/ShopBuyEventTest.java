package com.andrei1058.bedwars.api.events.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShopBuyEventTest {

    @Test
    void exposesBulkPurchaseCountWithoutBreakingOldConstructor() {
        assertEquals(1, new ShopBuyEvent(null, null, null).getPurchaseCount());
        assertEquals(7, new ShopBuyEvent(null, null, null, 7).getPurchaseCount());
        assertThrows(IllegalArgumentException.class, () -> new ShopBuyEvent(null, null, null, 0));
    }
}
