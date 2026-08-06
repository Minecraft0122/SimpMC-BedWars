package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEndListenerTest {

    @Test
    void removesDroppedItemsEvenWhenNoActivePlayerRemains() {
        RemovalState droppedItem = new RemovalState();
        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getEntities" -> List.of(item(droppedItem));
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IArena arena = (IArena) Proxy.newProxyInstance(
                IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        new GameEndListener().cleanDroppedItems(
                new GameEndEvent(arena, List.of(), List.of(), null, List.of()));

        assertTrue(droppedItem.removed);
    }

    private static Item item(RemovalState state) {
        return (Item) Proxy.newProxyInstance(
                Item.class.getClassLoader(), new Class<?>[]{Item.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "remove" -> {
                        state.removed = true;
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class RemovalState {
        private boolean removed;
    }
}
