from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]/"src/main/java/org/millenaire"
class BrickDryingWiring(unittest.TestCase):
    def test_registered_points_only_and_bounded_lookup(self):
        world=(ROOT/"common/world/MillWorldData.java").read_text(encoding="utf-8")
        block=(ROOT/"common/block/BlockWetBrick.java").read_text(encoding="utf-8")
        self.assertIn("now - brickDryingIndexTick >= 100L",world)
        self.assertIn('building.containsTags("brickkiln")',world)
        self.assertIn("isBrickWorkSpot(new Point(pos))",world)
        self.assertIn("getBrickWorkSpots()",world)
        self.assertIn("data.world == level",block)
        self.assertIn("BrickDryingPolicy.NORMAL_LEVEL",block)
        self.assertLess(block.index("getMaxLocalRawBrightness"),block.index("BrickDryingPolicy.advance"))
    def test_display_only(self):
        ui=(ROOT/"client/gui/text/BuildingMapTooltip.java").read_text(encoding="utf-8")
        self.assertIn("+(next.level+1)",ui)
        self.assertIn("+(loc.level+1)",ui)
        self.assertIn("dryingTime(current.level)",ui)
        self.assertIn("dryingTime(next.level)",ui)
    def test_old_upper_bricks_are_recoverable_but_never_refilled(self):
        manager=(ROOT/"common/village/buildingmanagers/ResManager.java").read_text(encoding="utf-8")
        cycle=(ROOT/"common/goal/BrickWorkCycle.java").read_text(encoding="utf-8")
        demand=(ROOT/"common/goal/BrickProductionDemand.java").read_text(encoding="utf-8")
        block=(ROOT/"common/block/BlockWetBrick.java").read_text(encoding="utf-8")
        self.assertIn("loc.level == set.plans.get(variation).length - 1",manager)
        self.assertIn("!brickspot.contains(p.getBelow().getBelow())",manager)
        self.assertIn("&& isProductionBrick(building.world.getBlockState(p.getBlockPos()))",manager)
        self.assertIn("if (isLegacyUpperBrick(p)) return ready;",manager)
        self.assertIn("return !isLegacyUpperBrick(p.getAbove());",manager)
        self.assertNotIn("Two drying tiers", (ROOT/"client/gui/text/BuildingMapTooltip.java").read_text(encoding="utf-8"))
        self.assertIn("getBrickWorkSpots()",cycle)
        self.assertIn("canWorkBrickSpot(p,ready)",cycle)
        self.assertIn("getBrickWorkSpots()",demand)
        self.assertIn("data.isLegacyStackedBrickBase(pos)",block)
        self.assertIn("getMaxLocalRawBrightness(pos.above().above())",block)
    def test_runtime_manager_is_the_one_checked(self):
        building=(ROOT/"common/village/Building.java").read_text(encoding="utf-8")
        manager=(ROOT/"common/village/buildingmanagers/ResManager.java").read_text(encoding="utf-8")
        block=(ROOT/"common/block/BlockWetBrick.java").read_text(encoding="utf-8")
        self.assertIn("public ResManager getResManager()",building)
        self.assertIn("class ResManager",manager)
        for signature in ("boolean wasTwoTierKiln()", "boolean isLegacyUpperBrick(Point p)",
                          "boolean isBrickWorkSpot(Point p)", "List<Point> getBrickWorkSpots()",
                          "boolean canWorkBrickSpot(Point p, boolean ready)"):
            self.assertIn(signature,manager)
        self.assertIn("buildingmanagers.ResManager.isProductionBrick",block)
