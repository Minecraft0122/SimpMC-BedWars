package com.andrei1058.bedwars.support.version.v1_8_R3;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollisionApiContractTest {

    @Test
    public void collisionStateIsWrittenOnlyThroughTheSpigotApi() throws IOException {
        String source = read("src/main/java/com/andrei1058/bedwars/support/version/v1_8_R3/v1_8_R3.java");

        assertTrue(source.contains("p.spigot().setCollidesWithEntities(value);"));
        assertFalse(source.contains("collidesWithEntities ="));
        assertFalse(Files.exists(Paths.get(
                "src/main/java/com/andrei1058/bedwars/support/version/v1_8_R3/LegacyArrowListener.java")));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
