"""Source-level regression checks for the cheat-only test controls.
Run in a checked-out project: python tools/tests/test_test_tuning_wiring.py.
Forge compilation and in-game tests are separate acceptance criteria.
"""
from pathlib import Path
import json
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire"

def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

class TestTuningWiring(unittest.TestCase):
    def test_server_owns_settings_and_authorizes_each_change(self):
        source = read("common/config/TestTuning.java")
        self.assertIn("new Settings(100,100,100,100)", source)
        self.assertIn("Map<MinecraftServer,Settings>", source)
        self.assertIn("player.hasPermissions(2)", source)
        change = source.split("Settings change(", 1)[1].split("public static void applyMovement", 1)[0]
        self.assertLess(change.index("if (!allowed(player))"), change.index("ACTIVE.put"))
        self.assertIn("valid(field,value)", change)
        self.assertIn("ACTIVE.remove(server)", change)
        self.assertIn("MILL-TEST-TUNING", change)
        self.assertIn("level.isClientSide ? DEFAULT", source)

    def test_network_and_menu(self):
        server = read("common/network/ServerReceiver.java")
        client = read("client/network/ClientReceiver.java")
        screen = read("client/gui/text/GuiTestTuning.java")
        menu = read("client/gui/text/GuiConfig.java")
        self.assertIn("packetBuffer.readableBytes() != 8", server)
        self.assertIn("TestTuning.change(sender, field, value)", server)
        self.assertIn("TestTuning.allowed(sender)", server)
        self.assertIn("screen.receiveState(allowed, settings)", client)
        self.assertIn("authorized && !pending", screen)
        self.assertIn("TestTuning.RESET", screen)
        self.assertIn("this.minecraft.player.hasPermissions(2)", menu)
        self.assertIn("new GuiTestTuning(this)", menu)
        self.assertIn('PROTOCOL="1.18.2-4"', read("common/network/MillNetwork.java"))

    def test_separate_speed_factors(self):
        source = read("common/config/TestTuning.java")
        villager = read("common/entity/MillVillager.java")
        self.assertIn("AttributeModifier.Operation.MULTIPLY_TOTAL", source)
        self.assertIn("attribute.removeModifier(MOVE_ID)", source)
        self.assertIn("attribute.addTransientModifier", source)
        self.assertIn("TestTuning.applyMovement(this)", villager)
        self.assertIn("TestTuning.duration(this, goal, goal.actionDuration(this))", villager)
        self.assertIn("percent = settings.build()", source)
        self.assertIn("percent = settings.work()", source)
        self.assertIn("Math.max(1L", source)
        self.assertIn("if (goal.leasure || goal == Goal.sleep)", source)

    def test_resource_yield_is_not_global_inventory_multiplier(self):
        source = read("common/config/TestTuning.java")
        self.assertIn("get(level).yield() / 100", source)
        self.assertNotIn("TestTuning.resourceAmount", read("common/entity/MillVillager.java"))
        self.assertNotIn("TestTuning.resourceAmount", read("common/village/Building.java"))
        for path in ("GoalMinerMineResource", "GoalFish", "GoalFishInuit",
                     "GoalLumbermanChopTrees", "GoalHarvestCacao", "GoalHarvestWarts",
                     "GoalIndianHarvestSugarCane", "GoalByzantineGatherSilk",
                     "GoalByzantineGatherSnails", "GoalShearSheep",
                     "generic/GoalGenericMining", "generic/GoalGenericHarvestCrop",
                     "generic/GoalGenericGatherBlocks", "generic/GoalGenericSlaughterAnimal"):
            self.assertIn("TestTuning.resourceAmount", read("common/goal/" + path + ".java"), path)
        for path in ("GoalBringBackResourcesHome", "GoalDeliverGoodsHousehold",
                     "GoalDeliverResourcesShop", "GoalConstructionStepByStep",
                     "generic/GoalGenericCrafting", "generic/GoalGenericCooking",
                     "generic/GoalGenericTendFurnace"):
            self.assertNotIn("TestTuning.resourceAmount", read("common/goal/" + path + ".java"), path)
        self.assertIn("Math.min(room,", read("common/goal/GoalFish.java"))

    def test_localizations(self):
        for lang in ("en_us", "uk_ua", "ru_ru"):
            strings = json.loads((ROOT / "src/main/resources/assets/millenaire/lang" / (lang + ".json")).read_text(encoding="utf-8"))
            for key in ("title", "move", "work", "yield", "build", "reset", "waiting", "session", "denied"):
                self.assertIn("millenaire.test_tuning." + key, strings)

if __name__ == "__main__":
    unittest.main(verbosity=2)
