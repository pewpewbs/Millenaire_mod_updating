from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common/buildingplan"


class ConstructionTerrainPreparationTests(unittest.TestCase):
    def test_new_building_keeps_terrain_preparation_before_structure(self):
        plan = (ROOT / "BuildingPlan.java").read_text(encoding="utf-8")
        generation = plan.split("public BuildingBlock[] getBuildingPoints(", 1)[1]
        self.assertIn("boolean prepareNewGround = !this.isUpdate && !this.isSubBuilding", generation)
        self.assertIn("!location.isSubBuildingLocation", generation)
        self.assertIn("!location.bedrocklevel && location.level == 0", generation)
        self.assertLess(generation.index("this.getBuildingPoints_prepareGround("),
                        generation.index("bblocksCombined.addAll(bblocksFirstPass)"))
        self.assertIn("ConstructionSupportOrder.prepare(world,bblocksCombined,true)", generation)
        self.assertIn("else ConstructionSupportOrder.prepare(world,bblocksCombined)", generation)

    def test_terrain_removal_is_not_optimized_away_at_wall_positions(self):
        order = (ROOT / "ConstructionSupportOrder.java").read_text(encoding="utf-8")
        self.assertIn("!preserveNewTerrainClear(world,b,prepareNewGround)", order)
        self.assertIn("b.special != BuildingBlock.CLEARGROUND", order)
        self.assertIn("prepare(world, operations, false)", order)
        self.assertIn("!world.hasChunkAt(p)", order)
        self.assertIn("world.getBlockEntity(p) != null", order)
        for material in ("Blocks.DIRT", "Blocks.RED_SAND", "Blocks.TERRACOTTA",
                         "Blocks.ORANGE_TERRACOTTA", "Blocks.STONE"):
            self.assertIn(material, order)
        self.assertIn("operations.addAll(end)", order)
        self.assertIn("if(finalFixtures.contains(b))end.add(b)", order)

    def test_footprint_and_foundation_offsets_agree(self):
        plan = (ROOT / "BuildingPlan.java").read_text(encoding="utf-8")
        ground = plan.split("private void getBuildingPoints_prepareGround(", 1)[1].split(
            "private void getBuildingPoints_specialBlocks(", 1)[0]
        self.assertIn("deltaX < this.length && deltaZ < this.width", ground)
        self.assertEqual(ground.count("XZCoords[xzPos + 1] - this.areaToClearWidthBefore"), 2)
        self.assertNotIn("XZCoords[xzPos + 1] - this.areaToClearWidthAfter", ground)


if __name__ == "__main__":
    unittest.main()
