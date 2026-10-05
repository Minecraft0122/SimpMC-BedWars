package com.andrei1058.bedwars.sidebar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class TeamNameTranslationsTest {
    @Test
    void translatesCanonicalAndLeavesCustomNamesUntouched() {
        assertEquals("红", TeamNameTranslations.chinese("Red"));
        assertEquals("深灰", TeamNameTranslations.chinese("Dark_Gray"));
        assertEquals("Custom", TeamNameTranslations.chinese("Custom"));
    }
}
