package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcFacingTest {

    @Test
    void calculatesMinecraftYawTowardTarget() {
        assertEquals(0.0F, NpcFacing.toward(0, 0, 0, 1));
        assertEquals(-90.0F, NpcFacing.toward(0, 0, 1, 0));
        assertEquals(-180.0F, NpcFacing.toward(0, 0, 0, -1));
        assertEquals(90.0F, NpcFacing.toward(0, 0, -1, 0));
    }

    @Test
    void normalizesStoredYaw() {
        assertEquals(-90.0F, NpcFacing.normalize(270.0F));
        assertEquals(90.0F, NpcFacing.normalize(-270.0F));
        assertEquals(-90.0F, NpcFacing.normalize(-88.328F));
        assertEquals(-180.0F, NpcFacing.normalize(-179.231F));
        assertEquals(90.0F, NpcFacing.normalize(83.4312F));
    }
}
