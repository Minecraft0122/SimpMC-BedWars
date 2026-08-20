package com.andrei1058.bedwars.api.language;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageFileNameTest {

    @Test
    void recognizesTheBundledChineseLanguageFile() {
        assertEquals(Optional.of("zh_cn"), Language.isoFromFileName("messages_zh_cn.yml"));
    }

    @Test
    void rejectsMigrationBackupsAndUnrelatedFiles() {
        assertTrue(Language.isoFromFileName("messages_zh_cn.yml.v1.bak").isEmpty());
        assertTrue(Language.isoFromFileName("messages_zh_cn.v2.bak.yml").isEmpty());
        assertTrue(Language.isoFromFileName("zh_cn.yml").isEmpty());
    }

    @Test
    void onlySimplifiedChineseIsSelectableForPlayers() {
        assertTrue(Language.isSimplifiedChineseIso("zh_cn"));
        assertTrue(Language.isSimplifiedChineseIso("ZH_CN"));
        assertFalse(Language.isSimplifiedChineseIso("en"));
        assertFalse(Language.isSimplifiedChineseIso(null));
        assertFalse(Language.isLanguageExist("en"));
    }
}
