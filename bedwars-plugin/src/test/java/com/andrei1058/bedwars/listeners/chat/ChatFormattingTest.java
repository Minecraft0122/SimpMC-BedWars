package com.andrei1058.bedwars.listeners.chat;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
