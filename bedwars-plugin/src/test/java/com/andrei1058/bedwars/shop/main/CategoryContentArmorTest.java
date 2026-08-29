package com.andrei1058.bedwars.shop.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryContentArmorTest {

    @Test
    void recognizesArmorCategoryEntries() {
        assertTrue(CategoryContent.isArmorCategoryPath("armor-category.category-content.iron-armor"));
        assertTrue(CategoryContent.isArmorCategoryPath("armor-category.category-content.diamond-armor"));
        assertFalse(CategoryContent.isArmorCategoryPath("tools-category.category-content.pickaxe"));
        assertFalse(CategoryContent.isArmorCategoryPath(null));
    }

    @Test
    void canonicalArmorWeightsKeepDiamondAboveIron() {
        assertEquals(0, CategoryContent.canonicalWeight(
                "armor-category.category-content.chainmail", (byte) 8));
        byte iron = CategoryContent.canonicalWeight(
                "armor-category.category-content.iron-armor", (byte) 2);
        byte diamond = CategoryContent.canonicalWeight(
                "armor-category.category-content.diamond-armor", (byte) 1);
        assertEquals(1, iron);
        assertEquals(2, diamond);
        assertTrue(iron < diamond);
        assertFalse(diamond < iron);
        assertEquals(7, CategoryContent.canonicalWeight(
                "custom-category.category-content.custom-armor", (byte) 7));
        assertEquals(7, CategoryContent.canonicalWeight(
                "custom-category.category-content.diamond-armor", (byte) 7));
    }

    @Test
    void doesNotCanonicalizeNearMissPaths() {
        assertFalse(CategoryContent.isArmorCategoryPath(
                "armor-category.category-contentual.diamond-armor"));
        assertEquals(7, CategoryContent.canonicalWeight(
                "armor-category.category-content.diamond-armor-extra", (byte) 7));
        assertEquals(7, CategoryContent.canonicalWeight(
                "armor-category.category-content.DIAMOND-ARMOR", (byte) 7));
    }
}
