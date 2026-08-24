package com.andrei1058.bedwars.support.party;

import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

final class PartiesPromotion {

    private PartiesPromotion() {
    }

    static void promote(PartiesAPI api, Player owner, Player target) {
        if (api == null || owner == null || target == null || api.isBungeeCordEnabled()) return;

        PartyPlayer ownerPlayer = api.getPartyPlayer(owner.getUniqueId());
        PartyPlayer targetPlayer = api.getPartyPlayer(target.getUniqueId());
        if (ownerPlayer == null || targetPlayer == null) return;

        UUID partyId = ownerPlayer.getPartyId();
        if (partyId == null || !partyId.equals(targetPlayer.getPartyId())) return;

        com.alessiodp.parties.api.interfaces.Party party = api.getParty(partyId);
        if (party != null) party.changeLeader(targetPlayer);
    }
}
