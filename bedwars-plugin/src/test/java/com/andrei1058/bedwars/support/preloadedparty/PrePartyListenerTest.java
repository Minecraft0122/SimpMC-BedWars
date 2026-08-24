package com.andrei1058.bedwars.support.preloadedparty;

import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent;
import com.andrei1058.bedwars.api.events.server.ArenaRestartEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class PrePartyListenerTest {

    @Test
    void legacyLifecycleCallbacksDoNotRemovePlayerKeyedParties() {
        PreLoadedParty party = new PreLoadedParty("party-owner");
        PrePartyListener listener = new PrePartyListener();

        listener.onDisable(new ArenaDisableEvent("arena", "party-owner"));
        assertSame(party, PreLoadedParty.getPartyByOwner("party-owner"));

        listener.onRestart(new ArenaRestartEvent("arena", "party-owner"));
        assertSame(party, PreLoadedParty.getPartyByOwner("party-owner"));

        party.clean();
    }
}
