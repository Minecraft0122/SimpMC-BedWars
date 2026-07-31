package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArenaSelectorPaginationTest {

    @Test
    void defaultLayoutShowsFortyFiveArenasPerPage() {
        List<Integer> slots = ArenaSelectorPagination.contentSlots(
                ArenaSelectorPagination.DEFAULT_CONTENT_SLOTS, ArenaSelectorPagination.DEFAULT_SIZE);

        assertEquals(45, slots.size());
        assertEquals(3, ArenaSelectorPagination.pageCount(100, slots.size()));
        assertEquals(2, ArenaSelectorPagination.clampPage(9, 100, slots.size()));
    }

    @Test
    void customSlotsRemainUsableButCannotReplacePageControls() {
        List<Integer> slots = ArenaSelectorPagination.contentSlots("0,45,49,53,8,8,bad,99", 54);

        assertEquals(List.of(0, 8), slots);
        assertFalse(slots.contains(ArenaSelectorPagination.previousSlot(54)));
        assertFalse(slots.contains(ArenaSelectorPagination.indicatorSlot(54)));
        assertFalse(slots.contains(ArenaSelectorPagination.nextSlot(54)));
    }

    @Test
    void invalidLayoutFallsBackToEveryNonControlSlot() {
        List<Integer> slots = ArenaSelectorPagination.contentSlots("invalid", 27);

        assertEquals(24, slots.size());
        assertEquals(2, ArenaSelectorPagination.pageCount(25, slots.size()));
    }

    @Test
    void legacySevenSlotDefaultExpandsOnTheLargeMenu() {
        List<Integer> slots = ArenaSelectorPagination.contentSlots(
                "10,11,12,13,14,15,16", ArenaSelectorPagination.DEFAULT_SIZE);

        assertEquals(45, slots.size());
    }

    @Test
    void expandsASmallInventoryWhenItsCurrentPageCannotFitAllArenas() {
        String sevenSlots = "10,11,12,13,14,15,16";
        assertEquals(27, ArenaSelectorPagination.effectiveSize(27, sevenSlots, 7));
        assertEquals(54, ArenaSelectorPagination.effectiveSize(27, sevenSlots, 8));
    }
}
