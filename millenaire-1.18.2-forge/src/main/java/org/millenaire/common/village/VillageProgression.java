package org.millenaire.common.village;

import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.buildingplan.PointType;
import org.millenaire.common.culture.VillagerType;
import org.millenaire.common.goal.*;
import org.millenaire.common.goal.generic.*;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.utilities.MillLog;

/** Runtime, culture-independent progression. Does not change saves or active constructions. */
final class VillageProgression {
    record Candidate(BuildingProject project, BuildingPlan plan) {}
    private record Recipe(InvItem output, Map<InvItem,Integer> inputs) {}
    private static final Map<BuildingPlan, Set<String>> PALETTES = new WeakHashMap<>();
    private static final Map<BuildingPlan, Set<InvItem>> QUARRY_OUTPUTS = new WeakHashMap<>();
    private VillageProgression() {}

    static boolean enabled(Building v) {
        return v != null && v.isTownhall && v.villageType != null && v.villageType.isRegularVillage()
                && !v.villageType.playerControlled && v.location != null && !v.location.isCustomBuilding;
    }

    static String styleFamily(ResourceLocation id) {
        return id == null ? null : VillageProgressionRules.styleFamily(id.getNamespace(), id.getPath());
    }

    private static synchronized Set<String> palette(BuildingPlan plan) {
        if (plan == null || plan.plan == null) return Set.of();
        return PALETTES.computeIfAbsent(plan, p -> {
            Set<String> result = new HashSet<>();
            for (PointType[][] floor : p.plan) {
                if (floor == null) continue;
                for (PointType[] row : floor) {
                    if (row == null) continue;
                    for (PointType point : row) {
                        if (point == null || point.getBlock() == null || point.getBlock() == Blocks.AIR) continue;
                        String family = styleFamily(Registry.BLOCK.getKey(point.getBlock()));
                        if (family != null) result.add(family);
                    }
                }
            }
            return Set.copyOf(result);
        });
    }

    private static BuildingProject centreProject(Building v) {
        if (!enabled(v)) return null;
        for (BuildingProject p : v.getFlatProjectList())
            if (p.location != null && p.location.isSameLocation(v.location) && p.planSet != null
                    && p.location.level >= 0) return p;
        return null;
    }

    record StyleGate(Candidate centre, Set<String> locked) {
        static final StyleGate NONE = new StyleGate(null, Set.of());
        boolean blocks(BuildingPlan p) {
            if (centre == null || p == null || Objects.equals(p.buildingKey, centre.plan().buildingKey)
                    || locked.isEmpty()) return false;
            Set<String> introduced = new HashSet<>(palette(p));
            BuildingPlan previous = p;
            while (previous.level > 0) {
                BuildingPlan older = previous.getPreviousBuildingPlan();
                if (older == null || older.level >= previous.level) break;
                introduced.removeAll(palette(older));
                previous = older;
            }
            return VillageProgressionRules.intersects(introduced, locked);
        }
    }

    static StyleGate styleGate(Building v, boolean ignoreCost) {
        BuildingProject centre = centreProject(v);
        if (centre == null || !centre.location.upgradesAllowed) return StyleGate.NONE;
        BuildingPlan next = centre.getNextBuildingPlan(false);
        if (next == null || !v.validProgressionPlan(centre, next)) return StyleGate.NONE;
        // Only enforce inferred barriers while the centre is ready or active.
        // Inaccessible producers/imports must not permanently deadlock a village.
        if (v.findConstructionIPforLocation(centre.location) == null
                && !ignoreCost && !v.canAffordProgression(next, false)) return StyleGate.NONE;
        BuildingPlan[] levels = centre.planSet.plans.get(centre.location.getVariation());
        Set<String> past = new HashSet<>(), future = new HashSet<>();
        for (int n = 0; n < levels.length; n++)
            (n <= centre.location.level ? past : future).addAll(palette(levels[n]));
        return new StyleGate(new Candidate(centre, next), VillageProgressionRules.futureOnly(past, future));
    }

    static boolean deferStyle(Building v, BuildingPlan p, boolean ignoreCost) {
        return styleGate(v, ignoreCost).blocks(p);
    }

    private static boolean busy(Building v, BuildingProject p) {
        return p.location == null ? v.findConstructionIPforBuildingPlanKey(p.key, true) != null
                : v.findConstructionIPforLocation(p.location) != null;
    }

