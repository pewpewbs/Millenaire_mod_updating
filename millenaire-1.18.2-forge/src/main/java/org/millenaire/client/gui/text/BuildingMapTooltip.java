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
    record Portrait(net.minecraft.resources.ResourceLocation texture,String detail,List<ItemStack> activities) {}
    record Line(String text,int color,List<ItemStack> icons,List<String> iconDetails,String detail,List<List<RecipeView>> recipes,boolean action,List<Portrait> portraits) {
        Line(String text,int color){this(text,color,List.of(),List.of(),null,List.of(),false,List.of());}
        Line(String text,int color,String detail){this(text,color,List.of(),List.of(),detail,List.of(),false,List.of());}
        Line(String text,int color,List<ItemStack> icons,List<String> iconDetails,String detail,List<List<RecipeView>> recipes,boolean action){
            this(text,color,icons,iconDetails,detail,recipes,action,List.of());
        }
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
            return sources==null?"":String.join("; ",sources);
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
        lines.add(new Line(title,action?0x99D9EA:0xD5DCC2,List.copyOf(goods.icons),goods.icons.stream().map(goods::detail).toList(),null,
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
        if(goal instanceof GoalGenericHarvestCrop || goal instanceof GoalHarvestWarts || goal instanceof GoalIndianHarvestSugarCane)return new ItemStack(Items.IRON_HOE);
        if(goal instanceof GoalGenericPlantSapling)return new ItemStack(Blocks.OAK_SAPLING);
        if(goal instanceof GoalLumbermanChopTrees)return new ItemStack(Items.IRON_AXE);
        if(goal instanceof GoalGenericMining mining&&mining.sourceBlockState!=null){
            var block=mining.sourceBlockState.getBlock();
            if(block==Blocks.SAND||block==Blocks.RED_SAND||block==Blocks.GRAVEL||block==Blocks.CLAY)
                return new ItemStack(Items.IRON_SHOVEL);
        }
        if(goal instanceof GoalGenericMining||goal instanceof GoalMinerMineResource)return new ItemStack(Items.IRON_PICKAXE);
        if(goal instanceof GoalFish)return new ItemStack(Items.FISHING_ROD);
        if(goal instanceof GoalBrewPotions)return new ItemStack(Items.BREWING_STAND);
        if(goal instanceof GoalGenericCrafting)return new ItemStack(Blocks.CRAFTING_TABLE);
        if(goal instanceof GoalGenericGatherBlocks gather&&softGather(gather))return new ItemStack(Items.IRON_SHOVEL);
        if(goal instanceof GoalGenericGatherBlocks||goal instanceof GoalGenericTakeFromBuilding)return new ItemStack(Blocks.CHEST);
        return new ItemStack(Items.BOOK);
    }
    private static void activity(Goods actions,Goal goal,String source,RecipeView recipe){
        // Activity icons describe the category, not the products or their recipes.
        actions.source=goal instanceof GoalGenericCrafting
                ? label("Crafts resources","Виготовляє ресурси","Изготавливает ресурсы")
                : goal instanceof GoalGenericCooking || goal instanceof GoalGenericTendFurnace
                ? label("Processes resources in a furnace","Переробляє ресурси в печі","Перерабатывает ресурсы в печи")
                : goal instanceof GoalGenericMining || goal instanceof GoalMinerMineResource || (goal instanceof GoalGenericGatherBlocks gather&&softGather(gather))
                ? label("Resource extraction site","Місце для добування ресурсів","Место добычи ресурсов")
                : goal instanceof GoalLumbermanChopTrees
                ? label("Logging site","Місце для заготівлі деревини","Место заготовки древесины")
                : goal instanceof GoalGenericHarvestCrop || goal instanceof GoalHarvestWarts || goal instanceof GoalIndianHarvestSugarCane
                ? label("Harvesting","Збір врожаю","Сбор урожая")
                : goal instanceof GoalGenericGatherBlocks
                ? label("Gathers resources","Збирає ресурси","Собирает ресурсы")
                : goal instanceof GoalGenericSlaughterAnimal
                ? label("Slaughters animals","Забиває тварин","Забивает животных")
                : goal instanceof GoalGenericPlantCrop
                ? label("Crop planting","Посадка культур","Посадка культур")
                : goal instanceof GoalGenericPlantSapling
                ? label("Plants saplings","Садить саджанці","Сажает саженцы")
                : goal instanceof GoalBreedAnimals
                ? label("Breeds animals","Розводить тварин","Разводит животных") : source;
        addStack(actions,actionIcon(goal));
    }
    private static void staff(List<Line> lines,BuildingLocation loc,Building townHall,boolean expanded){
        if(loc.culture==null||loc.level<0)return;
        // These are assigned residential slots, not workplace attendance.
        List<String> roles=new ArrayList<>(loc.getMaleResidents());roles.addAll(loc.getFemaleResidents());
        if(roles.isEmpty())return;
        int total=roles.size(),filled=0,children=0;
        String heading=label("Residents: ","Мешканці: ","Жители: ");
        List<org.millenaire.common.village.VillagerRecord> staff=new ArrayList<>();
        var records=townHall.getMapResidentRecords();
        if(records==null || loc.chestPos==null){
            add(lines,heading+label("Data unavailable","Дані недоступні","Данные недоступны"),MUTED);
            return;
        }
        for(var record:records){
            if(record.killed||record.raidingVillage||record.getHousePos()==null
                    ||!record.getHousePos().sameBlock(loc.chestPos))continue;
            VillagerType type=record.getType();
            if(type==null)continue;
            if(type.isChild)children++;else filled++;
            staff.add(record);
        }
        int lastDigit=children%10,lastTwo=children%100;
        boolean singular=lastDigit==1&&lastTwo!=11;
        boolean few=lastDigit>=2&&lastDigit<=4&&(lastTwo<12||lastTwo>14);
        String childWord=label(children==1?"child":"children",
                singular?"дитина":few?"дитини":"дітей",
                singular?"ребёнок":few?"ребёнка":"детей");
        String childNote=children>0?" (+"+children+" "+childWord+")":"";
        add(lines,heading+filled+"/"+total+childNote,filled==0?0xFF7777:filled<total?GOLD:0x88DD88);

        if(!expanded)return;
        List<Portrait> portraits=new ArrayList<>();
        staff.sort(java.util.Comparator.comparing((org.millenaire.common.village.VillagerRecord r)->r.getType().isChild).thenComparing(r->r.getName()));
        for(var record:staff){
            var npc=townHall.mw.getVillagerById(record.getVillagerId());
            String status;
            if(npc==null||!npc.isAlive())status=label("Live status unavailable","Поточний стан невідомий","Текущее состояние неизвестно");
            else{
                var goal=npc.getCurrentGoal();
                status=goal==null?label("No current task","Немає поточного завдання","Нет текущего задания"):goal.gameName(npc);
            }
            var texture=npc!=null&&npc.getTexture()!=null?npc.getTexture():record.texture;
            portraits.add(new Portrait(texture,record.getName()+"; "+record.getGameOccupation()+"; "
                    +label("Current task: ","Поточна дія: ","Текущее действие: ")+status, residentActivities(record.getType())));
        }
        if(!portraits.isEmpty())lines.add(new Line("",NORMAL,List.of(),List.of(),null,List.of(),false,List.copyOf(portraits)));

    }

    private static List<ItemStack> residentActivities(VillagerType type){
        Goods activities=new Goods();
        for(Goal goal:type.goals){
            if(!goal.travelBookShow||goal.leasure)continue;
            ItemStack icon=goal.getIcon();
            if(icon!=null&&!icon.isEmpty())addStack(activities,icon);
        }
        if(type.isForeignMerchant)addStack(activities,new ItemStack(org.millenaire.common.item.MillItems.PURSE));
        return List.copyOf(activities.icons);
    }

    /** Only report differences actually declared by the next authored plan. */
    private static String upgradeDetail(BuildingLocation loc,BuildingPlan current,BuildingPlan next){
        List<String> changes=new ArrayList<>();
        changes.add(label("Next level: ","Наступний рівень: ","Следующий уровень: ")+next.level);
        if(current!=null){
            int before=loc.getMaleResidents().size()+loc.getFemaleResidents().size();
            int after=next.getMaleResident().size()+next.getFemaleResident().size();
            if(before!=after)changes.add(label("Resident slots: ","Місця для мешканців: ","Места для жителей: ")+before+" → "+after);
            if(current.irrigation!=next.irrigation)changes.add(label("Irrigation: ","Зрошення: ","Орошение: ")+current.irrigation+" → "+next.irrigation);
            if(!java.util.Objects.equals(current.shop,next.shop))changes.add(label("Shop configuration changes","Змінюється конфігурація крамниці","Изменяется конфигурация лавки"));
            if(!current.tags.equals(next.tags)||!current.clearTags.equals(next.clearTags))
                changes.add(label("Building functions change according to the plan","Змінюються функції будівлі за планом","Меняются функции здания по плану"));
        }
        changes.add(label("Construction changes according to the next plan","Перебудова за планом наступного рівня","Перестройка по плану следующего уровня"));
        if(next.resCost!=null&&!next.resCost.isEmpty()){
            changes.add(label("Required materials:","Необхідні матеріали:","Необходимые материалы:"));
            next.resCost.entrySet().stream().filter(e->e.getValue()!=null&&e.getValue()>0)
                .sorted(java.util.Comparator.comparing(e->e.getKey().getName()))
                .forEach(e->changes.add(e.getKey().getName()+" ×"+e.getValue()));
        }
        return String.join("; ",changes);
    }

    static List<Line> describe(BuildingLocation loc, Building townHall, String nativeName, boolean construction,boolean expanded) {
        List<Line> lines = new ArrayList<>();
        add(lines, nativeName, 0xC8B99A);
        String name=loc.getIBuildingPlan()==null?"":loc.getGameName();
        Building building = loc.getBuilding(townHall.world);
        BuildingPlan plan = loc.getPlan();

        // Level zero is the completed initial building. Keep the name and
        // level together; no meaningless 0/0, and no level for unbuilt plans.
        int nameColor=NORMAL;
        String levelDetail=null;
        if(!construction&&loc.level>=0&&!loc.isCustomBuilding&&loc.culture!=null){
            var set=loc.culture.getBuildingPlanSet(loc.planKey);
            int variation=loc.getVariation();
            if(set!=null&&variation>=0&&variation<set.plans.size()
                    &&set.plans.get(variation).length>loc.level){
                int maximum=set.plans.get(variation).length-1;
                if(maximum>0){
                    name+=" lvl "+loc.level;
                    if(loc.level==maximum){
                        nameColor=GOLD;
                        levelDetail=label("Maximum level","Максимальний рівень","Максимальный уровень");
                    }else if(expanded)levelDetail=upgradeDetail(loc,plan,set.plans.get(variation)[loc.level+1]);
                }
            }
        }
        if(!name.isBlank())lines.add(new Line(name,nameColor,levelDetail));
        if(construction)add(lines,label("Under construction","Будується","Строится"),MUTED);
        else if(loc.level<0)add(lines,label("Not built yet","Ще не збудовано","Ещё не построено"),0xFF7777);
        else if(loc.isCustomBuilding)add(lines,label("Custom building (no standard upgrades)","Власна будівля (без стандартних покращень)","Пользовательская постройка (без стандартных улучшений)"),MUTED);

        // Planned residents describe home-based production; village worker types
        // describe tagged workplaces and the central building. These are potential
        // outputs, not a promise of current workers, stock or actual throughput.
        if(!construction||townHall.getLocations().stream().anyMatch(existing->existing.level>=0
                &&existing.chestPos!=null&&existing.chestPos.sameBlock(loc.chestPos)))
            staff(lines,loc,townHall,expanded);

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
            var roster=townHall.getMapResidentRecords();
            if(roster==null)roster=townHall.getVillagerRecords().values();
            for (var record : roster) {
                if (!record.killed) {
                    VillagerType type = record.getType();
                    if (type != null) workers.add(type);
                }
            }
        }
        workers.addAll(residents);
        Goods produced = new Goods();
        Goods extracted = new Goods(), gathered=new Goods(), obtained=new Goods(), grown=new Goods();
        Goods actions=new Goods();
        Set<String> activities = new TreeSet<>();
        boolean fruitTree=residents.isEmpty()&&workers.stream().flatMap(type->type.goals.stream()).anyMatch(goal->
                goal instanceof GoalGenericGatherBlocks gather&&gather.buildingTag!=null
                &&hasTag(loc,building,plan,gather.buildingTag)&&fruitGather(gather));
        boolean externalFarm=false;
        for (VillagerType type : workers) {
            for (Goal goal : type.goals) {
                boolean home = residents.contains(type);
                String action=goal.gameName();
                String source=(action==null||action.isBlank()||action.startsWith("goal."))?goal.getClass().getSimpleName():action;
                produced.source=null;extracted.source=null;produced.recipe=null;extracted.recipe=null;
                gathered.source=null;obtained.source=null;grown.source=null;
                if (!(goal instanceof GoalGeneric generic)) {
                    // Legacy goals have no generic output table. Describe only
                    // products whose destination contract is explicit in code.
                    if (goal instanceof GoalLumbermanChopTrees && hasTag(loc, building, plan, "grove")) {
                        activity(actions,goal,source,null);
                        for(var wood:groveWoodTypes(plan,building))
                            addStack(extracted,new ItemStack(wood.log()));
                    } else if (goal instanceof GoalFishInuit && hasTag(loc, building, plan, "fishingspot")) {
                        activity(actions,goal,source,null);
                        addStack(obtained, new ItemStack(Items.COD));
                        addStack(obtained, new ItemStack(Items.BONE_BLOCK));
                    } else if (goal instanceof GoalFish && hasTag(loc, building, plan, "fishingspot")) {
                        activity(actions,goal,source,null);
                        addStack(obtained, new ItemStack(Items.COD));
                    } else if (goal instanceof GoalIndianHarvestSugarCane && hasTag(loc, building, plan, "sugarplantation")) {
                        activity(actions,goal,source,null);
                        addStack(gathered, new ItemStack(Items.SUGAR_CANE));
                    } else if (goal instanceof GoalIndianGatherBrick && hasTag(loc, building, plan, "brickkiln")) {
                        activity(actions,goal,source,null);
                        addItem(produced, InvItem.createInvItem(org.millenaire.common.block.MillBlocks.BS_MUD_BRICK));
                    } else if (goal instanceof GoalMinerMineResource miner
                            && (miner.buildingTag == null ? home : hasTag(loc, building, plan, miner.buildingTag))) {
                        activities.add(label("Resource extraction", "Добування ресурсів", "Добыча ресурсов"));
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
                        addStack(gathered, new ItemStack(Items.NETHER_WART));
                    } else if (goal instanceof GoalShearSheep && home && hasTag(loc, building, plan, "sheeps")) {
                        activity(actions,goal,source,null);
                        activities.add(source);
                        obtained.source=label("Colour depends on the sheep","Колір залежить від вівці","Цвет зависит от овцы");
                        addStack(obtained,new ItemStack(Items.WHITE_WOOL));
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
                boolean central = loc.chestPos!=null&&loc.chestPos.sameBlock(townHall.getPos());
                if(home&&hasTag(loc,building,plan,"farm")&&generic.buildingTag!=null
                        &&(generic instanceof GoalGenericPlantCrop||generic instanceof GoalGenericHarvestCrop)
                        &&!hasTag(loc,building,plan,generic.buildingTag))externalFarm=true;
                if (generic.townHallGoal ? !central
                        : generic.buildingTag == null ? !home : !hasTag(loc, building, plan, generic.buildingTag)) continue;
                if (generic.requiredTag != null && !hasTag(loc, building, plan, generic.requiredTag)) continue;
                if(generic instanceof GoalGenericPlantCrop plant
                        && !org.millenaire.common.village.ProductionCapability.hasCrop(plan,building,plant.cropType))continue;
                if(generic instanceof GoalGenericHarvestCrop harvest
                        && !org.millenaire.common.village.ProductionCapability.hasCrop(plan,building,harvest.cropType))continue;
                // Supply pickup is not an output or the purpose of a building.
                if(generic instanceof GoalGenericTakeFromBuilding)continue;
                if(fruitTree&&generic instanceof GoalGenericPlantSapling)continue;
                // A configured workplace can also have a non-producing role:
                // planting, visiting, tending equipment or collecting supplies.
                if (!generic.leasure && !(fruitTree&&generic instanceof GoalGenericGatherBlocks)) {
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
                        // A free output needs only its item name; no duplicate activity or batch note.
                        produced.source=!crafting.input.isEmpty()&&recipe==null
                                ? label("Recipe not available","Рецепт не відображено","Рецепт не отображается") : null;
                        addItem(produced,item);
                    });
                } else if (generic instanceof GoalGenericMining mining) {
                    mining.loots.forEach((item, count) -> { if (count != null && count > 0) addItem(extracted, item); });
                } else if (generic instanceof GoalGenericHarvestCrop harvest) {
                    for (var bonus : harvest.harvestItem)
                        if(bonus.tag==null||hasTag(loc,building,plan,bonus.tag))addItem(gathered,bonus.item);
                    addItem(gathered, harvest.irrigationBonusCrop);
                } else if (generic instanceof GoalGenericGatherBlocks gather) {
                    Goods destination=fruitTree&&fruitGather(gather)?grown:softGather(gather)?extracted:gathered;
                    for (var bonus : gather.harvestItem)
                        if(bonus.tag==null||hasTag(loc,building,plan,bonus.tag))addItem(destination,bonus.item);
                } else if (generic instanceof GoalGenericSlaughterAnimal slaughter) {
                    for(var good:org.millenaire.common.village.ProductionCapability.animalGoods(slaughter.animalKey,type)){
                        if(GoalShearSheep.isSheepWool(good)&&slaughter.animalKey!=null
                                &&"sheep".equals(slaughter.animalKey.getPath())){
                            obtained.source=label("Colour depends on the sheep","Колір залежить від вівці","Цвет зависит от овцы");
                            addStack(obtained,new ItemStack(Items.WHITE_WOOL));obtained.source=null;
                        }else addItem(obtained,good);
                    }
                    for (var bonus : slaughter.bonusItem)
                        if(bonus.tag==null||hasTag(loc,building,plan,bonus.tag))addItem(obtained, bonus.item);
                } else if (generic instanceof GoalGenericCooking cooking && cooking.itemToCook != null
                        && cooking.itemToCook.staticStack != null && townHall.world != null) {
                    produced.source=null;
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
        planned.removeAll(gathered);planned.removeAll(obtained);planned.removeAll(grown);
        Goods sells = new Goods();
        if (loc.culture != null && loc.shop != null) {
            var goods = loc.culture.shopSells.get(loc.shop);
            if (goods != null) for (var good : goods) if (good != null) addItem(sells, good.item);
        }
        // Purpose icons describe the place, not duplicate product labels.
        boolean inn=hasTag(loc,building,plan,"inn");
        actions.recipe=null;
        if(inn){
            actions.source=label("Lodging for travelling merchants","Прихисток для мандрівних торговців","Приют для странствующих торговцев");
            addStack(actions,new ItemStack(Items.WHITE_BED));
            actions.source=label("Inter-village trade","Міжселищна торгівля","Межпоселенческая торговля");
            addStack(actions,new ItemStack(org.millenaire.common.item.MillItems.PURSE));
        }else if(!loc.getMaleResidents().isEmpty()||!loc.getFemaleResidents().isEmpty()){
            actions.source=label("Housing","Житло","Жильё");
            addStack(actions,new ItemStack(Items.WHITE_BED));
        }
        if(!loc.getVisitors().isEmpty()&&!inn){
            actions.source=label("Accommodation for visitors","Помешкання для відвідувачів","Жильё для посетителей");
            addStack(actions,new ItemStack(Items.WHITE_BED));
        }
        if(!sells.isEmpty()&&!inn){
            actions.source=label("Trade","Торгівля","Торговля");
            addStack(actions,new ItemStack(org.millenaire.common.item.MillItems.PURSE));
        }
        if(externalFarm){
            actions.source=label("Residents cultivate separate fields","Мешканці обробляють окремі поля","Жители обрабатывают отдельные поля");
            addStack(actions,new ItemStack(Items.IRON_HOE));
        }
        if(fruitTree&&!grown.isEmpty()){
            actions.source=label("Fruit growing","Вирощування плодів","Выращивание плодов");
            addStack(actions,new ItemStack(grown.icons.stream().anyMatch(s->s.is(org.millenaire.common.item.MillItems.OLIVES))
                    ? MillBlocks.LEAVES_OLIVETREE : Blocks.OAK_LEAVES));
        }
        if(visitPurpose(loc,building,plan,"patrol")){
            actions.source=label("Patrol point","Місце патрулювання","Место патрулирования");
            addStack(actions,new ItemStack(Items.SHIELD));
        }
        if(visitPurpose(loc,building,plan,"gopray")){
            actions.source=label("Place of prayer","Місце молитви","Место молитвы");
            addStack(actions,new ItemStack(Items.BOOK));
        }
        if(!extracted.icons.isEmpty()&&extracted.icons.stream().allMatch(stack->
                stack.is(Items.SAND)||stack.is(Items.RED_SAND)||stack.is(Items.GRAVEL)||stack.is(Items.CLAY_BALL))){
            actions.icons.removeIf(stack->stack.is(Items.IRON_PICKAXE));
            actions.source=label("Resource extraction site","Місце для добування ресурсів","Место добычи ресурсов");
            addStack(actions,new ItemStack(Items.IRON_SHOVEL));
        }
        section(lines,label("Purpose:","Призначення:","Назначение:"),actions,true);
        section(lines, label("Potential production:", "Може виробляти:", "Может производить:"), produced);
        section(lines,label("Can extract:","Може добувати:","Может добывать:"),extracted);
        section(lines,label("Can gather:","Може збирати:","Может собирать:"),gathered);
        section(lines,label("Can obtain:","Може отримувати:","Может получать:"),obtained);
        section(lines,label("Grows:","Вирощує:","Выращивает:"),grown);
        section(lines, label("Declared production:", "Виробництво за планом:", "Производство по плану:"), planned);
        section(lines, label("Sells:", "Продає:", "Продаёт:"), sells);

        List<String> effects = new ArrayList<>(loc.getBuildingEffects(townHall.world));
        if(loc.shop!=null&&loc.culture!=null)effects.remove(org.millenaire.common.utilities.LanguageUtilities.string(
                "effect.shop",loc.culture.getCultureString("shop."+loc.shop)));
        if (actions.isEmpty() && produced.isEmpty() && extracted.isEmpty() && gathered.isEmpty()
                && obtained.isEmpty() && grown.isEmpty() && planned.isEmpty() && sells.isEmpty()) {
            Set<String> roles = new LinkedHashSet<>(effects);

            if (hasTag(loc, building, plan, "grove")) roles.add(label("Forestry", "Лісозаготівля", "Лесозаготовка"));
            if (hasTag(loc, building, plan, "fishingspot")) roles.add(label("Fishing", "Рибальство", "Рыболовство"));
            if (hasTag(loc, building, plan, "sugarplantation")) roles.add(label("Sugar cane cultivation", "Вирощування цукрової тростини", "Выращивание сахарного тростника"));
            if (hasTag(loc, building, plan, "brickkiln")) roles.add(label("Brick drying", "Сушіння цегли", "Сушка кирпича"));
            if(!roles.isEmpty())add(lines, label("Purpose: ", "Призначення: ", "Назначение: ") + String.join("; ", roles), MUTED);
        } else {
            for (String effect : effects) add(lines, effect, MUTED);
        }
        return lines;
    }

    private static boolean softGather(GoalGenericGatherBlocks gather){
        if(gather.gatherBlockState==null)return false;
        var block=gather.gatherBlockState.getBlock();
        return block==Blocks.SAND||block==Blocks.RED_SAND||block==Blocks.GRAVEL||block==Blocks.CLAY;
    }

    private static boolean fruitGather(GoalGenericGatherBlocks gather){
        return gather.harvestItem.stream().anyMatch(bonus->bonus.item!=null
                &&(bonus.item.getItem()==org.millenaire.common.item.MillItems.OLIVES
                   ||bonus.item.getItem()==org.millenaire.common.item.MillItems.CIDER_APPLE));
    }

    // Only claim a configured visit destination, not merely a decorative tag.
    private static boolean visitPurpose(BuildingLocation loc,Building building,BuildingPlan plan,String key){
        Goal goal=Goal.goals.get(key);
        return goal instanceof GoalGenericVisit visit&&visit.targetVillagerGoals==null
                &&!visit.townHallGoal&&visit.buildingTag!=null
                &&hasTag(loc,building,plan,visit.buildingTag)
                &&(visit.requiredTag==null||hasTag(loc,building,plan,visit.requiredTag));
    }

    // Match the sapling markers consumed by GoalGenericPlantSapling and the
    // six ordinary log families actually harvested by GoalLumbermanChopTrees.
    private static final java.util.Map<BuildingPlan,Set<org.millenaire.common.block.WoodVariant>> PLAN_WOOD =
            new java.util.WeakHashMap<>();
    private static org.millenaire.common.block.WoodVariant woodMarker(String marker){
        if(marker==null)return null;
        return switch(marker){
            case "oakspawn" -> org.millenaire.common.block.WoodVariant.OAK;
            case "pinespawn" -> org.millenaire.common.block.WoodVariant.SPRUCE;
            case "birchspawn" -> org.millenaire.common.block.WoodVariant.BIRCH;
            case "junglespawn" -> org.millenaire.common.block.WoodVariant.JUNGLE;
            case "acaciaspawn" -> org.millenaire.common.block.WoodVariant.ACACIA;
            case "darkoakspawn" -> org.millenaire.common.block.WoodVariant.DARK_OAK;
            default -> null;
        };
    }
    private static Set<org.millenaire.common.block.WoodVariant> groveWoodTypes(BuildingPlan plan,Building building){
        var types=java.util.EnumSet.noneOf(org.millenaire.common.block.WoodVariant.class);
        if(plan!=null)types.addAll(PLAN_WOOD.computeIfAbsent(plan,key->{
            var found=java.util.EnumSet.noneOf(org.millenaire.common.block.WoodVariant.class);
            for(BuildingPlan p=key;p!=null;){
                if(p.plan!=null)for(var floor:p.plan)if(floor!=null)
                    for(var row:floor)if(row!=null)for(var point:row)if(point!=null){
                        var wood=woodMarker(point.getSpecialType());
                        if(wood!=null)found.add(wood);
                        if(point.getBlockState()!=null){
                            wood=org.millenaire.common.block.WoodVariant.fromState(point.getBlockState());
                            if(wood!=null&&(point.getBlock()==wood.log()||point.getBlock()==wood.sapling()))found.add(wood);
                        }
                    }
                if(p.level<=0)break;
                var previous=p.getPreviousBuildingPlan();
                if(previous==null||previous.level>=p.level)break;
                p=previous;
            }
            return Set.copyOf(found);
        }));
        if(building!=null)for(String marker:building.getResManager().woodspawnTypes.values()){
            var wood=woodMarker(marker);if(wood!=null)types.add(wood);
        }
        return types;
    }

    private static boolean hasTag(BuildingLocation loc, Building building, BuildingPlan plan, String tag) {
        if (building != null && building.containsTags(tag)) return true;
        if (plan != null && plan.containsTags(tag)) return true;
        var custom = loc.isCustomBuilding ? loc.getCustomPlan() : null;
        return custom != null && custom.tags.contains(tag);
    }
}
