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
    record RecipeView(List<ItemStack> inputs,List<ItemStack> outputs) {}
    record Line(String text,int color,List<ItemStack> icons,List<String> iconDetails,String detail,List<List<RecipeView>> recipes,boolean action) {
        Line(String text,int color){this(text,color,List.of(),List.of(),null,List.of(),false);}
        Line(String text,int color,String detail){this(text,color,List.of(),List.of(),detail,List.of(),false);}
    }

    /** Keep actual stacks (including metadata/NBT), not translated names. */
    private static final class Goods {
        final List<ItemStack> icons = new ArrayList<>();
        final Set<String> fallback = new TreeSet<>();
        final java.util.Map<ItemStack,Set<String>> details=new java.util.IdentityHashMap<>();
        String source;
        RecipeView recipe;
        final java.util.Map<ItemStack,List<RecipeView>> recipes=new java.util.IdentityHashMap<>();
        void remember(ItemStack stack){
            if(recipe==null)return;
            List<RecipeView> list=recipes.computeIfAbsent(stack,k->new ArrayList<>());
            if(list.stream().noneMatch(r->sameStacks(r.inputs(),recipe.inputs())&&sameStacks(r.outputs(),recipe.outputs())))list.add(recipe);
        }
        String detail(ItemStack stack){
            Set<String> sources=details.get(stack);
            return sources==null?label("Source not specified","Джерело не уточнено","Источник не указан"):String.join("; ",sources);
        }
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
            if (ItemStack.isSameItemSameTags(existing, stack)){
                if(result.source!=null)result.details.computeIfAbsent(existing,k->new LinkedHashSet<>()).add(result.source);
                result.remember(existing);
                return;
            }
        }
        ItemStack icon = stack.copy();
        icon.setCount(1);
        result.icons.add(icon);
        if(result.source!=null)result.details.computeIfAbsent(icon,k->new LinkedHashSet<>()).add(result.source);
        result.remember(icon);
    }

    private static void section(List<Line> lines, String title, Goods goods) {
        section(lines,title,goods,false);
    }
    private static void section(List<Line> lines,String title,Goods goods,boolean action) {
        if(goods.isEmpty())return;
        goods.icons.sort(java.util.Comparator.comparing(stack->stack.getHoverName().getString()));
        lines.add(new Line(title,NORMAL,List.copyOf(goods.icons),goods.icons.stream().map(goods::detail).toList(),null,
            goods.icons.stream().map(s->List.copyOf(goods.recipes.getOrDefault(s,List.of()))).toList(),action));
        // Special trade abstractions (e.g. any enchanted item) have no stack.
        // Only those retain a text description.
        for (String fallback : goods.fallback) add(lines, fallback, MUTED);
    }

    private static boolean sameStacks(List<ItemStack> a,List<ItemStack> b){
        if(a.size()!=b.size())return false;
        for(int i=0;i<a.size();i++)if(a.get(i).getCount()!=b.get(i).getCount()||!ItemStack.isSameItemSameTags(a.get(i),b.get(i)))return false;
        return true;
    }
    private static List<ItemStack> recipeStacks(java.util.Map<InvItem,Integer> items){
        List<ItemStack> stacks=new ArrayList<>();
        for(var entry:items.entrySet()){
            if(entry.getValue()==null||entry.getValue()<=0)continue;
            ItemStack s=entry.getKey().getItemStack();
            if(s==null||s.isEmpty())return List.of();
            s=s.copy();s.setCount(entry.getValue());stacks.add(s);
        }
        stacks.sort(java.util.Comparator.comparing(s->s.getHoverName().getString()));
        return stacks;
    }
    private static ItemStack actionIcon(Goal goal){
        if(goal instanceof GoalGenericCooking||goal instanceof GoalGenericTendFurnace)return new ItemStack(Blocks.FURNACE);
        if(goal instanceof GoalGenericSlaughterAnimal)return new ItemStack(Items.IRON_SWORD);
        if(goal instanceof GoalBreedAnimals)return new ItemStack(Items.WHEAT);
        if(goal instanceof GoalShearSheep)return new ItemStack(Items.SHEARS);
        if(goal instanceof GoalGenericPlantCrop)return new ItemStack(Items.WHEAT_SEEDS);
        if(goal instanceof GoalGenericHarvestCrop)return new ItemStack(Items.WHEAT);
        if(goal instanceof GoalGenericPlantSapling)return new ItemStack(Blocks.OAK_SAPLING);
        if(goal instanceof GoalLumbermanChopTrees)return new ItemStack(Items.IRON_AXE);
        if(goal instanceof GoalGenericMining||goal instanceof GoalMinerMineResource)return new ItemStack(Items.IRON_PICKAXE);
        if(goal instanceof GoalFish)return new ItemStack(Items.FISHING_ROD);
        if(goal instanceof GoalBrewPotions)return new ItemStack(Items.BREWING_STAND);
        if(goal instanceof GoalGenericCrafting)return new ItemStack(Blocks.CRAFTING_TABLE);
        if(goal instanceof GoalGenericGatherBlocks||goal instanceof GoalGenericTakeFromBuilding)return new ItemStack(Blocks.CHEST);
        return new ItemStack(Items.BOOK);
    }
    private static void activity(Goods actions,Goal goal,String source,RecipeView recipe){
        // Activity icons describe the category, not the products or their recipes.
        actions.source=goal instanceof GoalGenericCrafting
                ? label("Crafts resources","Виготовляє ресурси","Изготавливает ресурсы")
                : goal instanceof GoalGenericCooking || goal instanceof GoalGenericTendFurnace
                ? label("Works with a furnace","Працює з піччю","Работает с печью") : source;
        addStack(actions,actionIcon(goal));
    }
    private static boolean productive(VillagerType type){
        return type!=null&&!type.isChild&&type.goals.stream().anyMatch(g->!g.leasure&&(g instanceof GoalGeneric
            ||g instanceof GoalBreedAnimals||g instanceof GoalShearSheep||g instanceof GoalMinerMineResource
            ||g instanceof GoalFish||g instanceof GoalLumbermanChopTrees||g instanceof GoalHarvestWarts
            ||g instanceof GoalIndianHarvestSugarCane||g instanceof GoalIndianGatherBrick||g instanceof GoalBrewPotions));
    }

    private static void staff(List<Line> lines,BuildingLocation loc,Building townHall,Building building,boolean expanded){
        if(loc.culture==null)return;
        java.util.Map<VillagerType,Integer> expected=new java.util.LinkedHashMap<>();
        List<String> roles=new ArrayList<>(loc.getMaleResidents());roles.addAll(loc.getFemaleResidents());
        for(String key:roles){VillagerType type=loc.culture.getVillagerType(key);if(productive(type))expected.merge(type,1,Integer::sum);}
        if(expected.isEmpty()){
            add(lines,label("No fixed production posts specified","Окремі робочі місця не задано","Отдельные рабочие места не заданы"),MUTED);
            return;
        }
        int total=expected.values().stream().mapToInt(Integer::intValue).sum(),filled=0;
        List<org.millenaire.common.village.VillagerRecord> staff=new ArrayList<>();
        java.util.Map<VillagerType,Integer> counts=new java.util.HashMap<>();
        if(building!=null&&loc.level>=0)for(var record:townHall.getVillagerRecords().values()){
            if(record.killed||record.getHousePos()==null||!record.getHousePos().equals(building.getPos()))continue;
            VillagerType type=record.getType();
            if(!expected.containsKey(type))continue;
            if(counts.merge(type,1,Integer::sum)<=expected.get(type))filled++;
            staff.add(record);
        }
        add(lines,label("Resident workers: ","Працівники-резиденти: ","Работники-резиденты: ")+filled+"/"+total,filled==0?0xFF7777:filled<total?GOLD:0x88DD88);

        if(!expanded)return;
        for(var record:staff){
            var npc=townHall.mw.getVillagerById(record.getVillagerId());
            String status;
            if(npc==null||!npc.isAlive())status=label("Live status unavailable","Поточний стан невідомий","Текущее состояние неизвестно");
            else{
                var goal=npc.getCurrentGoal();
                status=goal==null?label("No current task","Немає поточного завдання","Нет текущего задания"):goal.gameName(npc);
            }
            lines.add(new Line(record.getName()+" — "+record.getGameOccupation(),NORMAL,label("Current task: ","Що робить: ","Что делает: ")+status));
        }

    }

    static List<Line> describe(BuildingLocation loc, Building townHall, String nativeName, boolean construction,boolean expanded) {
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
        staff(lines,loc,townHall,building,expanded);

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
        Goods actions=new Goods();
        Set<String> activities = new TreeSet<>();
        for (VillagerType type : workers) {
            for (Goal goal : type.goals) {
                boolean home = residents.contains(type);
                String action=goal.gameName();
                String source=(action==null||action.isBlank()||action.startsWith("goal."))?goal.getClass().getSimpleName():action;
                produced.source=source;extracted.source=source;produced.recipe=null;extracted.recipe=null;
                if (!(goal instanceof GoalGeneric generic)) {
                    // Legacy goals have no generic output table. Describe only
                    // products whose destination contract is explicit in code.
                    if (goal instanceof GoalLumbermanChopTrees && hasTag(loc, building, plan, "grove")) {
                        activity(actions,goal,source,null);
                        addItem(extracted, InvItem.createInvItem(Items.OAK_LOG, -1));
                    } else if (goal instanceof GoalFishInuit && hasTag(loc, building, plan, "fishingspot")) {
                        activity(actions,goal,source,null);
                        addStack(extracted, new ItemStack(Items.COD));
                        addStack(extracted, new ItemStack(Items.BONE_BLOCK));
                    } else if (goal instanceof GoalFish && hasTag(loc, building, plan, "fishingspot")) {
                        activity(actions,goal,source,null);
                        addStack(extracted, new ItemStack(Items.COD));
                    } else if (goal instanceof GoalIndianHarvestSugarCane && hasTag(loc, building, plan, "sugarplantation")) {
                        activity(actions,goal,source,null);
                        addStack(extracted, new ItemStack(Items.SUGAR_CANE));
                    } else if (goal instanceof GoalIndianGatherBrick && hasTag(loc, building, plan, "brickkiln")) {
                        activity(actions,goal,source,null);
                        addItem(produced, InvItem.createInvItem(org.millenaire.common.block.MillBlocks.BS_MUD_BRICK));
                    } else if (goal instanceof GoalMinerMineResource miner
                            && (miner.buildingTag == null ? home : hasTag(loc, building, plan, miner.buildingTag))) {
                        activities.add(label("Resource extraction", "Видобування ресурсів", "Добыча ресурсов"));
                        activity(actions,goal,source,null);
                        // Read registered source types only, not world blocks.
                        // The legacy mining goal transforms particular sources
                        // into specific drops; mirror its explicit output rules.
                        if (building != null) for (var sourceState : building.getResManager().getDisplaySourceTypes()) {
                            if (sourceState == null) continue;
                            var block = sourceState.getBlock();
                            if (LegacyBlockStates.matchesName(block, "minecraft:sand"))
                                addStack(extracted, new ItemStack(Blocks.SAND));
                            else if (LegacyBlockStates.matchesName(block, "minecraft:stone"))
                                addItem(extracted, block == Blocks.STONE ? InvItem.createInvItem(Blocks.COBBLESTONE) : InvItem.createInvItem(sourceState));
                            else if (LegacyBlockStates.matchesName(block, "minecraft:sandstone"))
                                addStack(extracted, new ItemStack(Blocks.SANDSTONE));
                            else if (block == Blocks.CLAY) addStack(extracted, new ItemStack(Items.CLAY_BALL));
                            else if (block == Blocks.GRAVEL) addStack(extracted, new ItemStack(Blocks.GRAVEL));
                            else if (block == Blocks.SNOW) addStack(extracted, new ItemStack(MillBlocks.SNOW_BRICK));
                            else if (block == Blocks.ICE) addStack(extracted, new ItemStack(MillBlocks.ICE_BRICK));
                        }
                    } else if (goal instanceof GoalHarvestWarts && home) {
                        activity(actions,goal,source,null);
                        addStack(extracted, new ItemStack(Items.NETHER_WART));
                    } else if (goal instanceof GoalShearSheep && home && hasTag(loc, building, plan, "sheeps")) {
                        activity(actions,goal,source,null);
                        activities.add(source);
                        extracted.source=label("Shear an adult sheep with wool","Стрижка дорослої вівці з вовною","Стрижка взрослой овцы с шерстью");
                        addStack(extracted, new ItemStack(Blocks.WHITE_WOOL));
                    } else if (goal instanceof GoalBreedAnimals && home
                            && (hasTag(loc, building, plan, "sheeps") || hasTag(loc, building, plan, "cattle")
                                || hasTag(loc, building, plan, "pigs") || hasTag(loc, building, plan, "chicken"))) {
                        activity(actions,goal,source,null);
                        activities.add(source);
                    } else if (goal instanceof GoalBrewPotions && home) {
                        activity(actions,goal,source,null);
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
                if (!generic.leasure) {
                    String activity = generic.gameName();
                    if (activity != null && !activity.isBlank() && !activity.startsWith("goal."))
                        { activities.add(activity); activity(actions,goal,activity,null); }
                }
                if (generic instanceof GoalGenericCrafting crafting) {
                    List<ItemStack> inputs=recipeStacks(crafting.input),outputs=recipeStacks(crafting.output);
                    RecipeView recipe=inputs.isEmpty()||outputs.isEmpty()?null:new RecipeView(inputs,outputs);
                    activity(actions,goal,source,recipe);
                    produced.recipe=recipe;
                    crafting.output.forEach((item,count)->{
                        if(count==null||count<=0)return;
                        // Some configured goals deliberately consume no ingredients.
                        // Report that fact and the batch size, not the repeated goal name.
                        produced.source=crafting.input.isEmpty()
                                ? label("No ingredients consumed","Без витрат сировини","Без расхода сырья")
                                    + "; " + count + label(" per cycle"," шт. за цикл"," шт. за цикл")
                                : recipe==null ? label("Recipe not available","Рецепт не відображено","Рецепт не отображается") : null;
                        addItem(produced,item);
                    });
                } else if (generic instanceof GoalGenericMining mining) {
                    mining.loots.forEach((item, count) -> { if (count != null && count > 0) addItem(extracted, item); });
                } else if (generic instanceof GoalGenericHarvestCrop harvest) {
                    extracted.source=source+" | "+label("Crop: ","Культура: ","Культура: ")+harvest.cropType;
                    for (var bonus : harvest.harvestItem) addItem(extracted, bonus.item);
                    extracted.source+=label(" (irrigation bonus)"," (бонус зрошення)"," (бонус орошения)");
                    addItem(extracted, harvest.irrigationBonusCrop);
                } else if (generic instanceof GoalGenericGatherBlocks gather) {
                    for (var bonus : gather.harvestItem) addItem(extracted, bonus.item);
                } else if (generic instanceof GoalGenericSlaughterAnimal slaughter) {
                    extracted.source=source+" | "+label("Chance-based drops","Імовірнісна здобич","Вероятностная добыча");
                    for (var bonus : slaughter.bonusItem) addItem(extracted, bonus.item);
                } else if (generic instanceof GoalGenericCooking cooking && cooking.itemToCook != null
                        && cooking.itemToCook.staticStack != null && townHall.world != null) {
                    produced.source=source;
                    ItemStack raw=cooking.itemToCook.staticStack.copy();raw.setCount(1);
                    var input=new SimpleContainer(raw.copy());
                    townHall.world.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,townHall.world)
                        .ifPresent(recipe->{
                            ItemStack output=recipe.getResultItem().copy();
                            if(output.isEmpty())return;
                            RecipeView view=new RecipeView(List.of(raw),List.of(output));
                            produced.recipe=view;
                            activity(actions,goal,source,view);
                            addStack(produced,output);
                        });
                }
            }
        }

        // Declared abstract production is a distinct planning figure, not proof
        // that a real NPC recipe currently produces this item.
        Goods planned = new Goods();
        planned.source=label("Abstract plan output; no confirmed runtime recipe","Планове виробництво; фактичний рецепт не підтверджено","Плановое производство; фактический рецепт не подтверждён");
        if (plan != null) plan.abstractedProduction.forEach((item, count) -> {
            if (count != null && count > 0) addItem(planned, item);
        });
        planned.removeAll(produced);
        planned.removeAll(extracted);
        Goods sells = new Goods();
        sells.source=label("For sale","Продається","Продаётся");
        if (loc.culture != null && loc.shop != null) {
            var goods = loc.culture.shopSells.get(loc.shop);
            if (goods != null) for (var good : goods) if (good != null) addItem(sells, good.item);
        }
        section(lines,label("Activities:","Що робить:","Что делает:"),actions,true);
        section(lines, label("Potential production:", "Може виробляти:", "Может производить:"), produced);
        section(lines, label("Can extract / gather:", "Може добувати / збирати:", "Может добывать / собирать:"), extracted);
        section(lines, label("Declared production:", "Виробництво за планом:", "Производство по плану:"), planned);
        section(lines, label("Sells:", "Продає:", "Продаёт:"), sells);

        List<String> effects = loc.getBuildingEffects(townHall.world);
        if (produced.isEmpty() && extracted.isEmpty() && planned.isEmpty() && sells.isEmpty()) {
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
