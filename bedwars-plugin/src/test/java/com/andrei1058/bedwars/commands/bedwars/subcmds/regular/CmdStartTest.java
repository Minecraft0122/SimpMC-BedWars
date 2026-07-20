package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmdStartTest {

    @Test
    void recognizesOnlyTheExactDebugArgument() {
        assertTrue(CmdStart.isDebugStartRequest(new String[]{"debug"}));
        assertTrue(CmdStart.isDebugStartRequest(new String[]{"DeBuG"}));
        assertFalse(CmdStart.isDebugStartRequest(new String[]{}));
        assertFalse(CmdStart.isDebugStartRequest(new String[]{"debug", "extra"}));
        assertFalse(CmdStart.isDebugStartRequest(new String[]{"other"}));
    }

    @Test
    void debugStartRequiresItsDedicatedAdministrativePermission() {
        assertTrue(CmdStart.hasDebugStartPermission(sender(false, "bw.command.start.debug")));
        assertTrue(CmdStart.hasDebugStartPermission(sender(false, "bw.command.*")));
        assertTrue(CmdStart.hasDebugStartPermission(sender(false, "bw.*")));
        assertTrue(CmdStart.hasDebugStartPermission(sender(true)));
        assertFalse(CmdStart.hasDebugStartPermission(sender(false, "bw.command.start")));
    }

    private static CommandSender sender(boolean op, String... permissions) {
        Set<String> granted = Set.of(permissions);
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isOp")) return op;
                    if (method.getName().equals("hasPermission") && args != null && args.length == 1
                            && args[0] instanceof String permission) {
                        return granted.contains(permission);
                    }
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    return null;
                });
    }
}
