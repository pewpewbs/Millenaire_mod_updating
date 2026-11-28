from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"


class CraftingWorkplaceFacingTests(unittest.TestCase):
    def test_only_active_server_crafting_faces_workbench(self):
        src = (ROOT / "entity/MillVillager.java").read_text(encoding="utf-8")
        facing = src.split("private void setFacingDirection()", 1)[1].split(
            "public void setGoalBuildingDestPoint", 1)[0]
        self.assertLess(facing.index("this.getTarget() != null"),
                        facing.index("this.craftingWorkplaceFacing.face(this)"))
        for guard in ("!this.level.isClientSide", "this.actionStart != 0L",
                      "this.stopMoving", "instanceof org.millenaire.common.goal.generic.GoalGenericCrafting"):
            self.assertIn(guard, facing)
        self.assertIn("if (this.craftingWorkplaceFacing.face(this)) return;", facing)
        self.assertIn("if (goal.lookAtGoal())", facing)

    def test_visual_target_is_local_bounded_cached_and_loaded(self):
        src = (ROOT / "goal/CraftingWorkplaceFacing.java").read_text(encoding="utf-8")
        for required in ("SEARCH_RADIUS = 3", "SEARCH_HEIGHT = 1", "SEARCH_INTERVAL = 40L",
                         "now >= nextSearch", "hasChunkAt(p)", "Blocks.CRAFTING_TABLE",
                         "WorkAccess.canAccessResource", "WorkAccess.loadedBetween",
                         "WorkAccess.visible", "getCraftingPos()", "distance >= bestDistance",
                         "setYBodyRot", "getLookControl().setLookAt", "table != null && !isTable"):
            self.assertIn(required, src)
        self.assertLess(src.index("hasChunkAt(p)"), src.index("getBlockState(p)"))
        for forbidden in ("setGoalInformation(", "setPathDestPoint(", "takeGoods(", "storeGoods(",
                          "getChunk(", "setBlock("):
            self.assertNotIn(forbidden, src)

    def test_no_table_preserves_normal_crafting(self):
        src = (ROOT / "goal/CraftingWorkplaceFacing.java").read_text(encoding="utf-8")
        self.assertIn("if (table == null) return false;", src)
        crafting = (ROOT / "goal/generic/GoalGenericCrafting.java").read_text(encoding="utf-8")
        self.assertNotIn("CraftingWorkplaceFacing", crafting)
        self.assertIn("dest.getResManager().getCraftingPos()", crafting)


if __name__ == "__main__":
    unittest.main()