    private static List<Candidate> candidates(Building v, List<BuildingProject> projects,
                                               boolean affordable, boolean ignoreCost, StyleGate gate) {
        List<Candidate> result = new ArrayList<>();
        for (BuildingProject p : projects) {
            if (p.planSet == null || busy(v,p)) continue;
            BuildingPlan plan = p.getNextBuildingPlan(false);
            if (p.location == null) {
                BuildingPlan variant = p.getNextBuildingPlan(true);
                if (v.validProgressionPlan(p,variant) && !gate.blocks(variant)
                        && (!affordable || ignoreCost || v.canAffordProgression(variant,true))) plan = variant;
            }
            if (!v.validProgressionPlan(p,plan) || gate.blocks(plan)) continue;
            if (affordable && !ignoreCost && !v.canAffordProgression(plan,true)) continue;
            result.add(new Candidate(p,plan));
        }
        return result;
    }

    private static int rank(Candidate c) {
        int tier = switch (c.project().projectTier) {
            case PLAYER -> 105; case CENTRE -> 86; case START -> 84;
            case CORE -> 78; case SECONDARY -> 60; case EXTRA -> 45; default -> 40;
        };
        return VillageProgressionRules.rank(tier, c.plan().level, c.plan().priority);
    }

    static Candidate select(Building v, List<BuildingProject> projects, BuildingPlan waiting,
                            boolean affordable, boolean ignoreCost) {
        StyleGate gate = styleGate(v,ignoreCost);
        List<Candidate> options = candidates(v,projects,affordable,ignoreCost,gate);
        if (options.isEmpty()) return null;
        if (gate.centre() != null && !gate.locked().isEmpty()) {
            boolean dependent = v.getFlatProjectList().stream().anyMatch(p ->
                    p.planSet != null && gate.blocks(p.getNextBuildingPlan(false)));
            if (dependent) for (Candidate c : options)
                if (c.project() == gate.centre().project()) {
                    if (MillConfigValues.LogBuildingPlan >= 1)
                        MillLog.major(v, "VILLAGE-PROGRESSION centre=" + c.project().key
                                + " level=" + c.plan().level + " lockedStyles=" + gate.locked());
                    return c;
                }
        }
        Candidate preferred = options.stream().max(Comparator.comparingInt(VillageProgression::rank)
                .thenComparing(c -> c.project().key, Comparator.reverseOrder())).orElse(null);
        BuildingPlan target = waiting != null ? waiting : preferred.plan();
        if (!ignoreCost && !v.canAffordProgression(target,false)) {
            Candidate supplier = prerequisite(v,options,target,affordable);
            if (supplier != null) return supplier;
        }
        return preferred;
    }

    static Candidate replacement(Building v, List<BuildingProject> projects, BuildingPlan waiting, boolean ignoreCost) {
        StyleGate gate = styleGate(v,ignoreCost);
        if (gate.blocks(waiting) && !busy(v,gate.centre().project())) return gate.centre();
        if (ignoreCost || v.canAffordProgression(waiting,false)) return null;
        return prerequisite(v,candidates(v,projects,false,false,gate),waiting,false);
    }

    private static void output(List<Recipe> out, InvItem item, Map<InvItem,Integer> inputs) {
        if (item != null && item.getItem() != null) out.add(new Recipe(item,inputs));
    }

    private static Set<String> tags(BuildingPlan p) {
        List<BuildingPlan> chain = new ArrayList<>();
        while (p != null) {
            chain.add(p);
            if (p.level <= 0) break;
            BuildingPlan old = p.getPreviousBuildingPlan();
            if (old == null || old.level >= p.level) break;
            p = old;
        }
        Set<String> tags = new HashSet<>();
        for (int n = chain.size()-1; n >= 0; n--) {
            tags.addAll(chain.get(n).tags);
            tags.removeAll(chain.get(n).clearTags);
        }
        return tags;
    }

