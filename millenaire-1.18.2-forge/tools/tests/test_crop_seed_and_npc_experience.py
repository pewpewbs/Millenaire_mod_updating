from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire/common"
GOALS = ROOT / "src/main/resources/todeploy/millenaire/goals/genericharvesting"

class CropSeedAndExperienceWiring(unittest.TestCase):
    def test_only_wheat_secondary_seed_configured(self):
        configured = {p.stem for p in GOALS.rglob("*.txt")
                      if "surplusseed=seeds" in p.read_text(encoding="utf-8-sig")}
        self.assertEqual(configured, {"harvestwheat", "harvestwheathome", "harvestwheatpaddy"})
        for name in configured:
            self.assertIn("harvestitem=wheat,100", (GOALS / (name + ".txt")).read_text(encoding="utf-8-sig"))

    def test_primary_crop_protected(self):
        code = (JAVA / "goal/generic/GoalGenericHarvestCrop.java").read_text(encoding="utf-8")
        self.assertIn("!this.surplusSeed.matches(this.harvestItem.get(0).item)", code)
        self.assertIn("!this.surplusSeed.matches(this.irrigationBonusCrop)", code)
        self.assertIn("SeedStockBudget.allowed", code)

    def test_no_forced_chunk_access_in_budget(self):
        code = (JAVA / "goal/generic/SeedStockBudget.java").read_text(encoding="utf-8")
        self.assertIn("hasChunkAt", code)
        self.assertNotIn("getChunk(", code)
        self.assertIn("worker.countInv(seed)", code)

    def test_experience_only_for_npc_animal_fatal_hit(self):
        code = (JAVA / "forge/MillEventController.java").read_text(encoding="utf-8")
        self.assertIn("animal && event.getSource().getEntity() instanceof MillVillager", code)
        self.assertIn("event.setDroppedExperience(0)", code)
        self.assertIn("public void addInuitDrops(LivingDropsEvent event)", code)
