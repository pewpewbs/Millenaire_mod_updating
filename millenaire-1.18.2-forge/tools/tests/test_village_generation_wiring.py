"""Source guards; not in-game performance tests."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"

class VillageGenerationWiring(unittest.TestCase):
    def test_probe_is_local_and_preserves_candidate_search(self):
        code = (ROOT / "world/WorldGenVillage.java").read_text(encoding="utf-8")
        area = code.split("private Point generateVillageAtPoint_checkForUnloaded(",1)[1].split("private VillageType generateVillageAtPoint_findVillageType",1)[0]
        self.assertIn("new VillageGenerationProbe(", area)
        self.assertEqual(area.count("probe.checkArea("),2)
        self.assertIn("WorldUtilities.isChunkGenerated(world, cx, cz)",area)
        self.assertIn("i = -8; i <= 8",area)
        self.assertIn("j = -8; j <= 8",area)
        self.assertNotIn("world.getChunk(",area)

    def test_generation_logs_distinguish_regular_and_lone_buildings(self):
        code = (ROOT / "world/WorldGenVillage.java").read_text(encoding="utf-8")
        self.assertIn('VILLAGE-GEN-REGISTER kind=',code)
        self.assertIn('villageType.lonebuilding?"lone":"village"',code)
        self.assertIn('VILLAGE-GEN-REJECT reason=area-incomplete',code)
        for reason in ("villages-disabled","distance","no-eligible-type","layout:"):
            self.assertIn('"'+reason+'"',code)

    def test_stages_have_separate_measurements(self):
        code = (ROOT / "world/WorldGenVillage.java").read_text(encoding="utf-8")
        self.assertIn("MILL-SLOW stage=village-area-check",code)
        for stage in ("biome","chunk-preparation","terrain-map","centre-site","start-layout","reserve-sites","build-and-register","hamlets"):
            self.assertIn('reportGenerationStage("'+stage+'"',code)

if __name__ == "__main__":
    unittest.main()
