package com.andrei1058.bedwars.listeners;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import net.kyori.adventure.util.TriState;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** 仅在登录/换手时移除旧版内置减速规则，挖掘过程中不改写手持物品。 */
public final class LegacyShearsMigration implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerInventory inventory = event.getPlayer().getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) restoreSlot(inventory, slot);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        restoreSlot(event.getPlayer().getInventory(), event.getNewSlot());
    }

    private static void restoreSlot(PlayerInventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (restore(item)) inventory.setItem(slot, item);
    }

    static boolean restore(ItemStack item) {
        if (item == null || item.getType() != Material.SHEARS) return false;
        Tool tool = item.getData(DataComponentTypes.TOOL);
        if (tool == null) return false;
        var rules = tool.rules().stream().filter(rule -> !isOldRule(rule)).toList();
        if (rules.size() == tool.rules().size()) return false;
        item.setData(DataComponentTypes.TOOL, Tool.tool()
                .defaultMiningSpeed(tool.defaultMiningSpeed()).damagePerBlock(tool.damagePerBlock())
                .canDestroyBlocksInCreative(tool.canDestroyBlocksInCreative()).addRules(rules).build());
        return true;
    }

    private static boolean isOldRule(Tool.Rule rule) {
        var blocks = rule.blocks().values();
        return Float.valueOf(2.4F).equals(rule.speed()) && rule.correctForDrops() == TriState.NOT_SET
                && blocks.size() == 16 && blocks.stream().allMatch(key -> key.key().namespace().equals("minecraft")
                && key.key().value().endsWith("_wool"));
    }
}
