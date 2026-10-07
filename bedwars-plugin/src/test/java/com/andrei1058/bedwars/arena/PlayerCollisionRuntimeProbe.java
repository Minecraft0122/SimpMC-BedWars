package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.spigot.sidebar.PlayerTab;
import com.andrei1058.spigot.sidebar.Sidebar;
import com.andrei1058.spigot.sidebar.SidebarLine;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** 用真实 Paper 1.21.11 的 CraftTeam 和 EntitySelector 验证玩家碰撞，不复制其判定公式。 */
public final class PlayerCollisionRuntimeProbe {
    private PlayerCollisionRuntimeProbe() {
    }

    public static void main(String[] args) throws Exception {
        Class.forName("net.minecraft.SharedConstants").getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap").getMethod("bootStrap").invoke(null);
        Class<?> registryAccess = Class.forName("net.minecraft.core.RegistryAccess");
        Class.forName("org.bukkit.craftbukkit.CraftRegistry").getMethod("setMinecraftRegistry", registryAccess)
                .invoke(null, registryAccess.getField("EMPTY").get(null));
        Class<?> configuration = Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
        Object config = configuration.getConstructor().newInstance();
        Class<?> collisions = Class.forName(configuration.getName() + "$Collisions");
        Object collisionConfig = collisions.getConstructor(configuration).newInstance(config);
        collisions.getField("enablePlayerCollisions").setBoolean(collisionConfig, true);
        configuration.getField("collisions").set(config, collisionConfig);
        Method set = configuration.getDeclaredMethod("set", configuration);
        set.setAccessible(true);
        set.invoke(null, config);

        verifyMainScoreboard();
        verifyViewerScoreboard();
        System.out.println("Paper 1.21.11 碰撞验证通过：同队、敌队、隐身、等待、复活、旁观、结算和恢复。");
    }

    private static void verifyMainScoreboard() throws Exception {
        Fixture fixture = new Fixture();
        Team red = fixture.scoreboard.registerNewTeam("red");
        Team blue = fixture.scoreboard.registerNewTeam("blue");
        Team inactive = fixture.scoreboard.registerNewTeam("bw_waiting");
        red.setOption(Team.Option.COLLISION_RULE, PlayerCollisionState.ACTIVE_TEAM_RULE);
        blue.setOption(Team.Option.COLLISION_RULE, PlayerCollisionState.ACTIVE_TEAM_RULE);
        inactive.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        red.addEntry("Alice");
        red.addEntry("Bob");
        blue.addEntry("Enemy");
        fixture.expect("同队", "Alice", "Bob", false);
        fixture.expect("敌队", "Alice", "Enemy", true);
        for (GameState state : GameState.values()) {
            if (PlayerCollisionState.shouldCollide(state, false, false)) continue;
            inactive.addEntry("Bob");
            fixture.expect(state.name(), "Alice", "Bob", false);
            fixture.expect(state.name(), "Enemy", "Bob", false);
        }
        // 等待复活和正式旁观都进入 NEVER，恢复后重新加入真实游戏队伍。
        for (boolean spectator : new boolean[]{false, true}) {
            if (PlayerCollisionState.shouldCollide(GameState.playing, spectator, !spectator)) {
                throw new AssertionError("非活动玩家被允许碰撞");
            }
            inactive.addEntry("Bob");
            fixture.expect("复活/旁观", "Enemy", "Bob", false);
            red.addEntry("Bob");
            fixture.expect("恢复后同队", "Alice", "Bob", false);
            fixture.expect("恢复后敌队", "Enemy", "Bob", true);
        }
    }

    private static void verifyViewerScoreboard() throws Exception {
        Fixture fixture = new Fixture();
        Sidebar sidebar = new Sidebar(new SidebarLine(), List.of(), List.of());
        Method apply = Sidebar.class.getDeclaredMethod("applyTab", Scoreboard.class, PlayerTab.class);
        apply.setAccessible(true);
        render(apply, sidebar, fixture, "Alice", "red", true, false);
        render(apply, sidebar, fixture, "Bob", "red", true, false);
        render(apply, sidebar, fixture, "Enemy", "blue", true, false);
        fixture.expect("客户端同队", "Alice", "Bob", false);
        fixture.expect("客户端敌队", "Alice", "Enemy", true);
        render(apply, sidebar, fixture, "Bob", "red", true, true);
        fixture.expect("隐身同队", "Alice", "Bob", false);
        fixture.expect("隐身敌队", "Enemy", "Bob", true);
        render(apply, sidebar, fixture, "Bob", "bw_waiting", false, true);
        fixture.expect("客户端等待复活", "Enemy", "Bob", false);
        render(apply, sidebar, fixture, "Bob", "red", true, false);
        fixture.expect("客户端复活同队", "Alice", "Bob", false);
        fixture.expect("客户端复活敌队", "Enemy", "Bob", true);
    }

    private static void render(Method apply, Sidebar sidebar, Fixture fixture, String name,
                               String group, boolean active, boolean invisible) throws Exception {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        PlayerTab tab = new PlayerTab(name, player, new SidebarLine(), new SidebarLine(),
                active ? PlayerTab.PushingRule.PUSH_OTHER_TEAMS : PlayerTab.PushingRule.NEVER,
                List.of(), ChatColor.WHITE,
                invisible ? PlayerTab.NameTagVisibility.NEVER : PlayerTab.NameTagVisibility.ALWAYS,
                PlayerTab.PlayerListMode.ACTUAL, group);
        apply.invoke(sidebar, fixture.scoreboard, tab);
    }

    private static final class Fixture {
        private final Class<?> entity = Class.forName("net.minecraft.world.entity.Entity");
        private final Class<?> player = Class.forName("net.minecraft.server.level.ServerPlayer");
        private final Class<?> scores = Class.forName("net.minecraft.world.scores.Scoreboard");
        private final Object handle = scores.getConstructor().newInstance();
        private final Object level = Mockito.mock(Class.forName("net.minecraft.server.level.ServerLevel"));
        private final Scoreboard scoreboard;

        private Fixture() throws Exception {
            var constructor = Class.forName("org.bukkit.craftbukkit.scoreboard.CraftScoreboard")
                    .getDeclaredConstructor(scores);
            constructor.setAccessible(true);
            scoreboard = (Scoreboard) constructor.newInstance(handle);
        }

        private Object player(String name) {
            // 仅替代玩家/世界环境；队伍、API 映射和碰撞筛选执行真实 Paper 代码。
            return Mockito.mock(player, invocation -> switch (invocation.getMethod().getName()) {
                case "getTeam" -> scores.getMethod("getPlayersTeam", String.class).invoke(handle, name);
                case "level" -> level;
                case "isPushable", "canCollideWithBukkit" -> true;
                default -> Mockito.RETURNS_DEFAULTS.answer(invocation);
            });
        }

        @SuppressWarnings("unchecked")
        private void expect(String label, String first, String second, boolean expected) throws Exception {
            Object a = player(first);
            Object b = player(second);
            Method pushable = Class.forName("net.minecraft.world.entity.EntitySelector").getMethod("pushableBy", entity);
            boolean forward = ((Predicate<Object>) pushable.invoke(null, a)).test(b);
            boolean reverse = ((Predicate<Object>) pushable.invoke(null, b)).test(a);
            if (forward != expected || reverse != expected) {
                throw new AssertionError(label + ": expected=" + expected + ", forward=" + forward + ", reverse=" + reverse);
            }
        }
    }
}
