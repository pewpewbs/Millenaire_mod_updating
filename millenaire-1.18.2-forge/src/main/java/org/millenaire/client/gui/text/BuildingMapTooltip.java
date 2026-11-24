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
    record Line(String text, int color) {}
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
    private static void addItem(Set<String> result, InvItem item) {
        if (item != null) {
            String name = item.getName();
            if (name != null && !name.isBlank()) result.add(name);
        }
    }
    private static void addStack(Set<String> result, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) result.add(stack.getHoverName().getString());
    }

    static List<Line> describe(BuildingLocation loc, Building townHall, String nativeName) {
        List<Line> lines = new ArrayList<>();
        add(lines, nativeName, NORMAL);
        if (loc.getIBuildingPlan() != null) add(lines, loc.getGameName(), NORMAL);
        Building building = loc.getBuilding(townHall.world);
        BuildingPlan plan = loc.getPlan();

        // Plan index zero is the initial completed building, not upgrade one.
        if (loc.level < 0) {
            add(lines, label("Not built yet", "Ще не збудовано", "Ещё не построено"), MUTED);
        } else if (loc.isCustomBuilding) {
            add(lines, label("Custom building (no standard upgrades)", "Власна будівля (без стандартних покращень)", "Пользовательская постройка (без стандартных улучшений)"), MUTED);
        } else if (loc.culture != null) {
            var set = loc.culture.getBuildingPlanSet(loc.planKey);
            int variation = loc.getVariation();
            if (set != null && variation >= 0 && variation < set.plans.size()
                    && set.plans.get(variation).length > loc.level) {
                int maximum = set.plans.get(variation).length - 1;
                String level = label("Upgrade: ", "Покращення: ", "Улучшение: ") + loc.level + "/" + maximum;
                boolean max = loc.level == maximum;
                if (max) level += label(" (MAX)", " (МАКС.)", " (МАКС.)");
                add(lines, level, max ? GOLD : NORMAL);
            } else {
                add(lines, label("Upgrade: ", "Покращення: ", "Улучшение: ") + loc.level, NORMAL);
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
                if (!record.killed && record.getType() != null) workers.add(record.getType());
            }
        }
        workers.addAll(residents);
        Set<String> produced = new TreeSet<>();
        for (VillagerType type : workers) {
            for (Goal goal : type.goals) {
                boolean home = residents.contains(type);
                if (!(goal instanceof GoalGeneric generic)) {
                    // Legacy goals have no generic output table. Describe only
                    // products whose destination contract is explicit in code.
                    if (goal instanceof GoalLumbermanChopTrees && hasTag(loc, building, plan, "grove")) {
                        addItem(produced, InvItem.createInvItem(Items.OAK_LOG, -1));
                    } else if (goal instanceof GoalFishInuit && hasTag(loc, building, plan, "fishingspot")) {
                        addStack(produced, new ItemStack(Items.COD));
                        addStack(produced, new ItemStack(Items.BONE_BLOCK));
                    } else if (goal instanceof GoalFish && hasTag(loc, building, plan, "fishingspot")) {
                        addStack(produced, new ItemStack(Items.COD));
                    } else if (goal instanceof GoalIndianHarvestSugarCane && hasTag(loc, building, plan, "sugarplantation")) {
                        addStack(produced, new ItemStack(Items.SUGAR_CANE));
                    } else if (goal instanceof GoalIndianGatherBrick && hasTag(loc, building, plan, "brickkiln")) {
                        addItem(produced, InvItem.createInvItem(org.millenaire.common.block.MillBlocks.BS_MUD_BRICK));
                    } else if (goal instanceof GoalHarvestWarts && home) {
                        addStack(produced, new ItemStack(Items.NETHER_WART));
                    } else if (goal instanceof GoalShearSheep && home && hasTag(loc, building, plan, "sheeps")) {
                        produced.add(label("Wool (various colours)", "Вовна (різних кольорів)", "Шерсть (разных цветов)"));
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
                if (generic instanceof GoalGenericCrafting crafting) {
                    crafting.output.forEach((item, count) -> { if (count != null && count > 0) addItem(produced, item); });
                } else if (generic instanceof GoalGenericMining mining) {
                    mining.loots.forEach((item, count) -> { if (count != null && count > 0) addItem(produced, item); });
                } else if (generic instanceof GoalGenericHarvestCrop harvest) {
                    for (var bonus : harvest.harvestItem) addItem(produced, bonus.item);
                    addItem(produced, harvest.irrigationBonusCrop);
                } else if (generic instanceof GoalGenericGatherBlocks gather) {
                    for (var bonus : gather.harvestItem) addItem(produced, bonus.item);
                } else if (generic instanceof GoalGenericSlaughterAnimal slaughter) {
                    for (var bonus : slaughter.bonusItem) addItem(produced, bonus.item);
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
        Set<String> planned = new TreeSet<>();
        if (plan != null) plan.abstractedProduction.forEach((item, count) -> {
            if (count != null && count > 0) addItem(planned, item);
        });
        planned.removeAll(produced);
        Set<String> sells = new TreeSet<>();
        if (loc.culture != null && loc.shop != null) {
            var goods = loc.culture.shopSells.get(loc.shop);
            if (goods != null) for (var good : goods) if (good != null) addItem(sells, good.item);
        }
        if (!produced.isEmpty()) add(lines, label("Can produce: ", "Може виготовляти: ", "Может производить: ")
                + String.join(", ", produced), NORMAL);
        if (!planned.isEmpty()) add(lines, label("Declared production: ", "Виробництво за планом: ", "Производство по плану: ")
                + String.join(", ", planned), NORMAL);
        if (!sells.isEmpty()) add(lines, label("Sells (assortment): ", "Продає (асортимент): ", "Продаёт (ассортимент): ")
                + String.join(", ", sells), NORMAL);

        List<String> effects = loc.getBuildingEffects(townHall.world);
        if (produced.isEmpty() && planned.isEmpty() && sells.isEmpty()) {
            Set<String> roles = new LinkedHashSet<>(effects);
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
