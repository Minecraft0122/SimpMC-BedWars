package com.andrei1058.bedwars.listeners.chat;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatFormattingTest {

    @Test
    void replacesColonWithWhiteSeparator() {
        assertEquals("name " + ChatColor.WHITE + "> " + ChatColor.GRAY + "{message}",
                ChatFormatting.withMessageSeparator("name：{message}"));
    }

    @Test
    void keepsCanonicalSeparatorIdempotent() {
        String format = "name " + ChatColor.WHITE + "> " + ChatColor.GRAY + "{message}";
        assertEquals(format, ChatFormatting.withMessageSeparator(format));
    }

    @Test
    void migratesLegacyDoubleSeparatorAtRuntime() {
        String legacy = "name " + ChatColor.WHITE + ">> " + ChatColor.WHITE + "{message}";
        assertEquals("name " + ChatColor.WHITE + "> " + ChatColor.GRAY + "{message}",
                ChatFormatting.withMessageSeparator(legacy));
    }

    @Test
    void inlineShoutUsesTheSameOfficialPermissionAsTheCommand() {
        assertFalse(ChatFormatting.hasShoutPermission(sender()));
        assertFalse(ChatFormatting.hasShoutPermission(sender("bw.player")));
        assertTrue(ChatFormatting.hasShoutPermission(sender("bw.shout")));
        assertTrue(ChatFormatting.hasShoutPermission(sender("bw.command.shout")));
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
