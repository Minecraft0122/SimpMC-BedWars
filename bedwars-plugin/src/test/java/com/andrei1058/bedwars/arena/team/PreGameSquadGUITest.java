package com.andrei1058.bedwars.arena.team;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PreGameSquadGUITest {

    @Test
    void reservesFourCompleteRowsForInvitations() {
        List<Integer> slots = PreGameSquadGUI.contentSlots();

        assertEquals(36, slots.size());
        assertEquals(36, new HashSet<>(slots).size());
        assertEquals(9, slots.getFirst());
        assertEquals(44, slots.getLast());
        assertFalse(slots.contains(49), "the leave button must not be overwritten by player heads");
        assertFalse(slots.contains(53), "the close button must not be overwritten by player heads");
    }
}
