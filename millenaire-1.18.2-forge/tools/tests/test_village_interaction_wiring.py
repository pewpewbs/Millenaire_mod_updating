"""Source regression guards for the interaction patch; not Minecraft tests."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire"

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

class VillageInteractionWiring(unittest.TestCase):
    def test_resource_panel_uses_same_availability_rule(self):
        building = read("common/village/Building.java")
        panel = read("common/village/buildingmanagers/PanelContentGenerator.java")
        self.assertIn("public int availableForCurrentProject(InvItem item)", building)
        self.assertIn("house.availableForCurrentProject(key)", panel)
        self.assertIn("townHall.availableForCurrentProject(key)", panel)
        self.assertIn("appendAvailabilityDetails(page,house,res,goalPlan)", panel)
        self.assertIn("appendAvailabilityDetails(page,townHall,res,goal)", panel)

    def test_operational_priority_is_soft(self):
        code = read("common/village/VillageProgression.java")
        self.assertIn("operatingSupplier(v,options,preferred.plan(),ignoreCost)", code)
        select = code.split("static Candidate select(", 1)[1].split("static Candidate replacement(", 1)[0]
        self.assertRegex(select, r"Candidate chosen\s*=\s*operating\s*==\s*null\s*\?\s*preferred\s*:\s*operating\s*;")
        self.assertIn("return chosen;", select)
        self.assertIn("if(!operatingNeeds(v,c.plan()).isEmpty())continue;", code)

    def test_pen_pairing_keeps_existing_spawn_limits(self):
        code = read("common/village/Building.java")
        self.assertIn("AnimalPenAccess.choose(this.world,animalx,markers,penAnimals)", code)
        self.assertIn("this.resManager.spawns.get(i).size() * multipliyer - nb", code)
        self.assertIn("penAnimals.add(animalx)", code)
        access = read("common/village/AnimalPenAccess.java")
        self.assertIn("PenAllocationRules.score(", access)
        self.assertIn("level.noCollision(animal,box)", access)
        self.assertIn("ClipContext.Block.COLLIDER", access)
        self.assertNotIn("getChunk(", access)
        self.assertIn("hasBreedingPartner(villager,animal,herd)", read("common/goal/GoalBreedAnimals.java"))

    def test_paired_door_geometry_is_shared(self):
        self.assertIn("DoorPairRules.isRight", read("common/buildingplan/BuildingPlan.java"))
        doors = read("common/pathing/VillageDoorAccess.java")
        self.assertNotIn("pairedDoor(", doors)
        self.assertNotIn("openPartner(", doors)
        self.assertIn("PLAYER_CONTROL.computeIfAbsent", doors)
        self.assertIn("remember(level, pos, actor, source)", doors)

    def test_map_details_use_real_records_and_recipe_inputs(self):
        map_info = read("client/gui/text/BuildingMapTooltip.java")
        self.assertIn("record.getHousePos().sameBlock(loc.chestPos)", map_info)
        self.assertIn("townHall.mw.getVillagerById(record.getVillagerId())", map_info)
        self.assertIn("recipeStacks(crafting.input)", map_info)
        self.assertIn("Live status unavailable", map_info)
        ui = read("client/gui/text/GuiPanelParchment.java")
        self.assertIn("drawPinnedDetail(", ui)
        self.assertIn("pinnedLocation=null;tooltipRefreshNanos=0", ui)

    def test_map_pin_and_overlay(self):
        ui = read("client/gui/text/GuiPanelParchment.java")
        self.assertIn("pinLeft=panelLeft;pinTop=panelTop", ui)
        self.assertIn("closeX=left+w-closeSize", ui)
        self.assertIn("framePose.translate(left,top,600)", ui)
        self.assertIn("itemRenderer.blitOffset=700", ui)

    def test_action_groups_and_real_recipes(self):
        code = read("client/gui/text/BuildingMapTooltip.java")
        self.assertIn("goal instanceof GoalGenericCooking||goal instanceof GoalGenericTendFurnace", code)
        self.assertIn("recipeStacks(crafting.output)", code)
        self.assertIn("recipe.getResultItem().copy()", code)

    def test_map_purpose_is_compact_and_distinct(self):
        code = read("client/gui/text/BuildingMapTooltip.java")
        self.assertIn('label("Purpose:","Призначення:"', code)
        self.assertIn('label("Residents: ","Мешканці: "', code)
        self.assertIn("if(roles.isEmpty())return;", code)
        self.assertIn("if(!roles.isEmpty())add", code)
        self.assertIn("new ItemStack(Items.IRON_HOE)", code)
        self.assertIn("new ItemStack(Items.WHEAT)", code)
        self.assertNotIn('sells.source=label("For sale"', code)
        self.assertNotIn("No ingredients consumed", code)
        self.assertNotIn("Purpose not specified in available data", code)

    def test_grove_and_sheep_outputs_are_not_hardcoded_to_one_variant(self):
        code = read("client/gui/text/BuildingMapTooltip.java")
        for marker in ("oakspawn", "pinespawn", "birchspawn", "junglespawn", "acaciaspawn", "darkoakspawn"):
            self.assertIn('case "'+marker+'"', code)
        self.assertIn("point.getSpecialType()", code)
        self.assertIn("getPreviousBuildingPlan()", code)
        self.assertIn("woodspawnTypes.values()", code)
        self.assertNotIn("DyeColor.values()", code)
        self.assertIn("new ItemStack(Items.WHITE_WOOL)",code)
        self.assertIn("Colour depends on the sheep",code)
        shear=read("common/goal/GoalShearSheep.java")
        self.assertIn("animal.getColor().getName()",shear)
        self.assertIn('"byzantines".equals(culture.key)',shear)
        self.assertIn("usesWhiteWool(villager.getCulture())?Items.WHITE_WOOL:",shear)
        events=read("common/forge/MillEventController.java")
        self.assertIn("normalizeByzantineSheepWool(LivingDropsEvent event)",events)
        self.assertIn("event.getSource().getEntity() instanceof MillVillager villager",events)
        self.assertIn("Items.WHITE_WOOL,stack.getCount()",events)
        self.assertNotIn("getChunk(", code)

    def test_portraits_and_count_overlay_use_synced_data_and_vanilla_renderer(self):
        code = read("client/gui/text/BuildingMapTooltip.java")
        self.assertIn("record.texture", code)
        self.assertIn("npc.getCurrentGoal()", code)
        self.assertIn("new Portrait(texture", code)
        ui = read("client/gui/text/GuiPanelParchment.java")
        self.assertIn("line.portraits()", ui)
        self.assertIn("portraitAvailable(texture)", ui)
        self.assertIn("font.width(cross)", ui)
        self.assertIn("renderGuiItemDecorations(font,item.stack()", ui)
        self.assertIn("Integer.toString(item.stack().getCount())", ui)
        self.assertNotIn("framePose.translate(left,top,850)", ui)

    def test_crop_and_slaughter_capability_shared_with_progression(self):
        code = read("common/village/ProductionCapability.java")
        for marker in ("soil","ricesoil","turmericsoil","maizesoil","carrotsoil","potatosoil",
                       "flowersoil","vinesoil","cacaospot","cottonsoil"):
            self.assertIn('case "'+marker+'"', code)
        self.assertIn("getSoilPoints(crop)", code)
        self.assertIn("if(!existing.world.isClientSide)return false;", code)
        self.assertIn("Items.CHICKEN,Items.FEATHER,Items.EGG", code)
        self.assertIn("worker.collectGoods.stream()", code)
        self.assertIn("worker.bringBackHomeGoods.stream()", code)
        ui = read("client/gui/text/BuildingMapTooltip.java")
        planner = read("common/village/VillageProgression.java")
        for source in (ui,planner):
            self.assertIn("ProductionCapability.hasCrop(", source)
            self.assertIn("ProductionCapability.animalGoods(", source)
        self.assertIn('label("Slaughters animals","Забиває тварин"', ui)
        self.assertIn('label("Crop planting","Посадка культур"', ui)
        self.assertIn("if(maximum>0){",ui)
        self.assertIn("if(loc.level==maximum){",ui)
        self.assertIn("nameColor=GOLD",ui)
        self.assertNotIn('label("Irrigation bonus"', ui)
        self.assertIn('hasTag(loc,building,plan,"inn")', ui)
        self.assertIn("getCount()>1?Integer.toString", read("client/gui/text/GuiPanelParchment.java"))

    def test_child_inflection_level_and_travelbook_village_icons(self):
        ui=read("client/gui/text/BuildingMapTooltip.java")
        self.assertIn('singular?"дитина":few?"дитини":"дітей"',ui)
        self.assertIn("lastTwo!=11",ui)
        self.assertIn("lastTwo<12||lastTwo>14",ui)
        self.assertIn('levelLabel=label("Level: ","Рівень: ","Уровень: ")+loc.level;',ui)
        self.assertIn("levelDetail=upgradeDetail(loc,plan",ui)
        self.assertIn("new RecipeView(stacks,List.of())",ui)
        self.assertNotIn("Перебудова за планом наступного рівня",ui)
        self.assertIn("if(!constructionHover)",read("client/gui/text/GuiPanelParchment.java"))
        self.assertIn("produced.icons.removeIf(BuildingMapTooltip::strawBed)",ui)
        self.assertIn("cycleGroup(goods,BuildingMapTooltip::road",ui)
        self.assertIn("s->s.getItem().isEdible()",ui)
        self.assertIn("crafting.isOnlyStrawBedProduction()",ui)
        self.assertIn("displaySortKey",ui)
        self.assertIn("MillBlocks.BS_WET_BRICK",ui)
        self.assertIn("craftingGoal.isOnlyStrawBedProduction()",read("client/book/BookManagerTravelBook.java"))
        self.assertIn("r.getType().isChild).thenComparing",ui)
        self.assertIn("residentActivities(record.getType())",ui)
        self.assertIn('label("Not built yet","Ще не збудовано","Ещё не построено"),0xFF7777',ui)
        self.assertIn("if(loc.level==maximum){",ui)
        self.assertIn("nameColor=GOLD",ui)
        book=read("client/book/BookManagerTravelBook.java")
        detail=book.split("public TextBook getBookBuildingDetail(",1)[1].split("private void getBookBuildingDetail_exportVariation(",1)[0]
        self.assertIn('new TextLine("", new GuiText.GuiButtonReference(village))',detail)
        self.assertIn("splitInColumns(infoColumns, 6)",detail)
        self.assertNotIn("new TextLine(village.name,",detail)

    def test_resident_roster_does_not_require_loaded_house_or_entity(self):
        ui = read("client/gui/text/BuildingMapTooltip.java")
        staff = ui.split("private static void staff(",1)[1].split("static List<Line> describe(",1)[0]
        self.assertIn("townHall.getMapResidentRecords()",staff)
        self.assertIn("record.getHousePos().sameBlock(loc.chestPos)",staff)
        self.assertNotIn("building!=null",staff)
        self.assertIn("records==null",staff)
        self.assertIn("if(type.isChild)children++;else filled++;",staff)
        self.assertIn("record.texture",staff)
        building = read("common/village/Building.java")
        self.assertIn("building.mapResidentSnapshot=Map.copyOf(vrecords)",building)
        self.assertIn("this.mapResidentSnapshot==null?null",building)
        send = building.split("public void sendMapInfo(Player player)",1)[1].split("public void sendShopPacket",1)[0]
        self.assertIn("this.getTownHall().sendBuildingPacket(player,false)",send)

    def test_map_housing_trade_patrol_and_compact_header(self):
        ui=read("client/gui/text/BuildingMapTooltip.java")
        self.assertIn("MillItems.PURSE",ui)
        self.assertIn('label("Housing","Житло"',ui)
        self.assertIn("Items.WHITE_BED",ui)
        self.assertIn('visitPurpose(loc,building,plan,"patrol")',ui)
        self.assertLess(ui.index("MillItems.PURSE"),ui.index('section(lines,label("Purpose:"'))
        gui=read("client/gui/text/GuiPanelParchment.java")
        self.assertIn("int y = 0; // keep the title",gui)
        self.assertIn("headerReserve=y<12?16:0",gui)

    def test_map_uses_house_registration_key_and_distinct_resource_groups(self):
        ui=read("client/gui/text/BuildingMapTooltip.java")
        self.assertIn("record.getHousePos().sameBlock(loc.chestPos)",ui)
        self.assertNotIn("record.getHousePos().sameBlock(loc.pos)",ui)
        self.assertIn("if(loc.culture==null||loc.level<0)return;",ui)
        self.assertIn("this.pos = location.chestPos",read("common/village/Building.java"))
        self.assertIn("getBuilding(this.chestPos)",read("common/village/BuildingLocation.java"))
        for label in ("Може добувати:","Може збирати:","Може отримувати:","Вирощує:"):
            self.assertIn(label,ui)
        self.assertNotIn("Імовірнісна здобич",ui)
        self.assertNotIn("Може добувати / збирати",ui)
        self.assertIn("Items.IRON_SHOVEL",ui)
        self.assertIn("GoalGenericTakeFromBuilding)continue",ui)
        self.assertIn("visit.targetVillagerGoals==null",ui)
        self.assertIn("residents.isEmpty()&&workers.stream()",ui)

    def test_lone_building_discovery_includes_ordinary_structures(self):
        world=read("common/world/MillWorldData.java")
        registration=world.split("public void registerLoneBuildingsLocation(",1)[1].split("public void registerVillageLocation(",1)[0]
        self.assertIn("command.newlonebuildingfound",registration)
        self.assertNotIn("!type.keyLonebuilding",registration)
        self.assertIn("!newVillage || world.isClientSide",registration)

    def test_current_stock_overrides_stale_missing_label_not_other_status(self):
        panel=read("common/village/buildingmanagers/PanelContentGenerator.java")
        self.assertIn('"ui.lackingresources".equals(house.buildingGoalIssue)',panel)
        self.assertIn('"ui.lackingresources".equals(townHall.buildingGoalIssue)',panel)
        self.assertEqual(panel.count("res.stream().allMatch(key->resHas.get(key)>=resCost.get(key))"),2)
        self.assertIn('"поза спостереженням"',panel)

    def test_ukrainian_goal_labels(self):
        lang = ROOT.parents[2] / "resources/todeploy/millenaire/languages/uk/strings.txt"
        data = lang.read_text(encoding="utf-8-sig")
        for value in ("goal.construction=Будує", "goal.shearsheep=Стриже овець",
                      "goal.breed=Розводить тварин", "goal.cooksteak=Готує стейки"):
            self.assertIn(value, data)

if __name__ == "__main__":
    unittest.main()
