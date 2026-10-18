package com.andrei1058.bedwars.listeners.chat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChatFormattingTest {

    @Test
    public void removesSoloTeamPrefixWithoutChangingMessageFormat() {
        assertEquals("Player: {message}", ChatFormatting.withoutSoloTeamPrefix("{team} Player: {message}"));
        assertEquals("Player: {message}", ChatFormatting.withoutSoloTeamPrefix("{team}Player: {message}"));
    }
}
