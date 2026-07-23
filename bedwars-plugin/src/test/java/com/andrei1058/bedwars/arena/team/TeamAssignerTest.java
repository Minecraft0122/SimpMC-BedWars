package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.api.tasks.StartingTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAssignerTest {

    @Test
    void appliesOneTeamAllocationOnlyForTheCurrentDebugTask() {
        assertFalse(TeamAssigner.canApplyAllocation(List.of(), 1, debugTask(true)));
        assertFalse(TeamAssigner.canApplyAllocation(List.of(1), 1, null));
        assertFalse(TeamAssigner.canApplyAllocation(List.of(1), 1, debugTask(false)));
        assertTrue(TeamAssigner.canApplyAllocation(List.of(1), 4, debugTask(true)));
        assertTrue(TeamAssigner.canApplyAllocation(List.of(2, 2), 2, null));
        assertFalse(TeamAssigner.canApplyAllocation(List.of(3, 1), 2, null));
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
