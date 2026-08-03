package com.andrei1058.spigot.sidebar;

import io.papermc.paper.scoreboard.numbers.FixedFormat;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SidebarPlaceholderRefreshTest {

    @Test
    void unusedPlaceholdersDoNotEvaluateTheirSuppliers() {
        AtomicInteger evaluations = new AtomicInteger();
        PlaceholderProvider unused = new PlaceholderProvider("{unused}", () -> {
            evaluations.incrementAndGet();
            return "value";
        });

        assertEquals("static text", Sidebar.replacePlaceholders("static text", List.of(unused)));
        assertEquals(0, evaluations.get());
    }

    @Test
    void unchangedPlaceholderRefreshDoesNotWriteOrRebuildScoreboardState() {
        AtomicReference<String> alive = new AtomicReference<>("1");
        Fixture fixture = fixture(alive);

        fixture.sidebar.refreshPlaceholders();

        assertEquals(0, fixture.state.registrations.get());
        assertEquals(0, fixture.state.unregistrations.get());
        assertEquals(0, fixture.state.titleWrites.get());
        assertEquals(0, fixture.state.prefixWrites.get());
        assertEquals(0, fixture.state.numberFormatWrites.get());
    }

    @Test
    void changedPlaceholderUpdatesValuesWithoutRecreatingObjectiveOrTeams() {
        AtomicReference<String> alive = new AtomicReference<>("1");
        Fixture fixture = fixture(alive);

        alive.set("2");
        fixture.sidebar.refreshPlaceholders();

        assertEquals(0, fixture.state.registrations.get());
        assertEquals(0, fixture.state.unregistrations.get());
        assertEquals(1, fixture.state.titleWrites.get());
        assertEquals(1, fixture.state.prefixWrites.get());
        assertEquals(1, fixture.state.numberFormatWrites.get());
        assertEquals(Sidebar.component(ChatColor.GREEN + "Game 2"), fixture.state.title);
        assertEquals(Sidebar.component(ChatColor.GRAY + "Alive: 2"), fixture.state.prefix);
        assertEquals(Sidebar.component("2"),
                ((FixedFormat) fixture.state.scoreNumberFormat).component());

        fixture.sidebar.refreshPlaceholders();
        assertEquals(1, fixture.state.titleWrites.get(), "same title must not be resent");
        assertEquals(1, fixture.state.prefixWrites.get(), "same line must not be resent");
        assertEquals(1, fixture.state.numberFormatWrites.get(), "same score must not be resent");
    }

    private static Fixture fixture(AtomicReference<String> alive) {
        PlaceholderProvider provider = new PlaceholderProvider("{alive}", alive::get);
        Sidebar sidebar = new Sidebar(
                new SidebarLine("&aGame {alive}"),
                List.of(new TestScoredLine("&7Alive: {alive}", "{alive}")),
                List.of(provider));
        SidebarState state = new SidebarState();
        Scoreboard scoreboard = scoreboard(state);
        fieldMap(sidebar, "scoreboards").put(UUID.randomUUID(), scoreboard);
        return new Fixture(sidebar, state);
    }

    private static Scoreboard scoreboard(SidebarState state) {
        Score score = (Score) Proxy.newProxyInstance(
                Score.class.getClassLoader(), new Class<?>[]{Score.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isScoreSet" -> true;
                    case "getScore" -> state.score;
                    case "setScore" -> {
                        state.score = (int) args[0];
                        yield null;
                    }
                    case "numberFormat" -> {
                        if (args == null || args.length == 0) yield state.scoreNumberFormat;
                        state.scoreNumberFormat = (NumberFormat) args[0];
                        state.numberFormatWrites.incrementAndGet();
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Objective objective = (Objective) Proxy.newProxyInstance(
                Objective.class.getClassLoader(), new Class<?>[]{Objective.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "displayName" -> {
                        if (args == null || args.length == 0) yield state.title;
                        state.title = (Component) args[0];
                        state.titleWrites.incrementAndGet();
                        yield null;
                    }
                    case "getScore" -> score;
                    case "getDisplaySlot" -> org.bukkit.scoreboard.DisplaySlot.SIDEBAR;
                    case "unregister" -> {
                        state.unregistrations.incrementAndGet();
                        yield null;
                    }
                    case "setDisplaySlot" -> null;
                    case "numberFormat" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Team team = (Team) Proxy.newProxyInstance(
                Team.class.getClassLoader(), new Class<?>[]{Team.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "prefix" -> {
                        if (args == null || args.length == 0) yield state.prefix;
                        state.prefix = (Component) args[0];
                        state.prefixWrites.incrementAndGet();
                        yield null;
                    }
                    case "hasEntry" -> ChatColor.BLACK.toString().equals(args[0]);
                    case "addEntry" -> true;
                    case "unregister" -> {
                        state.unregistrations.incrementAndGet();
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (Scoreboard) Proxy.newProxyInstance(
                Scoreboard.class.getClassLoader(), new Class<?>[]{Scoreboard.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getObjective" -> objective;
                    case "getTeam" -> "bw_l_0".equals(args[0]) ? team : null;
                    case "registerNewObjective" -> {
                        state.registrations.incrementAndGet();
                        yield objective;
                    }
                    case "registerNewTeam" -> {
                        state.registrations.incrementAndGet();
                        yield team;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> fieldMap(Sidebar sidebar, String fieldName) {
        try {
            Field field = Sidebar.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Map<K, V>) field.get(sidebar);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to inspect Sidebar." + fieldName, exception);
        }
    }

    private record Fixture(Sidebar sidebar, SidebarState state) {
    }

    private static final class TestScoredLine extends SidebarLine implements ScoredLine {
        private final String score;

        private TestScoredLine(String line, String score) {
            super(line);
            this.score = score;
        }

        @Override
        public String getScore() {
            return score;
        }
    }

    private static final class SidebarState {
        private Component title = Sidebar.component(ChatColor.GREEN + "Game 1");
        private Component prefix = Sidebar.component(ChatColor.GRAY + "Alive: 1");
        private int score = 1;
        private NumberFormat scoreNumberFormat = NumberFormat.fixed(Sidebar.component("1"));
        private final AtomicInteger registrations = new AtomicInteger();
        private final AtomicInteger unregistrations = new AtomicInteger();
        private final AtomicInteger titleWrites = new AtomicInteger();
        private final AtomicInteger prefixWrites = new AtomicInteger();
        private final AtomicInteger numberFormatWrites = new AtomicInteger();
    }
}
