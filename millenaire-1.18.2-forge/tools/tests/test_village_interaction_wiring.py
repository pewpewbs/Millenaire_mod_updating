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
        self.assertIn("return operating == null ? preferred : operating;", code)
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
        self.assertIn("record.getHousePos().sameBlock(loc.pos)", map_info)
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
        self.assertIn("DyeColor.values()", code)
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
        self.assertIn('label("Plants crops","Садить культури"', ui)
        self.assertIn("if(maximum>0)add", ui)
        self.assertNotIn('label("Irrigation bonus"', ui)
        self.assertIn('hasTag(loc,building,plan,"inn")', ui)
        self.assertIn("getCount()>1?Integer.toString", read("client/gui/text/GuiPanelParchment.java"))

    def test_resident_roster_does_not_require_loaded_house_or_entity(self):
        ui = read("client/gui/text/BuildingMapTooltip.java")
        staff = ui.split("private static void staff(",1)[1].split("static List<Line> describe(",1)[0]
        self.assertIn("townHall.getMapResidentRecords()",staff)
        self.assertIn("record.getHousePos().sameBlock(loc.pos)",staff)
        self.assertNotIn("building!=null",staff)
        self.assertIn("records==null",staff)
        self.assertIn("if(type.isChild)children++;else filled++;",staff)
        self.assertIn("record.texture",staff)
        building = read("common/village/Building.java")
        self.assertIn("building.mapResidentSnapshot=Map.copyOf(vrecords)",building)
        self.assertIn("this.mapResidentSnapshot==null?null",building)
        send = building.split("public void sendMapInfo(Player player)",1)[1].split("public void sendShopPacket",1)[0]
        self.assertIn("this.getTownHall().sendBuildingPacket(player,false)",send)

    def test_ukrainian_goal_labels(self):
        lang = ROOT.parents[2] / "resources/todeploy/millenaire/languages/uk/strings.txt"
        data = lang.read_text(encoding="utf-8-sig")
        for value in ("goal.construction=Будує", "goal.shearsheep=Стриже овець",
                      "goal.breed=Розводить тварин", "goal.cooksteak=Готує стейки"):
            self.assertIn(value, data)

if __name__ == "__main__":
    unittest.main()
