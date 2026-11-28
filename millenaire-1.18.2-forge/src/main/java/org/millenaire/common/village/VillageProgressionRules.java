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

    record TagProject<K>(K key, Set<String> requires, Set<String> provides) {}

    /** Only projects whose required tags are already present or reachable
     * from an authored project enter the dependency graph. Unseeded cycles
     * and unavailable specializations cannot create a fictitious supplier. */
    static <K> Set<K> reachableTagProjects(Set<String> existing, Collection<TagProject<K>> projects) {
        Set<String> tags = new HashSet<>(existing);
        Set<K> reachable = new HashSet<>();
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            boolean changed = false;
            for (TagProject<K> project : projects) {
                if (!tags.containsAll(project.requires())) continue;
                if (reachable.add(project.key())) changed = true;
                if (tags.addAll(project.provides())) changed = true;
            }
            if (!changed) break;
        }
        return reachable;
    }

    /** Preserve tier/level weighting while resolving saturated priority ties
     * by the authored priority instead of alphabetical building names. */
    static int compareRank(int rankA, int priorityA, int rankB, int priorityB) {
        int comparison = Integer.compare(rankA, rankB);
        return comparison != 0 ? comparison : Integer.compare(priorityA, priorityB);
    }

    static int rank(int tier, int level, int priority) {
        return tier + (level == 0 ? 20 : 0)
                - 10 * Math.min(5, Math.max(0,level))
                + 4 * Math.min(25, Math.max(1,priority));
    }
}
