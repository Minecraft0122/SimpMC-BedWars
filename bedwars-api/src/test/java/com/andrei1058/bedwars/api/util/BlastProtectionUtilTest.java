package com.andrei1058.bedwars.api.util;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlastProtectionUtilTest {

    @Test
    void fireCannotShieldBlocksFromExplosionRays() {
        assertTrue(BlastProtectionUtil.isExplosionRayTransparent(Material.AIR));
        assertTrue(BlastProtectionUtil.isExplosionRayTransparent(Material.CAVE_AIR));
        assertTrue(BlastProtectionUtil.isExplosionRayTransparent(Material.FIRE));
        assertTrue(BlastProtectionUtil.isExplosionRayTransparent(Material.SOUL_FIRE));
        assertFalse(BlastProtectionUtil.isExplosionRayTransparent(Material.WHITE_WOOL));
    }
}
