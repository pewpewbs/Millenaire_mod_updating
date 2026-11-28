package org.millenaire.common.world;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/** Read-only, per-generation-attempt memoization of chunk availability. */
final class VillageGenerationProbe {
    private final Map<Long, Boolean> checked = new HashMap<>();
    private final BiPredicate<Integer, Integer> generated;
    private int hits;

    VillageGenerationProbe(BiPredicate<Integer, Integer> generated) {
        this.generated = generated;
    }

    boolean checkArea(int startX, int startZ, int endX, int endZ) {
        // Match WorldUtilities.checkChunksGenerated exactly, including its
        // extra end chunk and arithmetic shift for negative coordinates.
        for (int x = startX >> 4; x <= (endX >> 4) + 1; x++) {
            for (int z = startZ >> 4; z <= (endZ >> 4) + 1; z++) {
                long key = ((long)x & 0xffffffffL) | ((long)z << 32);
                Boolean available = checked.get(key);
                if (available == null) {
                    available = generated.test(x, z);
                    checked.put(key, available);
                } else {
                    hits++;
                }
                if (!available) return false;
            }
        }
        return true;
    }

    int uniqueChecks() { return checked.size(); }
    int cacheHits() { return hits; }
}
