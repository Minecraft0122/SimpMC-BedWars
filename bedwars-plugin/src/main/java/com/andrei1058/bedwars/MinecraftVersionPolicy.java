package com.andrei1058.bedwars;

final class MinecraftVersionPolicy {

    private MinecraftVersionPolicy() {
    }

    static boolean isSupported(String minecraftVersion) {
        return "1.21.11".equals(minecraftVersion);
    }
}
