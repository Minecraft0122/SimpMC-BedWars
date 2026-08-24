package com.andrei1058.bedwars.support.party;

import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PartiesPromotionTest {

    @Test
    void transfersLeadershipForMembersOfTheSameLocalParty() {
        UUID partyId = UUID.randomUUID();
        Player owner = player(UUID.randomUUID());
        Player target = player(UUID.randomUUID());
        PartyPlayer ownerPlayer = partyPlayer(partyId);
        PartyPlayer targetPlayer = partyPlayer(partyId);
        AtomicReference<PartyPlayer> promoted = new AtomicReference<>();

        PartiesPromotion.promote(api(false, owner, ownerPlayer, target, targetPlayer, promoted), owner, target);

        assertSame(targetPlayer, promoted.get());
    }

    @Test
    void leavesLeadershipToTheProxyInBungeeMode() {
        UUID partyId = UUID.randomUUID();
        Player owner = player(UUID.randomUUID());
        Player target = player(UUID.randomUUID());
        AtomicReference<PartyPlayer> promoted = new AtomicReference<>();

        PartiesPromotion.promote(api(true, owner, partyPlayer(partyId), target, partyPlayer(partyId), promoted),
                owner, target);

        assertNull(promoted.get());
    }

    @Test
    void rejectsPlayersFromDifferentParties() {
        Player owner = player(UUID.randomUUID());
        Player target = player(UUID.randomUUID());
        AtomicReference<PartyPlayer> promoted = new AtomicReference<>();

        PartiesPromotion.promote(api(false, owner, partyPlayer(UUID.randomUUID()), target,
                partyPlayer(UUID.randomUUID()), promoted), owner, target);

        assertNull(promoted.get());
    }

    private static PartiesAPI api(boolean bungee, Player owner, PartyPlayer ownerPlayer,
                                  Player target, PartyPlayer targetPlayer,
                                  AtomicReference<PartyPlayer> promoted) {
        com.alessiodp.parties.api.interfaces.Party party = proxy(
                com.alessiodp.parties.api.interfaces.Party.class,
                (method, args) -> {
                    if (method.equals("changeLeader")) promoted.set((PartyPlayer) args[0]);
                    return defaultValue(method, args);
                });
        return proxy(PartiesAPI.class, (method, args) -> switch (method) {
            case "isBungeeCordEnabled" -> bungee;
            case "getPartyPlayer" -> args[0].equals(owner.getUniqueId()) ? ownerPlayer
                    : args[0].equals(target.getUniqueId()) ? targetPlayer : null;
            case "getParty" -> party;
            default -> defaultValue(method, args);
        });
    }

    private static PartyPlayer partyPlayer(UUID partyId) {
        return proxy(PartyPlayer.class, (method, args) ->
                method.equals("getPartyId") ? partyId : defaultValue(method, args));
    }

    private static Player player(UUID uuid) {
        return proxy(Player.class, (method, args) ->
                method.equals("getUniqueId") ? uuid : defaultValue(method, args));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, MethodHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method.getName(), args));
    }

    private static Object defaultValue(String method, Object[] args) {
        return null;
    }

    @FunctionalInterface
    private interface MethodHandler {
        Object invoke(String method, Object[] args);
    }
}
