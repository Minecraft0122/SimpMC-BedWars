/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySqlSchemaCoordinatorTest {

    @Test
    void schemaLockNamesAreStableAndFitMySqlLimit() {
        String shortName = MySqlSchemaCoordinator.buildLockName("match-statistics", "bedwars");
        assertEquals(shortName, MySqlSchemaCoordinator.buildLockName("match-statistics", "bedwars"));
        assertNotEquals(shortName, MySqlSchemaCoordinator.buildLockName("discipline", "bedwars"));

        String longName = MySqlSchemaCoordinator.buildLockName(
                "match-statistics-with-an-unusually-long-component-name",
                "an_unusually_long_database_name_used_by_a_hosting_provider");
        assertTrue(longName.length() <= 64);
        assertEquals(longName, MySqlSchemaCoordinator.buildLockName(
                "match-statistics-with-an-unusually-long-component-name",
                "an_unusually_long_database_name_used_by_a_hosting_provider"));
    }
}
