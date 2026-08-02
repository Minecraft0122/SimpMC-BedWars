package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.api.arena.team.TeamEnchant;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedWarsTeamRespawnTest {

    private static final TeamEnchant TEST_ENCHANTMENT = new TeamEnchant() {
        @Override
        public Enchantment getEnchantment() {
            return null;
        }

        @Override
        public int getAmplifier() {
            return 1;
        }
    };

    @Test
    void appliesAllEnchantmentsWithOneInventorySynchronization() {
        TestItemStack bow = new TestItemStack(Material.BOW, true);
        TestItemStack sword = new TestItemStack(Material.DIAMOND_SWORD, true);
        TestItemStack boots = new TestItemStack(Material.DIAMOND_BOOTS, true);
        TestItemStack unrelated = new TestItemStack(Material.STICK, true);
        AtomicInteger synchronizations = new AtomicInteger();
        Player player = playerWithInventory(
                new ItemStack[]{bow, sword, unrelated},
                new ItemStack[]{boots},
                synchronizations
        );

        boolean changed = BedWarsTeam.applyRespawnEnchantments(
                player,
                List.of(TEST_ENCHANTMENT),
                List.of(TEST_ENCHANTMENT),
                List.of(TEST_ENCHANTMENT),
                item -> item.getType().name().endsWith("_SWORD"),
                item -> item.getType().name().endsWith("_BOOTS")
        );

        assertTrue(changed);
        assertEquals(1, synchronizations.get());
        assertEquals(1, bow.metaUpdates);
        assertEquals(1, sword.metaUpdates);
        assertEquals(1, boots.metaUpdates);
        assertEquals(0, unrelated.metaUpdates);
    }

    @Test
    void skipsSynchronizationWhenEnchantmentsAreAlreadyCurrent() {
        TestItemStack bow = new TestItemStack(Material.BOW, false);
        AtomicInteger synchronizations = new AtomicInteger();
        Player player = playerWithInventory(new ItemStack[]{bow}, new ItemStack[0], synchronizations);

        boolean changed = BedWarsTeam.applyRespawnEnchantments(
                player,
                List.of(TEST_ENCHANTMENT),
                List.of(),
                List.of(),
                item -> false,
                item -> false
        );

        assertFalse(changed);
        assertEquals(0, synchronizations.get());
        assertEquals(0, bow.metaUpdates);
    }

    private static Player playerWithInventory(ItemStack[] contents, ItemStack[] armor,
                                              AtomicInteger synchronizations) {
        PlayerInventory inventory = proxy(PlayerInventory.class, (method, args) -> switch (method) {
            case "getContents" -> contents;
            case "getArmorContents" -> armor;
            default -> null;
        });
        return proxy(Player.class, (method, args) -> switch (method) {
            case "getInventory" -> inventory;
            case "updateInventory" -> {
                synchronizations.incrementAndGet();
                yield null;
            }
            default -> null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    Object value = invocation.invoke(method.getName(), args);
                    if (value != null || !method.getReturnType().isPrimitive()) return value;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == char.class) return '\0';
                    return 0;
                });
    }

    private static final class TestItemStack extends ItemStack {
        private final Material material;
        private final ItemMeta meta;
        private int metaUpdates;

        private TestItemStack(Material material, boolean enchantmentChanges) {
            this.material = material;
            this.meta = proxy(ItemMeta.class, (method, args) ->
                    method.equals("addEnchant") ? enchantmentChanges : null);
        }

        @Override
        public Material getType() {
            return material;
        }

        @Override
        public ItemMeta getItemMeta() {
            return meta;
        }

        @Override
        public boolean setItemMeta(ItemMeta itemMeta) {
            metaUpdates++;
            return true;
        }
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] arguments);
    }
}
