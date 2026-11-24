package org.millenaire.common.village;

import java.util.Set;

/** javac --release 17 -d build/progression-smoke VillageProgressionRules.java VillageProgressionRulesSmoke.java */
public final class VillageProgressionRulesSmoke {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    public static void main(String[] args) {
        String tiles = VillageProgressionRules.styleFamily("millenaire","byzantine_tiles");
        check(tiles.equals(VillageProgressionRules.styleFamily("millenaire","stairs_byzantine_tiles")), "stairs share tile milestone");
        check(tiles.equals(VillageProgressionRules.styleFamily("millenaire","byzantine_tiles_slab")), "slabs share tile milestone");
        check(!tiles.equals(VillageProgressionRules.styleFamily("millenaire","japanese_tiles")), "cultures keep distinct styles");
        check(VillageProgressionRules.styleFamily("minecraft","sandstone").equals(
              VillageProgressionRules.styleFamily("minecraft","sandstone_wall")), "sandstone walls share milestone");
        check(VillageProgressionRules.styleFamily("minecraft","stone_bricks").equals(
              VillageProgressionRules.styleFamily("minecraft","stone_brick_stairs")), "stone brick family");
        check(VillageProgressionRules.styleFamily("millenaire","pathsandstone") == null, "paths are not style gates");
        check(VillageProgressionRules.styleFamily("minecraft","oak_log") == null, "ordinary inputs not gates");
        check(VillageProgressionRules.styleFamily("minecraft","glass") == null, "glass not a guessed gate");
        Set<String> locked = VillageProgressionRules.futureOnly(Set.of("old"),Set.of("old",tiles));
        check(locked.equals(Set.of(tiles)), "completed central style is unlocked");
        check(VillageProgressionRules.intersects(Set.of(tiles),locked), "new dependent material is blocked");
        check(!VillageProgressionRules.intersects(Set.of("old"),locked), "existing style not blocked");
        check(!VillageProgressionRules.intersects(Set.of(tiles),Set.of()), "empty gate never blocks");
        check(VillageProgressionRules.rank(78,0,1) > VillageProgressionRules.rank(78,3,1), "foundations before late upgrades");
        check(VillageProgressionRules.rank(86,1,1) > VillageProgressionRules.rank(45,1,1), "centre outranks decorative upgrades");
        check(VillageProgressionRules.rank(78,1,5) > VillageProgressionRules.rank(78,1,1), "authored priority retained");
        check(VillageProgressionRules.MAX_PASSES == 8 && VillageProgressionRules.MAX_ITEMS == 128, "dependency bounds");
        System.out.println("PASS: 16 village progression rule checks");
    }
}
