package com.andrei1058.bedwars.listeners.chat;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatFormattingTest {

    @Test
    void replacesColonWithWhiteSeparator() {
        assertEquals("name " + ChatColor.WHITE + ">> " + ChatColor.WHITE + "{message}",
                ChatFormatting.withMessageSeparator("name：{message}"));
    }

    @Test
    void doesNotDuplicateExistingSeparator() {
        String format = "name " + ChatColor.WHITE + ">> " + ChatColor.WHITE + "{message}";
        assertEquals(format, ChatFormatting.withMessageSeparator(format));
    }
}
