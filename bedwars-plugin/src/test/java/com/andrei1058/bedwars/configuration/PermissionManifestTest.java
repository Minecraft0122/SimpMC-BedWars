package com.andrei1058.bedwars.configuration;

import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionManifestTest {

    @Test
    void playerBundleContainsEveryCommonCommandButNoAdminCommand() throws Exception {
        File manifest = new File(System.getProperty("basedir"), "src/main/resources/plugin.yml");
        PluginDescriptionFile description;
        try (FileInputStream input = new FileInputStream(manifest)) {
            description = new PluginDescriptionFile(input);
        }
        Permission player = description.getPermissions().stream()
                .filter(permission -> permission.getName().equals("bw.player"))
                .findFirst().orElseThrow();
        Permission allCommands = description.getPermissions().stream()
                .filter(permission -> permission.getName().equals("bw.command.*"))
                .findFirst().orElseThrow();
        Set<String> common = Set.of("help", "cmds", "join", "leave", "lang", "gui", "stats", "team",
                "invite", "teleporter", "upgradesmenu", "shout", "rejoin", "party");

        for (String command : common) {
            assertTrue(player.getChildren().getOrDefault("bw.command." + command, false), command);
        }
        assertFalse(player.getChildren().getOrDefault("bw.command.reload", false));
        assertFalse(player.getChildren().getOrDefault("bw.command.setuparena", false));
        assertTrue(allCommands.getChildren().getOrDefault("bw.command.start.debug", false));
    }
}
