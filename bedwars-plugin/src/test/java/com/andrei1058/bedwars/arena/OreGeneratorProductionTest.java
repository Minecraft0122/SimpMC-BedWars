package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.generator.GeneratorType;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OreGeneratorProductionTest {
    private BedWars previousPlugin;
    private MockedStatic<BedWars> bedWars;
    private IArena arena;
    private ITeam team;
    private ConfigManager arenaConfig;
    private World world;
    private final List<Player> members = new ArrayList<>();

    @BeforeEach void setUp() {
        previousPlugin = BedWars.plugin;
        BedWars.plugin = mock(BedWars.class);
        when(BedWars.plugin.getConfig()).thenReturn(new YamlConfiguration());
        ConfigManager generators = mock(ConfigManager.class);
        when(generators.getYml()).thenReturn(new YamlConfiguration());
        when(generators.getInt(anyString())).thenReturn(2);
        bedWars = mockStatic(BedWars.class);
        bedWars.when(BedWars::getGeneratorsCfg).thenReturn(generators);
        arenaConfig = mock(ConfigManager.class);
        when(arenaConfig.getBoolean(ConfigPath.ARENA_DISABLE_GENERATOR_FOR_EMPTY_TEAMS)).thenReturn(true);
        arena = mock(IArena.class);
        when(arena.getConfig()).thenReturn(arenaConfig);
        when(arena.getStatus()).thenReturn(GameState.playing);
        when(arena.getRegionsList()).thenReturn(new ArrayList<>());
        team = mock(ITeam.class);
        when(team.getMembers()).thenReturn(members);
        world = mock(World.class);
    }

    @AfterEach void tearDown() {
        bedWars.close();
        BedWars.plugin = previousPlugin;
    }

    @Test void upstreamCountdownDecrementsToZeroBeforeSpawningAndAllowsZeroDelay() {
        OreGenerator generator = generator(GeneratorType.IRON, null);
        generator.spawn();
        generator.spawn();
        verify(generator, never()).dropItem(any());
        assertEquals(0, generator.getNextSpawn());
        generator.spawn();
        verify(generator).dropItem(any());
        generator.setDelay(0);
        generator.setNextSpawn(0);
        generator.spawn();
        generator.spawn();
        verify(generator, times(3)).dropItem(any());
    }

    @ParameterizedTest @EnumSource(value=GeneratorType.class, names={"IRON", "GOLD", "DIAMOND", "EMERALD"})
    void backlogCountsEntitiesAndResumesAfterPickup(GeneratorType type) {
        OreGenerator generator = generator(type, null);
        Item backlog = mock(Item.class);
        ItemStack stack = mock(ItemStack.class);
        when(backlog.getItemStack()).thenReturn(stack);
        Material material = generator.getOre().getType();
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenReturn(64);
        when(world.getNearbyEntitiesByType(eq(Item.class), any(Location.class), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(backlog));
        generator.setSpawnLimit(2);
        cycle(generator);
        verify(generator).dropItem(any());
        when(world.getNearbyEntitiesByType(eq(Item.class), any(Location.class), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(backlog, backlog));
        cycle(generator);
        verify(generator).dropItem(any());
        when(world.getNearbyEntitiesByType(eq(Item.class), any(Location.class), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of());
        cycle(generator);
        verify(generator, times(2)).dropItem(any());
    }

    @Test void emptyEliminatedIslandStopsButTravelRespawnAndDisabledOptionDoNot() {
        OreGenerator generator = generator(GeneratorType.GOLD, team);
        when(team.isBedDestroyed()).thenReturn(true);
        cycle(generator);
        verify(generator, never()).dropItem(any());
        Player player = mock(Player.class);
        members.add(player);
        when(arena.isReSpawning(player)).thenReturn(true);
        cycle(generator);
        verify(generator).dropItem(any());
        members.clear();
        when(arenaConfig.getBoolean(ConfigPath.ARENA_DISABLE_GENERATOR_FOR_EMPTY_TEAMS)).thenReturn(false);
        cycle(generator);
        verify(generator, times(2)).dropItem(any());
    }

    @Test void zeroLimitNeverQueriesBacklogAndNonPlayingNeverProduces() {
        OreGenerator generator = generator(GeneratorType.DIAMOND, null);
        generator.setSpawnLimit(0);
        cycle(generator);
        verify(generator).dropItem(any());
        verifyNoInteractions(world);
        when(arena.getStatus()).thenReturn(GameState.restarting);
        cycle(generator);
        verify(generator).dropItem(any());
    }

    @Test void stackOptionStillEmitsOneEntityPerDropAsUpstreamDoes() {
        OreGenerator generator = generator(GeneratorType.IRON, null);
        doCallRealMethod().when(generator).dropItem(any());
        generator.stack = true;
        generator.setAmount(3);
        when(world.dropItem(any(), any())).thenReturn(mock(Item.class));
        try (var stacks = mockConstruction(ItemStack.class)) {
            generator.dropItem(generator.getLocation());
            assertEquals(3, stacks.constructed().size());
        }
        verify(world, times(3)).dropItem(any(), any());
    }

    private void cycle(OreGenerator generator) {
        for (int second = 0; second <= generator.getDelay(); second++) generator.spawn();
    }

    private OreGenerator generator(GeneratorType type, ITeam owner) {
        try (var stacks = mockConstruction(ItemStack.class, (item, context) ->
                when(item.getType()).thenReturn((Material) context.arguments().getFirst()))) {
            OreGenerator generator = spy(new OreGenerator(new Location(world, 0, 65, 0), arena, type, owner));
            doNothing().when(generator).dropItem(any());
            return generator;
        }
    }
}
