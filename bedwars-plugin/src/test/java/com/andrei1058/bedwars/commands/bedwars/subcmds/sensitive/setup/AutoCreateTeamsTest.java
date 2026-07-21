package com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoCreateTeamsTest {

    @Test
    void countsActualNearbyBlocksInsteadOfRepeatingTheCenterBlock() {
        Block isolated = centerBlock(Material.GREEN_WOOL, Set.of("0,0,0"));
        Block cluster = centerBlock(Material.GREEN_WOOL,
                Set.of("0,0,0", "1,0,0", "-1,0,0", "0,1,0", "0,0,1"));

        assertEquals(1, AutoCreateTeams.countNearbyWool(isolated, Material.GREEN_WOOL));
        assertEquals(5, AutoCreateTeams.countNearbyWool(cluster, Material.GREEN_WOOL));
        assertEquals(0, AutoCreateTeams.countNearbyWool(cluster, Material.LIME_WOOL));
    }

    private static Block centerBlock(Material wool, Set<String> woolOffsets) {
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getRelative") && args != null && args.length == 3) {
                        String offset = args[0] + "," + args[1] + "," + args[2];
                        Material type = woolOffsets.contains(offset) ? wool : Material.AIR;
                        return typedBlock(type);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Block typedBlock(Material type) {
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> method.getName().equals("getType")
                        ? type : defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
