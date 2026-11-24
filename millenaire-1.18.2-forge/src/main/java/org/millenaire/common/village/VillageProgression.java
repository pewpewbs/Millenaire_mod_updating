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
    private VillageProgression() {}

    static boolean enabled(Building v) {
        return v != null && v.isTownhall && v.villageType != null && v.villageType.isRegularVillage()
                && !v.villageType.playerControlled && v.location != null && !v.location.isCustomBuilding;
    }

    static String styleFamily(ResourceLocation id) {
        if (id == null) return null;
        String p = id.getPath();
        if (p.startsWith("path") || p.contains("road")) return null;
        if (p.startsWith("stairs_")) p = p.substring(7);
        if (p.startsWith("slab_")) p = p.substring(5);
        for (String suffix : List.of("_stairs", "_slab", "_wall"))
            if (p.endsWith(suffix)) p = p.substring(0, p.length() - suffix.length());
        if (!(p.contains("sandstone") || p.contains("tiles") || p.contains("terracotta")
                || p.contains("stone_brick") || p.contains("stonebrick") || p.contains("painted_brick")
                || p.contains("mudbrick") || p.contains("mud_brick") || p.contains("cookedbrick")
                || p.contains("timberframe") || p.contains("timber_frame") || p.contains("thatch")
                || p.equals("paper_wall") || p.contains("marble") || p.contains("snowbrick")
                || p.contains("icebrick") || p.equals("bricks"))) return null;
        return id.getNamespace() + ":" + p;
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
            return !Collections.disjoint(introduced, locked);
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
        future.removeAll(past);
        return new StyleGate(new Candidate(centre, next), Set.copyOf(future));
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
        return tier + (c.plan().level == 0 ? 20 : 0)
                - 10 * Math.min(5, Math.max(0,c.plan().level))
                + 4 * Math.min(25, Math.max(1,c.plan().priority));
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
                if (c.project() == gate.centre().project()) return c;
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
            if (needs.size() >= 128) break;
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
        for (int pass = 0; pass < 8 && needs.size() < 128; pass++) {
            int size = needs.size();
            for (Recipe r : existing) if (matches(needs,r.output())) missing(v,r.inputs(),needs);
            for (var e : potential.entrySet()) for (Recipe r : e.getValue()) if (matches(needs,r.output())) {
                missing(v,r.inputs(),needs);
                missing(v,e.getKey().plan().resCost,needs);
            }
            if (needs.size() == size) break;
        }
        List<InvItem> already = existing.stream().map(Recipe::output).toList();
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
