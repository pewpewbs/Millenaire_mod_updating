"""Source-level regression guards for two separate wooden-door mechanisms.

Run from millenaire-1.18.2-forge with:
    py -3 tools/tests/test_door_lifecycle_wiring.py

These checks cannot compile Minecraft/Forge or simulate actual NPC movement.
In-game DOOR-BLOCKED and DOOR-LEASE logs are required to confirm success.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"
VILLAGER = (ROOT / "entity/MillVillager.java").read_text(encoding="utf-8")
DOORS = (ROOT / "pathing/VillageDoorAccess.java").read_text(encoding="utf-8")
NAVIGATOR = (ROOT / "entity/PathNavigateSimple.java").read_text(encoding="utf-8")
DOOR_EDGES = (ROOT / "pathing/DoorTransitionRules.java").read_text(encoding="utf-8")
WALK_EVALUATOR = (ROOT / "pathing/VillagerWalkNodeEvaluator.java").read_text(encoding="utf-8")
CLASSIC_ASTAR = (ROOT / "pathing/atomicstryker/AStarWorker.java").read_text(encoding="utf-8")
JPS = (ROOT / "pathing/atomicstryker/AStarWorkerJPS.java").read_text(encoding="utf-8")


class DoorLifecycleWiring(unittest.TestCase):
    def test_door_occupancy_uses_closed_physical_panel(self):
        self.assertIn("BlockState closed = state.setValue(DoorBlock.OPEN, false)", DOORS)
        self.assertIn("closed.getCollisionShape(level, pos)", DOORS)
        self.assertIn(".move(pos.getX(), pos.getY(), pos.getZ())", DOORS)
        self.assertIn(".inflate(0.10D, 0.85D, 0.10D)", DOORS)

    def test_fence_gate_geometry_remains_unchanged(self):
        self.assertIn("} else {\n                portal = new AABB(pos).inflate(0.12D, 0.75D, 0.12D);", DOORS)

    def test_closer_still_checks_living_occupants(self):
        self.assertIn("getEntitiesOfClass(LivingEntity.class, portal,", DOORS)
        self.assertIn("LivingEntity::isAlive", DOORS)
        self.assertIn('trace(level, pos, "CLOSING state="', DOORS)

    def test_opening_uses_real_closed_panel_not_block_center(self):
        self.assertIn("doorState.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, false)", VILLAGER)
        self.assertIn("getCollisionShape(this.level, door).bounds()", VILLAGER)
        self.assertIn("DoorApproachRules.closestPanelCoordinate", VILLAGER)

    def test_opening_has_second_real_panel_sightline(self):
        self.assertIn("if (!woodenDoor) continue;", VILLAGER)
        self.assertIn("Vec3 panelMiddle = new Vec3(panelMidX", VILLAGER)
        self.assertIn("centerHit = this.level.clip(", VILLAGER)

    def test_rejection_has_bounded_debug_diagnostics(self):
        self.assertIn('DOOR-BLOCKED reason=', VILLAGER)
        self.assertIn('this.level.getGameTime() + this.getVillagerId()) % 100L', VILLAGER)
        self.assertIn('NO_VISIBLE_DOOR_CANDIDATE', VILLAGER)


    def test_opener_traces_actor_and_reason_on_actual_transition(self):
        self.assertIn("LivingEntity actor, String source", DOORS)
        self.assertIn('"OPENED" + origin', DOORS)
        self.assertIn('" actorId=" + actor.getId()', DOORS)
        self.assertIn('" actorFeet=" + actor.blockPosition()', DOORS)
        self.assertIn("remember(level, pos, actor, source)", DOORS)

    def test_route_and_scanner_sources_are_distinct(self):
        self.assertIn('this.level, pos, this, "ROUTE"', VILLAGER)
        self.assertIn('this.level, door, this, "SCANNER-RENEW"', VILLAGER)
        self.assertIn('this.level, best, this, "SCANNER-OPEN"', VILLAGER)



    def test_stuck_wooden_door_scanner_has_bounded_retry(self):
        self.assertIn("DoorApproachRules.suppressStationaryScannerReopen(", VILLAGER)
        self.assertIn("boolean woodenCandidate =", VILLAGER)
        self.assertIn("boolean repeatedWithoutProgress = woodenCandidate", VILLAGER)
        self.assertIn('DOOR-SCANNER-SUPPRESSED door=', VILLAGER)

    def test_stall_diagnostics_show_route_nodes(self):
        self.assertIn("private String doorRoutePreview(Path route)", VILLAGER)
        self.assertIn('" next=" + this.doorRoutePreview(route)', VILLAGER)



    def test_renewal_diagnostics_preserve_actor_source(self):
        self.assertIn('trace(level, pos, "RENEWED" + origin)', DOORS)
        self.assertIn('remember(level, pos, actor, source)', DOORS)
        # Applies to both the wooden-door branch and the gate branch.
        self.assertEqual(DOORS.count('remember(level, pos, actor, source);'), 4)

    def test_navigator_traces_door_waypoint_without_changing_movement(self):
        self.assertIn('"DOOR-WAYPOINT next=" + rung', NAVIGATOR)
        self.assertIn('" collision=" + this.mob.horizontalCollision', NAVIGATOR)
        self.assertIn('this.mob.getDeltaMovement()', NAVIGATOR)
        self.assertIn('now + 40L', NAVIGATOR)
        self.assertIn('if (!state.is(net.minecraft.world.level.block.Blocks.LADDER)) return;', NAVIGATOR)



    def test_door_transitions_check_both_ends_of_edge(self):
        self.assertIn("private static boolean allows(BlockState state, int dx, int dz)", DOOR_EDGES)
        self.assertIn("if (!allows(from, dx, dz)) return false;", DOOR_EDGES)
        self.assertIn("return allows(to, dx, dz);", DOOR_EDGES)
        self.assertIn("DoorApproachRules.followsDoorNormal(dx, dz, eastWestFacing)", DOOR_EDGES)

    def test_vanilla_pathfinder_respects_door_transition(self):
        self.assertIn("public int getNeighbors(", WALK_EVALUATOR)
        self.assertIn("DoorTransitionRules.allowsEdge(this.level,", WALK_EVALUATOR)

    def test_both_astar_workers_respect_door_transition(self):
        self.assertIn("DoorTransitionRules.allowsEdge(", CLASSIC_ASTAR)
        self.assertIn("DoorTransitionRules.allowsEdge(", JPS)
        self.assertIn("this.world, px, py, pz, nextX, nextY, nextZ", JPS)



    def test_closed_sideways_doors_are_not_forced_into_detours(self):
        self.assertIn("DoorApproachRules.canTraverseDoorEdge(dx, dz, eastWestFacing,", DOOR_EDGES)
        self.assertIn("state.getValue(DoorBlock.OPEN)", DOOR_EDGES)
        self.assertIn("DoorTransitionRules.needsOpeningForEdge(", VILLAGER)
        self.assertIn("matchedDoorNode && !routeCrossesPanel", VILLAGER)

    def test_scanner_avoids_opening_sideways_passage(self):
        self.assertIn("DoorApproachRules.facingDoorPanel(", VILLAGER)
        self.assertIn("if (woodenDoor)", VILLAGER)
        self.assertIn("door.getX() + 0.5D, door.getZ() + 0.5D", VILLAGER)



if __name__ == "__main__":
    unittest.main(verbosity=2)
