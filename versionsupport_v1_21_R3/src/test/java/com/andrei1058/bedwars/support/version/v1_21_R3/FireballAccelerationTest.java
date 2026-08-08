package com.andrei1058.bedwars.support.version.v1_21_R3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FireballAccelerationTest {

    @Test
    void writesThePersistentPaperAccelerationField() {
        FireballHandle handle = new FireballHandle();

        v1_21_R3.setAccelerationPower(handle, 0.2D);

        assertEquals(0.2D, handle.accelerationPower, 1.0E-9D);
    }

    public static final class FireballHandle {
        public double accelerationPower = 0.1D;
    }
}
