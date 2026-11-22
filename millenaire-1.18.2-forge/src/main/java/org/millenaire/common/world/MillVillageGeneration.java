package org.millenaire.common.world;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.event.world.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.config.MillConfigValues;

/** Decoration only records candidates. Village construction runs on the owning server thread. */
@Mod.EventBusSubscriber(modid="millenaire", bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MillVillageGeneration {
    private static final Map<ServerLevel, Set<ChunkPos>> PENDING = new ConcurrentHashMap<>();
    private static final Map<ConfiguredStructureFeature<?, ?>, HolderSet<net.minecraft.world.level.biome.Biome>> VANILLA_BIOMES = new IdentityHashMap<>();
    private static final WorldGenVillage GENERATOR = new WorldGenVillage();
    private static PlacedFeature placed;
    @SubscribeEvent public static void register(RegistryEvent.Register<Feature<?>> event) {
        Feature<NoneFeatureConfiguration> feature = new Feature<>(NoneFeatureConfiguration.CODEC) {
            @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
                ServerLevel world = context.level().getLevel();
                if (!world.dimension().equals(Level.OVERWORLD)) return false;
                PENDING.computeIfAbsent(world, ignored -> ConcurrentHashMap.newKeySet())
                    .add(new ChunkPos(context.origin()));
                return true;
            }
        };
        feature.setRegistryName("millenaire", "village_candidate");
        event.getRegistry().register(feature);
        placed = new PlacedFeature(Holder.direct(new ConfiguredFeature<>(feature, NoneFeatureConfiguration.INSTANCE)), List.of());
        MinecraftForge.EVENT_BUS.register(new Events());
    }
    public static final class Events {
        @SubscribeEvent public void biome(BiomeLoadingEvent event) {
            if (placed != null) event.getGeneration().addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, Holder.direct(placed));
        }
        @SubscribeEvent public void tick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.world instanceof ServerLevel world)
                || !Mill.loadingComplete || Mill.startupError || Mill.getMillWorld(world) == null) return;
            var candidates = PENDING.get(world);
            if (candidates == null) return;
            for (ChunkPos pos : candidates) {
                if (!world.hasChunk(pos.x, pos.z)) continue;
                if (!candidates.remove(pos)) continue;
                Random random = new Random(world.getSeed());
                long xSalt = random.nextLong() | 1L, zSalt = random.nextLong() | 1L;
                random.setSeed(((long)pos.x * xSalt + (long)pos.z * zSalt) ^ world.getSeed());
                GENERATOR.generate(random, pos.x, pos.z, world);
                break;
            }
        }
        @SubscribeEvent public void unload(WorldEvent.Unload event) {
            if (event.getWorld() instanceof ServerLevel world) PENDING.remove(world);
        }
        @SubscribeEvent public void starting(ServerAboutToStartEvent event) {
            if (!MillConfigValues.stopDefaultVillages) return;
            var registry = event.getServer().registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
            for (var structure : registry) if (structure.feature == StructureFeature.VILLAGE) {
                VANILLA_BIOMES.put(structure, structure.biomes);
                net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(
                    ConfiguredStructureFeature.class, structure, HolderSet.direct(List.of()), "f_209743_");
            }
        }
        @SubscribeEvent public void stopped(ServerStoppedEvent event) {
            VANILLA_BIOMES.forEach((structure, biomes) -> net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(
                ConfiguredStructureFeature.class, structure, biomes, "f_209743_"));
            VANILLA_BIOMES.clear();
            PENDING.clear();
        }
    }
}