    // Mirror the old quarry goal's explicitly supported drops from the same
    // source markers BuildingPlan.referenceBuildingPoints registers.
    // Quartz/red sandstone are not guessed: the old action may not harvest them.
    private static synchronized Set<InvItem> quarryOutputs(BuildingPlan plan) {
        if (plan == null || plan.plan == null) return Set.of();
        return QUARRY_OUTPUTS.computeIfAbsent(plan, p -> {
            Set<InvItem> outputs = new HashSet<>();
            for (PointType[][] floor : p.plan) {
                if (floor == null) continue;
                for (PointType[] row : floor) {
                    if (row == null) continue;
                    for (PointType pt : row) {
                        if (pt == null) continue;
                        if (pt.isType("stonesource")) outputs.add(InvItem.createInvItem(Blocks.COBBLESTONE));
                        else if (pt.isType("sandsource")) outputs.add(InvItem.createInvItem(Blocks.SAND));
                        else if (pt.isType("sandstonesource")) outputs.add(InvItem.createInvItem(Blocks.SANDSTONE));
                        else if (pt.isType("claysource")) outputs.add(InvItem.createInvItem(Items.CLAY_BALL));
                        else if (pt.isType("gravelsource")) outputs.add(InvItem.createInvItem(Blocks.GRAVEL));
                        else if (pt.isType("granitesource")) outputs.add(InvItem.createInvItem(Blocks.GRANITE));
                        else if (pt.isType("dioritesource")) outputs.add(InvItem.createInvItem(Blocks.DIORITE));
                        else if (pt.isType("andesitesource")) outputs.add(InvItem.createInvItem(Blocks.ANDESITE));
                        else if (pt.isType("snowsource")) outputs.add(InvItem.createInvItem(org.millenaire.common.block.MillBlocks.SNOW_BRICK));
                        else if (pt.isType("icesource")) outputs.add(InvItem.createInvItem(org.millenaire.common.block.MillBlocks.ICE_BRICK));
                    }
                }
            }
            return Set.copyOf(outputs);
        });
    }

