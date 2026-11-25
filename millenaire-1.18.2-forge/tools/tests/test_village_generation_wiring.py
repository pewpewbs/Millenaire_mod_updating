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

    def test_deferred_preparation_is_natural_only_and_persistent(self):
        gen=(ROOT / "world/WorldGenVillage.java").read_text(encoding="utf-8")
        self.assertIn("VillagePreparationQueue.suppressNatural(world,chunkX,chunkZ)",gen)
        self.assertIn("VillagePreparationQueue.offer(server,mw,this",gen)
        self.assertIn("specificVillageType==null && parentVillage==null && !alwaysGenerate",gen)
        self.assertIn("minDistance==Integer.MAX_VALUE",gen)
        q=(ROOT / "world/VillagePreparationQueue.java").read_text(encoding="utf-8")
        self.assertIn("extends SavedData",q)
        self.assertIn('tag.put("pending",list)',q)
        self.assertIn("computeIfAbsent(VillagePreparationQueue::load",q)
        self.assertIn("q.pending.size()>=VillagePreparationPolicy.MAX_PENDING",q)
        self.assertIn("getServer().isSameThread()",q)
        self.assertIn("getAverageTickTime()",q)

    def test_preparation_requests_one_full_chunk_and_releases_tickets(self):
        q=(ROOT / "world/VillagePreparationQueue.java").read_text(encoding="utf-8")
        self.assertIn("if(waiting!=null)",q)
        self.assertIn("getChunkNow(waiting.x,waiting.z)==null)return",q)
        self.assertIn("addRegionTicket(TICKET,pos,0,pos.toLong())",q)
        self.assertIn("removeRegionTicket(TICKET,pos,0,pos.toLong())",q)
        self.assertIn("preparationTypeValid",q)
        self.assertIn("finish(world,\"stale-candidate\")",q)
        self.assertIn("world.players().isEmpty()){release(world);return;}",q)
        self.assertIn("MillCommonUtilities.random=previous",q)
        self.assertIn("VillagePreparationQueue.unload(server)",(ROOT / "forge/MillEventController.java").read_text(encoding="utf-8"))
        self.assertIn("VillagePreparationQueue.tick(server,this,totalStarted)",(ROOT / "world/MillWorldData.java").read_text(encoding="utf-8"))
        self.assertNotIn("world.getChunk(",q)
        self.assertNotIn(".join(",q)

    def test_stages_have_separate_measurements(self):
        code = (ROOT / "world/WorldGenVillage.java").read_text(encoding="utf-8")
        self.assertIn("MILL-SLOW stage=village-area-check",code)
        for stage in ("biome","chunk-preparation","terrain-map","centre-site","start-layout","reserve-sites","build-and-register","hamlets"):
            self.assertIn('reportGenerationStage("'+stage+'"',code)

if __name__ == "__main__":
    unittest.main()
