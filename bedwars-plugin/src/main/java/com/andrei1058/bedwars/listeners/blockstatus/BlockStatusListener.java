/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.listeners.blockstatus;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.server.ArenaEnableEvent;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BlockStatusListener implements Listener {

    @EventHandler
    public void onArenaEnable(ArenaEnableEvent e) {
        if (e == null) return;
        updateBlock((Arena) e.getArena());
    }

    @EventHandler
    public void onStatusChange(GameStateChangeEvent e) {
        if (e == null) return;
        updateBlock((Arena) e.getArena());
    }

    /**
     * Update sign block
     */
    public static void updateBlock(Arena a) {
        updateBlock(a, null);
    }

    /** Update only join signs whose chunk is already loaded. */
    public static void updateBlock(Arena a, Chunk loadedChunk) {
        if (a == null) return;
        Iterable<Block> candidates = loadedChunk == null ? a.getSigns() : a.getSignsNearChunk(loadedChunk);
        for (Block s : candidates) {
            if (!canReadSign(s, loadedChunk)) continue;
            if (!(s.getState() instanceof Sign sign)) continue;
            if (!canUpdateSignBackground(sign, loadedChunk)) continue;
            String path = switch (a.getStatus()) {
                case waiting -> ConfigPath.SIGNS_STATUS_BLOCK_WAITING_MATERIAL;
                case playing -> ConfigPath.SIGNS_STATUS_BLOCK_PLAYING_MATERIAL;
                case starting -> ConfigPath.SIGNS_STATUS_BLOCK_STARTING_MATERIAL;
                case restarting -> ConfigPath.SIGNS_STATUS_BLOCK_RESTARTING_MATERIAL;
            };
            BedWars.nms.setJoinSignBackground(sign, Material.valueOf(BedWars.signs.getString(path)));
        }
    }

    /**
     * Reading a block state can synchronously load its region. Population
     * updates therefore skip unloaded sign chunks and let ChunkLoad replay the
     * latest arena state later.
     */
    public static boolean canReadSign(Block sign, Chunk loadedChunk) {
        if (sign == null) return false;
        World world = sign.getWorld();
        int chunkX = sign.getX() >> 4;
        int chunkZ = sign.getZ() >> 4;
        if (loadedChunk != null) {
            if (loadedChunk.getWorld() != world) return false;
            int chunkDistance = Math.abs(loadedChunk.getX() - chunkX)
                    + Math.abs(loadedChunk.getZ() - chunkZ);
            if (chunkDistance > 1) return false;
        }
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    /**
     * A wall sign background lives in the block behind the sign. At a chunk
     * border that support block may be in a different chunk, so never call the
     * version bridge until both chunks are already available. The support
     * chunk load event is also accepted as the replay trigger.
     */
    public static boolean canUpdateSignBackground(Sign sign, Chunk loadedChunk) {
        if (!(sign.getBlockData() instanceof WallSign wallSign)) return false;
        Block signBlock = sign.getBlock();
        World world = signBlock.getWorld();
        int signChunkX = signBlock.getX() >> 4;
        int signChunkZ = signBlock.getZ() >> 4;
        int supportX = signBlock.getX() + wallSign.getFacing().getOppositeFace().getModX();
        int supportZ = signBlock.getZ() + wallSign.getFacing().getOppositeFace().getModZ();
        int supportChunkX = supportX >> 4;
        int supportChunkZ = supportZ >> 4;
        if (!world.isChunkLoaded(signChunkX, signChunkZ)
                || !world.isChunkLoaded(supportChunkX, supportChunkZ)) return false;
        if (loadedChunk == null) return true;
        if (loadedChunk.getWorld() != world) return false;
        return loadedChunk.getX() == signChunkX && loadedChunk.getZ() == signChunkZ
                || loadedChunk.getX() == supportChunkX && loadedChunk.getZ() == supportChunkZ;
    }
}
