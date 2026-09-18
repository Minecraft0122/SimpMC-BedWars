package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaPlayerListNamesTest {

    @AfterEach
    void clearNames() {
        ArenaPlayerListNames.clear();
    }

    @Test
    void synchronizeRestoresTeamColorAfterAnotherPluginOverwritesTheName() {
        AtomicReference<Component> listName = new AtomicReference<>(Component.text("[VIP] Alice"));
        Player player = player("Alice", listName);
        IArena arena = arena(player, false, false);
        Component redName = Component.text("Alice", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false);

        ArenaPlayerListNames.synchronize(player, arena);
        listName.set(Component.text("Alice"));
        ArenaPlayerListNames.synchronize(player, arena);

        assertEquals(redName, listName.get());
        ArenaPlayerListNames.release(player);
        assertEquals(Component.text("[VIP] Alice"), listName.get());
    }

    @Test
    void respawningPlayersStayColoredAndItalicWhileSpectatorsAreNeutral() {
        AtomicReference<Component> respawningName = new AtomicReference<>(Component.text("Respawning"));
        AtomicReference<Component> spectatorName = new AtomicReference<>(Component.text("Spectator"));
        Player respawning = player("Respawning", respawningName);
        Player spectator = player("Spectator", spectatorName);
        IArena arena = arena(respawning, true, false);
        IArena spectatorArena = arena(spectator, false, true);

        ArenaPlayerListNames.synchronize(respawning, arena);
        ArenaPlayerListNames.synchronize(spectator, spectatorArena);

        assertEquals(Component.text("Respawning", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true), respawningName.get());
        assertEquals(Component.text("Spectator", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true), spectatorName.get());
    }

    private static IArena arena(Player player, boolean respawning, boolean spectator) {
        ITeam red = (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(), new Class<?>[]{ITeam.class},
                (proxy, method, args) -> method.getName().equals("getColor") ? TeamColor.RED
                        : throwUnsupported(method));
        return (IArena) Proxy.newProxyInstance(
                IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isSpectator" -> spectator;
                    case "isReSpawning" -> respawning;
                    case "getTeam" -> spectator ? null : red;
                    case "getExTeam" -> null;
                    case "getPlayers" -> List.of(player);
                    case "getSpectators" -> spectator ? List.of(player) : List.of();
                    default -> throwUnsupported(method);
                });
    }

    private static Player player(String name, AtomicReference<Component> listName) {
        UUID uniqueId = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUniqueId" -> uniqueId;
                    case "isOnline" -> true;
                    case "playerListName" -> {
                        if (args == null || args.length == 0) yield listName.get();
                        listName.set((Component) args[0]);
                        yield null;
                    }
                    default -> throwUnsupported(method);
                });
    }

    private static UnsupportedOperationException throwUnsupported(java.lang.reflect.Method method) {
        throw new UnsupportedOperationException(method.getName());
    }
}
