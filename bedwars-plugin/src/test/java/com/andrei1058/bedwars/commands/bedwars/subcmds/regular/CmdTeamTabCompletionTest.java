package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CmdTeamTabCompletionTest {

    @Test
    void filtersPlayerNamesByCaseInsensitivePrefixAndSortsThem() {
        assertEquals(List.of("alex", "ALexandra", "Alice"),
                CmdTeam.filterPlayerSuggestions(List.of("ALexandra", "Alice", "Bob", "alex"), "al"));
    }

    @Test
    void emptyPrefixReturnsAllNamesAndNullEntriesAreIgnored() {
        assertEquals(List.of("Alpha", "beta"),
                CmdTeam.filterPlayerSuggestions(Arrays.asList("beta", null, "Alpha"), ""));
    }

    @Test
    void teamCommandDoesNotExposePlayerColorSelection() {
        TestParent parent = new TestParent();
        List<String> commands = new CmdTeam(parent, "team").getTabComplete();

        assertFalse(commands.contains("select"));
        assertFalse(commands.contains("choose"));
        assertFalse(commands.contains("clear"));
        assertFalse(commands.contains("list"));
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
