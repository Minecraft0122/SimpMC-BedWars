package com.andrei1058.bedwars.shop;

import com.andrei1058.bedwars.BedWars;
import org.bukkit.inventory.ItemStack;

public final class ShopItemIdentifier {

    public static final String RECALL_SCROLL = "recall-scroll";
    public static final String SELF_RESCUE_PLATFORM = "self-rescue-platform";
    private static final String TAG_KEY = "shop-item-id";

    private ShopItemIdentifier() {
    }

    public static ItemStack mark(ItemStack itemStack, String identifier) {
        return BedWars.nms.setTag(itemStack, TAG_KEY, identifier);
    }

    public static boolean matches(ItemStack itemStack, String identifier) {
        return itemStack != null && identifier.equals(BedWars.nms.getTag(itemStack, TAG_KEY));
    }
}
