package com.andrei1058.bedwars.arena.tasks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRestartingTaskTest {

    @Test
    void announcesOnlyTheRequestedChatMilestones() {
        List<Integer> announcements = IntStream.iterate(65, value -> value >= 0, value -> value - 1)
                .filter(GameRestartingTask::shouldAnnounceRestartCountdown)
                .boxed()
                .toList();

        assertEquals(List.of(60, 30, 15, 10, 5, 4, 3, 2, 1, 0), announcements);
    }

    @Test
    void unloadsOnlyAfterEvacuationAndTeleportsComplete() {
        assertFalse(GameRestartingTask.canUnloadArenaWorld(false, 0, 0));
        assertFalse(GameRestartingTask.canUnloadArenaWorld(true, 1, 0));
        assertFalse(GameRestartingTask.canUnloadArenaWorld(true, 0, 1));
        assertTrue(GameRestartingTask.canUnloadArenaWorld(true, 0, 0));
    }
}
