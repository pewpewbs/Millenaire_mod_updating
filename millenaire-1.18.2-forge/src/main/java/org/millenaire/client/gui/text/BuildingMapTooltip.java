package org.millenaire.client.gui.text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import org.millenaire.common.utilities.LegacyBlockStates;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.VillagerType;
import org.millenaire.common.goal.*;
import org.millenaire.common.goal.generic.*;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;

/** Read-only map description. Never runs a goal, changes stock or requests chunks. */
final class BuildingMapTooltip {
    record Line(String text, int color, List<ItemStack> icons) {
        Line(String text, int color) { this(text, color, List.of()); }
    }

    /** Keep actual stacks (including metadata/NBT), not translated names. */
    private static final class Goods {
        final List<ItemStack> icons = new ArrayList<>();
        final Set<String> fallback = new TreeSet<>();
        boolean isEmpty() { return icons.isEmpty() && fallback.isEmpty(); }
        void removeAll(Goods other) {
            icons.removeIf(stack -> other.icons.stream().anyMatch(
                    candidate -> ItemStack.isSameItemSameTags(stack, candidate)));
            fallback.removeAll(other.fallback);
        }
    }
    private static final int NORMAL = 0xE0E0E0;
    private static final int GOLD = 0xFFAA00;
    private static final int MUTED = 0xAAAAAA;
    private BuildingMapTooltip() {}

    static String label(String en, String uk, String ru) {
        String language = MillConfigValues.effective_language;
        language = language == null ? "" : language.toLowerCase(Locale.ROOT);
        return language.startsWith("uk") ? uk : language.startsWith("ru") ? ru : en;
    }

    private static void add(List<Line> lines, String text, int color) {
        if (text != null && !text.isBlank()) lines.add(new Line(text, color));
    }
    private static void addItem(Goods result, InvItem item) {
        if (item == null) return;
        ItemStack stack = item.getItemStack();
        if (stack != null && !stack.isEmpty()) {
            addStack(result, stack);
        } else {
            String name = item.getName();
            if (name != null && !name.isBlank()) result.fallback.add(name);
        }
    }

    private static void addStack(Goods result, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        for (ItemStack existing : result.icons) {
            if (ItemStack.isSameItemSameTags(existing, stack)) return;
        }
        ItemStack icon = stack.copy();
        icon.setCount(1);
        result.icons.add(icon);
    }

    private static void section(List<Line> lines, String title, Goods goods) {
        if (goods.isEmpty()) return;
        goods.icons.sort(java.util.Comparator.comparing(stack -> stack.getHoverName().getString()));
        lines.add(new Line(title, NORMAL, List.copyOf(goods.icons)));
        // Special trade abstractions (e.g. any enchanted item) have no stack.
        // Only those retain a text description.
        for (String fallback : goods.fallback) add(lines, fallback, MUTED);
    }

