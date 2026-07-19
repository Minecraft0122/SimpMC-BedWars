package com.andrei1058.bedwars.configuration;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionsTest {

    @Test
    void playerBundleWorksWithoutBukkitChildPermissionExpansion() {
        CommandSender player = sender("bw.player");

        assertTrue(Permissions.hasCommandPermission(player, "join"));
        assertTrue(Permissions.hasCommandPermission(player, "shout", Permissions.PERMISSION_SHOUT_COMMAND));
        assertTrue(Permissions.hasCommandPermission(player, "party"));
        assertFalse(Permissions.hasCommandPermission(player, "reload", Permissions.PERMISSION_RELOAD));
    }

    @Test
    void acceptsExactWildcardAndLegacyPermissions() {
        assertTrue(Permissions.hasCommandPermission(sender("bw.command.reload"), "reload"));
        assertTrue(Permissions.hasCommandPermission(sender("bw.command.*"), "reload"));
        assertTrue(Permissions.hasCommandPermission(sender("bw.reload"), "reload", Permissions.PERMISSION_RELOAD));
        assertFalse(Permissions.hasCommandPermission(sender(), "reload", Permissions.PERMISSION_RELOAD));
    }

    private static CommandSender sender(String... permissions) {
        Set<String> granted = Set.of(permissions);
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class}, (proxy, method, args) -> {
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
