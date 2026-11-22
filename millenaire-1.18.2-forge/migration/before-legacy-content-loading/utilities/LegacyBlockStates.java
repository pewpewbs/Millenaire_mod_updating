package org.millenaire.common.utilities;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.Registry;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.block.LegacyVariantBlock;

/** Decodes an explicitly named pre-flattening block. Never guesses a legacy family from a modern block. */
public final class LegacyBlockStates {
    private static final Map<String, Integer> IDS = readIds();
    private static final Map<Integer, BlockState> VANILLA = new ConcurrentHashMap<>();
    private LegacyBlockStates() {}
    private static Map<String, Integer> readIds() {
        Map<String, Integer> result = new HashMap<>();
        try (InputStream input = LegacyBlockStates.class.getResourceAsStream("/millenaire/legacy-block-ids.txt")) {
            if (input == null) throw new IOException("Missing legacy block ID table");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null;) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] pair = line.split("=", 2);
                    result.put(pair[0], Integer.parseInt(pair[1]));
                }
            }
        } catch (IOException | RuntimeException error) { throw new IllegalStateException("Cannot load legacy block IDs", error); }
        return Map.copyOf(result);
    }
    /** Numeric IDs are valid only for the historical vanilla registry. Modded numeric IDs need their old registry mapping. */
    public static BlockState fromLegacyId(int blockId, int metadata) {
        String name = IDS.entrySet().stream().filter(entry -> entry.getValue() == blockId)
            .map(Map.Entry::getKey).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown legacy block ID: " + blockId));
        return fromName(name, metadata);
    }
    public static BlockState fromName(String name, int metadata) {
        if (name.startsWith("v2975/")) {
            if (metadata != 0) throw new IllegalArgumentException("Modern states use named properties, not metadata: " + name);
            return fromName(name, "");
        }
        ResourceLocation location = new ResourceLocation(name);
        String key = location.toString();
        if (key.equals("minecraft:flower_pot")) return LegacyFlowerPots.fromPlan(metadata);
        Integer legacyId = IDS.get(key);
        if (legacyId != null) {
            if (metadata < 0 || metadata > 15) throw new IllegalArgumentException("Invalid block metadata: " + key + "/" + metadata);
            return VANILLA.computeIfAbsent((legacyId << 4) | metadata, LegacyBlockStates::decodeVanilla);
        }
        Block block = Registry.BLOCK.getOptional(location).orElseThrow(() -> new IllegalArgumentException("Unknown block: " + key));
        if (block instanceof LegacyVariantBlock legacy) return legacy.func_176203_a(metadata);
        if (metadata != 0) throw new IllegalArgumentException("Block has no legacy metadata codec: " + key + "/" + metadata);
        return block.defaultBlockState();
    }
    private record OldState(int id, CompoundTag properties) {}
    private static class PropertyTable {
        static final Map<String, List<OldState>> STATES = load();
        static Map<String, List<OldState>> load() {
            Map<String, List<OldState>> result = new HashMap<>();
            try (InputStream input = LegacyBlockStates.class.getResourceAsStream("/millenaire/legacy-block-properties.txt")) {
                if (input == null) throw new IOException("Missing legacy property table");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    for (String line; (line = reader.readLine()) != null;) {
                        String[] pair = line.split("\t", 2);
                        CompoundTag tag = TagParser.parseTag(pair[1]);
                        result.computeIfAbsent(tag.getString("Name"), key -> new ArrayList<>())
                            .add(new OldState(Integer.parseInt(pair[0]), tag.getCompound("Properties")));
                    }
                }
            } catch (Exception error) { throw new IllegalStateException("Cannot load legacy block properties", error); }
            result.values().forEach(list -> list.sort(Comparator.comparingInt(OldState::id)));
            return result;
        }
    }
    private record NamedState(BlockState state, int metadata) {}
    private static final Map<String, NamedState> NAMED = new ConcurrentHashMap<>();
    public static boolean containsName(String name) {
        if (name.startsWith("v2975/")) return Registry.BLOCK.containsKey(new ResourceLocation(name.substring(6)));
        ResourceLocation id = new ResourceLocation(name);
        return IDS.containsKey(id.toString()) || Registry.BLOCK.containsKey(id);
    }
    public static BlockState fromName(String name, String values) { return named(name, values).state; }
    public static int metadata(String name, String values) { return named(name, values).metadata; }
    private static NamedState named(String name, String values) {
        return NAMED.computeIfAbsent(name + ";" + values, key -> decodeNamed(name, values));
    }
    private static NamedState decodeNamed(String name, String values) {
        // Newly written states carry the data version: legacy grass and modern grass are different blocks.
        if (name.startsWith("v2975/")) {
            ResourceLocation id = new ResourceLocation(name.substring(6));
            Block block = Registry.BLOCK.getOptional(id).orElseThrow(() -> new IllegalArgumentException("Unknown modern block: " + id));
            BlockState state = block.defaultBlockState();
            if (!values.isBlank() && !values.equals("default")) for (String entry : values.split(",")) {
                String[] pair = entry.trim().split("=", 2);
                var property = pair.length == 2 ? block.getStateDefinition().getProperty(pair[0].trim()) : null;
                if (property == null || property.getValue(pair[1].trim()).isEmpty())
                    throw new IllegalArgumentException("Invalid modern block property: " + name + ";" + entry);
                state = setProperty(state, property, pair[1].trim());
            }
            return new NamedState(state, block instanceof LegacyVariantBlock legacy ? legacy.func_176201_c(state) : 0);
        }

        String key = new ResourceLocation(name).toString();
        if (!IDS.containsKey(key)) {
            BlockState base = fromName(key, 0);
            BlockState state = values.isBlank() || values.equals("default") ? base : BlockStateUtilities.getBlockStateWithValues(base, values);
            return new NamedState(state, state.getBlock() instanceof LegacyVariantBlock legacy ? legacy.func_176201_c(state) : 0);
        }
        if (values.isBlank() || values.equals("default")) values = "";
        Map<String, String> wanted = new LinkedHashMap<>();
        if (!values.isEmpty()) for (String entry : values.split(",")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length != 2) throw new IllegalArgumentException("Invalid block properties: " + values);
            wanted.put(pair[0].trim(), pair[1].trim());
        }
        BlockState best = null;
        int bestScore = Integer.MAX_VALUE;
        int bestMetadata = 0;
        for (OldState old : PropertyTable.STATES.getOrDefault(key, List.of())) {
            if (!wanted.entrySet().stream().allMatch(entry -> old.properties.contains(entry.getKey())
                    && old.properties.getString(entry.getKey()).equals(entry.getValue()))) continue;
            BlockState candidate = VANILLA.computeIfAbsent(old.id, LegacyBlockStates::decodeVanilla);
            // Metadata discards neighbour-derived properties (door hinge, stair shape, etc.).
            // Explicit plan values must survive even when they were not stored in the old metadata.
            for (var entry : wanted.entrySet()) {
                var property = candidate.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property != null) candidate = setProperty(candidate, property, entry.getValue());
            }
            BlockState defaults = candidate.getBlock().defaultBlockState();
            int score = 0;
            for (var property : candidate.getProperties())
                if (!wanted.containsKey(property.getName()) && !candidate.getValue(property).equals(defaults.getValue(property))) score++;
            if (score < bestScore) { best = candidate; bestScore = score; bestMetadata = old.id & 15; }
        }
        if (best == null && Registry.BLOCK.containsKey(new ResourceLocation(key))) {
            BlockState modern = Registry.BLOCK.get(new ResourceLocation(key)).defaultBlockState();
            boolean valid = true;
            for (var entry : wanted.entrySet()) {
                var property = modern.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property == null || property.getValue(entry.getValue()).isEmpty()) { valid = false; break; }
                modern = setProperty(modern, property, entry.getValue());
            }
            if (valid) return new NamedState(modern, 0);
        }
        if (best == null) throw new IllegalArgumentException("Unknown legacy block properties: " + key + ";" + values);
        return new NamedState(best, bestMetadata);
    }
    private static <T extends Comparable<T>> BlockState setProperty(BlockState state,
            net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }
    private static final Map<String, Set<Block>> FAMILIES = new ConcurrentHashMap<>();
    public static boolean matchesName(Block block, String configured) {
        ResourceLocation id = ResourceLocation.tryParse(configured.trim());
        if (id == null) return false;
        if (Registry.BLOCK.getKey(block).equals(id)) return true;
        String key = id.toString();
        if (!IDS.containsKey(key)) return false;
        return FAMILIES.computeIfAbsent(key, name -> {
            Set<Block> blocks = new HashSet<>();
            int count = name.equals("minecraft:flower_pot") ? 22 : 16;
            for (int metadata = 0; metadata < count; metadata++) blocks.add(fromName(name, metadata).getBlock());
            return Set.copyOf(blocks);
        }).contains(block);
    }
    private static BlockState decodeVanilla(int stateId) {
        // BlockStateData has already performed the 1.13 flattening. Apply later renames through 1.18.2.
        Dynamic<?> flattened = BlockStateData.getTag(stateId);
        Dynamic<?> current = DataFixers.getDataFixer().update(References.BLOCK_STATE, flattened, 1451, 2975);
        CompoundTag tag = (CompoundTag)current.convert(NbtOps.INSTANCE).getValue();
        ResourceLocation location = ResourceLocation.tryParse(tag.getString("Name"));
        if (location == null) throw new IllegalArgumentException("Legacy block state needs additional data or has invalid metadata: " + stateId);
        if (!Registry.BLOCK.containsKey(location)) throw new IllegalArgumentException("Missing converted block: " + location);
        return NbtUtils.readBlockState(tag);
    }
}
