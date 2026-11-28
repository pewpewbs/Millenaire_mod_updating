from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]/"src/main/java/org/millenaire/common/goal"

class BrickCycleConstruction(unittest.TestCase):
    def read(self,name):
        return (ROOT/name).read_text(encoding="utf-8")
    def test_refill_not_cancelled_when_harvest_covers_demand(self):
        cycle=self.read("BrickWorkCycle.java")
        active=cycle.split("if(visit!=null && visit.current!=null)",1)[1].split("int completedKilns=",1)[0]
        self.assertIn("if(empty!=null)",active)
        self.assertNotIn("shouldForm",active)
        self.assertNotIn("shouldGather",active)
        dry=self.read("GoalIndianDryBrick.java")
        action=dry.split("public boolean performAction(",1)[1].split("public int priority(",1)[0]
        self.assertIn("} else if(!demand.shouldForm",action)
        self.assertLess(action.index("BrickWorkCycle.beginRefill"),action.index("demand.shouldForm"))
        priority=dry.split("public int priority(",1)[1]
        self.assertLess(priority.index("Work.REFILL"),priority.index("demand.shouldForm"))
    def test_building_cannot_take_worker_during_available_continuation(self):
        for name in ("GoalGetResourcesForBuild.java","GoalConstructionStepByStep.java","GoalBuildPath.java"):
            s=self.read(name)
            possible=s.split("public boolean isPossibleSpecific(",1)[1].split("@Override",1)[0]
            self.assertIn("hasAvailableBrickContinuation(villager)",possible)
            self.assertIn("BRICK_BATCH_IN_PROGRESS",possible)
        helper=self.read("BrickProductionDemand.java").split("static boolean hasAvailableBrickContinuation(",1)[1].split("static long inDryingArea",1)[0]
        self.assertIn("goal.isPossible(villager)",helper)
        self.assertIn("BrickWorkCycle.raw(villager)<=0",helper)
        self.assertIn("Work.REFILL",helper)
        self.assertIn("Work.GATHER",helper)
        self.assertIn("Work.DELIVER",helper)
        self.assertIn("deliverRaw && goal instanceof GoalBringBackResourcesHome",helper)
        self.assertIn("storageRoom",helper)
    def test_raw_delivery_precedes_construction(self):
        policy=self.read("BrickProductionPolicy.java")
        self.assertIn("RAW_BATCH_DELIVERY_PRIORITY = 1600",policy)
        delivery=self.read("GoalBringBackResourcesHome.java")
        self.assertIn("return BrickProductionPolicy.RAW_BATCH_DELIVERY_PRIORITY",delivery)
        self.assertIn("BrickProductionDemand.waitForRawBatch(villager)",delivery)
if __name__=="__main__":
    unittest.main()
