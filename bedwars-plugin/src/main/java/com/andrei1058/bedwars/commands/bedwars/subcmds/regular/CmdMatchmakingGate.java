package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.MatchStartGate;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.language.MatchmakingMessages;
import org.bukkit.command.CommandSender;

/** Admin command for pausing or resuming new match starts on this server. */
public final class CmdMatchmakingGate extends SubCommand {
    private final Action action;

    public CmdMatchmakingGate(ParentCommand parent, String name, Action action) {
        super(parent, name);
        this.action = action;
        setPriority(12);
        showInList(true);
        String description = action == Action.STOP
                ? "暂停本服务器新对局开局；已开始的对局继续进行。"
                : "恢复本服务器新对局开局。";
        setDisplayInfo(MainCommand.createTC("§6 ▪ §7/" + MainCommand.getInstance().getName()
                        + " " + getSubCommandName() + " §8 - §e" + description,
                "/" + getParent().getName() + " " + getSubCommandName(),
                "§f" + description + "\n§f权限：§cbw.command." + getSubCommandName().toLowerCase()));
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (args.length != 0) return false;

        boolean changed = action == Action.STOP ? MatchStartGate.stop() : MatchStartGate.resume();
        if (changed && action == Action.STOP) {
            for (var arena : Arena.getArenas()) {
                if (arena.getStatus() == GameState.starting) arena.changeStatus(GameState.waiting);
            }
        } else if (changed) {
            for (var arena : Arena.getArenas()) {
                if (arena instanceof Arena concreteArena) concreteArena.reevaluateStartEligibility();
            }
        }

        MatchmakingMessages.Key message;
        if (action == Action.STOP) {
            message = changed ? MatchmakingMessages.Key.MATCHES_BLOCKED
                    : MatchmakingMessages.Key.MATCHES_ALREADY_BLOCKED;
        } else {
            message = changed ? MatchmakingMessages.Key.MATCHES_RESUMED
                    : MatchmakingMessages.Key.MATCHES_ALREADY_RESUMED;
        }
        AdventureText.send(sender, MatchmakingMessages.forSender(sender).text(message));
        return true;
    }

    @Override
    public java.util.List<String> getTabComplete() {
        return java.util.List.of();
    }

    public enum Action { STOP, RESUME }
}
