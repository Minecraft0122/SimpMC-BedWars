package com.andrei1058.bedwars.api.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubCommandPermissionTest {

    @Test
    void assignsAStablePermissionToEverySubCommand() {
        TestCommand command = new TestCommand(new TestParent(), "SetLobby");

        assertEquals("bw.command.setlobby", command.getCommandPermission());
        assertFalse(command.hasPermission(sender()));
        assertTrue(command.hasPermission(sender("bw.command.setlobby")));
        assertTrue(command.hasPermission(sender("bw.command.*")));
    }

    @Test
    void officialPermissionFreeCommandsWorkWithoutAnyGrant() {
        assertTrue(new TestCommand(new TestParent(), "join").hasPermission(sender()));
        assertTrue(new TestCommand(new TestParent(), "party").hasPermission(sender()));
        assertTrue(new TestCommand(new TestParent(), "arenaList").hasPermission(sender()));
        assertFalse(new TestCommand(new TestParent(), "shout").hasPermission(sender()));
        assertFalse(new TestCommand(new TestParent(), "rejoin").hasPermission(sender()));
    }

    @Test
    void playerBundleDoesNotBypassOfficialRestrictedCommands() {
        assertTrue(new TestCommand(new TestParent(), "join").hasPermission(sender("bw.player")));
        assertFalse(new TestCommand(new TestParent(), "shout").hasPermission(sender("bw.player")));
        assertFalse(new TestCommand(new TestParent(), "rejoin").hasPermission(sender("bw.player")));
        assertFalse(new TestCommand(new TestParent(), "reload").hasPermission(sender("bw.player")));
    }

    @Test
    void legacyPermissionsRemainValidAlongsideTheNewNode() {
        TestCommand command = new TestCommand(new TestParent(), "setupArena");
        command.setPermission("bw.setup");

        assertTrue(command.hasPermission(sender("bw.setup")));
        assertTrue(command.hasPermission(sender("bw.command.setuparena")));
    }

    @Test
    void acceptsAdditionalPermissionAliases() {
        TestCommand command = new TestCommand(new TestParent(), "enableArena");
        command.setPermission("bw.enable");
        command.addPermission("bw.enableRotation");

        assertTrue(command.hasPermission(sender("bw.enable")));
        assertTrue(command.hasPermission(sender("bw.enableRotation")));
        assertFalse(command.hasPermission(sender()));
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

    private static final class TestCommand extends SubCommand {
        private TestCommand(ParentCommand parent, String name) {
            super(parent, name);
        }

        @Override
        public boolean execute(String[] args, CommandSender sender) {
            return true;
        }

        @Override
        public List<String> getTabComplete() {
            return List.of();
        }
    }

    private static final class TestParent implements ParentCommand {
        private final List<SubCommand> commands = new ArrayList<>();

        @Override
        public boolean hasSubCommand(String name) {
            return commands.stream().anyMatch(command -> command.getSubCommandName().equalsIgnoreCase(name));
        }

        @Override
        public void addSubCommand(SubCommand subCommand) {
            commands.add(subCommand);
        }

        @Override
        public void sendSubCommands(Player player) {
        }

        @Override
        public List<SubCommand> getSubCommands() {
            return commands;
        }

        @Override
        public String getName() {
            return "bw";
        }
    }
}
