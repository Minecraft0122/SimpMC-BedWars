package com.andrei1058.bedwars.maprestore.internal;

/**
 * Validates world names before they are used as direct children of the server
 * world container or the arena configuration directory.
 */
public final class WorldNameValidator {

    private static final int MAX_LENGTH = 128;

    private WorldNameValidator() {
    }

    public static boolean isSafe(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_LENGTH) return false;
        if (name.equals(".") || name.equals("..")) return false;
        if (name.contains("/") || name.contains("\\") || name.contains(":")) return false;
        return name.chars().noneMatch(character -> Character.isISOControl(character) || character == 0);
    }
}
