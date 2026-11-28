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
        check(VillageProgressionRules.compareRank(198,6000,198,5900)>0, "authored priority breaks saturated tie");
        check(VillageProgressionRules.compareRank(198,5900,198,5000)>0, "weaver before lower priority");
        check(VillageProgressionRules.compareRank(199,1,198,6000)>0, "existing tier and level weighting retained");
        var field = new VillageProgressionRules.TagProject<>("field",Set.of(),Set.of("cottonfield"));
        var weaver = new VillageProgressionRules.TagProject<>("weaver",Set.of("cottonfield"),Set.of("textiles"));
        var market = new VillageProgressionRules.TagProject<>("market",Set.of("textiles"),Set.<String>of());
        var cycleA = new VillageProgressionRules.TagProject<>("cycleA",Set.of("b"),Set.of("a"));
        var cycleB = new VillageProgressionRules.TagProject<>("cycleB",Set.of("a"),Set.of("b"));
        check(VillageProgressionRules.reachableTagProjects(Set.of(),java.util.List.of(market,weaver,field,cycleA,cycleB))
                .equals(Set.of("field","weaver","market")), "multi-step tags resolved; isolated cycle rejected");
        check(VillageProgressionRules.reachableTagProjects(Set.of(),java.util.List.of(weaver,market)).isEmpty(),
                "specialized village without field cannot invent cotton supplier");
        check(VillageProgressionRules.reachableTagProjects(Set.of("cottonfield"),java.util.List.of(weaver,market))
                .equals(Set.of("weaver","market")), "existing field unlocks workshop");
        System.out.println("PASS: village progression rule checks");
    }
}
