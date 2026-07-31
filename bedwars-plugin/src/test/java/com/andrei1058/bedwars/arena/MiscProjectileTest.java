package com.andrei1058.bedwars.arena;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiscProjectileTest {

    @Test
    void recognizesPaper12111ProjectileShopMaterialsWithoutVersionBridge() {
        assertTrue(Misc.isProjectile(Material.EGG));
        assertTrue(Misc.isProjectile(Material.FIRE_CHARGE));
        assertTrue(Misc.isProjectile(Material.SNOWBALL));
        assertTrue(Misc.isProjectile(Material.ARROW));
        assertFalse(Misc.isProjectile(Material.IRON_INGOT));
    }
}
