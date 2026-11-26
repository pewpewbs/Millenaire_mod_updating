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
        self.assertIn("MAX_READY_PER_TICK=4",code)
        self.assertIn("getChunkNow(chunk.x,chunk.z)==null",code)
        self.assertEqual(code.count("claim(world,cold,tick)"),1)
        self.assertNotIn("getAverageTickTime()",code)
        self.assertNotIn("addRegionTicket",code)
        self.assertIn("if(lastLoadTick==tick)return",code)
        self.assertIn("forcedChunks.containsAll(required)",code)
        self.assertIn("MILL-SLOW stage=village-tickets",code)

    def test_loaded_area_gate_and_slow_building_stages(self):
        code=read("common/village/Building.java")
        gate=code.split("boolean isVillageChunksLoaded(",1)[1].split("private void killMobs()",1)[0]
        self.assertIn("chunkProvider.getChunkNow(x,z)==null",gate)
        self.assertNotIn("chunkProvider.hasChunk(",gate)
        self.assertIn("bounds.startX()+bounds.length()/16",gate)
        self.assertIn("bounds.startZ()+bounds.width()/16",gate)
        self.assertIn("getChunkNow(p.getChunkX(),p.getChunkZ())==null)continue;",code)
        for stage in ("activation","grove","pens","signs","initial-packets","refresh-goods"):
            self.assertIn('reportSlowBuildingPart("'+stage+'"',code)

    def test_cold_saved_village_bootstraps_before_active_gate(self):
        code=read("common/village/Building.java")
        bounds=code.split("ActivationBounds getActivationBounds()",1)[1].split("private boolean isVillageChunksLoaded(",1)[0]
        self.assertIn("return VillageMapInfo.activationBounds(this.activationLocations()",bounds)
        update=code.split("public void updateBuildingServer()",1)[1].split("private boolean updateConstructionQueue",1)[0]
        self.assertLess(update.index("this.loadChunks(bounds)"),update.index("this.isVillageChunksLoaded(bounds)"))
        self.assertLess(update.index("this.updateWorldInfo()"),update.index("this.isActive = true"))
        self.assertLess(update.index("this.isActive = true"),update.index("if (!this.isActive)"))
        self.assertNotIn("this.chunkLoader.chunksLoaded",update)
        self.assertIn("MILL-ACTIVATION waiting",update)
        self.assertIn("MILL-ACTIVATION active",update)
        terrain=read("common/village/VillageMapInfo.java")
        geometry=terrain.split("static ActivationBounds activationBounds(",1)[1].split("public boolean update(",1)[0]
        self.assertNotIn("getChunk",geometry)
        self.assertIn("location.length/2",geometry)
        self.assertIn("location.width/2",geometry)
        self.assertIn("ActivationBounds bounds=activationBounds(locations,centre,radius)",terrain)

    def test_compact_acacia_is_scoped_and_respects_growth_veto(self):
        building=read("common/village/Building.java")
        self.assertIn('family==WoodVariant.ACACIA && this.containsTags("grove")',building)
        self.assertIn('"acaciaspawn".equals(this.resManager.woodspawnTypes.get',building)
        code=read("common/village/GroveAcaciaGrowth.java")
        self.assertIn("getChunkNow(x,z)==null",code)
        self.assertNotIn("getChunk(",code)
        self.assertIn("!world.getBlockState(p).isAir()",code)
        self.assertIn("sapling.canSurvive(world,pos)",code)
        self.assertIn("ForgeEventFactory.blockGrowFeature",code)
        self.assertIn("event.getResult()==Event.Result.DENY",code)
        self.assertIn("if(!grown&&world.getBlockState(pos).isAir())world.setBlock(pos,sapling,4)",code)
        self.assertIn("new StraightTrunkPlacer(4,0,0)",code)

    def test_acacia_planting_uses_same_clearance_as_compact_growth(self):
        # Follow the actual declared return type of Building.getResManager.
        # The old BuildingResManager class is not the live implementation.
        building=read("common/village/Building.java")
        self.assertIn("import org.millenaire.common.village.buildingmanagers.ResManager;",building)
        self.assertIn("public ResManager getResManager()",building)
        manager=read("common/village/buildingmanagers/ResManager.java")
        check=manager.split("public boolean canPlantAcaciaAt(",1)[1].split("public Point getPlantingLocation()",1)[0]
        self.assertIn("GroveAcaciaGrowth.hasCompactRoom",check)
        self.assertIn('this.building.containsTags("grove")',check)
        self.assertIn("this.woodspawn.contains(p)",check)
        self.assertIn("getChunkNow(p.getChunkX(),p.getChunkZ())==null",check)
        selection=manager.split("public Point getPlantingLocation()",1)[1].split("public String getPlantingLocationType(",1)[0]
        self.assertIn("!canPlantAcaciaAt(p)",selection)
        growth=read("common/village/GroveAcaciaGrowth.java")
        self.assertIn("public final class GroveAcaciaGrowth",growth)
        self.assertIn("public static boolean hasCompactRoom(",growth)
        self.assertIn("if(!hasCompactRoom(world,pos,sapling))return false;",growth)
        self.assertIn("if(!hasLoadedArea(world,pos))return false;",growth)
        goal=read("common/goal/generic/GoalGenericPlantSapling.java")
        self.assertIn("canPlantAcaciaAt(target)",goal)
        self.assertLess(goal.index("if(!villager.setBlockstate(target,saplingBS))return true;"),goal.index("villager.takeFromInv(saplingBS, 1)"))

    def test_scan_rule_cache_is_local_and_state_tags_are_preserved(self):
        code=read("common/village/VillageMapInfo.java")
        self.assertIn("ScanRules rules=new ScanRules();",code)
        self.assertIn("IdentityHashMap<Block,Integer> flags",code)
        self.assertIn("return TERRAIN_RULES.tree(state)",code)
        self.assertIn("yBaseline, minY, maxY, rules",code)
        self.assertNotIn("static final ScanRules",code)

    def test_startup_and_generation_substage_timings(self):
        startup=read("common/forge/Mill.java")
        for stage in ("block-states","items","building-points","villager-configs","goals","cultures","quests","languages"):
            self.assertIn('startupStage("'+stage+'"',startup)
        generation=read("common/world/WorldGenVillage.java")
        for stage in ("build-centre","build-start-buildings","initialise-village","rush-construction"):
            self.assertIn('reportGenerationStage("'+stage+'"',generation)

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
