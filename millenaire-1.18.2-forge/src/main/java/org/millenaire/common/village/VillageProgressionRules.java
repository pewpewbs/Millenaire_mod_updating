package org.millenaire.common.village;

import java.util.*;

/** Pure Java rules, shared by the runtime scheduler and standalone smoke tests. */
final class VillageProgressionRules {
    static final int MAX_PASSES = 8;
    static final int MAX_ITEMS = 128;
    private VillageProgressionRules() {}

    static String styleFamily(String namespace, String path) {
        if (namespace == null || path == null || path.startsWith("path") || path.contains("road")) return null;
        if (path.startsWith("stairs_")) path = path.substring(7);
        if (path.startsWith("slab_")) path = path.substring(5);
        for (String suffix : List.of("_stairs", "_slab", "_wall"))
            if (path.endsWith(suffix)) path = path.substring(0, path.length() - suffix.length());
        if (path.endsWith("stone_bricks")) path = path.substring(0, path.length() - 1);
        if (!(path.contains("sandstone") || path.contains("tiles") || path.contains("terracotta")
                || path.contains("stone_brick") || path.contains("stonebrick") || path.contains("painted_brick")
                || path.contains("mudbrick") || path.contains("mud_brick") || path.contains("cookedbrick")
                || path.contains("timberframe") || path.contains("timber_frame") || path.contains("thatch")
                || path.equals("paper_wall") || path.contains("marble") || path.contains("snowbrick")
                || path.contains("icebrick") || path.equals("bricks") || path.equals("brick"))) return null;
        if (path.equals("brick")) path = "bricks";
        return namespace + ":" + path;
    }

    static Set<String> futureOnly(Set<String> past, Set<String> future) {
        Set<String> result = new HashSet<>(future);
        result.removeAll(past);
        return Set.copyOf(result);
    }

    static boolean intersects(Set<String> introduced, Set<String> locked) {
        return !Collections.disjoint(introduced, locked);
    }

    static int rank(int tier, int level, int priority) {
        return tier + (level == 0 ? 20 : 0)
                - 10 * Math.min(5, Math.max(0,level))
                + 4 * Math.min(25, Math.max(1,priority));
    }
}
