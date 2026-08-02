package com.andrei1058.spigot.sidebar;

/**
 * Runs in CI with the real patched Paper 1.21.11 server and its libraries on
 * the classpath. This is deliberately a standalone probe: regular unit tests
 * use only paper-api and must remain independent from NMS.
 */
public final class PlayerListDisplayNamePacketsRuntimeProbe {

    private PlayerListDisplayNamePacketsRuntimeProbe() {
    }

    public static void main(String[] args) {
        bootstrapMinecraftRegistries();
        PlayerListDisplayNamePackets.verifyCompatibility();
        if (!PlayerListDisplayNamePackets.isCompatible()) {
            throw new IllegalStateException("Paper 1.21.11 TAB packet bridge is unavailable");
        }
    }

    private static void bootstrapMinecraftRegistries() {
        try {
            Class.forName("net.minecraft.SharedConstants").getMethod("tryDetectVersion").invoke(null);
            Class.forName("net.minecraft.server.Bootstrap").getMethod("bootStrap").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to bootstrap Paper 1.21.11 runtime", exception);
        }
    }
}