    private static List<Recipe> recipes(Building v, BuildingPlan plan, BuildingProject project, Set<VillagerType> workers) {
        List<Recipe> out = new ArrayList<>();
        if (plan == null || plan.culture == null) return out;
        Set<VillagerType> residents = new HashSet<>();
        for (String key : plan.maleResident) {
            VillagerType type = plan.culture.getVillagerType(key);
            if (type != null) residents.add(type);
        }
        for (String key : plan.femaleResident) {
            VillagerType type = plan.culture.getVillagerType(key);
            if (type != null) residents.add(type);
        }
        Set<VillagerType> types = new HashSet<>(workers);
        types.addAll(residents);
        Set<String> tags = tags(plan);
        boolean centre = project.location != null && project.location.isSameLocation(v.location);
        for (VillagerType type : types) for (Goal goal : type.goals) {
            boolean home = residents.contains(type);
            if (goal instanceof GoalGeneric g) {
                if (g.townHallGoal ? !centre : g.buildingTag == null ? !home : !tags.contains(g.buildingTag)) continue;
                if (g.requiredTag != null && !tags.contains(g.requiredTag)) continue;
                if (g instanceof GoalGenericCrafting craft)
                    craft.output.forEach((item,n) -> { if (n != null && n > 0) output(out,item,craft.input); });
                else if (g instanceof GoalGenericMining mine)
                    mine.loots.forEach((item,n) -> { if (n != null && n > 0) output(out,item,Map.of()); });
                else if (g instanceof GoalGenericHarvestCrop crop) {
                    for (var bonus : crop.harvestItem) output(out,bonus.item,Map.of());
                    output(out,crop.irrigationBonusCrop,Map.of());
                } else if (g instanceof GoalGenericGatherBlocks gather) {
                    for (var bonus : gather.harvestItem) output(out,bonus.item,Map.of());
                } else if (g instanceof GoalGenericSlaughterAnimal slaughter) {
                    for (var bonus : slaughter.bonusItem) output(out,bonus.item,Map.of());
                } else if (g instanceof GoalGenericCooking cook && cook.itemToCook != null && cook.itemToCook.staticStack != null) {
                    v.world.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(cook.itemToCook.staticStack.copy()),v.world)
                            .ifPresent(recipe -> {
                                var stack = recipe.getResultItem();
                                if (!stack.isEmpty()) output(out,InvItem.createInvItem(stack),Map.of(cook.itemToCook,1));
                            });
                }
            } else if (goal instanceof GoalMinerMineResource miner
                    && (miner.buildingTag == null ? home : tags.contains(miner.buildingTag))) {
                for (BuildingPlan p = plan; p != null; ) {
                    for (InvItem item : quarryOutputs(p)) output(out,item,Map.of());
                    if (p.level <= 0) break;
                    BuildingPlan previous = p.getPreviousBuildingPlan();
                    if (previous == null || previous.level >= p.level) break;
                    p = previous;
                }
            } else if (goal instanceof GoalLumbermanChopTrees && tags.contains("grove"))
                output(out,InvItem.createInvItem(Items.OAK_LOG,-1),Map.of());
            else if (goal instanceof GoalFish && tags.contains("fishingspot")) {
                output(out,InvItem.createInvItem(Items.COD),Map.of());
                if (goal instanceof GoalFishInuit) output(out,InvItem.createInvItem(Blocks.BONE_BLOCK),Map.of());
            } else if (goal instanceof GoalIndianHarvestSugarCane && tags.contains("sugarplantation"))
                output(out,InvItem.createInvItem(Items.SUGAR_CANE),Map.of());
            else if (goal instanceof GoalIndianGatherBrick && tags.contains("brickkiln"))
                output(out,InvItem.createInvItem(org.millenaire.common.block.MillBlocks.BS_MUD_BRICK),Map.of());
            else if (goal instanceof GoalHarvestWarts && home)
                output(out,InvItem.createInvItem(Items.NETHER_WART),Map.of());
            else if (goal instanceof GoalShearSheep && home && tags.contains("sheeps"))
                output(out,InvItem.createInvItem(Blocks.WHITE_WOOL),Map.of());
        }
        return out;
    }

    private static boolean matches(Collection<InvItem> items, InvItem item) {
        for (InvItem other : items) if (item.matches(other)) return true;
        return false;
    }

    private static void missing(Building v, Map<InvItem,Integer> costs, Set<InvItem> needs) {
        if (costs == null) return;
        for (var e : costs.entrySet()) {
            if (needs.size() >= VillageProgressionRules.MAX_ITEMS) break;
            if (e.getValue() != null && e.getValue() > v.progressionAvailable(e.getKey())) needs.add(e.getKey());
        }
    }

    private static Candidate prerequisite(Building v, List<Candidate> options, BuildingPlan target, boolean reservePending) {
        Set<InvItem> needs = new LinkedHashSet<>();
        missing(v,target.resCost,needs);
        if (needs.isEmpty()) return null;
        Set<VillagerType> workers = new HashSet<>();
        for (var record : v.getVillagerRecords().values())
            if (!record.killed && record.getType() != null) workers.add(record.getType());
        List<Recipe> existing = new ArrayList<>();
        Map<Candidate,List<Recipe>> potential = new LinkedHashMap<>();
        for (BuildingProject p : v.getFlatProjectList())
            if (p.getExistingPlan() != null) existing.addAll(recipes(v,p.getExistingPlan(),p,workers));
        for (Candidate c : options) {
            if (c.plan() == target) continue;
            List<Recipe> outputs = recipes(v,c.plan(),c.project(),workers);
            BuildingPlan old = c.project().getExistingPlan();
            if (old != null) {
                List<Recipe> before = recipes(v,old,c.project(),workers);
                outputs.removeIf(recipe -> before.stream().anyMatch(r -> r.output().matches(recipe.output())));
            }
            if (!outputs.isEmpty()) potential.put(c,outputs);
        }
        // Bounded reverse expansion of recipe ingredients and producer build
        // costs. Cycles terminate; never execute an unaffordable prerequisite.
        List<InvItem> already = existing.stream().map(Recipe::output).toList();
        for (int pass = 0; pass < VillageProgressionRules.MAX_PASSES && needs.size() < VillageProgressionRules.MAX_ITEMS; pass++) {
            int size = needs.size();
            for (Recipe r : existing) if (matches(needs,r.output())) missing(v,r.inputs(),needs);
            for (var e : potential.entrySet()) for (Recipe r : e.getValue()) if (matches(needs,r.output()) && !matches(already,r.output())) {
                missing(v,r.inputs(),needs);
                missing(v,e.getKey().plan().resCost,needs);
            }
            if (needs.size() == size) break;
        }
        Candidate best = null;
        int score = Integer.MIN_VALUE;
        for (var e : potential.entrySet()) {
            Candidate c = e.getKey();
            if (!v.canAffordProgression(c.plan(),reservePending)) continue;
            int benefit = 0;
            for (InvItem need : needs)
                if (!matches(already,need) && e.getValue().stream().anyMatch(r -> r.output().matches(need))) benefit++;
            if (benefit == 0) continue;
            int candidateScore = benefit * 1000 + rank(c);
            if (candidateScore > score) { score = candidateScore; best = c; }
        }
        if (best != null && MillConfigValues.LogBuildingPlan >= 1)
            MillLog.major(v,"VILLAGE-PROGRESSION supplier=" + best.project().key
                    + " target=" + target.buildingKey + " missingTypes=" + needs.size());
        return best;
    }
}
