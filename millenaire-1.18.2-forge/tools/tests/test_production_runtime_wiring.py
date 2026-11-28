"""Source-level regression guards; Forge and live village tests remain separate."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire/common"
DATA = ROOT / "src/main/resources/todeploy/millenaire"

def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

class ProductionRuntimeWiring(unittest.TestCase):
    def test_mining_and_fishing_do_not_generate_on_stuck(self):
        for name in ("goal/GoalMinerMineResource.java", "goal/GoalFish.java",
                     "goal/generic/GoalGenericMining.java"):
            source = read(name)
            with self.subTest(name=name):
                stuck = source.split("public boolean stuckAction(",1)[1].split("}",1)[0]
                self.assertIn("return false;",stuck)
                self.assertNotIn("performAction",stuck)
                self.assertIn("WorkAccess.canAccessResource",source)
                action = source.split("public boolean performAction(",1)[1]
                self.assertIn("isWithinActionRange",action)
                self.assertIn("isClientSide",action)
        access = read("goal/WorkAccess.java").split("public static boolean canAccessResource(",1)[1]
        self.assertIn("hasChunkAt",access)
        self.assertIn("distanceToSqr(center)",access)
        self.assertIn("canAccessResource(villager.level, villager.position()",access)
        self.assertIn("visible(world, from, center, target)",access)

    def test_registered_sources_and_fishing_points(self):
        legacy = read("goal/GoalMinerMineResource.java")
        generic = read("goal/generic/GoalGenericMining.java")
        fish = read("goal/GoalFish.java")
        for source in (legacy,generic):
            self.assertIn("validTarget(",source)
            self.assertIn("sources.get(",source)
            self.assertIn(".contains(point)",source)
            self.assertIn("isStillValidSpecific",source)
        self.assertIn("fishingspots.contains(point)",fish)
        self.assertIn("validFishingPoint",fish)

    def test_crafting_and_cooking_recheck_stock_and_workplace(self):
        goal = read("goal/Goal.java")
        self.assertIn("protected boolean passesStockLimits(",goal)
        self.assertIn("if (!passesStockLimits(villager)) return false;",goal)
        craft = read("goal/generic/GoalGenericCrafting.java").split("public boolean performAction(",1)[1]
        self.assertLess(craft.index("passesStockLimits"),craft.index("beginGoodsTransaction"))
        self.assertIn("getCraftingPos()",craft)
        self.assertIn("isWithinActionRange",craft)
        cook = read("goal/generic/GoalGenericCooking.java").split("public boolean performAction(",1)[1]
        self.assertIn("furnaces.contains(target)",cook)
        self.assertIn("firepits.contains(target)",cook)
        self.assertIn("boolean allowInput = this.passesStockLimits",cook)
        self.assertIn("if (allowInput)",cook)
        self.assertIn("storeCookingOutput",cook)

    def test_gathering_keeps_output_and_limits(self):
        source = read("goal/generic/GoalGenericGatherBlocks.java")
        self.assertIn("!this.validateDest(villager,buildingDest)",source)
        action = source.split("public boolean performAction(",1)[1]
        self.assertIn("passesStockLimits",action)
        self.assertIn("beginGoodsTransaction",action)
        self.assertIn("dest.storeGoods(entry.getKey(),entry.getValue()) != entry.getValue()",action)
        self.assertIn("if (next == null) return true;",action)
        brick = read("goal/GoalIndianGatherBrick.java").split("public boolean performAction(",1)[1]
        self.assertLess(brick.index("if (!villager.setBlockAndMetadata"),brick.index("villager.addToInv"))

    def test_brick_goals_share_construction_demand_and_recheck_before_action(self):
        demand = read("goal/BrickProductionDemand.java")
        policy = read("goal/BrickProductionPolicy.java")
        self.assertIn("getCurrentGoalBuildingPlan()", demand)
        self.assertIn("location.isSameLocation(townHall.buildingGoalLocation)", demand)
        self.assertIn("availableForCurrentProject(raw)", demand)
        self.assertIn("worker.countInv(raw)", demand)
        self.assertIn("building.countGoods(raw)", demand)
        self.assertIn("hasChunkAt(point.getBlockPos())", demand)
        self.assertIn("RESERVE_PER_LEVEL = 128", policy)
        self.assertIn("MAX_RESERVE = 512", policy)
        self.assertIn("BrickProductionPolicy.reserveForCentreLevel(", demand)
        self.assertIn("townHall.location == null ? 0 : townHall.location.level", demand)
        self.assertNotIn("tradeGood.targetQuantity", demand)
        self.assertIn("Math.max(0, reserve)", policy)
        self.assertIn("gathering ? 260 : 220", policy)
        cycle=read("goal/BrickWorkCycle.java")
        self.assertIn("demand.shouldGather()",cycle)
        self.assertNotIn("demand.shouldForm(BrickProductionDemand.inDryingArea(v))",cycle)
        self.assertIn("if(empty!=null)",cycle)
        self.assertIn("demand.shouldGather()",cycle)
        for file, gathering in (("GoalIndianDryBrick.java","false"),("GoalIndianGatherBrick.java","true")):
            code=read("goal/"+file)
            destination=code.split("getDestination(MillVillager",1)[1].split("@Override",1)[0]
            action=code.split("public boolean performAction(",1)[1].split("@Override",1)[0]
            self.assertIn("BrickWorkCycle.plan(",destination)
            self.assertIn("BrickProductionDemand.inspect(villager)",action)
            self.assertIn("canWorkBrickSpot",action)
            self.assertIn("demand.priority("+gathering+")",code)
            self.assertIn("BrickWorkCycle.finished(villager,kiln)",action)
            if gathering=="true":
                self.assertIn("follow.work()==BrickWorkCycle.Work.REFILL",action)
                self.assertIn("!kiln.getPos().equals(follow.kiln().getPos())",action)
            else:
                self.assertIn("next == null || !kiln.getPos().equals(next.getDestBuildingPos())",action)
        self.assertNotIn("takeFromInv",read("goal/GoalIndianDryBrick.java"))

    def test_brick_delivery_and_nearest_workplace(self):
        demand=read("goal/BrickProductionDemand.java")
        gather=read("goal/GoalIndianGatherBrick.java")
        delivery=read("goal/GoalBringBackResourcesHome.java")
        cook=read("goal/generic/GoalGenericCooking.java")
        self.assertIn("state == MillBlocks.BS_MUD_BRICK",demand)
        self.assertIn("DELIVERY_BATCH = 32",read("goal/BrickProductionPolicy.java"))
        self.assertIn("BrickWorkCycle.plan(villager,inspect(villager)).work()==BrickWorkCycle.Work.DELIVER",demand)
        self.assertIn("BrickProductionDemand.waitForRawBatch(villager)",delivery)
        self.assertNotIn("Math.min(8,missing)",demand)
        self.assertNotIn("BrickProductionDemand.deliveryDue(villager)",gather)
        self.assertIn("return 900",delivery)
        self.assertIn("return villager.getTownHall();",delivery)
        self.assertIn("p.horizontalDistanceToSquared(villager)<best.horizontalDistanceToSquared(villager)",cook)
        self.assertIn("constructionMissing(villager,this.itemToCook)",cook)
        planner=read("village/VillageProgression.java")
        self.assertIn("p.level>centre.location.level",planner)
        self.assertIn("ProductionCapability.hasCooking(plan,null)",planner)

    def test_delivery_display_matches_actual_cargo_destination(self):
        delivery=read("goal/GoalBringBackResourcesHome.java")
        scope=delivery.split("private static boolean deliverableAt(",1)[1].split("private static int countDeliverableGoods",1)[0]
        self.assertIn("isDeliverable(villager, key)",scope)
        self.assertIn("dest == villager.getHouse() || BrickProductionDemand.isBrick(key)",scope)
        display=delivery.split("public ItemStack[] getHeldItemsTravelling(",1)[1].split("@Override",1)[0]
        self.assertIn("villager.getGoalBuildingDest()",display)
        self.assertIn("deliverableAt(villager, key, dest)",display)
        self.assertIn("dest.storageRoom(key) > 0",display)
        action=delivery.split("public boolean performAction(",1)[1]
        self.assertIn("deliverableAt(villager, key, dest)",action)
        villager=read("entity/MillVillager.java")
        held=villager.split("private void checkGoalHeldItems(",1)[1].split("public void checkGoals()",1)[0]
        self.assertIn("this.heldItem = ItemStack.EMPTY;",held)
        self.assertIn("this.heldItemOffHand = ItemStack.EMPTY;",held)

    def test_refill_local_kiln_before_raw_delivery(self):
        cycle=read("goal/BrickWorkCycle.java")
        dry=read("goal/GoalIndianDryBrick.java")
        gather=read("goal/GoalIndianGatherBrick.java")
        delivery=read("goal/GoalBringBackResourcesHome.java")
        self.assertIn("visit.refilling?null:nearestSpot(v,kiln,true)",cycle)
        self.assertLess(cycle.index("if(stockAllowed && ready!=null)"),cycle.index("mayStartAnotherKiln"))
        self.assertIn("visit.visited.contains(kiln.getPos())",cycle)
        self.assertIn("BrickProductionPolicy.shouldDeliverRaw(cargo,false)?DELIVER:NONE",cycle)
        self.assertIn("visit.lastWorked=v.level.getGameTime()",cycle)
        self.assertIn("blocked.put(kiln.getPos(),now+200L)",cycle)
        self.assertIn("BrickWorkCycle.gathered(villager,kiln,newCargo)",gather)
        self.assertIn("mayStartAnotherKiln(cargo,demand.shouldGather(),completedKilns)",cycle)
        self.assertIn("kiln.location.level>=0",cycle)
        self.assertIn(".map(Building::getPos).distinct().count()",cycle)
        self.assertIn('th.getBuildingsWithTag("brickkiln")',cycle)
        self.assertIn("rawDeliveryTarget(completedKilns)",read("goal/BrickProductionPolicy.java"))
        self.assertNotIn("visitDeliveryTarget",cycle)
        self.assertIn("if(raw(v)==0)VISITS.remove(v)",cycle)
        self.assertIn("BrickWorkCycle.beginRefill(villager,kiln)",dry)
        self.assertIn("BrickWorkCycle.failed(villager,villager.getGoalBuildingDest())",dry)
        self.assertIn("BrickWorkCycle.failed(villager,villager.getGoalBuildingDest())",gather)
        self.assertIn("BrickWorkCycle.delivered(villager)",delivery)
        self.assertIn("Math.min(villager.countInv(key),dest.storageRoom(key))",delivery)
        self.assertIn("hasChunkAt(p.getBlockPos())",cycle)

    def test_brick_firing_uses_physical_reserved_stock_supply(self):
        cook=read("goal/generic/GoalGenericCooking.java")
        self.assertIn('"cookindianbrick".equals(this.key)',cook)
        self.assertIn("th.nbGoodAvailable(this.itemToCook,false,false,false)",cook)
        self.assertIn("countGoods+(long)supply>=required",cook)
        self.assertIn("WorkAccess.canAccessStock(villager,target,2.0)",cook)
        action=cook.split("public boolean performAction(",1)[1]
        self.assertLess(action.index("isWithinActionRange"),action.index("villager.takeFromBuilding"))
        self.assertIn("villager.setGoalInformation(next)",action)

    def test_brick_firing_can_use_carried_batch_when_raw_delivery_not_due(self):
        cook=read("goal/generic/GoalGenericCooking.java")
        self.assertIn("constructionMissing(villager,this.itemToCook)==0",cook)
        self.assertIn("this.getDestination(villager, false, true)!=null) return 925",cook)
        self.assertIn("REFILL_BEFORE_DELIVERY_PRIORITY = 1700",read("goal/BrickProductionPolicy.java"))
        self.assertIn("RAW_BATCH_DELIVERY_PRIORITY = 1600",read("goal/BrickProductionPolicy.java"))
        self.assertIn("return 900",read("goal/GoalBringBackResourcesHome.java"))
        self.assertIn("if ((brick || inputOnly) && bestInput != null)",cook)
        self.assertIn("if (inputOnly && !allowSupply) return null",cook)
        self.assertIn("next=this.getDestination(villager, false, true)",cook)
        fill=cook.split("private ItemStack fillCookingInput(",1)[1]
        branch=fill.split("if (brickKilnSupply(villager))",1)[1].split("} else {",1)[0]
        self.assertLess(branch.index("villager.takeFromInv"),branch.index("dest.takeGoods"))

    def test_brick_supply_activity_is_synced_to_client(self):
        cook=read("goal/generic/GoalGenericCooking.java")
        self.assertIn("villager.townHallPoint.equals(villager.getGoalBuildingDestPoint())",cook)
        self.assertEqual(cook.count('atBrickSupply(villager) ? "getbrickforfiring"'),2)
        villager=read("entity/MillVillager.java")
        self.assertIn("this.setGoalBuildingDestPoint(StreamReadWrite.readNullablePoint(data));",villager)
        self.assertIn("StreamReadWrite.writeNullablePoint(this.getGoalBuildingDestPoint(), data);",villager)

    def test_source_migration_is_scoped_and_persisted(self):
        source = read("buildingplan/ProductionSourceRepair.java")
        for key in ("indian/quarry/0","indian/quarry/1","mayan/mayanmining/0",
                    "japanese/japanesequarry/0","japanese/lonemine/0"):
            self.assertIn(key,source)
        for marker in ("isCustomBuilding","getPreviousBuildingPlan()","isType(marker.type())",
                       "adjustForOrientation", "hasChunkAt", "isFaceSturdy", "isAir()",
                       "ProductionSourceMigrationPolicy.Decision.REPLACE", "requestSave",
                       "MILL-PRODUCTION-SOURCE-REVIEW"):
            self.assertIn(marker,source)
        building = read("village/Building.java").split("public void updateBuildingServer()",1)[1]
        self.assertIn("!this.productionSourcesChecked",building)
        self.assertIn("ProductionSourceRepair.reconcile(this)",building)

    def test_indian_quarry_furnace_progression(self):
        try:
            from PIL import Image
        except ImportError:
            self.skipTest("Pillow not installed")
        white = (255, 255, 255)
        mud = (166, 156, 136)
        for variant, row, front, facing in (
            ("A", 12, 11, (25, 25, 26)),
            ("B", 1, 2, (25, 25, 27)),
        ):
            with self.subTest(variant=variant):
                levels = []
                for level in range(4):
                    with Image.open(DATA / f"cultures/indian/buildings/basic/quarry_{variant}{level}.png") as raw:
                        levels.append(raw.convert("RGB"))
                counts = [sum(1 for c in im.getdata()
                              if c[:2] == (25, 25) and 25 <= c[2] <= 29) for im in levels]
                self.assertEqual(counts, [0, 1, 1, 0])
                def pixel(level, floor, k, j):
                    return levels[level].getpixel((floor * 15 + k, j))
                self.assertEqual(pixel(1, 6, 11, row), facing)
                self.assertEqual(pixel(2, 6, 11, row), white)
                self.assertEqual(pixel(2, 6, 12, row), facing)
                for j in (row, front):
                    self.assertEqual(pixel(1, 5, 11, j), mud)
                    self.assertEqual(pixel(2, 5, 11, j), white)
                for level in (0, 1, 2, 3):
                    for k in (11, 12):
                        for floor in (6, 7):
                            self.assertEqual(pixel(level, floor, k, front), white)

    def test_migrated_positions_match_bundled_markers(self):
        try:
            from PIL import Image
        except ImportError:
            self.skipTest("Pillow not installed")
        for path, stride, points in (
            ("indian/buildings/basic/quarry_A0.png",15,[(1,2,5,200),(1,6,6,200)]),
            ("indian/buildings/basic/quarry_B0.png",15,[(2,3,3,200),(3,9,8,200)]),
            ("mayan/buildings/core/mayanmining_A0.png",13,[(2,4,5,200),(2,3,9,200)]),
            ("japanese/buildings/core/japanesequarry_A0.png",16,[(1,9,11,200),(1,11,16,200)]),
            ("japanese/buildings/lone/lonemine_A0.png",21,[(7,4,2,60),(7,2,4,60),(7,4,4,200)]),
        ):
            with Image.open(DATA / "cultures" / path) as raw:
                image = raw.convert("RGB")
                for floor,k,j,kind in points:
                    expected = (200,20,20) if kind == 200 else (60,60,60)
                    self.assertEqual(image.getpixel((floor*stride+k,j)),expected,path)

if __name__ == "__main__":
    unittest.main(verbosity=2)
