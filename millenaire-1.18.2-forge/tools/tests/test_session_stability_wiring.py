"""Static regression guards for the 2026-11-25 live-session findings.
These are source wiring checks, not a Forge or in-game performance test.
"""
from pathlib import Path
import unittest

PROJECT=Path(__file__).resolve().parents[2]
JAVA=PROJECT/"src/main/java/org/millenaire"

def read(path):
    return (JAVA/path).read_text(encoding="utf-8")

class SessionStability(unittest.TestCase):
    def test_sleep_is_not_treated_as_physical_entombment(self):
        code=read("common/pathing/PhysicalTrapRecovery.java")
        self.assertLess(code.index("if (villager.isVillagerSleeping()) return false;"),
                        code.index("if (villager.isInWall()) return true;"))

    def test_repeated_trap_discards_route_and_avoids_destination_temporarily(self):
        code=read("common/entity/MillVillager.java")
        rescue=code.split("private void recoverPhysicalTrap()",1)[1].split("private void recoverStationaryMovement()",1)[0]
        self.assertIn("old.distSqr(lastPhysicalTrap)<=4",rescue)
        self.assertIn("avoidedTrapUntil=now+200L",rescue)
        self.assertIn("this.clearGoal()",rescue)
        self.assertIn("this.setPathDestPoint(null,0)",rescue)
        self.assertIn("retryDestination.distanceTo(avoidedTrapDestination)<1.5D",code)

    def test_chat_rate_is_independent_of_work_target_pursuit(self):
        code=read("common/entity/MillVillager.java")
        compute=code.split("private List<Node> computeNewPath(Point dest)",1)[1].split("public int countInv",1)[0]
        self.assertIn('if("chat".equals(this.goalKey))',compute)
        self.assertIn("nextChatPathAttempt=now+40L",compute)
        self.assertIn("this.nextTrackedEntityPathAttempt = now + 10L",compute)
        self.assertIn("this.pathPlannerJPS.getPath(",compute)

    def test_chunk_ticket_batch_and_activation_gate(self):
        code=read("common/forge/BuildingChunkLoader.java")
        self.assertIn("MAX_NEW_TICKETS_PER_CALL=4",code)
        self.assertIn("if(lastLoadTick==tick)return",code)
        self.assertIn("forcedChunks.containsAll(required)",code)
        self.assertIn("MILL-SLOW stage=village-tickets",code)
        self.assertIn("this.chunkLoader.chunksLoaded",read("common/village/Building.java"))

    def test_progression_decisions_and_retired_config(self):
        planner=read("common/village/VillageProgression.java")
        for reason in ("no-eligible-project","centre-style","construction-supplier","operating-supplier","rank"):
            self.assertIn('"'+reason+'"',planner)
        self.assertIn("now-last<200L",planner)
        self.assertIn("VILLAGE-PROGRESSION picked=",read("common/village/Building.java"))
        conf=read("common/config/MillConfigValues.java")
        self.assertIn("if(isRetiredConfig(key))continue;",conf)
        self.assertIn('toWrite.add("// Legacy (unused in Forge port): "+line)',conf)

    def test_generated_icons_are_derived_from_skins(self):
        gradle=(PROJECT/"build.gradle").read_text(encoding="utf-8")
        self.assertIn("generateMillenaireEntityIcons",gradle)
        self.assertIn("source.getSubimage(8*scale,8*scale",gradle)
        self.assertIn("source.getSubimage(40*scale,8*scale",gradle)
        self.assertIn("new File(millAuthoredIcons, relative).isFile()",gradle)
        self.assertIn("dependsOn 'generateMillenaireEntityIcons'",gradle)

if __name__=="__main__":
    unittest.main()
