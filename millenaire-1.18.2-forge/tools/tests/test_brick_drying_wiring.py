from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]/"src/main/java/org/millenaire"
class BrickDryingWiring(unittest.TestCase):
    def test_registered_points_only_and_bounded_lookup(self):
        world=(ROOT/"common/world/MillWorldData.java").read_text(encoding="utf-8")
        block=(ROOT/"common/block/BlockWetBrick.java").read_text(encoding="utf-8")
        self.assertIn("now - brickDryingIndexTick >= 100L",world)
        self.assertIn('building.containsTags("brickkiln")',world)
        self.assertIn("brickspot.contains(new Point(pos))",world)
        self.assertIn("data.world == level",block)
        self.assertIn("BrickDryingPolicy.NORMAL_LEVEL",block)
        self.assertLess(block.index("getMaxLocalRawBrightness"),block.index("BrickDryingPolicy.advance"))
    def test_display_only(self):
        ui=(ROOT/"client/gui/text/BuildingMapTooltip.java").read_text(encoding="utf-8")
        self.assertIn("+(next.level+1)",ui)
        self.assertIn("+(loc.level+1)",ui)
        self.assertIn("dryingTime(current.level)",ui)
        self.assertIn("dryingTime(next.level)",ui)
