package com.andrei1058.bedwars.support.version.v1_21_R3;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.shop.ShopHolo;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.entity.Despawnable;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.VersionSupport;
import com.andrei1058.bedwars.support.version.common.VersionCommon;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.entity.Display;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class v1_21_R3 extends VersionSupport {

    private static final double NPC_POSITION_EPSILON_SQUARED = 1.0E-6D;
    private static final float NPC_ROTATION_EPSILON = 0.01F;
    private final Map<UUID, LockedShopkeeper> lockedShopkeepers = new HashMap<>();

    public v1_21_R3(Plugin plugin, String name) {
        super(plugin, minecraftVersionName(name));
        loadDefaultEffects();
    }

    private static String minecraftVersionName(String fallback) {
        String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion != null && !bukkitVersion.isBlank()) {
            return bukkitVersion.split("-")[0];
        }
        return fallback == null || fallback.isBlank() ? "1.21+" : fallback;
    }

    @Override
    public void registerVersionListeners() {
        new VersionCommon(this);
        Bukkit.getScheduler().runTaskTimer(getPlugin(), this::refreshDespawnableTargets, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(getPlugin(), this::refreshLockedShopkeepers, 1L, 1L);
    }

    @Override
    public void registerCommand(String name, Command cmd) {
        Bukkit.getCommandMap().register(name, cmd);
    }

    @Override
    public void sendTitle(Player p, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        p.sendTitle(title == null ? " " : title, subtitle == null ? " " : subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void playAction(Player p, String text) {
        p.sendActionBar(ChatColor.translateAlternateColorCodes('&', text));
    }

    @Override
    public boolean isBukkitCommandRegistered(String command) {
        return Bukkit.getCommandMap().getCommand(command) != null;
    }

    @Override
    public ItemStack getItemInHand(Player p) {
        return p.getInventory().getItemInMainHand();
    }

    @Override
    public void hideEntity(Entity e, Player p) {
        p.hideEntity(getPlugin(), e);
    }

    @Override
    public boolean isArmor(ItemStack itemStack) {
        if (itemStack == null) return false;
        Material type = itemStack.getType();
        return Tag.ITEMS_HEAD_ARMOR.isTagged(type)
                || Tag.ITEMS_CHEST_ARMOR.isTagged(type)
                || Tag.ITEMS_LEG_ARMOR.isTagged(type)
                || Tag.ITEMS_FOOT_ARMOR.isTagged(type)
                || type == Material.ELYTRA;
    }

    @Override
    public boolean isTool(ItemStack itemStack) {
        if (itemStack == null) return false;
        String type = itemStack.getType().name();
        return isSword(itemStack)
                || isAxe(itemStack)
                || type.endsWith("_PICKAXE")
                || type.endsWith("_SHOVEL")
                || type.endsWith("_HOE")
                || type.equals("SHEARS")
                || type.equals("FLINT_AND_STEEL")
                || type.equals("FISHING_ROD")
                || type.equals("BRUSH");
    }

    @Override
    public boolean isSword(ItemStack itemStack) {
        return itemStack != null && Tag.ITEMS_SWORDS.isTagged(itemStack.getType());
    }

    @Override
    public boolean isAxe(ItemStack itemStack) {
        return itemStack != null && Tag.ITEMS_AXES.isTagged(itemStack.getType());
    }

    @Override
    public boolean isBow(ItemStack itemStack) {
        if (itemStack == null) return false;
        Material type = itemStack.getType();
        return type == Material.BOW || type == Material.CROSSBOW;
    }

    @Override
    public boolean isProjectile(ItemStack itemStack) {
        if (itemStack == null) return false;
        Material type = itemStack.getType();
        return Tag.ITEMS_ARROWS.isTagged(type)
                || type == Material.EGG
                || type == Material.SNOWBALL
                || type == Material.FIRE_CHARGE
                || type == Material.ENDER_PEARL
                || type == Material.TRIDENT
                || type == Material.WIND_CHARGE;
    }

    @Override
    public boolean isInvisibilityPotion(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.POTION) return false;
        if (!(itemStack.getItemMeta() instanceof PotionMeta meta)) return false;
        return meta.hasCustomEffects() && meta.hasCustomEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public void registerEntities() {
        // Paper API adapter uses vanilla Bukkit entities.
    }

    @Override
    public void spawnShop(Location loc, String name1, List<Player> players, IArena arena) {
        Location location = loc.clone();
        World world = location.getWorld();
        if (world == null) return;

        Villager villager = (Villager) world.spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setAware(false);
        villager.setGravity(false);
        villager.setRotation(location.getYaw(), 0.0F);
        villager.setBodyYaw(location.getYaw());
        villager.setRemoveWhenFarAway(false);
        villager.setCollidable(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCustomName(null);
        villager.setCustomNameVisible(false);
        lockedShopkeepers.put(villager.getUniqueId(), new LockedShopkeeper(villager, location));

        for (Player player : players) {
            String[] name = Language.getMsg(player, name1).split(",");
            if (name.length == 1) {
                TextDisplay text = createTextDisplay(name[0], location.clone().add(0, 1.85, 0));
                new ShopHolo(Language.getPlayerLanguage(player).getIso(), text, null, location, arena);
            } else {
                TextDisplay first = createTextDisplay(name[0], location.clone().add(0, 2.1, 0));
                TextDisplay second = createTextDisplay(name[1], location.clone().add(0, 1.85, 0));
                new ShopHolo(Language.getPlayerLanguage(player).getIso(), first, second, location, arena);
            }
        }

        for (ShopHolo shopHolo : ShopHolo.getShopHolo()) {
            if (shopHolo.getA() == arena) {
                shopHolo.update();
            }
        }
    }

    @Override
    public double getDamage(ItemStack itemStack) {
        if (itemStack == null) return 0D;
        ItemMeta meta = itemStack.getItemMeta();
        Attribute attackDamage = attribute("attack_damage", "generic.attack_damage", "ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE");
        if (meta != null) {
            Collection<AttributeModifier> modifiers = attackDamage == null ? null : meta.getAttributeModifiers(attackDamage);
            if (modifiers != null && !modifiers.isEmpty()) {
                double damage = 1D;
                for (AttributeModifier modifier : modifiers) {
                    if (modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                        damage += modifier.getAmount();
                    } else if (modifier.getOperation() == AttributeModifier.Operation.ADD_SCALAR) {
                        damage += damage * modifier.getAmount();
                    } else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_SCALAR_1) {
                        damage *= 1D + modifier.getAmount();
                    }
                }
                return damage;
            }
        }
        return defaultWeaponDamage(itemStack.getType());
    }

    @Override
    public void spawnSilverfish(Location loc, ITeam team, double speed, double health, int despawn, double damage) {
        if (loc == null || loc.getWorld() == null || team == null) return;
        Silverfish silverfish = loc.getWorld().spawn(loc, Silverfish.class);
        configureDespawnable(silverfish, speed, health, damage);
        new Despawnable(
                silverfish,
                team,
                despawn,
                Messages.SHOP_UTILITY_NPC_SILVERFISH_NAME,
                PlayerKillEvent.PlayerKillCause.SILVERFISH_FINAL_KILL,
                PlayerKillEvent.PlayerKillCause.SILVERFISH
        );
    }

    @Override
    public void spawnIronGolem(Location loc, ITeam team, double speed, double health, int despawn) {
        if (loc == null || loc.getWorld() == null || team == null) return;
        IronGolem ironGolem = loc.getWorld().spawn(loc, IronGolem.class);
        ironGolem.setPlayerCreated(false);
        configureDespawnable(ironGolem, speed, health, 4D);
        new Despawnable(
                ironGolem,
                team,
                despawn,
                Messages.SHOP_UTILITY_NPC_IRON_GOLEM_NAME,
                PlayerKillEvent.PlayerKillCause.IRON_GOLEM_FINAL_KILL,
                PlayerKillEvent.PlayerKillCause.IRON_GOLEM
        );
    }

    @Override
    public void minusAmount(Player p, ItemStack itemStack, int amount) {
        if (itemStack == null) return;
        if (itemStack.getAmount() - amount <= 0) {
            if (p.getInventory().getItemInOffHand().equals(itemStack)) {
                p.getInventory().setItemInOffHand(null);
            } else {
                p.getInventory().removeItem(itemStack);
            }
            return;
        }
        itemStack.setAmount(itemStack.getAmount() - amount);
        p.updateInventory();
    }

    @Override
    public void setSource(TNTPrimed tnt, Player owner) {
        tnt.setSource(owner);
    }

    @Override
    public void voidKill(Player p) {
        p.damage(1000D);
        if (!p.isDead() && p.getHealth() > 0D) {
            p.setHealth(0D);
        }
    }

    @Override
    public void hideArmor(Player victim, Player receiver) {
        ItemStack air = new ItemStack(Material.AIR);
        EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.put(EquipmentSlot.HEAD, air);
        equipment.put(EquipmentSlot.CHEST, air);
        equipment.put(EquipmentSlot.LEGS, air);
        equipment.put(EquipmentSlot.FEET, air);
        receiver.sendEquipmentChange(victim, equipment);
    }

    @Override
    public void showArmor(Player victim, Player receiver) {
        EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.put(EquipmentSlot.HEAD, emptyIfNull(victim.getInventory().getHelmet()));
        equipment.put(EquipmentSlot.CHEST, emptyIfNull(victim.getInventory().getChestplate()));
        equipment.put(EquipmentSlot.LEGS, emptyIfNull(victim.getInventory().getLeggings()));
        equipment.put(EquipmentSlot.FEET, emptyIfNull(victim.getInventory().getBoots()));
        receiver.sendEquipmentChange(victim, equipment);
    }

    @Override
    public void spawnDragon(Location location, ITeam team) {
        if (location == null || location.getWorld() == null) {
            getPlugin().getLogger().log(Level.WARNING, "Could not spawn Dragon. Location is null");
            return;
        }
        EnderDragon dragon = (EnderDragon) location.getWorld().spawnEntity(location, EntityType.ENDER_DRAGON);
        dragon.setPhase(EnderDragon.Phase.CIRCLING);
    }

    @Override
    public void colorBed(ITeam team) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockState bed = team.getBed().clone().add(x, 0, z).getBlock().getState();
                if (bed instanceof Bed) {
                    bed.setType(team.getColor().bedMaterial());
                    bed.update();
                }
            }
        }
    }

    @Override
    public void registerTntWhitelist(float endStoneBlast, float glassBlast) {
        // Paper 1.21.11+ uses event-based blast protection instead of mutating block resistance.
    }

    @Override
    public void setBlockTeamColor(Block block, TeamColor teamColor) {
        String type = block.getType().toString();
        if (type.contains("STAINED_GLASS") || type.equals("GLASS")) {
            block.setType(teamColor.glassMaterial());
        } else if (type.contains("_TERRACOTTA")) {
            block.setType(teamColor.glazedTerracottaMaterial());
        } else if (type.contains("_WOOL")) {
            block.setType(teamColor.woolMaterial());
        }
    }

    @Override
    public void setCollide(Player p, IArena arena, boolean value) {
        p.setCollidable(value);
        if (arena != null) {
            arena.updateSpectatorCollideRule(p, value);
        }
    }

    @Override
    public ItemStack addCustomData(ItemStack itemStack, String data) {
        return setTag(itemStack, VersionSupport.PLUGIN_TAG_GENERIC_KEY, data);
    }

    @Override
    public ItemStack setTag(ItemStack itemStack, String key, String value) {
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        meta.getPersistentDataContainer().set(key(key), PersistentDataType.STRING, value);
        applyKnownItemTag(meta, key, value);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @Override
    public String getTag(ItemStack itemStack, String key) {
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(key(key), PersistentDataType.STRING);
    }

    @Override
    public boolean isCustomBedWarsItem(ItemStack itemStack) {
        if (itemStack == null) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key(VersionSupport.PLUGIN_TAG_GENERIC_KEY), PersistentDataType.STRING);
    }

    @Override
    public String getCustomData(ItemStack itemStack) {
        return getTag(itemStack, VersionSupport.PLUGIN_TAG_GENERIC_KEY);
    }

    @Override
    public ItemStack colourItem(ItemStack itemStack, ITeam team) {
        if (itemStack == null) return null;
        String type = itemStack.getType().toString();
        if (isBed(itemStack.getType())) {
            return new ItemStack(team.getColor().bedMaterial(), itemStack.getAmount());
        } else if (type.contains("_STAINED_GLASS_PANE")) {
            return new ItemStack(team.getColor().glassPaneMaterial(), itemStack.getAmount());
        } else if (type.contains("STAINED_GLASS") || type.equals("GLASS")) {
            return new ItemStack(team.getColor().glassMaterial(), itemStack.getAmount());
        } else if (type.contains("_TERRACOTTA")) {
            return new ItemStack(team.getColor().glazedTerracottaMaterial(), itemStack.getAmount());
        } else if (type.contains("_WOOL")) {
            return new ItemStack(team.getColor().woolMaterial(), itemStack.getAmount());
        }
        return itemStack;
    }

    @Override
    public ItemStack createItemStack(String material, int amount, short data) {
        Material type = materialOf(material);
        if (type == null) {
            getPlugin().getLogger().log(Level.WARNING, material + " is not a valid " + getName() + " material!");
            type = Material.BEDROCK;
        }
        return new ItemStack(type, amount);
    }

    @Override
    public Material materialFireball() {
        return Material.FIRE_CHARGE;
    }

    @Override
    public Material materialPlayerHead() {
        return Material.PLAYER_HEAD;
    }

    @Override
    public Material materialSnowball() {
        return Material.SNOWBALL;
    }

    @Override
    public Material materialGoldenHelmet() {
        return Material.GOLDEN_HELMET;
    }

    @Override
    public Material materialGoldenChestPlate() {
        return Material.GOLDEN_CHESTPLATE;
    }

    @Override
    public Material materialGoldenLeggings() {
        return Material.GOLDEN_LEGGINGS;
    }

    @Override
    public Material materialNetheriteHelmet() {
        return Material.NETHERITE_HELMET;
    }

    @Override
    public Material materialNetheriteChestPlate() {
        return Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public Material materialNetheriteLeggings() {
        return Material.NETHERITE_LEGGINGS;
    }

    @Override
    public Material materialElytra() {
        return Material.ELYTRA;
    }

    @Override
    public Material materialCake() {
        return Material.CAKE;
    }

    @Override
    public Material materialCraftingTable() {
        return Material.CRAFTING_TABLE;
    }

    @Override
    public Material materialEnchantingTable() {
        return Material.ENCHANTING_TABLE;
    }

    @Override
    public Material woolMaterial() {
        return Material.WHITE_WOOL;
    }

    @Override
    public String getShopUpgradeIdentifier(ItemStack itemStack) {
        String value = getTag(itemStack, VersionSupport.PLUGIN_TAG_TIER_KEY);
        return value == null ? "" : value;
    }

    @Override
    public ItemStack setShopUpgradeIdentifier(ItemStack itemStack, String identifier) {
        return setTag(itemStack, VersionSupport.PLUGIN_TAG_TIER_KEY, identifier);
    }

    @Override
    public ItemStack getPlayerHead(Player player, ItemStack copyTagFrom) {
        ItemStack head = copyTagFrom == null ? new ItemStack(materialPlayerHead()) : copyTagFrom.clone();
        head.setType(materialPlayerHead());
        ItemMeta meta = head.getItemMeta();

        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
        }

        if (meta != null) {
            head.setItemMeta(meta);
        }
        return head;
    }

    @Override
    public void sendPlayerSpawnPackets(Player respawned, IArena arena) {
        if (respawned == null || arena == null || !arena.isPlayer(respawned)) return;
        if (arena.getRespawnSessions().containsKey(respawned)) return;

        for (Player player : arena.getPlayers()) {
            if (player == null || player.equals(respawned)) continue;
            player.showPlayer(getPlugin(), respawned);
            respawned.showPlayer(getPlugin(), player);
            if (respawned.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                hideArmor(respawned, player);
            } else {
                showArmor(respawned, player);
            }
        }

        for (Player spectator : arena.getSpectators()) {
            if (spectator == null || spectator.equals(respawned)) continue;
            spectator.showPlayer(getPlugin(), respawned);
            respawned.hidePlayer(getPlugin(), spectator);
        }
    }

    @Override
    public String getInventoryName(InventoryEvent e) {
        return e.getView().getTitle();
    }

    @Override
    public void setUnbreakable(ItemMeta itemMeta) {
        itemMeta.setUnbreakable(true);
    }

    @Override
    public String getMainLevel() {
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0).getName();
        }

        File serverProperties = new File("server.properties");
        if (serverProperties.isFile()) {
            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream(serverProperties)) {
                properties.load(input);
                return properties.getProperty("level-name", "world");
            } catch (IOException ignored) {
                return "world";
            }
        }
        return "world";
    }

    @Override
    public void setJoinSignBackground(BlockState blockState, Material material) {
        if (blockState.getBlockData() instanceof WallSign wallSign) {
            Block sign = blockState.getBlock();
            BlockFace supportFace = wallSign.getFacing().getOppositeFace();
            int supportX = sign.getX() + supportFace.getModX();
            int supportZ = sign.getZ() + supportFace.getModZ();
            World world = sign.getWorld();
            if (!world.isChunkLoaded(supportX >> 4, supportZ >> 4)) return;
            sign.getRelative(supportFace).setType(material);
        }
    }

    @Override
    public void spigotShowPlayer(Player victim, Player receiver) {
        receiver.showPlayer(getPlugin(), victim);
    }

    @Override
    public void spigotHidePlayer(Player victim, Player receiver) {
        receiver.hidePlayer(getPlugin(), victim);
    }

    @Override
    public Fireball setFireballDirection(Fireball fireball, Vector vector) {
        if (vector.lengthSquared() > 0D) {
            Vector normalized = vector.clone().normalize();
            fireball.setDirection(normalized);
            fireball.setAcceleration(normalized.multiply(0.1D));
        }
        return fireball;
    }

    @Override
    public void playRedStoneDot(Player player) {
        Location location = player.getLocation().add(0, 2.6, 0);
        Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1F);
        List<Player> receivers = player.getWorld().getPlayers().stream()
                .filter(viewer -> !viewer.equals(player))
                .filter(viewer -> viewer.getLocation().distanceSquared(location) <= 1024D)
                .toList();
        if (receivers.isEmpty()) return;
        player.getWorld().spawnParticle(Particle.DUST, receivers, player,
                location.getX(), location.getY(), location.getZ(), 1,
                0D, 0D, 0D, 0D, dust);
    }

    @Override
    public void clearArrowsFromPlayerBody(Player player) {
        player.setArrowsInBody(0);
        player.setArrowsStuck(0);
    }

    @Override
    public void placeTowerBlocks(Block block, IArena arena, TeamColor color, int x, int y, int z) {
        Block relative = block.getRelative(x, y, z);
        relative.setType(color.woolMaterial());
        arena.addPlacedBlock(relative);
    }

    @Override
    public void placeLadder(Block block, int x, int y, int z, IArena arena, int ladderData) {
        Block relative = block.getRelative(x, y, z);
        relative.setType(Material.LADDER);
        Ladder ladder = (Ladder) relative.getBlockData();
        arena.addPlacedBlock(relative);
        switch (ladderData) {
            case 2 -> ladder.setFacing(BlockFace.NORTH);
            case 3 -> ladder.setFacing(BlockFace.SOUTH);
            case 4 -> ladder.setFacing(BlockFace.WEST);
            case 5 -> ladder.setFacing(BlockFace.EAST);
            default -> {
            }
        }
        relative.setBlockData(ladder);
    }

    @Override
    public void playVillagerEffect(Player player, Location location) {
        player.spawnParticle(Particle.HAPPY_VILLAGER, location, 1);
    }

    private static TextDisplay createTextDisplay(String name, Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class);
        display.text(LegacyComponentSerializer.legacySection().deserialize(name));
        display.setBillboard(Display.Billboard.CENTER);
        display.setDefaultBackground(false);
        display.setSeeThrough(true);
        display.setShadowed(true);
        display.setPersistent(false);
        return display;
    }

    private void configureDespawnable(LivingEntity entity, double speed, double health, double damage) {
        entity.setRemoveWhenFarAway(false);
        entity.setPersistent(true);
        entity.setCustomNameVisible(true);
        entity.setCanPickupItems(false);
        entity.setAI(true);

        setAttribute(entity, attribute("max_health", "generic.max_health", "MAX_HEALTH", "GENERIC_MAX_HEALTH"), health);
        setAttribute(entity, attribute("movement_speed", "generic.movement_speed", "MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED"), speed);
        setAttribute(entity, attribute("attack_damage", "generic.attack_damage", "ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE"), damage);
        entity.setHealth(Math.min(health, entity.getMaxHealth()));

        if (entity instanceof Mob mob) {
            mob.setAware(true);
            mob.setAggressive(true);
        }
    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        if (attribute == null) return;
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.setBaseValue(value);
        }
    }

    private Attribute attribute(String... candidates) {
        for (String candidate : candidates) {
            NamespacedKey key = NamespacedKey.minecraft(candidate.toLowerCase(Locale.ROOT));
            Attribute attribute = Registry.ATTRIBUTE.get(key);
            if (attribute != null) {
                return attribute;
            }
        }
        return null;
    }

    private void refreshDespawnableTargets() {
        for (Despawnable despawnable : getDespawnablesList().values()) {
            LivingEntity entity = despawnable.getEntity();
            ITeam team = despawnable.getTeam();
            if (!(entity instanceof Mob mob) || entity.isDead() || team == null || team.getArena() == null) {
                continue;
            }

            Player target = null;
            double bestDistance = Double.MAX_VALUE;
            for (Player player : team.getArena().getPlayers()) {
                if (player == null || player.isDead()) continue;
                if (!player.getWorld().equals(entity.getWorld())) continue;
                if (team.wasMember(player.getUniqueId())) continue;
                if (team.getArena().isReSpawning(player.getUniqueId())) continue;
                if (team.getArena().isSpectator(player.getUniqueId())) continue;

                double distance = player.getLocation().distanceSquared(entity.getLocation());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    target = player;
                }
            }

            mob.setTarget(target);
            mob.setAggressive(target != null);
        }
    }

    /**
     * Keep shop and upgrade villagers at their configured position and cardinal
     * facing. The task compares first, so stationary NPCs do not generate a
     * teleport or rotation packet every tick.
     */
    private void refreshLockedShopkeepers() {
        Iterator<LockedShopkeeper> iterator = lockedShopkeepers.values().iterator();
        while (iterator.hasNext()) {
            LockedShopkeeper locked = iterator.next();
            Villager villager = locked.villager();
            if (!villager.isValid() || villager.isDead()) {
                iterator.remove();
                continue;
            }

            Location fixed = locked.location();
            Location current = villager.getLocation();
            boolean wrongWorld = current.getWorld() == null || fixed.getWorld() == null
                    || !current.getWorld().equals(fixed.getWorld());
            if (wrongWorld || (!wrongWorld && current.distanceSquared(fixed) > NPC_POSITION_EPSILON_SQUARED)) {
                villager.teleport(fixed);
                current = villager.getLocation();
            }

            if (angleDifference(current.getYaw(), fixed.getYaw()) > NPC_ROTATION_EPSILON
                    || Math.abs(current.getPitch()) > NPC_ROTATION_EPSILON) {
                villager.setRotation(fixed.getYaw(), 0.0F);
            }
            if (angleDifference(villager.getBodyYaw(), fixed.getYaw()) > NPC_ROTATION_EPSILON) {
                villager.setBodyYaw(fixed.getYaw());
            }
            if (villager.getVelocity().lengthSquared() > NPC_POSITION_EPSILON_SQUARED) {
                villager.setVelocity(new Vector());
            }
            if (villager.hasAI()) villager.setAI(false);
            if (villager.isAware()) villager.setAware(false);
            if (villager.hasGravity()) villager.setGravity(false);
        }
    }

    static float angleDifference(float first, float second) {
        return Math.abs((float) Math.IEEEremainder(first - second, 360.0D));
    }

    private record LockedShopkeeper(Villager villager, Location location) {
        private LockedShopkeeper {
            location = location.clone();
            location.setPitch(0.0F);
        }
    }

    private NamespacedKey key(String key) {
        return new NamespacedKey(getPlugin(), key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_"));
    }

    private void applyKnownItemTag(ItemMeta meta, String key, String value) {
        if (!(meta instanceof PotionMeta potionMeta)) return;
        if ("CustomPotionColor".equalsIgnoreCase(key)) {
            Color color = parseColor(value);
            if (color != null) {
                potionMeta.setColor(color);
            }
        } else if ("Potion".equalsIgnoreCase(key)) {
            PotionType potionType = potionType(value);
            if (potionType != null) {
                potionMeta.setBasePotionType(potionType);
            }
        }
    }

    private Color parseColor(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.contains(",")) {
                String[] rgb = value.split(",");
                if (rgb.length != 3) return null;
                return Color.fromRGB(
                        Integer.parseInt(rgb[0].trim()),
                        Integer.parseInt(rgb[1].trim()),
                        Integer.parseInt(rgb[2].trim())
                );
            }
            return Color.fromRGB(Integer.decode(value) & 0xFFFFFF);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private PotionType potionType(String value) {
        if (value == null || value.isBlank()) return null;
        NamespacedKey key = NamespacedKey.fromString(value);
        if (key != null) {
            PotionType potionType = Registry.POTION.get(key);
            if (potionType != null) return potionType;
        }
        try {
            return PotionType.valueOf(value.toUpperCase(Locale.ROOT).replace("MINECRAFT:", "").replace(':', '_'));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Material materialOf(String material) {
        if (material == null || material.isBlank()) return null;
        String normalized = material.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        int legacyDataSeparator = normalized.indexOf(':');
        if (legacyDataSeparator > 0 && normalized.substring(legacyDataSeparator + 1).chars().allMatch(Character::isDigit)) {
            normalized = normalized.substring(0, legacyDataSeparator);
        }
        normalized = switch (normalized) {
            case "SNOW_BALL" -> "SNOWBALL";
            case "FIREBALL" -> "FIRE_CHARGE";
            case "BREWING_STAND_ITEM" -> "BREWING_STAND";
            case "GOLD_HELMET" -> "GOLDEN_HELMET";
            case "GOLD_CHESTPLATE" -> "GOLDEN_CHESTPLATE";
            case "GOLD_LEGGINGS" -> "GOLDEN_LEGGINGS";
            case "GOLD_BOOTS" -> "GOLDEN_BOOTS";
            case "GOLD_SWORD" -> "GOLDEN_SWORD";
            case "GOLD_AXE" -> "GOLDEN_AXE";
            case "GOLD_PICKAXE" -> "GOLDEN_PICKAXE";
            case "MONSTER_EGG" -> "HORSE_SPAWN_EGG";
            default -> normalized;
        };
        return Material.getMaterial(normalized);
    }

    private double defaultWeaponDamage(Material material) {
        return switch (material) {
            case WOODEN_SWORD, GOLDEN_SWORD -> 4D;
            case STONE_SWORD -> 5D;
            case IRON_SWORD -> 6D;
            case DIAMOND_SWORD -> 7D;
            case NETHERITE_SWORD -> 8D;
            case WOODEN_AXE, GOLDEN_AXE -> 7D;
            case STONE_AXE, IRON_AXE, DIAMOND_AXE -> 9D;
            case NETHERITE_AXE -> 10D;
            default -> 1D;
        };
    }

    private ItemStack emptyIfNull(ItemStack itemStack) {
        return itemStack == null ? new ItemStack(Material.AIR) : itemStack;
    }
}
