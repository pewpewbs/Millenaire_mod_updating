from pathlib import Path
import unittest

JAVA=Path(__file__).resolve().parents[2]/"src/main/java/org/millenaire/common/buildingplan"

class ConstructionSupportWiring(unittest.TestCase):
    def test_temporary_clears_are_removed_before_plan_pruning(self):
        plan=(JAVA/"BuildingPlan.java").read_text(encoding="utf-8")
        order=(JAVA/"ConstructionSupportOrder.java").read_text(encoding="utf-8")
        self.assertIn("ConstructionSupportOrder.prepare(world,bblocksCombined)",plan)
        self.assertLess(plan.index("ConstructionSupportOrder.prepare("),plan.index("boolean[] toDelete"))
        self.assertIn("laterReplacement.contains(p)",order)
        self.assertIn("operations.removeIf(redundant::contains)",order)
        self.assertIn("else laterReplacement.remove(p)",order)
        self.assertIn("if(other!=null)laterReplacement.add(other)",order)
        self.assertIn("!villageGeneration && !includeSpecialPoints",plan)

    def test_planned_fixtures_follow_supports_and_are_rechecked(self):
        order=(JAVA/"ConstructionSupportOrder.java").read_text(encoding="utf-8")
        plan=(JAVA/"BuildingPlan.java").read_text(encoding="utf-8")
        self.assertLess(order.index("operations.addAll(early)"),order.index("operations.addAll(main)"))
        self.assertLess(order.index("operations.addAll(main)"),order.index("operations.addAll(end)"))
        self.assertIn("ConstructionSupportOrder.retainFixtureChecks(bblocksCombined,toDelete)",plan)
        self.assertIn("near(touched,p)",order)
        self.assertIn("ConstructionPairPlacement.matches(world,bb.p.getBlockPos(),bb.getBlockstate())",plan)
        self.assertIn("other==null || !seen.contains(other)",order)
        for block in ("DoorBlock","AbstractBannerBlock","TorchBlock","LadderBlock","BedBlock","SignBlock","BaseRailBlock","FaceAttachedHorizontalDirectionalBlock"):
            self.assertIn("instanceof "+block,order)

    def test_pair_placement_preserves_native_checks(self):
        pair=(JAVA/"ConstructionPairPlacement.java").read_text(encoding="utf-8")
        block=(JAVA/"BuildingBlock.java").read_text(encoding="utf-8")
        door=(JAVA/"DoorPlacement.java").read_text(encoding="utf-8")
        self.assertIn("ConstructionPairPlacement.matches",block)
        self.assertIn("ConstructionPairPlacement.place",block)
        self.assertIn("ConstructionPairPlacement.removeIfReplaced",block)
        self.assertIn("this.blockState.canSurvive(world,this.p.getBlockPos())",block)
        self.assertIn("lower.canSurvive(world,bottom)",door)
        self.assertIn("lower.canSurvive(world,bottom)",pair)
        self.assertLess(pair.index("world.setBlock(q,other,"),pair.index("finish(world,p);"))
        self.assertIn("current.updateNeighbourShapes",pair)
        self.assertIn("!p.equals(partner(q,other))",pair)

if __name__=="__main__":
    unittest.main()
