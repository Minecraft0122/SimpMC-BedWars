package com.andrei1058.spigot.sidebar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Paper 1.21.11 player-list packet bridge. Reflection keeps NMS out of the
 * published API artifact while all reflective members are resolved only once.
 */
final class PlayerListDisplayNamePackets implements PlayerListDisplayNameRenderer {

    private static final PlayerListDisplayNameRenderer INSTANCE = new LazyRenderer();

    private final Method craftPlayerGetHandle;
    private final Field serverPlayerConnection;
    private final Method connectionSend;
    private final Method packetActions;
    private final Method packetEntries;
    private final Method entryProfileId;
    private final Method entryDisplayName;
    private final Constructor<?> snapshotPacket;
    private final Constructor<?> entryConstructor;
    private final Constructor<?> entriesPacket;
    private final EnumSet<?> displayNameAction;
    private boolean packetFailureLogged;

    private PlayerListDisplayNamePackets() throws ReflectiveOperationException {
        Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
        Class<?> serverPlayer = Class.forName("net.minecraft.server.level.ServerPlayer");
        Class<?> packet = Class.forName("net.minecraft.network.protocol.Packet");
        Class<?> playerInfoPacket = Class.forName(
                "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
        Class<?> playerInfoEntry = Class.forName(
                "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
        Class<?> playerInfoAction = Class.forName(
                "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
        Class<?> gameProfile = Class.forName("com.mojang.authlib.GameProfile");
        Class<?> gameType = Class.forName("net.minecraft.world.level.GameType");
        Class<?> component = Class.forName("net.minecraft.network.chat.Component");
        Class<?> chatSessionData = Class.forName("net.minecraft.network.chat.RemoteChatSession$Data");
        Class<?> connection = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
        craftPlayerGetHandle = craftPlayer.getMethod("getHandle");
        serverPlayerConnection = serverPlayer.getField("connection");
        connectionSend = connection.getMethod("send", packet);
        snapshotPacket = playerInfoPacket.getConstructor(EnumSet.class, Collection.class);
        entryConstructor = playerInfoEntry.getConstructor(
                UUID.class, gameProfile, boolean.class, int.class, gameType,
                component, boolean.class, int.class, chatSessionData);
        entriesPacket = playerInfoPacket.getConstructor(EnumSet.class, List.class);
        packetActions = playerInfoPacket.getMethod("actions");
        packetEntries = playerInfoPacket.getMethod("entries");
        entryProfileId = playerInfoEntry.getMethod("profileId");
        entryDisplayName = playerInfoEntry.getMethod("displayName");
        displayNameAction = displayNameAction(playerInfoAction);
        verifyPacketConstruction();
    }

    static @NotNull PlayerListDisplayNameRenderer instance() {
        return INSTANCE;
    }

    static void verifyCompatibility() {
        if (INSTANCE instanceof LazyRenderer lazyRenderer) lazyRenderer.delegate();
    }

    static boolean isCompatible() {
        PlayerListDisplayNameRenderer renderer = INSTANCE instanceof LazyRenderer lazyRenderer
                ? lazyRenderer.delegate()
                : INSTANCE;
        return renderer != UnavailableRenderer.INSTANCE;
    }

    @Override
    public boolean clear(@NotNull Player viewer, @NotNull Collection<Player> targets) {
        if (targets.isEmpty()) return true;
        try {
            ArrayList<Object> entries = new ArrayList<>(targets.size());
            for (Player target : targets) {
                // A null PlayerInfo display name makes the 1.21.11 client call
                // PlayerTeam.formatNameForTeam. Non-null names bypass the
                // scoreboard team and split TAB from the overhead name tag.
                entries.add(entryConstructor.newInstance(
                        target.getUniqueId(), null, false, 0, null,
                        null, false, 0, null));
            }
            send(viewer, entriesPacket.newInstance(displayNameAction, entries));
            return true;
        } catch (ReflectiveOperationException exception) {
            logPacketFailure(viewer, exception);
            return false;
        }
    }

    @Override
    public boolean restore(@NotNull Player viewer, @NotNull Collection<Player> targets) {
        if (targets.isEmpty()) return true;
        try {
            ArrayList<Object> targetHandles = new ArrayList<>(targets.size());
            for (Player target : targets) targetHandles.add(craftPlayerGetHandle.invoke(target));
            // The normal packet constructor reads Paper's current nullable
            // player-list name, so third-party display names are restored too.
            Object packet = snapshotPacket.newInstance(displayNameAction, targetHandles);
            send(viewer, packet);
            return true;
        } catch (ReflectiveOperationException exception) {
            logPacketFailure(viewer, exception);
            return false;
        }
    }

    private void send(Player viewer, Object packet) throws ReflectiveOperationException {
        Object viewerHandle = craftPlayerGetHandle.invoke(viewer);
        Object connection = serverPlayerConnection.get(viewerHandle);
        connectionSend.invoke(connection, packet);
    }

    /**
     * Construct and inspect the exact packet shape during plugin startup. Merely
     * resolving reflective members would not catch a constructor whose fields
     * no longer represent UPDATE_DISPLAY_NAME in the supported Paper build.
     */
    private void verifyPacketConstruction() throws ReflectiveOperationException {
        UUID expectedId = new UUID(0L, 1L);
        Object entry = entryConstructor.newInstance(
                expectedId, null, false, 0, null, null, false, 0, null);
        Object packet = entriesPacket.newInstance(displayNameAction, List.of(entry));

        Object actions = packetActions.invoke(packet);
        Object entries = packetEntries.invoke(packet);
        if (!(actions instanceof Collection<?> packetActionValues)
                || !packetActionValues.containsAll(displayNameAction)
                || !(entries instanceof List<?> packetEntryValues)
                || packetEntryValues.size() != 1
                || !expectedId.equals(entryProfileId.invoke(packetEntryValues.getFirst()))
                || entryDisplayName.invoke(packetEntryValues.getFirst()) != null) {
            throw new ReflectiveOperationException(
                    "Paper 1.21.11 PlayerInfo UPDATE_DISPLAY_NAME packet contract changed");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EnumSet<?> displayNameAction(Class<?> actionType) {
        Class<? extends Enum> enumType = actionType.asSubclass(Enum.class);
        EnumSet actions = EnumSet.noneOf(enumType);
        actions.add(Enum.valueOf(enumType, "UPDATE_DISPLAY_NAME"));
        return actions;
    }

    private void logPacketFailure(Player viewer, Exception cause) {
        if (packetFailureLogged) return;
        packetFailureLogged = true;
        Bukkit.getLogger().log(Level.SEVERE, "SimpMC-BedWars 无法更新 " + viewer.getName()
                + " 看到的 TAB 名称；已保留 scoreboard 降级显示。", cause);
    }

    private static final class LazyRenderer implements PlayerListDisplayNameRenderer {
        private volatile PlayerListDisplayNameRenderer delegate;

        @Override
        public boolean clear(@NotNull Player viewer, @NotNull Collection<Player> targets) {
            return delegate().clear(viewer, targets);
        }

        @Override
        public boolean restore(@NotNull Player viewer, @NotNull Collection<Player> targets) {
            return delegate().restore(viewer, targets);
        }

        private PlayerListDisplayNameRenderer delegate() {
            PlayerListDisplayNameRenderer current = delegate;
            if (current != null) return current;
            synchronized (this) {
                if (delegate == null) delegate = createRenderer();
                return delegate;
            }
        }

        private static PlayerListDisplayNameRenderer createRenderer() {
            try {
                return new PlayerListDisplayNamePackets();
            } catch (ReflectiveOperationException exception) {
                Bukkit.getLogger().log(Level.SEVERE,
                        "SimpMC-BedWars 无法初始化 Paper 1.21.11 TAB 数据包支持；玩家名颜色不会生效。",
                        exception);
                return UnavailableRenderer.INSTANCE;
            }
        }
    }

    private enum UnavailableRenderer implements PlayerListDisplayNameRenderer {
        INSTANCE;

        @Override
        public boolean clear(@NotNull Player viewer, @NotNull Collection<Player> targets) {
            return false;
        }

        @Override
        public boolean restore(@NotNull Player viewer, @NotNull Collection<Player> targets) {
            return false;
        }
    }
}
