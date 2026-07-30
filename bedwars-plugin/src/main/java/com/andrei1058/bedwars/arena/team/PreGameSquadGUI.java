package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GUI entry point for the pre-game teammate invitation system. */
public final class PreGameSquadGUI implements Listener {

    private static final PreGameSquadGUI INSTANCE = new PreGameSquadGUI();
    private static final String TITLE = ChatColor.DARK_GRAY + "开局组队";
    private static final int INVENTORY_SIZE = 54;
    private static final int[] MEMBER_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    private final PreGameSquadManager squads = PreGameSquadManager.getInstance();

    private PreGameSquadGUI() {
    }

    public static PreGameSquadGUI getInstance() {
        return INSTANCE;
    }

    public void open(@NotNull Player player) {
        IArena arena = preGameArena(player);
        if (arena == null) {
            player.sendMessage(ChatColor.GOLD + "[组队] " + ChatColor.RED + "只能在开局前打开组队界面。");
            return;
        }
        if (arena.getMaxInTeam() <= 1) {
            player.sendMessage(ChatColor.GOLD + "[组队] " + ChatColor.YELLOW + "当前地图为单人队模式，无需选择队友。");
            return;
        }

        SquadHolder holder = new SquadHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, TITLE);
        holder.inventory = inventory;
        renderHeader(inventory, player, arena);
        renderActions(inventory, holder, player);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof SquadHolder holder)) return;
        event.setCancelled(true);
        if (!holder.viewer.equals(player.getUniqueId())) return;

        SlotAction action = holder.actions.get(event.getRawSlot());
        if (action == null) return;
        if (action.close) {
            player.closeInventory();
            return;
        }

        String command = event.isRightClick() && action.secondaryCommand != null
                ? action.secondaryCommand : action.primaryCommand;
        if (command == null) return;
        Bukkit.dispatchCommand(player, BedWars.mainCmd + " team " + command);
        Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
            if (player.isOnline() && preGameArena(player) != null) open(player);
        });
    }

    private void renderHeader(Inventory inventory, Player player, IArena arena) {
        List<Player> members = squads.getMembers(player);
        Player leader = squads.getLeader(player);
        for (int index = 0; index < members.size() && index < MEMBER_SLOTS.length; index++) {
            Player member = members.get(index);
            String suffix = member.equals(leader) ? ChatColor.GOLD + "（队长）" : "";
            inventory.setItem(MEMBER_SLOTS[index], playerHead(member,
                    ChatColor.GREEN + member.getName() + suffix,
                    List.of(ChatColor.GRAY + "当前开局队伍成员")));
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "开局组队 " + ChatColor.GRAY + "(" + members.size() + "/"
                + arena.getMaxInTeam() + ")");
        meta.setLore(List.of(
                ChatColor.GRAY + "点击下方玩家头像发送邀请。",
                ChatColor.GRAY + "收到的邀请会显示为绿色头像。",
                "",
                ChatColor.YELLOW + "命令入口：/" + BedWars.mainCmd + " team",
                ChatColor.DARK_GRAY + "右键收到的邀请可拒绝。"
        ));
        info.setItemMeta(meta);
        inventory.setItem(4, info);
    }

    private void renderActions(Inventory inventory, SquadHolder holder, Player player) {
        List<Integer> slots = contentSlots();
        int index = 0;
        for (Player inviter : squads.getPendingInviters(player)) {
            if (index >= slots.size()) break;
            int slot = slots.get(index++);
            inventory.setItem(slot, playerHead(inviter, ChatColor.GREEN + "接受 " + inviter.getName() + " 的邀请",
                    List.of(ChatColor.YELLOW + "左键接受", ChatColor.RED + "右键拒绝")));
            holder.actions.put(slot, new SlotAction("accept " + inviter.getName(),
                    "decline " + inviter.getName(), false));
        }

        if (squads.isLeader(player) && squads.getMembers(player).size() < preGameArena(player).getMaxInTeam()) {
            for (Player target : squads.getAvailableTargets(player)) {
                if (index >= slots.size()) break;
                int slot = slots.get(index++);
                inventory.setItem(slot, playerHead(target, ChatColor.YELLOW + "邀请 " + target.getName(),
                        List.of(ChatColor.GREEN + "点击发送组队邀请")));
                holder.actions.put(slot, new SlotAction("invite " + target.getName(), null, false));
            }
        }

        if (squads.isGrouped(player)) {
            ItemStack leave = namedItem(Material.BARRIER, ChatColor.RED + "退出当前队伍",
                    List.of(ChatColor.GRAY + "退出后将作为单人参与自动分配"));
            inventory.setItem(49, leave);
            holder.actions.put(49, new SlotAction("leave", null, false));
        }

        inventory.setItem(53, namedItem(Material.OAK_DOOR, ChatColor.RED + "关闭", List.of()));
        holder.actions.put(53, new SlotAction(null, null, true));
    }

    static List<Integer> contentSlots() {
        List<Integer> slots = new ArrayList<>(36);
        for (int slot = 9; slot < 45; slot++) slots.add(slot);
        return List.copyOf(slots);
    }

    private static ItemStack playerHead(Player owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(owner);
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static IArena preGameArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.isPlayer(player)) return null;
        return arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting ? arena : null;
    }

    static final class SquadHolder implements InventoryHolder {
        private final UUID viewer;
        private final Map<Integer, SlotAction> actions = new HashMap<>();
        private Inventory inventory;

        private SquadHolder(UUID viewer) {
            this.viewer = viewer;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    record SlotAction(String primaryCommand, String secondaryCommand, boolean close) {
    }
}
