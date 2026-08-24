package com.andrei1058.bedwars.api;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.party.Party;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OptionalApiHooksTest {

    @Test
    void partyPromotionDefaultIsSafeForProvidersWithoutPromotionSupport() {
        Party party = proxy(Party.class);
        Player owner = playerProxy();
        Player target = playerProxy();

        assertDoesNotThrow(() -> party.promote(owner, target));
    }

    @Test
    void teamBedDestroyDefaultIsSafeForThirdPartyTeams() {
        ITeam team = proxy(ITeam.class);

        assertDoesNotThrow(() -> team.onBedDestroy((Location) null));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player playerProxy() {
        return proxy(Player.class);
    }
}
