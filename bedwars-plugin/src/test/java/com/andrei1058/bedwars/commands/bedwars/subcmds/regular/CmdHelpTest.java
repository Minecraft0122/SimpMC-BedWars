package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CmdHelpTest {
    @Test
    void bareCommandExecutesHelpForConsoleWithoutTab() {
        MainCommand main = mock(MainCommand.class, CALLS_REAL_METHODS);
        SubCommand help = mock(SubCommand.class);
        doReturn(help).when(main).getSubCommand("help");
        CommandSender console = mock(org.bukkit.command.ConsoleCommandSender.class);
        assertTrue(main.execute(console, "bw", new String[0]));
        verify(help).execute(argThat(args -> args.length == 0), eq(console));
    }

    @Test
    void consoleHelpIncludesUsageDescriptionsAndPermissionFiltering() {
        ParentCommand parent = mock(ParentCommand.class);
        when(parent.getName()).thenReturn("bw");
        CommandSender sender = mock(org.bukkit.command.ConsoleCommandSender.class);
        SubCommand history = command("history", sender, true);
        SubCommand hidden = command("delArena", sender, false);
        SubCommand setup = command("setSpawn", sender, true);
        when(setup.isArenaSetupCommand()).thenReturn(true);
        when(parent.getSubCommands()).thenReturn(List.of(history, hidden, setup));
        CmdHelp help = new CmdHelp(parent, "help");
        List<String> lines = new ArrayList<>();
        try (var text = mockStatic(AdventureText.class)) {
            text.when(() -> AdventureText.send(eq(sender), anyString())).thenAnswer(call -> {
                lines.add(call.getArgument(1)); return null;
            });
            assertTrue(help.execute(new String[0], sender));
        }
        assertTrue(lines.stream().anyMatch(line -> line.contains("history [玩家名|UUID|all] [页码]") && line.contains("离线")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("setSpawn <队伍名>") && line.contains("设置会话")));
        assertFalse(lines.stream().anyMatch(line -> line.contains("delArena") || line.contains("safemode")));
    }

    @Test
    void everyBuiltInSubcommandHasHelpInsteadOfAnExtensionFallback() {
        String names = "help cmds join leave message lang teleporter gui stats match history record team invite forceStart start setLobby setupArena arenaList delArena enableArena disableArena cloneArena arenaGroup build level reload autoCreateTeams setWaitingSpawn setSpectSpawn createTeam listTeams waitingPos removeTeam setMaxInTeam setMinPlayers setMinInTeam setMaxBuildHeight setSpawn setBed setShop setUpgrade addGenerator removeGenerator setType save npc tp upgradesmenu setKillDrops";
        for (String name : names.split(" ")) assertFalse(CmdHelp.describe(name).description().contains("附属插件"), name);
    }

    private static SubCommand command(String name, CommandSender sender, boolean allowed) {
        SubCommand command = mock(SubCommand.class);
        when(command.getSubCommandName()).thenReturn(name);
        when(command.hasPermission(sender)).thenReturn(allowed);
        return command;
    }
}
