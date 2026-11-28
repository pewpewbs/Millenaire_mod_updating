"""Static regression guards for DOOR-15 controlled baseline rollback.

Run from millenaire-1.18.2-forge with:
    py -3 tools/tests/test_door_lifecycle_wiring.py

These tests inspect actual source wiring, but do NOT compile Forge and
do NOT prove that a resident can physically pass through a door.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"
VILLAGER = (ROOT / "entity/MillVillager.java").read_text(encoding="utf-8")
DOORS = (ROOT / "pathing/VillageDoorAccess.java").read_text(encoding="utf-8")
NAVIGATOR = (ROOT / "entity/PathNavigateSimple.java").read_text(encoding="utf-8")
WALK = (ROOT / "pathing/VillagerWalkNodeEvaluator.java").read_text(encoding="utf-8")
CLASSIC = (ROOT / "pathing/atomicstryker/AStarWorker.java").read_text(encoding="utf-8")
JPS = (ROOT / "pathing/atomicstryker/AStarWorkerJPS.java").read_text(encoding="utf-8")

DOOR_HANDLER = VILLAGER.split("private void handleDoorsAndFenceGates()", 1)[1].split(
    "private void handleLeaveClearing()", 1)[0]


class DoorLifecycleWiring(unittest.TestCase):
    def test_route_opener_is_back_to_simple_node_lookahead(self):
        self.assertIn("Path route = this.navigation.getPath()", DOOR_HANDLER)
        self.assertIn("route.getNextNodeIndex() + 3", DOOR_HANDLER)
        self.assertIn('this.level, pos, this, "ROUTE"', DOOR_HANDLER)
        self.assertIn("isWoodenDoor(state.getBlock())", DOOR_HANDLER)
        self.assertNotIn("matchedDoorNode && !routeCrossesPanel", DOOR_HANDLER)
        self.assertNotIn("DoorApproachRules.passedPanel(", DOOR_HANDLER)

    def test_stationary_wooden_door_scanner_is_inactive(self):
        self.assertIn("if (BlockItemUtilities.isWoodenDoor(block)) continue;", DOOR_HANDLER)
        self.assertIn("block instanceof net.minecraft.world.level.block.FenceGateBlock", DOOR_HANDLER)
        # All current door scanner behavior remains downstream of the skip,
        # preserving the gate scanner, but not the experimental wooden scan.
        a = DOOR_HANDLER.index("if (BlockItemUtilities.isWoodenDoor(block)) continue;")
        b = DOOR_HANDLER.index("boolean woodenDoor = BlockItemUtilities.isWoodenDoor(doorState.getBlock());")
        self.assertLess(a, b)

    def test_all_pathfinders_use_original_neighbor_expansion(self):
        self.assertNotIn("DoorTransitionRules.allowsEdge(", WALK)
        self.assertNotIn("DoorTransitionRules.allowsEdge(", CLASSIC)
        self.assertNotIn("DoorTransitionRules.allowsEdge(", JPS)
        self.assertIn("super.getBlockPathType(world, x, y, z)", WALK)

    def test_world_lease_retains_player_priority_and_occupancy(self):
        self.assertIn("if (isPlayerControlled(level, pos))", DOORS)
        self.assertIn("getEntitiesOfClass(LivingEntity.class, portal,", DOORS)
        self.assertIn("LivingEntity::isAlive", DOORS)
        self.assertIn('trace(level, pos, "CLOSING state="', DOORS)

    def test_gate_collision_logic_is_unchanged(self):
        self.assertIn("} else {\n                portal = new AABB(pos).inflate(0.12D, 0.75D, 0.12D);", DOORS)
        self.assertIn("FenceGateBlock", DOOR_HANDLER)

    def test_actor_open_and_renew_logs_retained(self):
        self.assertIn('trace(level, pos, "OPENED" + origin)', DOORS)
        self.assertIn('trace(level, pos, "RENEWED" + origin)', DOORS)

    def test_door_waypoint_collision_diagnostics_retained(self):
        self.assertIn('"DOOR-WAYPOINT next=" + rung', NAVIGATOR)
        self.assertIn("this.mob.horizontalCollision", NAVIGATOR)
        self.assertIn("this.mob.getDeltaMovement()", NAVIGATOR)

    def test_stair_diagnostics_are_observational_and_rate_limited(self):
        self.assertIn('this.traceNearbyStair(rung);', NAVIGATOR)
        self.assertIn('"STAIR-WAYPOINT stair=" + stair', NAVIGATOR)
        self.assertIn('this.nextStairTraceTick = now + 60L;', NAVIGATOR)
        self.assertIn('shapeState.getCollisionShape(level, stair)', NAVIGATOR)
        self.assertIn('level.noCollision(this.mob, targetBox)', NAVIGATOR)
        self.assertIn('this.mob.getBbHeight()', NAVIGATOR)
        self.assertIn('now - this.stairStationarySince >= 40L', NAVIGATOR)

    def test_ladder_assist_remains_present(self):
        tick = NAVIGATOR.split("public void tick()", 1)[1].split("private boolean isLadderNode", 1)[0]
        self.assertIn("if (!this.mob.level.hasChunkAt(rung)) return;", tick)
        self.assertIn("if (!state.is(net.minecraft.world.level.block.Blocks.LADDER)) {", tick)
        self.assertLess(tick.index("this.assistLeavingLadder(next);"), tick.index("this.assistLadderRung(next, state);"))
        self.assertIn("this.assistLeavingLadder(next);\n            return;", tick)
        rung = NAVIGATOR.split("private void assistLadderRung(", 1)[1].split("private void assistLeavingLadder(", 1)[0]
        self.assertIn("LadderTransitionRules.shouldClimb(", rung)
        self.assertIn("if (this.canRiseInsideLadder())", rung)
        self.assertIn("this.mob.setDeltaMovement(", rung)
        self.assertIn("LADDER-ASSIST next=", rung)
        exit = NAVIGATOR.split("private void assistLeavingLadder(", 1)[1].split("private void traceNearbyStair(", 1)[0]
        self.assertIn("if (!this.isLadderNode(previous)) return;", exit)
        self.assertIn("LadderTransitionRules.needsTopOut(", exit)
        self.assertIn("if (!this.canRiseInsideLadder()) return;", exit)
        self.assertIn("LADDER-EXIT lastRung=", exit)


if __name__ == "__main__":
    unittest.main(verbosity=2)
