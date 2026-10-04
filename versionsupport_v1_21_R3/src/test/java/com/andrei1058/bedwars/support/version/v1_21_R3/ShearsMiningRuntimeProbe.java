package com.andrei1058.bedwars.support.version.v1_21_R3;

import com.andrei1058.bedwars.support.version.common.ShearsMining;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Checks real Paper tool components and native mining speeds on both supported runtimes. */
public final class ShearsMiningRuntimeProbe {
    public static void main(String[] args) throws ReflectiveOperationException {
        try {
            verifyMining();
        } catch (Throwable failure) {
            failure.printStackTrace(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err)));
            throw failure;
        }
    }

    private static void verifyMining() throws ReflectiveOperationException {
        Class.forName("net.minecraft.SharedConstants").getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap").getMethod("bootStrap").invoke(null);
        Class<?> nativeStackClass = Class.forName("net.minecraft.world.item.ItemStack");
        Class<?> craftStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
        Object nativeShears = nativeStackClass.getConstructor(Class.forName("net.minecraft.world.level.ItemLike"))
                .newInstance(Class.forName("net.minecraft.world.item.Items").getField("SHEARS").get(null));
        ItemStack shears = (ItemStack) craftStackClass.getMethod("asCraftMirror", nativeStackClass).invoke(null, nativeShears);
        var originalTool = shears.getData(DataComponentTypes.TOOL);
        float cobwebSpeed = speed(shears, Material.COBWEB);
        if (!ShearsMining.apply(shears) || ShearsMining.apply(shears)) {
            throw new IllegalStateException("Shears tool initialization is not idempotent");
        }
        if (originalTool.damagePerBlock() != shears.getData(DataComponentTypes.TOOL).damagePerBlock()
                || speed(shears, Material.COBWEB) != cobwebSpeed) {
            throw new IllegalStateException("Non-wool shears behavior changed");
        }
        int woolCount = 0;
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.name().endsWith("_WOOL")) continue;
            float progress = speed(shears.clone(), material) / material.getHardness() / 30F;
            float total = 0F;
            int ticks = 0;
            while (total < 1F && ticks < 100) { total += progress; ticks++; }
            if (ticks != 10) throw new IllegalStateException(material + " breaks in " + ticks + " ticks");
            woolCount++;
        }
        if (woolCount != 16) throw new IllegalStateException("Expected all 16 wool colors, found " + woolCount);
        System.out.println("Shears: all 16 wool colors take 10 mining ticks; other rules and clones preserved.");
    }

    private static float speed(ItemStack item, Material material) throws ReflectiveOperationException {
        Object nativeItem = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack")
                .getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
        Object block = Class.forName("org.bukkit.craftbukkit.util.CraftMagicNumbers")
                .getMethod("getBlock", Material.class).invoke(null, material);
        Object state = Class.forName("net.minecraft.world.level.block.Block")
                .getMethod("defaultBlockState").invoke(block);
        return (float) nativeItem.getClass().getMethod("getDestroySpeed",
                Class.forName("net.minecraft.world.level.block.state.BlockState")).invoke(nativeItem, state);
    }
}
