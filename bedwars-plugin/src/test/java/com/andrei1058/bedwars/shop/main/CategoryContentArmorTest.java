package com.andrei1058.bedwars.shop.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CategoryContentArmorTest {

    @Test
    public void recognizesArmorCategoryEntries() {
        assertTrue(CategoryContent.isArmorCategoryPath("armor-category.category-content.iron-armor"));
        assertTrue(CategoryContent.isArmorCategoryPath("armor-category.category-content.diamond-armor"));
        assertFalse(CategoryContent.isArmorCategoryPath("tools-category.category-content.pickaxe"));
        assertFalse(CategoryContent.isArmorCategoryPath(null));
    }

    @Test
    public void canonicalArmorWeightsKeepDiamondAboveIron() {
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
                "custom-category.category-content.diamond-armor", (byte) 7));
    }

    @Test
    public void doesNotCanonicalizeNearMissPaths() {
        assertFalse(CategoryContent.isArmorCategoryPath(
                "armor-category.category-contentual.diamond-armor"));
        assertEquals(7, CategoryContent.canonicalWeight(
                "armor-category.category-content.diamond-armor-extra", (byte) 7));
        assertEquals(7, CategoryContent.canonicalWeight(
                "armor-category.category-content.DIAMOND-ARMOR", (byte) 7));
    }
}
