package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.api.tasks.StartingTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAssignerTest {

    @Test
    void appliesOneTeamAllocationOnlyForTheCurrentDebugTask() {
        assertFalse(TeamAssigner.canApplyAllocation(List.of(), 1, 4, debugTask(true)));
        assertFalse(TeamAssigner.canApplyAllocation(List.of(1), 1, 4, null));
        assertFalse(TeamAssigner.canApplyAllocation(List.of(1), 1, 4, debugTask(false)));
        assertTrue(TeamAssigner.canApplyAllocation(List.of(1), 4, 4, debugTask(true)));
        assertTrue(TeamAssigner.canApplyAllocation(List.of(2, 2), 2, 4, null));
        assertFalse(TeamAssigner.canApplyAllocation(List.of(3, 1), 2, 4, null));
        assertFalse(TeamAssigner.canApplyAllocation(List.of(2, 5), 2, 4, null));
    }

    @Test
    void canStartAsSoonAsTwoMinimumTeamsCanBeFormed() {
        assertTrue(TeamAssigner.canFormValidGroups(
                List.of(List.of("A"), List.of("B")), 4, 1, 2, new Random(3)));
        assertFalse(TeamAssigner.canFormValidGroups(
                List.of(List.of("A")), 4, 1, 2, new Random(3)));
        assertFalse(TeamAssigner.canFormValidGroups(
                List.of(List.of("A", "B")), 4, 1, 2, new Random(3)));
        assertTrue(TeamAssigner.canFormValidGroups(
                List.of(List.of("A", "B"), List.of("C")), 4, 1, 2, new Random(3)));
    }

    private static StartingTask debugTask(boolean debugStart) {
        return (StartingTask) Proxy.newProxyInstance(StartingTask.class.getClassLoader(),
                new Class<?>[]{StartingTask.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isSingleTeamDebugStart")) return debugStart;
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    return null;
                });
    }
}
