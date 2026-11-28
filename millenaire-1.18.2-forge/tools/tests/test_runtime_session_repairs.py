"""Regression guards for moving goals, chest lifecycle and world stalls."""
from pathlib import Path
import json
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire/common"

def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

class RuntimeSessionRepairs(unittest.TestCase):
    def test_moving_land_target_is_not_a_wide_static_goal(self):
        villager = read("entity/MillVillager.java")
        config = villager.split("private AStarConfig getPathingConfig()",1)[1].split("public Node getPathPointPos()",1)[0]
        self.assertIn("this.tracksLandEntity() && config.tolerance",config)
        self.assertIn("new AStarConfig(config.canUseDoors, config.canTakeDiagonals,",config)
        self.assertIn("config.allowDropping, config.canSwim, config.canClearLeaves);",config)
        self.assertIn("!target.isInWater() && !this.isInWater()",villager)
        self.assertIn("entityGoal ? 0 : Math.min(2, range - 1)",villager)

    def test_moving_target_anchor_and_bounded_retry(self):
        villager = read("entity/MillVillager.java")
        refresh = villager.split("private void refreshTrackedEntityDestination()",1)[1].split("private List<Node> computeNewPath",1)[0]
        self.assertIn("previous.sameBlock(live)",refresh)
        self.assertIn("!this.pathEntity.isDone()",refresh)
        self.assertIn("previous.distanceTo(live) < 1.5D",refresh)
        self.assertIn("new Point(live.getiX(), live.getiY(), live.getiZ()), 0",refresh)
        self.assertIn("this.nextTrackedEntityPathAttempt = now + 10L;",villager)
        self.assertIn("path.getNodeCount() <= 1 && active != null",villager)
        self.assertIn("!active.isWithinActionRange(this, this.getPathDestPoint())",villager)

    def test_real_entity_range_and_target_lifecycle(self):
        goal = read("goal/Goal.java")
        self.assertIn("villager.distanceToSqr(target) < reach * reach",goal)
        sheep = read("goal/GoalShearSheep.java")
        self.assertIn("target instanceof Sheep sheep && sheep.isAlive() && !sheep.isBaby()",sheep)
        self.assertIn("!sheep.isSheared()",sheep)
        self.assertIn("WorkAccess.visible(villager, target.getBoundingBox().getCenter(), null)",sheep)
        hunt = read("goal/GoalHuntMonster.java")
        self.assertIn("target instanceof Monster && !(target instanceof Creeper) && target.isAlive()",hunt)

    def test_chest_unlock_requires_known_defeat(self):
        code = read("village/Building.java").split("public void checkBattleStatus()",1)[1]
        self.assertIn("knownResidents > 0 && !unknownResidentType",code)
        self.assertIn("survivingDefenders == 0 : livingResidents == 0",code)
        self.assertIn("survivingDefenders > 0 : livingResidents > 0",code)
        self.assertIn("if (this.chestLocked && defeated)",code)
        self.assertNotIn("if (this.chestLocked && nbLiveDefenders == 0)",code)
        self.assertIn("if (!vr.awayraiding && !vr.awayhired) nbLiveDefenders++",code)

    def test_partial_region_header_does_not_discard_other_regions(self):
        code = read("utilities/RegionChunkIndex.java")
        self.assertIn("if (channel.size() < 8192L) continue;",code)
        self.assertIn("if (header.hasRemaining()) continue;",code)
        self.assertIn("StandardOpenOption.READ",code)
        self.assertNotIn("Truncated region header",code)

    def test_bounded_preload_and_slow_stage_diagnostics(self):
        world = read("world/MillWorldData.java")
        preload = world.split("public void forcePreload()",1)[1].split("public Collection<MillVillager>",1)[0]
        self.assertIn("if (nbGenerated >= 1) break preload;",preload)
        self.assertNotIn("getChunkSource().save(false)",preload)
        for stage in ("villager-index","building-loop","profiles-and-quests","force-preload","world-total"):
            self.assertIn('reportSlowStage("'+stage+'"',world)
        self.assertIn("MILL-SLOW stage=village-generation",read("world/WorldGenVillage.java"))

    def test_inuit_carving_all_faces_have_existing_texture(self):
        path = ROOT / "src/main/resources/assets/millenaire/models/block/inuitcarving.json"
        model = json.loads(path.read_text(encoding="utf-8"))
        for element in model["elements"]:
            for face in element["faces"].values():
                texture = face["texture"]
                self.assertTrue(texture.startswith("#"))
                self.assertIn(texture[1:],model["textures"])

if __name__ == "__main__":
    unittest.main(verbosity=2)
