package com.andrei1058.bedwars.shop.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SafeSpawnResolver;
import com.andrei1058.bedwars.shop.ShopItemIdentifier;
import com.andrei1058.bedwars.support.paper.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class RecallScrollListener implements Listener {

    static final int CHANNEL_SECONDS = 5;
    static final double PARTICLE_VIEW_RADIUS = 24.0D;
    static final int PARTICLE_COUNT = 8;
    private static final Particle.DustOptions BLUE_DUST =
            new Particle.DustOptions(Color.fromRGB(64, 160, 255), 1.0F);

    private final RecallScrollChannelStore<Channel> channels = new RecallScrollChannelStore<>();
    private BukkitTask ticker;

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!ShopItemIdentifier.matches(item, ShopItemIdentifier.RECALL_SCROLL)) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (channels.isActive(playerId)) {
            showActionBar(player, Messages.RECALL_SCROLL_ALREADY_CHANNELING);
            return;
        }

        IArena arena = Arena.getArenaByPlayer(player);
        ITeam team = validTeam(player, arena);
        if (team == null) {
            showActionBar(player, Messages.RECALL_SCROLL_UNAVAILABLE);
            return;
        }

        Channel channel = new Channel(player, arena, team, new RecallScrollCountdown(CHANNEL_SECONDS));
        if (!channels.start(playerId, channel)) {
            showActionBar(player, Messages.RECALL_SCROLL_ALREADY_CHANNELING);
            return;
        }
        announce(channel);
        spawnParticles(channel);
        ensureTicker();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (channels.remove(event.getPlayer().getUniqueId()) != null) {
            stopTickerIfIdle();
        }
    }

    private void ensureTicker() {
        if (ticker != null && !ticker.isCancelled()) return;
        ticker = Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        Iterator<Channel> iterator = channels.values().iterator();
        while (iterator.hasNext()) {
            Channel channel = iterator.next();
            if (validTeam(channel.player(), channel.arena()) != channel.team()) {
                iterator.remove();
                if (channel.player().isOnline()) {
                    showActionBar(channel.player(), Messages.RECALL_SCROLL_CANCELLED);
                }
                continue;
            }

            ChannelAdvance advance = advanceChannel(channel.countdown(),
                    () -> consumeRecallScroll(channel.player()));
            if (advance != ChannelAdvance.WAITING) {
                iterator.remove();
                if (advance == ChannelAdvance.READY_TO_TELEPORT) {
                    teleport(channel);
                } else if (channel.player().isOnline()) {
                    showActionBar(channel.player(), Messages.RECALL_SCROLL_UNAVAILABLE);
                }
                continue;
            }

            announce(channel);
            spawnParticles(channel);
        }
        stopTickerIfIdle();
    }

    static ChannelAdvance advanceChannel(RecallScrollCountdown countdown, BooleanSupplier consumeScroll) {
        if (!countdown.advance()) return ChannelAdvance.WAITING;
        return consumeScroll.getAsBoolean()
                ? ChannelAdvance.READY_TO_TELEPORT
                : ChannelAdvance.MISSING_SCROLL;
    }

    private static boolean consumeRecallScroll(Player player) {
        boolean consumed = consumeRecallScroll(player.getInventory(),
                item -> ShopItemIdentifier.matches(item, ShopItemIdentifier.RECALL_SCROLL));
        if (consumed) player.updateInventory();
        return consumed;
    }

    static boolean consumeRecallScroll(PlayerInventory inventory, Predicate<ItemStack> isRecallScroll) {
        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack item = storage[slot];
            if (!isConsumableRecallScroll(item, isRecallScroll)) continue;
            consumeFromStorageSlot(inventory, slot, item);
            return true;
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (!isConsumableRecallScroll(offHand, isRecallScroll)) return false;
        if (offHand.getAmount() <= 1) inventory.setItemInOffHand(null);
        else {
            offHand.setAmount(offHand.getAmount() - 1);
            inventory.setItemInOffHand(offHand);
        }
        return true;
    }

    private static boolean isConsumableRecallScroll(ItemStack item, Predicate<ItemStack> isRecallScroll) {
        return item != null && item.getAmount() > 0 && isRecallScroll.test(item);
    }

    private static void consumeFromStorageSlot(PlayerInventory inventory, int slot, ItemStack item) {
        if (item.getAmount() <= 1) inventory.setItem(slot, null);
        else {
            item.setAmount(item.getAmount() - 1);
            inventory.setItem(slot, item);
        }
    }

    private static ITeam validTeam(Player player, IArena arena) {
        if (player == null || arena == null || !player.isOnline() || player.isDead()) return null;
        if (Arena.getArenaByPlayer(player) != arena || arena.getStatus() != GameState.playing) return null;
        if (!arena.isPlayer(player) || arena.isSpectator(player) || arena.isReSpawning(player)) return null;
        ITeam team = arena.getTeam(player);
        if (team == null || team.getSpawn() == null || team.getSpawn().getWorld() == null) return null;
        return team;
    }

    private static void announce(Channel channel) {
        String message = Language.getMsg(channel.player(), Messages.RECALL_SCROLL_COUNTDOWN)
                .replace("{time}", String.valueOf(channel.countdown().secondsRemaining()));
        BedWars.nms.playAction(channel.player(), AdventureText.section(message));
    }

    private static void spawnParticles(Channel channel) {
        Player player = channel.player();
        Location source = player.getLocation();
        Location center = source.clone().add(0.0D, 1.0D, 0.0D);
        for (Player viewer : source.getWorld().getNearbyPlayers(source, PARTICLE_VIEW_RADIUS,
                PARTICLE_VIEW_RADIUS, PARTICLE_VIEW_RADIUS,
                candidate -> candidate != player && Arena.getArenaByPlayer(candidate) == channel.arena())) {
            if (!ConfigManager.isSameWorldWithin(source, viewer.getLocation(), PARTICLE_VIEW_RADIUS)) continue;
            viewer.spawnParticle(Particle.DUST, center, PARTICLE_COUNT,
                    0.45D, 0.8D, 0.45D, 0.0D, BLUE_DUST);
        }
    }

    private static void teleport(Channel channel) {
        Player player = channel.player();
        SafeSpawnResolver.Result destination = SafeSpawnResolver.resolve(channel.team().getSpawn());
        SafeSpawnResolver.applyPose(player, destination.crawling());
        player.setFallDistance(0.0F);
        TeleportManager.teleportC(player, destination.location(), PlayerTeleportEvent.TeleportCause.PLUGIN)
                .whenComplete((success, error) -> runOnMainThread(() -> {
                    if (!player.isOnline()) return;
                    showActionBar(player, error == null && Boolean.TRUE.equals(success)
                            ? Messages.RECALL_SCROLL_COMPLETED
                            : Messages.RECALL_SCROLL_FAILED);
                }));
    }

    private static void runOnMainThread(Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
        } else {
            Bukkit.getScheduler().runTask(BedWars.plugin, action);
        }
    }

    private static void showActionBar(Player player, String messagePath) {
        BedWars.nms.playAction(player, AdventureText.section(Language.getMsg(player, messagePath)));
    }

    private void stopTickerIfIdle() {
        if (!channels.isEmpty() || ticker == null) return;
        ticker.cancel();
        ticker = null;
    }

    private record Channel(Player player, IArena arena, ITeam team, RecallScrollCountdown countdown) {
    }

    enum ChannelAdvance {
        WAITING,
        MISSING_SCROLL,
        READY_TO_TELEPORT
    }
}
