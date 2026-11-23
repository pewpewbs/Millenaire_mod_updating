"""Source-level STAIR-01 guards (does not replace compileJava or playtesting).

Run from millenaire-1.18.2-forge:
    python3 tools/tests/test_stair_transition_wiring.py
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
ASTAR = ROOT / "src/main/java/org/millenaire/common/pathing/atomicstryker"
STATIC = (ASTAR / "AStarStatic.java").read_text(encoding="utf-8")
CLASSIC = (ASTAR / "AStarWorker.java").read_text(encoding="utf-8")
JPS = (ASTAR / "AStarWorkerJPS.java").read_text(encoding="utf-8")
GEOMETRY = (ASTAR / "TransitionClearanceRules.java").read_text(encoding="utf-8")


class TransitionWiring(unittest.TestCase):
    def test_classic_checks_edge_before_heap(self):
        candidate_block = CLASSIC.split("public void getNextCandidates(", 1)[1]
        before_queue = candidate_block.split("this.addToBinaryHeap(check);", 1)[0]
        self.assertIn("isViable(this.world, check", before_queue)
        self.assertIn("isTransitionViable(this.world, parent, check", before_queue)

    def test_jps_validates_expanded_route_and_orthogonal_corners(self):
        self.assertIn("return this.validatedBackTrace(start);", JPS)
        validator = JPS.split("private ArrayList<AStarNode> validatedBackTrace(", 1)[1]
        validator = validator.split("private ArrayList<AStarNode> findNeighbours(", 1)[0]
        self.assertIn("isTransitionViable(this.world, from, to", validator)
        self.assertIn("findSafeOrthogonalCorner(", validator)

    def test_collision_uses_voxel_shapes_not_just_block_is_passable(self):
        method = STATIC.split("public static boolean isTransitionViable(", 1)[1]
        method = method.split("public static AS_PathEntity translateAStarPathtoPathEntity(", 1)[0]
        self.assertIn("TransitionClearanceRules.canTraverse", method)
        self.assertIn("getCollisionShape(world, pos)", method)
        self.assertIn("shape.toAabbs()", method)
        self.assertIn("body.intersects", method)

    def test_stair_step_requires_jumping_headroom(self):
        self.assertIn("feetY + STEP_UP", GEOMETRY)
        self.assertIn("feetY + 1.0D", GEOMETRY)
        self.assertIn("SAMPLES = 8", GEOMETRY)

    def test_elbow_is_not_invented_without_clear_edges(self):
        self.assertIn("newNode = findSafeOrthogonalCorner(", STATIC)
        self.assertIn("if (newNode != null) input.add(newNode);", STATIC)


if __name__ == "__main__":
    unittest.main(verbosity=2)
