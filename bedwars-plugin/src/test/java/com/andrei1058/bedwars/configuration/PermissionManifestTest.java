package com.andrei1058.bedwars.configuration;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionManifestTest {

    @Test
    void manifestKeepsCompatibilityBundlesWithoutGrantingRestrictedPlayerCommands() throws Exception {
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
                "invite", "teleporter", "upgradesmenu", "party");

        for (String command : common) {
            assertTrue(player.getChildren().getOrDefault("bw.command." + command, false), command);
        }
        assertFalse(player.getChildren().getOrDefault("bw.command.reload", false));
        assertFalse(player.getChildren().getOrDefault("bw.command.setuparena", false));
        assertFalse(player.getChildren().getOrDefault("bw.command.shout", false));
        assertFalse(player.getChildren().getOrDefault("bw.command.rejoin", false));
        assertFalse(allCommands.getChildren().getOrDefault("bw.command.start.debug", false));
        assertFalse(allCommands.getChildren().getOrDefault("bw.command.autodetectgenerators", false));
        assertTrue(allCommands.getChildren().getOrDefault("bw.command.setmininteam", false));
    }

    @Test
    void manifestDeclaresOfficialPermissionNodes() throws Exception {
        File manifest = new File(System.getProperty("basedir"), "src/main/resources/plugin.yml");
        PluginDescriptionFile description;
        try (FileInputStream input = new FileInputStream(manifest)) {
            description = new PluginDescriptionFile(input);
        }
        Set<String> permissions = description.getPermissions().stream()
                .map(Permission::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(permissions.containsAll(Set.of(
                "bw.*", "bw.rejoin", "bw.shout", "bw.forcestart", "bw.tp", "bw.groups",
                "bw.build", "bw.clone", "bw.delete", "bw.disable", "bw.enable", "bw.npc",
                "bw.reload", "bw.setup", "bw.level", "bw.vip", "bw.chatcolor",
                "bw.cmd.bypass", "bw.shout.bypass")));
        assertFalse(permissions.contains("bw.command.start.debug"));
    }

    @Test
    void shoutIsGrantedToOrdinaryPlayersByDefault() throws Exception {
        File manifest = new File(System.getProperty("basedir"), "src/main/resources/plugin.yml");
        PluginDescriptionFile description;
        try (FileInputStream input = new FileInputStream(manifest)) {
            description = new PluginDescriptionFile(input);
        }

        Permission shout = description.getPermissions().stream()
                .filter(permission -> permission.getName().equals("bw.shout"))
                .findFirst().orElseThrow();
        assertTrue(shout.getDefault() == PermissionDefault.TRUE,
                "ordinary players should be able to use /shout without an extra grant");
    }
}
