package com.andrei1058.bedwars.arena;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerFacingTest {

    @Test
    void serializesNormalizedDirection() {
        Location location = new Location(null, 1.5, 64, 2.5, 83.4312F, 12.5F);

        assertEquals("90.0,0.0", PlayerFacing.serialize(location));
    }

    @Test
    void appliesDirectionWithoutChangingCoordinates() {
        Location location = new Location(null, 1.5, 64, 2.5);

        PlayerFacing.apply(location, "90.0,-100.0");

        assertEquals(1.5, location.getX());
        assertEquals(64.0, location.getY());
        assertEquals(2.5, location.getZ());
        assertEquals(90.0F, location.getYaw());
        assertEquals(0.0F, location.getPitch());
    }
}
