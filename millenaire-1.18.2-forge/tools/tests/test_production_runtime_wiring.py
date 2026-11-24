"""Source-level regression guards; Forge and live village tests remain separate."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire/common"
DATA = ROOT / "src/main/resources/todeploy/millenaire"

def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

class ProductionRuntimeWiring(unittest.TestCase):
    def test_mining_and_fishing_do_not_generate_on_stuck(self):
        for name in ("goal/GoalMinerMineResource.java", "goal/GoalFish.java",
                     "goal/generic/GoalGenericMining.java"):
            source = read(name)
            with self.subTest(name=name):
                stuck = source.split("public boolean stuckAction(",1)[1].split("}",1)[0]
                self.assertIn("return false;",stuck)
                self.assertNotIn("performAction",stuck)
                self.assertIn("WorkAccess.canAccessResource",source)
                action = source.split("public boolean performAction(",1)[1]
                self.assertIn("isWithinActionRange",action)
                self.assertIn("isClientSide",action)
        access = read("goal/WorkAccess.java").split("public static boolean canAccessResource(",1)[1]
        self.assertIn("hasChunkAt",access)
        self.assertIn("distanceToSqr(center)",access)
        self.assertIn("visible(villager, center",access)

    def test_registered_sources_and_fishing_points(self):
        legacy = read("goal/GoalMinerMineResource.java")
        generic = read("goal/generic/GoalGenericMining.java")
        fish = read("goal/GoalFish.java")
        for source in (legacy,generic):
            self.assertIn("validTarget(",source)
            self.assertIn("sources.get(",source)
            self.assertIn(".contains(point)",source)
            self.assertIn("isStillValidSpecific",source)
        self.assertIn("fishingspots.contains(point)",fish)
        self.assertIn("validFishingPoint",fish)

    def test_crafting_and_cooking_recheck_stock_and_workplace(self):
        goal = read("goal/Goal.java")
        self.assertIn("protected boolean passesStockLimits(",goal)
        self.assertIn("if (!passesStockLimits(villager)) return false;",goal)
        craft = read("goal/generic/GoalGenericCrafting.java").split("public boolean performAction(",1)[1]
        self.assertLess(craft.index("passesStockLimits"),craft.index("beginGoodsTransaction"))
        self.assertIn("getCraftingPos()",craft)
        self.assertIn("isWithinActionRange",craft)
        cook = read("goal/generic/GoalGenericCooking.java").split("public boolean performAction(",1)[1]
        self.assertIn("furnaces.contains(target)",cook)
        self.assertIn("firepits.contains(target)",cook)
        self.assertIn("boolean allowInput = this.passesStockLimits",cook)
        self.assertIn("if (allowInput)",cook)
        self.assertIn("storeCookingOutput",cook)

    def test_gathering_keeps_output_and_limits(self):
        source = read("goal/generic/GoalGenericGatherBlocks.java")
        self.assertIn("!this.validateDest(villager,buildingDest)",source)
        action = source.split("public boolean performAction(",1)[1]
        self.assertIn("passesStockLimits",action)
        self.assertIn("beginGoodsTransaction",action)
        self.assertIn("dest.storeGoods(entry.getKey(),entry.getValue()) != entry.getValue()",action)
        self.assertIn("if (next == null) return true;",action)
        brick = read("goal/GoalIndianGatherBrick.java").split("public boolean performAction(",1)[1]
        self.assertLess(brick.index("if (!villager.setBlockAndMetadata"),brick.index("villager.addToInv"))

    def test_source_migration_is_scoped_and_persisted(self):
        source = read("buildingplan/ProductionSourceRepair.java")
        for key in ("indian/quarry/0","indian/quarry/1","mayan/mayanmining/0",
                    "japanese/japanesequarry/0","japanese/lonemine/0"):
            self.assertIn(key,source)
        for marker in ("isCustomBuilding","getPreviousBuildingPlan()","isType(marker.type())",
                       "adjustForOrientation", "hasChunkAt", "isFaceSturdy", "isAir()",
                       "ProductionSourceMigrationPolicy.Decision.REPLACE", "requestSave",
                       "MILL-PRODUCTION-SOURCE-REVIEW"):
            self.assertIn(marker,source)
        building = read("village/Building.java").split("public void updateBuildingServer()",1)[1]
        self.assertIn("!this.productionSourcesChecked",building)
        self.assertIn("ProductionSourceRepair.reconcile(this)",building)

    def test_migrated_positions_match_bundled_markers(self):
        try:
            from PIL import Image
        except ImportError:
            self.skipTest("Pillow not installed")
        for path, stride, points in (
            ("indian/buildings/basic/quarry_A0.png",15,[(1,2,5,200),(1,6,6,200)]),
            ("indian/buildings/basic/quarry_B0.png",15,[(2,3,3,200),(3,9,8,200)]),
            ("mayan/buildings/core/mayanmining_A0.png",13,[(2,4,5,200),(2,3,9,200)]),
            ("japanese/buildings/core/japanesequarry_A0.png",16,[(1,9,11,200),(1,11,16,200)]),
            ("japanese/buildings/lone/lonemine_A0.png",21,[(7,4,2,60),(7,2,4,60),(7,4,4,200)]),
        ):
            with Image.open(DATA / "cultures" / path) as raw:
                image = raw.convert("RGB")
                for floor,k,j,kind in points:
                    expected = (200,20,20) if kind == 200 else (60,60,60)
                    self.assertEqual(image.getpixel((floor*stride+k,j)),expected,path)

if __name__ == "__main__":
    unittest.main(verbosity=2)
