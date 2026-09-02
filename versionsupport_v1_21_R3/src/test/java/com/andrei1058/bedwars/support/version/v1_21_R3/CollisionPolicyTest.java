package com.andrei1058.bedwars.support.version.v1_21_R3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollisionPolicyTest {

    @Test
    void doesNotMutateEntityCollidableState() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/andrei1058/bedwars/support/version/v1_21_R3/v1_21_R3.java"))
                .replace("\r\n", "\n");
        int methodStart = source.indexOf("public void setCollide(Player p, IArena arena, boolean value)");
        int methodEnd = source.indexOf("\n    }", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertFalse(method.contains("setCollidable"));
        assertTrue(method.contains("Scoreboard Team"));
    }
}
