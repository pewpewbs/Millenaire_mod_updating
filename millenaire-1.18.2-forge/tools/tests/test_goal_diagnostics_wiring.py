"""Source-level checks for on-demand diagnostics. No Forge runtime simulation."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire"

def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

class GoalDiagnosticsWiring(unittest.TestCase):
    def test_authorization_and_request_bounds(self):
        source = read("common/goal/GoalDiagnostics.java")
        network = read("common/network/ServerReceiver.java")
        self.assertIn("player.hasPermissions(2)", source)
        self.assertIn("player.distanceToSqr(villager) > 256.0", source)
        self.assertIn("villager.level != player.level", source)
        self.assertIn("now - last < 10", source)
        self.assertIn("MAX_ROWS = 96, MAX_NOTES = 4", source)
        self.assertIn("WATCH_TICKS = 1200", source)
        self.assertIn("packetBuffer.readableBytes() != 4", network)
        self.assertIn("GoalDiagnostics.allowRequest(sender)", network)
        self.assertIn("GoalDiagnostics.watchAndReport(sender", network)

    def test_observe_actual_selection_without_replaying_ai(self):
        villager = read("common/entity/MillVillager.java")
        selection = villager.split("public void setNextGoal()", 1)[1].split("public void setPathDestPoint", 1)[0]
        self.assertEqual(selection.count("goal.isPossible(this)"), 1)
        self.assertEqual(selection.count("nextGoal.priority(this)"), 1)
        self.assertEqual(selection.count("goal.priority(this)"), 2) # existing debug logging and actual comparison
        self.assertIn("diagnostic.possible(possible)", selection)
        self.assertIn("diagnostic.priority(nextGoal, previousPriority)", selection)
        self.assertIn("diagnostic.priority(goal, candidatePriority)", selection)
        self.assertIn("diagnostic.finish(nextGoal)", selection)
        self.assertIn("diagnostic.close()", selection)
        observer = read("common/goal/GoalDiagnostics.java")
        self.assertNotIn(".isPossible(", observer)
        self.assertNotIn(".getDestination(", observer)
        self.assertNotIn(".priority(", observer)
        self.assertIn("SPECIFIC_CONDITION_FALSE (not instrumented)", observer)

    def test_reasons_and_destination_are_separate(self):
        goal = read("common/goal/Goal.java")
        for reason in ("TOWNHALL_LIMIT", "VILLAGE_LIMIT", "BUILDING_LIMIT", "RESOURCE_BALANCE",
                       "DAY_NOT_ALLOWED", "NIGHT_NOT_ALLOWED", "MAX_SIMULTANEOUS_TOTAL"):
            self.assertIn(reason, goal)
        self.assertIn("GoalDiagnostics.destination(villager, this, info)", goal)
        cook = read("common/goal/generic/GoalGenericCooking.java")
        self.assertIn("INPUT_BELOW_MINIMUM", cook)
        self.assertIn("WORKPLACE_RESOURCE_BALANCE", cook)
        self.assertIn("NO_REGISTERED_FURNACE_OR_FIREPIT", cook)
        fish = read("common/goal/GoalFish.java")
        self.assertIn("FISH_HOME_STOCK_LIMIT", fish)
        self.assertIn("NO_ELIGIBLE_FISHING_SPOT", fish)
        observer = read("common/goal/GoalDiagnostics.java")
        self.assertIn("NO_ACCESSIBLE_MINING_DESTINATION", observer)
        self.assertIn("LAST_DESTINATION", observer)

    def test_client_screen(self):
        gui = read("client/gui/text/GuiConfig.java")
        screen = read("client/gui/text/GuiGoalDiagnostics.java")
        client = read("client/network/ClientReceiver.java")
        self.assertIn("new GuiGoalDiagnostics(this)", gui)
        self.assertIn("hasPermissions(2)", gui)
        self.assertIn("if (entityId != selectedId) return", screen)
        self.assertIn("if (++ticks % 40 == 0) request();", screen)
        self.assertIn("screen.receiveState(entityId, rows)", client)
        self.assertIn("count > 512", client)

if __name__ == "__main__":
    unittest.main(verbosity=2)
