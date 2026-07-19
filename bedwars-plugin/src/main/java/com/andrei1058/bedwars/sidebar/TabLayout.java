package com.andrei1058.bedwars.sidebar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Keeps TAB header/footer readable without restoring the old oversized spacer. */
final class TabLayout {

    static final int MINIMUM_VISIBLE_WIDTH = 30;
    static final String MINIMUM_WIDTH_LINE = "&8&m------------------------------";

    private TabLayout() {
    }

    static @NotNull List<String> ensureMinimumHeaderWidth(@NotNull Collection<String> header,
                                                           @NotNull Collection<String> footer) {
        List<String> result = new ArrayList<>(header);
        int widest = 0;
        for (String line : header) widest = Math.max(widest, visibleWidth(line));
        for (String line : footer) widest = Math.max(widest, visibleWidth(line));
        if (widest < MINIMUM_VISIBLE_WIDTH) result.add(MINIMUM_WIDTH_LINE);
        return result;
    }

    static int visibleWidth(String line) {
        if (line == null || line.isEmpty()) return 0;
        int width = 0;
        for (int index = 0; index < line.length(); ) {
            char current = line.charAt(index);
            if (current == '&' && index + 1 < line.length() && isLegacyColorCode(line.charAt(index + 1))) {
                index += 2;
                continue;
            }
            int codePoint = line.codePointAt(index);
            width += isWideCodePoint(codePoint) ? 2 : 1;
            index += Character.charCount(codePoint);
        }
        return width;
    }

    private static boolean isLegacyColorCode(char value) {
        char code = Character.toLowerCase(value);
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')
                || (code >= 'k' && code <= 'o') || code == 'r' || code == 'x';
    }

    private static boolean isWideCodePoint(int codePoint) {
        return (codePoint >= 0x2E80 && codePoint <= 0x9FFF)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0xFF01 && codePoint <= 0xFF60);
    }
}
