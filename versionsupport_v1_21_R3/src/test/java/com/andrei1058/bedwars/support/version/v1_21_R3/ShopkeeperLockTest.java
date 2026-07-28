package com.andrei1058.bedwars.support.version.v1_21_R3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopkeeperLockTest {

    @Test
    void comparesRotationAcrossTheYawWrapBoundary() {
        assertEquals(2.0F, v1_21_R3.angleDifference(179.0F, -179.0F), 0.0001F);
        assertEquals(0.0F, v1_21_R3.angleDifference(90.0F, 90.0F), 0.0001F);
        assertEquals(90.0F, v1_21_R3.angleDifference(-180.0F, 90.0F), 0.0001F);
    }
}
