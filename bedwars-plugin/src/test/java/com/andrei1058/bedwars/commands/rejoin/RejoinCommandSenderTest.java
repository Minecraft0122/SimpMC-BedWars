package com.andrei1058.bedwars.commands.rejoin;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RejoinCommandSenderTest {

    @Test
    void rejectsAnyNonPlayerSenderWithoutCastingItToPlayer() {
        CommandSender sender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> null);

        RejoinCommand command = new RejoinCommand("rejoin");

        assertDoesNotThrow(() -> assertTrue(command.execute(sender, "rejoin", new String[0])));
    }
}
