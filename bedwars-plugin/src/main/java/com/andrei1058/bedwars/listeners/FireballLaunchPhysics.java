/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.listeners;

import org.bukkit.util.Vector;

/** Pure launch calculations shared by the fireball listener and its tests. */
final class FireballLaunchPhysics {

    static final double MAX_SNEAK_RECOIL = 0.08D;

    private FireballLaunchPhysics() {
    }

    static double launchSpeed(double baseSpeed, boolean sneaking, double sneakMultiplier) {
        double safeBase = Math.max(0.1D, baseSpeed);
        return sneaking ? safeBase * Math.max(1.0D, sneakMultiplier) : safeBase;
    }

    static Vector sneakRecoil(Vector launchDirection, double configuredStrength) {
        if (launchDirection == null) return new Vector();
        Vector horizontal = new Vector(-launchDirection.getX(), 0D, -launchDirection.getZ());
        if (horizontal.lengthSquared() < 1.0E-8D) return new Vector();
        double strength = Math.min(MAX_SNEAK_RECOIL, Math.max(0D, configuredStrength));
        return horizontal.normalize().multiply(strength);
    }
}
