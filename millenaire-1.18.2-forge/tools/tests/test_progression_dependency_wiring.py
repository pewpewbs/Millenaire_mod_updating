"""Regression guards for resource and village-tag progression lookahead."""
from pathlib import Path
import unittest

JAVA = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common/village"

class ProgressionDependencyWiring(unittest.TestCase):
    def test_locked_producers_are_discovered_but_not_started(self):
        source = (JAVA / "VillageProgression.java").read_text(encoding="utf-8")
        lookahead = source.split("private static Map<Candidate,List<Recipe>> dependencyPlans(",1)[1].split("private static Candidate prerequisite(",1)[0]
        self.assertIn("v.getFlatProjectList()",lookahead)
        self.assertIn("v.validProgressionDependencyPlan(p,plan)",lookahead)
        self.assertIn("reachableTagProjects",lookahead)
        self.assertIn("c.plan().requiredVillageTags",lookahead)
        self.assertIn("c.plan().villageTags",lookahead)
        action = source.split("private static Candidate prerequisite(",1)[1]
        self.assertIn("for (Candidate c : options)",action)
        self.assertIn("v.validProgressionPlan(c.project(),c.plan())",action)
        self.assertIn("v.canAffordProgression(c.plan(),reservePending)",action)
        self.assertIn("neededTags.add(tag)",action)
        self.assertIn("neededTags.contains(tag)",action)

    def test_other_validity_constraints_are_preserved(self):
        source = (JAVA / "Building.java").read_text(encoding="utf-8")
        self.assertIn("return validProgressionPlan(project, plan, false)",source)
        self.assertIn("return validProgressionPlan(project, plan, true)",source)
        self.assertIn("if (!lookahead && !this.containsTags(tagx))",source)
        self.assertIn("plan.requiredGlobalTag != null",source)
        self.assertIn("plan.forbiddenTagsInVillage",source)
        self.assertIn("plan.requiredParentTags",source)

    def test_existing_unstaffed_workshop_is_not_a_new_building_requirement(self):
        source = (JAVA / "VillageProgression.java").read_text(encoding="utf-8")
        action = source.split("private static Candidate prerequisite(",1)[1]
        self.assertIn("if (p.getExistingPlan() != null) existing.addAll(recipes",action)
        self.assertIn("!matches(already,need)",action)
        self.assertIn("!matches(already,r.output())",action)

if __name__ == "__main__":
    unittest.main()
