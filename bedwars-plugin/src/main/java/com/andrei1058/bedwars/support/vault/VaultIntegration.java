package com.andrei1058.bedwars.support.vault;

import com.andrei1058.bedwars.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Objects;

/**
 * 根据 Bukkit 服务注册表动态接入 Vault，而不是依赖固定的插件名称或加载时序。
 */
public final class VaultIntegration implements Listener {

    private static final String VAULT_PACKAGE_PREFIX = "net.milkbowl.vault.";

    private final BedWars plugin;
    private Object economyProvider;
    private Object chatProvider;
    private boolean economyInitialized;
    private boolean chatInitialized;
    private boolean refreshQueued;

    public VaultIntegration(BedWars plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * 从当前服务注册表刷新 Vault 适配器。
     */
    public void refresh() {
        refreshEconomy();
        refreshChat();
    }

    private void refreshEconomy() {
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> registration =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        net.milkbowl.vault.economy.Economy provider = registration == null ? null : registration.getProvider();

        if (provider == null) {
            if (!economyInitialized || economyProvider != null) {
                plugin.getLogger().warning("已检测到 Vault API，但没有已注册的经济服务。Vault 只是桥接层，请另行安装支持 Vault 的经济插件。");
            }
            BedWars.setEconomyAdapter(new NoEconomy());
        } else if (!economyInitialized || economyProvider != provider) {
            BedWars.setEconomyAdapter(new WithEconomy(provider));
            plugin.getLogger().info("已接入 Vault 经济服务：" + provider.getName()
                    + "（提供插件：" + registration.getPlugin().getName() + "）。");
        }

        economyProvider = provider;
        economyInitialized = true;
    }

    private void refreshChat() {
        RegisteredServiceProvider<net.milkbowl.vault.chat.Chat> registration =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        net.milkbowl.vault.chat.Chat provider = registration == null ? null : registration.getProvider();

        if (provider == null) {
            if (!chatInitialized || chatProvider != null) {
                plugin.getLogger().info("Vault 未提供聊天前后缀服务，将使用插件默认聊天格式。");
            }
            BedWars.setChatAdapter(new NoChat());
        } else if (!chatInitialized || chatProvider != provider) {
            BedWars.setChatAdapter(new WithChat(provider));
            plugin.getLogger().info("已接入 Vault 聊天服务：" + provider.getName()
                    + "（提供插件：" + registration.getPlugin().getName() + "）。");
        }

        chatProvider = provider;
        chatInitialized = true;
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (isVaultServiceName(event.getProvider().getService().getName())) {
            queueRefresh();
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (isVaultServiceName(event.getProvider().getService().getName())) {
            queueRefresh();
        }
    }

    private void queueRefresh() {
        if (refreshQueued) return;
        refreshQueued = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            refreshQueued = false;
            if (plugin.isEnabled()) refresh();
        });
    }

    static boolean isVaultServiceName(String serviceName) {
        return serviceName != null && serviceName.startsWith(VAULT_PACKAGE_PREFIX);
    }
}
