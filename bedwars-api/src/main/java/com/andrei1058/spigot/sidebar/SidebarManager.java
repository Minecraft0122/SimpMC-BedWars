package com.andrei1058.spigot.sidebar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SidebarManager {

    private static final SidebarManager INSTANCE = new SidebarManager();

    private final Map<Player, String> headerFooterCache = new WeakHashMap<>();
    private JavaPlugin owningPlugin;
    private Constructor<?> packetConstructor;
    private Constructor<?> componentConstructor;
    private Field headerField;
    private Field footerField;
    private Field connectionField;
    private Method getHandleMethod;
    private Method sendPacketMethod;
    private boolean headerFooterAvailable;
    private boolean packetFailureLogged;

    private SidebarManager() {
    }

    public static SidebarManager init() {
        INSTANCE.initialize();
        return INSTANCE;
    }

    private void initialize() {
        try {
            owningPlugin = JavaPlugin.getProvidingPlugin(SidebarManager.class);
        } catch (IllegalArgumentException ignored) {
            owningPlugin = null;
        }

        headerFooterAvailable = false;
        try {
            String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
            String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
            if (!"v1_8_R3".equals(version)) {
                Bukkit.getLogger().warning("TAB header/footer is only available on v1_8_R3.");
                return;
            }

            String nmsPackage = "net.minecraft.server." + version + ".";
            Class<?> packetClass = Class.forName(nmsPackage + "PacketPlayOutPlayerListHeaderFooter");
            Class<?> componentClass = Class.forName(nmsPackage + "ChatComponentText");
            Class<?> packetInterface = Class.forName(nmsPackage + "Packet");
            Class<?> craftPlayerClass = Class.forName(craftPackage + ".entity.CraftPlayer");
            Class<?> entityPlayerClass = Class.forName(nmsPackage + "EntityPlayer");
            Class<?> connectionClass = Class.forName(nmsPackage + "PlayerConnection");

            packetConstructor = packetClass.getConstructor();
            componentConstructor = componentClass.getConstructor(String.class);
            headerField = packetClass.getDeclaredField("a");
            footerField = packetClass.getDeclaredField("b");
            headerField.setAccessible(true);
            footerField.setAccessible(true);
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            connectionField = entityPlayerClass.getField("playerConnection");
            sendPacketMethod = connectionClass.getMethod("sendPacket", packetInterface);
            headerFooterAvailable = true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Bukkit.getLogger().warning("Could not initialize the v1_8_R3 TAB header/footer packet: "
                    + exception.getMessage());
        }
    }

    @NotNull
    public static SidebarManager getInstance() {
        return INSTANCE;
    }

    static void runSync(@NotNull Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        JavaPlugin plugin = INSTANCE.owningPlugin;
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @NotNull
    public Sidebar createSidebar(@NotNull SidebarLine title, @NotNull Collection<SidebarLine> lines,
                                 @NotNull ConcurrentLinkedQueue<PlaceholderProvider> placeholders) {
        return new Sidebar(title, lines, placeholders);
    }

    public void sendHeaderFooter(@NotNull Player player, @NotNull TabHeaderFooter headerFooter) {
        if (!Bukkit.isPrimaryThread()) {
            runSync(() -> sendHeaderFooter(player, headerFooter));
            return;
        }
        if (!headerFooterAvailable || !player.isOnline()) {
            return;
        }
        String header = renderLines(headerFooter.getHeader(), headerFooter.getPlaceholders());
        String footer = renderLines(headerFooter.getFooter(), headerFooter.getPlaceholders());
        String cacheKey = header + '\u0000' + footer;
        if (cacheKey.equals(headerFooterCache.get(player))) {
            return;
        }

        try {
            Object packet = packetConstructor.newInstance();
            headerField.set(packet, componentConstructor.newInstance(header));
            footerField.set(packet, componentConstructor.newInstance(footer));
            Object handle = getHandleMethod.invoke(player);
            Object connection = connectionField.get(handle);
            sendPacketMethod.invoke(connection, packet);
            headerFooterCache.put(player, cacheKey);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!packetFailureLogged) {
                packetFailureLogged = true;
                Bukkit.getLogger().warning("Could not send the v1_8_R3 TAB header/footer packet: "
                        + exception.getMessage());
            }
        }
    }

    public void clearHeaderFooter(@NotNull Player player) {
        clearHeaderFooterCache(player);
        if (headerFooterAvailable && player.isOnline()) {
            sendRawHeaderFooter(player, "", "");
        }
    }

    public void clearHeaderFooterCache(@NotNull Player player) {
        headerFooterCache.remove(player);
    }

    private void sendRawHeaderFooter(@NotNull Player player, @NotNull String header, @NotNull String footer) {
        try {
            Object packet = packetConstructor.newInstance();
            headerField.set(packet, componentConstructor.newInstance(header));
            footerField.set(packet, componentConstructor.newInstance(footer));
            Object handle = getHandleMethod.invoke(player);
            sendPacketMethod.invoke(connectionField.get(handle), packet);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    @NotNull
    private static String renderLines(@NotNull List<SidebarLine> lines,
                                      @NotNull Collection<PlaceholderProvider> placeholders) {
        List<String> rendered = new ArrayList<>(lines.size());
        for (SidebarLine line : lines) {
            rendered.add(Sidebar.renderText(line, placeholders));
        }
        return join(rendered, "\n");
    }

    @NotNull
    private static String join(@NotNull List<String> values, @NotNull String separator) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(value);
        }
        return result.toString();
    }
}
