package com.andrei1058.bedwars.commands.party;

import com.andrei1058.bedwars.api.party.Party;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyCommandTargetTest {

    @Test
    void rejectsMissingTargetWithoutCallingPartyProvider() {
        AtomicBoolean providerCalled = new AtomicBoolean();
        Party party = partyProxy(providerCalled);
        Player owner = playerProxy();

        assertFalse(PartyCommand.isPartyMember(party, owner, null));
        assertFalse(providerCalled.get());
    }

    @Test
    void delegatesOnlineTargetMembershipCheck() {
        AtomicBoolean providerCalled = new AtomicBoolean();
        Party party = partyProxy(providerCalled);

        assertTrue(PartyCommand.isPartyMember(party, playerProxy(), playerProxy()));
        assertTrue(providerCalled.get());
    }

    private static Party partyProxy(AtomicBoolean providerCalled) {
        return proxy(Party.class, (proxy, method, args) -> {
            if (method.getName().equals("isMember")) {
                providerCalled.set(true);
                return true;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static Player playerProxy() {
        return proxy(Player.class, (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        return null;
    }
}
