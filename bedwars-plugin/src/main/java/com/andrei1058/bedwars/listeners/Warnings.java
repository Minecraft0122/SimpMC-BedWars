package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Warnings implements Listener {
    private final BedWars plugin;

    public Warnings(BedWars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if(!player.isOp()) return;

        if (Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.RED + "[SimpMC-BedWars] 检测到 Multiverse-Core！请移除它，或确保它不会管理 BedWars 地图！");
            }, 5); // run after 5 ticks to make sure its after any update spam on join
        }

        if(Bukkit.getServer().getSpawnRadius() > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.RED + "[SimpMC-BedWars] server.properties 中的 spawn-protection 已启用。"+ChatColor.YELLOW+"这可能影响竞技场！"+ChatColor.GRAY+"强烈建议将其设为 0。");
            }, 5);
        }
    }
}
