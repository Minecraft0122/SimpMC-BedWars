package com.andrei1058.bedwars.sidebar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TabColorFallbackTest {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @AfterEach
    void clearFallbacks() {
        TabColorFallback.clear();
    }

    @Test
    void claimAppliesTeamColorAndReleaseRestoresTheOriginalName() {
        Component original = Component.text("[VIP] Alice");
        AtomicReference<Component> listName = new AtomicReference<>(original);
        Player player = player("Alice", listName);
        UUID owner = UUID.nameUUIDFromBytes("viewer".getBytes(StandardCharsets.UTF_8));

        TabColorFallback.claim(owner, player, ChatColor.RED);

        Component expected = LEGACY.deserialize("§cAlice");
        assertEquals(expected, listName.get());
        assertNotEquals(original, listName.get());

        TabColorFallback.release(owner, player);
        assertEquals(original, listName.get());
    }

    @Test
    void fallbackRemainsUntilTheLastViewerReleasesTheRow() {
        Component original = Component.text("Alice");
        AtomicReference<Component> listName = new AtomicReference<>(original);
        Player player = player("Alice", listName);
        UUID firstViewer = UUID.nameUUIDFromBytes("first".getBytes(StandardCharsets.UTF_8));
        UUID secondViewer = UUID.nameUUIDFromBytes("second".getBytes(StandardCharsets.UTF_8));

        TabColorFallback.claim(firstViewer, player, ChatColor.BLUE);
        TabColorFallback.claim(secondViewer, player, ChatColor.BLUE);
        TabColorFallback.release(firstViewer, player);
        assertEquals(LEGACY.deserialize("§9Alice"), listName.get());

        TabColorFallback.release(secondViewer, player);
        assertEquals(original, listName.get());
    }

    @Test
    void reconnectUsesTheCurrentPlayerObjectForTheFallback() {
        Component firstOriginal = Component.text("Alice");
        Component secondOriginal = Component.text("Alice (new connection)");
        AtomicReference<Component> firstName = new AtomicReference<>(firstOriginal);
        AtomicReference<Component> secondName = new AtomicReference<>(secondOriginal);
        Player firstConnection = player("Alice", firstName);
        Player secondConnection = player("Alice", secondName);
        UUID owner = UUID.nameUUIDFromBytes("reconnect-viewer".getBytes(StandardCharsets.UTF_8));

        TabColorFallback.claim(owner, firstConnection, ChatColor.GREEN);
        TabColorFallback.claim(owner, secondConnection, ChatColor.GREEN);

        assertEquals(LEGACY.deserialize("§aAlice"), secondName.get());
        assertEquals(firstOriginal, firstName.get());

        TabColorFallback.release(owner, secondConnection);
        assertEquals(secondOriginal, secondName.get());
    }

    private static Player player(String name, AtomicReference<Component> listName) {
        UUID uniqueId = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUniqueId" -> uniqueId;
                    case "playerListName" -> {
                        if (args == null || args.length == 0) yield listName.get();
                        listName.set((Component) args[0]);
                        yield null;
                    }
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
