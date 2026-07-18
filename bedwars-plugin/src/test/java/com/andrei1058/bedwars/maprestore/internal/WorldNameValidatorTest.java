package com.andrei1058.bedwars.maprestore.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldNameValidatorTest {

    @Test
    void acceptsNormalAndLocalizedWorldNames() {
        assertTrue(WorldNameValidator.isSafe("bedwars_solo-01"));
        assertTrue(WorldNameValidator.isSafe("起床战争_双人"));
    }

    @Test
    void rejectsTraversalAbsoluteAndControlNames() {
        assertFalse(WorldNameValidator.isSafe("../world"));
        assertFalse(WorldNameValidator.isSafe("..\\world"));
        assertFalse(WorldNameValidator.isSafe("C:\\server"));
        assertFalse(WorldNameValidator.isSafe(".."));
        assertFalse(WorldNameValidator.isSafe("world\nname"));
        assertFalse(WorldNameValidator.isSafe(" "));
    }
}
