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
        self.assertIn("record.getHousePos().equals(building.getPos())", map_info)
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

    def test_ukrainian_goal_labels(self):
        lang = ROOT.parents[2] / "resources/todeploy/millenaire/languages/uk/strings.txt"
        data = lang.read_text(encoding="utf-8-sig")
        for value in ("goal.construction=Будує", "goal.shearsheep=Стриже овець",
                      "goal.breed=Розводить тварин", "goal.cooksteak=Готує стейки"):
            self.assertIn(value, data)

if __name__ == "__main__":
    unittest.main()
