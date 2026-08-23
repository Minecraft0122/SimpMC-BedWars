package com.andrei1058.bedwars;

final class MinecraftVersionPolicy {

    private MinecraftVersionPolicy() {
    }

    static boolean isSupported(String minecraftVersion) {
        return "1.21.11".equals(minecraftVersion)
                || "26.2".equals(minecraftVersion)
                || minecraftVersion != null && minecraftVersion.matches("26\\.2\\.build\\.[0-9]+");
    }
}
