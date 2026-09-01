package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RejoinWithoutRespawnContractTest {

    @Test
    void arenaUsesTheDirectTeamRestorePath() throws IOException {
        String arena = source("arena/Arena.java");
        String team = source("arena/team/BedWarsTeam.java");

        assertTrue(arena.contains("reJoin.getBwt().reJoin(p);"));
        assertFalse(arena.contains("reJoin.getBwt().reJoin(p, ev.getRespawnTime());"));
        assertTrue(team.contains("public void reJoin(@NotNull Player p) {\n        addPlayers(p);\n        activateMember(p, false);"));
        assertFalse(team.contains("arena.startReSpawnSession(p, respawnTime);"));
        assertTrue(team.contains("public void respawnMember(@NotNull Player p) {\n        activateMember(p, true);"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/andrei1058/bedwars").resolve(relativePath))
                .replace("\r\n", "\n");
    }
}
