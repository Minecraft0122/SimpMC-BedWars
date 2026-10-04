package com.andrei1058.bedwars.support.version.common;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.util.TriState;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/** Native mining progress, shared by shop items and already-owned shears. */
public final class ShearsMining {
    // Wool hardness 0.8 * the vanilla 30 divisor / ten ticks = 2.4.
    // Vanilla haste, fatigue, enchantments, water and airborne modifiers still apply.
    public static final float WOOL_MINING_SPEED = 2.4F;

    private ShearsMining() { }

    public static boolean apply(ItemStack item) {
        if (item == null || item.getType() != Material.SHEARS) return false;
        Tool original = item.getData(DataComponentTypes.TOOL);
        if (original == null) return false;
        RegistryKeySet<BlockType> wool = WoolBlocks.KEYS;
        if (!original.rules().isEmpty()) {
            Tool.Rule first = original.rules().getFirst();
            if (Float.valueOf(WOOL_MINING_SPEED).equals(first.speed())
                    && first.blocks().values().equals(wool.values())) return false;
        }
        item.setData(DataComponentTypes.TOOL, Tool.tool()
                .defaultMiningSpeed(original.defaultMiningSpeed())
                .damagePerBlock(original.damagePerBlock())
                .canDestroyBlocksInCreative(original.canDestroyBlocksInCreative())
                .addRule(Tool.rule(wool, WOOL_MINING_SPEED, TriState.NOT_SET))
                .addRules(original.rules()).build());
        return true;
    }

    private static final class WoolBlocks {
        private static final RegistryKeySet<BlockType> KEYS = RegistrySet.keySet(RegistryKey.BLOCK,
                Arrays.stream(Material.values()).filter(material -> !material.isLegacy()
                                && material.name().endsWith("_WOOL"))
                        .map(material -> TypedKey.create(RegistryKey.BLOCK, material.key())).toList());
    }
}