    static List<Line> describe(BuildingLocation loc, Building townHall, String nativeName, boolean construction) {
        List<Line> lines = new ArrayList<>();
        add(lines, nativeName, NORMAL);
        if (loc.getIBuildingPlan() != null) add(lines, loc.getGameName(), NORMAL);
        Building building = loc.getBuilding(townHall.world);
        BuildingPlan plan = loc.getPlan();

        // Plan index zero is the initial completed building, not upgrade one.
        if (construction) {
            add(lines, label("Under construction", "Будується", "Строится"), MUTED);
        } else if (loc.level < 0) {
            add(lines, label("Not built yet", "Ще не збудовано", "Ещё не построено"), MUTED);
        } else if (loc.isCustomBuilding) {
            add(lines, label("Custom building (no standard upgrades)", "Власна будівля (без стандартних покращень)", "Пользовательская постройка (без стандартных улучшений)"), MUTED);
        } else if (loc.culture != null) {
            var set = loc.culture.getBuildingPlanSet(loc.planKey);
            int variation = loc.getVariation();
            if (set != null && variation >= 0 && variation < set.plans.size()
                    && set.plans.get(variation).length > loc.level) {
                int maximum = set.plans.get(variation).length - 1;
                String level = label("Upgrade level: ", "Рівень покращення: ", "Уровень улучшения: ") + loc.level + "/" + maximum;
                boolean max = loc.level == maximum;
                if (max) level += label(" (MAX)", " (МАКС.)", " (МАКС.)");
                add(lines, level, max ? GOLD : NORMAL);
            } else {
                add(lines, label("Upgrade level: ", "Рівень покращення: ", "Уровень улучшения: ") + loc.level, NORMAL);
            }
        }

        // Planned residents describe home-based production; village worker types
        // describe tagged workplaces and the central building. These are potential
        // outputs, not a promise of current workers, stock or actual throughput.
        Set<VillagerType> residents = new LinkedHashSet<>();
        Set<VillagerType> workers = new LinkedHashSet<>();
        if (loc.culture != null) {
            for (String key : loc.getMaleResidents()) {
                VillagerType type = loc.culture.getVillagerType(key);
                if (type != null) residents.add(type);
            }
            for (String key : loc.getFemaleResidents()) {
                VillagerType type = loc.culture.getVillagerType(key);
                if (type != null) residents.add(type);
            }
            for (var record : townHall.getVillagerRecords().values()) {
                if (!record.killed) {
                    VillagerType type = record.getType();
                    if (type != null) workers.add(type);
                }
            }
        }
        workers.addAll(residents);
        Goods produced = new Goods();
        Goods extracted = new Goods();
        Set<String> activities = new TreeSet<>();
        for (VillagerType type : workers) {
            for (Goal goal : type.goals) {
                boolean home = residents.contains(type);
                if (!(goal instanceof GoalGeneric generic)) {
                    // Legacy goals have no generic output table. Describe only
                    // products whose destination contract is explicit in code.
                    if (goal instanceof GoalLumbermanChopTrees && hasTag(loc, building, plan, "grove")) {
                        addItem(extracted, InvItem.createInvItem(Items.OAK_LOG, -1));
                    } else if (goal instanceof GoalFishInuit && hasTag(loc, building, plan, "fishingspot")) {
                        addStack(extracted, new ItemStack(Items.COD));
                        addStack(extracted, new ItemStack(Items.BONE_BLOCK));
                    } else if (goal instanceof GoalFish && hasTag(loc, building, plan, "fishingspot")) {
                        addStack(extracted, new ItemStack(Items.COD));
                    } else if (goal instanceof GoalIndianHarvestSugarCane && hasTag(loc, building, plan, "sugarplantation")) {
                        addStack(extracted, new ItemStack(Items.SUGAR_CANE));
                    } else if (goal instanceof GoalIndianGatherBrick && hasTag(loc, building, plan, "brickkiln")) {
                        addItem(produced, InvItem.createInvItem(org.millenaire.common.block.MillBlocks.BS_MUD_BRICK));
                    } else if (goal instanceof GoalMinerMineResource miner
                            && (miner.buildingTag == null ? home : hasTag(loc, building, plan, miner.buildingTag))) {
                        activities.add(label("Resource extraction", "Видобування ресурсів", "Добыча ресурсов"));
                        // Read registered source types only, not world blocks.
                        // The legacy mining goal transforms particular sources
                        // into specific drops; mirror its explicit output rules.
                        if (building != null) for (var source : building.getResManager().sourceTypes) {
                            if (source == null) continue;
                            var block = source.getBlock();
                            if (LegacyBlockStates.matchesName(block, "minecraft:sand"))
                                addStack(extracted, new ItemStack(Blocks.SAND));
                            else if (LegacyBlockStates.matchesName(block, "minecraft:stone"))
                                addItem(extracted, block == Blocks.STONE ? InvItem.createInvItem(Blocks.COBBLESTONE) : InvItem.createInvItem(source));
                            else if (LegacyBlockStates.matchesName(block, "minecraft:sandstone"))
                                addStack(extracted, new ItemStack(Blocks.SANDSTONE));
                            else if (block == Blocks.CLAY) addStack(extracted, new ItemStack(Items.CLAY_BALL));
                            else if (block == Blocks.GRAVEL) addStack(extracted, new ItemStack(Blocks.GRAVEL));
                            else if (block == Blocks.SNOW) addStack(extracted, new ItemStack(MillBlocks.SNOW_BRICK));
                            else if (block == Blocks.ICE) addStack(extracted, new ItemStack(MillBlocks.ICE_BRICK));
                        }
                    } else if (goal instanceof GoalHarvestWarts && home) {
                        addStack(extracted, new ItemStack(Items.NETHER_WART));
                    } else if (goal instanceof GoalShearSheep && home && hasTag(loc, building, plan, "sheeps")) {
                        addStack(extracted, new ItemStack(Blocks.WHITE_WOOL));
                    } else if (goal instanceof GoalBrewPotions && home) {
                        addStack(produced, net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                                new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.AWKWARD));
                    }
                    continue;
                }
                boolean central = building != null && building.isTownhall;
                if (generic.townHallGoal ? !central
                        : generic.buildingTag == null ? !home : !hasTag(loc, building, plan, generic.buildingTag)) continue;
                if (generic.requiredTag != null && !hasTag(loc, building, plan, generic.requiredTag)) continue;
                // A configured workplace can also have a non-producing role:
                // planting, visiting, tending equipment or collecting supplies.
                if (generic.buildingTag != null || generic.townHallGoal) {
                    String activity = generic.gameName();
                    if (activity != null && !activity.isBlank() && !activity.startsWith("goal."))
                        activities.add(activity);
                }
                if (generic instanceof GoalGenericCrafting crafting) {
                    crafting.output.forEach((item, count) -> { if (count != null && count > 0) addItem(produced, item); });
                } else if (generic instanceof GoalGenericMining mining) {
                    mining.loots.forEach((item, count) -> { if (count != null && count > 0) addItem(extracted, item); });
                } else if (generic instanceof GoalGenericHarvestCrop harvest) {
                    for (var bonus : harvest.harvestItem) addItem(extracted, bonus.item);
                    addItem(extracted, harvest.irrigationBonusCrop);
                } else if (generic instanceof GoalGenericGatherBlocks gather) {
                    for (var bonus : gather.harvestItem) addItem(extracted, bonus.item);
                } else if (generic instanceof GoalGenericSlaughterAnimal slaughter) {
                    for (var bonus : slaughter.bonusItem) addItem(extracted, bonus.item);
                } else if (generic instanceof GoalGenericCooking cooking && cooking.itemToCook != null
                        && cooking.itemToCook.staticStack != null && townHall.world != null) {
                    var input = new SimpleContainer(cooking.itemToCook.staticStack.copy());
                    townHall.world.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, townHall.world)
                            .ifPresent(recipe -> addStack(produced, recipe.getResultItem()));
                }
            }
        }

        // Declared abstract production is a distinct planning figure, not proof
        // that a real NPC recipe currently produces this item.
        Goods planned = new Goods();
        if (plan != null) plan.abstractedProduction.forEach((item, count) -> {
            if (count != null && count > 0) addItem(planned, item);
        });
        planned.removeAll(produced);
        planned.removeAll(extracted);
        Goods sells = new Goods();
        if (loc.culture != null && loc.shop != null) {
            var goods = loc.culture.shopSells.get(loc.shop);
            if (goods != null) for (var good : goods) if (good != null) addItem(sells, good.item);
        }
        section(lines, label("Produces:", "Виробляє:", "Производит:"), produced);
        section(lines, label("Extracts / gathers:", "Добуває / збирає:", "Добывает / собирает:"), extracted);
        section(lines, label("Declared production:", "Виробництво за планом:", "Производство по плану:"), planned);
        section(lines, label("Sells:", "Продає:", "Продаёт:"), sells);

        List<String> effects = loc.getBuildingEffects(townHall.world);
        if (produced.isEmpty() && extracted.isEmpty() && planned.isEmpty() && sells.isEmpty()) {
            Set<String> roles = new LinkedHashSet<>(effects);
            roles.addAll(activities);
            if (!residents.isEmpty()) roles.add(label("Housing for villagers", "Житло для мешканців", "Жильё для жителей"));
            if (!loc.getVisitors().isEmpty()) roles.add(label("Accommodation for visitors", "Помешкання для відвідувачів", "Жильё для посетителей"));
            if (hasTag(loc, building, plan, "grove")) roles.add(label("Forestry", "Лісозаготівля", "Лесозаготовка"));
            if (hasTag(loc, building, plan, "fishingspot")) roles.add(label("Fishing", "Рибальство", "Рыболовство"));
            if (hasTag(loc, building, plan, "sugarplantation")) roles.add(label("Sugar cane cultivation", "Вирощування цукрової тростини", "Выращивание сахарного тростника"));
            if (hasTag(loc, building, plan, "brickkiln")) roles.add(label("Brick drying", "Сушіння цегли", "Сушка кирпича"));
            if (roles.isEmpty()) roles.add(label("Purpose not specified in available data", "Призначення не вказано в доступних даних", "Назначение не указано в доступных данных"));
            add(lines, label("Purpose: ", "Призначення: ", "Назначение: ") + String.join("; ", roles), MUTED);
        } else {
            for (String effect : effects) add(lines, effect, MUTED);
        }
        return lines;
    }

    private static boolean hasTag(BuildingLocation loc, Building building, BuildingPlan plan, String tag) {
        if (building != null && building.containsTags(tag)) return true;
        if (plan != null && plan.containsTags(tag)) return true;
        var custom = loc.isCustomBuilding ? loc.getCustomPlan() : null;
        return custom != null && custom.tags.contains(tag);
    }
}
