package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.BungeeNodeRole;

/** Entry point for the dedicated arena distribution. */
public final class ArenaBedWarsPlugin extends BedWars {

    @Override
    public BungeeNodeRole getRequiredBungeeNodeRole() {
        return BungeeNodeRole.ARENA;
    }
}
