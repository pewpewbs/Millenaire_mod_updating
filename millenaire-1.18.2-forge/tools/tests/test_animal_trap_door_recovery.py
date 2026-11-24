"""Source wiring regression guards. Not a substitute for Forge or gameplay."""
from pathlib import Path
import unittest

JAVA = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"

def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

class AnimalTrapDoorRecovery(unittest.TestCase):
    def test_work_always_precedes_leisure(self):
        code = read("entity/MillVillager.java").split("public void setNextGoal()", 1)[1]
        self.assertIn("nextGoal.leasure && !goal.leasure", code)
        self.assertIn("if (!nextGoal.leasure && goal.leasure) continue;", code)

    def test_animal_approach_uses_physical_progress_and_cooldown(self):
        code = read("entity/MillVillager.java")
        self.assertIn("this.trackAnimalWorkProgress(goal)", code)
        self.assertIn("approachAnimalProgressPosition.distanceToSqr(position)", code)
        self.assertIn("now - this.approachAnimalProgressTick < 160L", code)
        self.assertIn("this.unavailableAnimalTargets.put(animal.getId(), now + 200L)", code)
        for goal in ("goal/GoalShearSheep.java", "goal/GoalBreedAnimals.java",
                     "goal/generic/GoalGenericSlaughterAnimal.java"):
            self.assertIn("isAnimalWorkTargetCoolingDown(ent)", read(goal))

    def test_animal_route_checks_actual_action_position(self):
        code = read("entity/MillVillager.java")
        self.assertIn("foot.distanceToSqr(animal.position()) < range * range", code)
        self.assertIn("this.canReachTrackedAnimalFromNode(candidate.x", code)
        self.assertIn("Path alternative = this.navigation.createPath(access, 0)", code)
        self.assertIn("this.pathEntity.isDone() || this.navigation.isDone()", code)

    def test_slaughter_respects_herd_reserve_and_selected_animal(self):
        code = read("goal/generic/GoalGenericSlaughterAnimal.java")
        self.assertIn("this.getAnimals(villager, b.getPos(), 15, 10)", code)
        self.assertIn("this.aggressiveSlaughter ? targetAnimals / 2 : targetAnimals", code)
        self.assertIn("HERD_AT_RESERVE", code)
        self.assertIn("villager.setTarget(living)", code)
        self.assertIn("!this.isDestPossible(villager, dest)", code)

    def test_trap_rescue_requires_solid_enclosure_and_safe_loaded_exit(self):
        code = read("pathing/PhysicalTrapRecovery.java")
        self.assertIn("villager.isInWall()", code)
        self.assertIn("isCollisionShapeFullBlock", code)
        self.assertIn("Direction.Plane.HORIZONTAL", code)
        self.assertIn("!level.hasChunkAt(pos)", code)
        self.assertIn("level.noCollision(villager, box)", code)
        self.assertIn("!level.containsAnyLiquid(box)", code)
        self.assertIn("!surrounded(level, pos)", code)
        self.assertNotIn("getChunk(", code)
        villager = read("entity/MillVillager.java")
        self.assertIn("this.recoverPhysicalTrap();", villager)
        self.assertIn("NAV-TRAP-RESCUE", villager)

    def test_door_recovery_uses_physical_panel_and_normal_lease(self):
        villager = read("entity/MillVillager.java")
        self.assertIn("physicallyStalled && this.openContactWoodenDoor(dx, dz)", villager)
        self.assertIn("swept.intersects(panel.move", villager)
        self.assertIn('"PHYSICAL-CONTACT"', villager)
        self.assertIn("if (BlockItemUtilities.isWoodenDoor(block)) continue;", villager)
        nav = read("entity/PathNavigateSimple.java")
        self.assertIn("this.assistOpenDoorWaypoint(rung, state)", nav)
        self.assertIn("this.mob.level.noCollision(this.mob, targetBox)", nav)
        self.assertIn("getMoveControl().setWantedPosition", nav)

if __name__ == "__main__":
    unittest.main()
