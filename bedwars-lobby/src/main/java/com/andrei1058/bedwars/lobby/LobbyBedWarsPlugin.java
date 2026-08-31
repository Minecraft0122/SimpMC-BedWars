package com.andrei1058.bedwars.lobby;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.BungeeNodeRole;

/** Entry point for the dedicated lobby distribution. */
public final class LobbyBedWarsPlugin extends BedWars {

    @Override
    public BungeeNodeRole getRequiredBungeeNodeRole() {
        return BungeeNodeRole.LOBBY;
    }
}
