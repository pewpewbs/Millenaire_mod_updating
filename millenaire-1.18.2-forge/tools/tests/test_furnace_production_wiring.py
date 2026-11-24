"""Regression wiring and exhaustive bundled role inventory (not a Forge runtime test)."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire/common"
DATA = ROOT / "src/main/resources/todeploy/millenaire"

def read(path):
    return path.read_text(encoding="utf-8-sig")

def fields(path):
    result = {}
    for line in read(path).splitlines():
        line = line.strip()
        if not line or line.startswith(("//", "#")) or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result.setdefault(key.strip().lower(), []).append(value.strip().lower())
    return result

def scope(data):
    return (data.get("townhallgoal", ["false"])[-1],
            data.get("buildingtag", [""])[-1])

def audit():
    goals = {}
    for folder in ("genericcooking", "generictendfurnace"):
        for path in sorted((DATA / "goals" / folder).rglob("*.txt")):
            goals[path.stem.lower()] = (folder, fields(path))
    for culture in sorted((DATA / "cultures").iterdir()):
        if not culture.is_dir():
            continue
        roles = list((culture / "villagers").rglob("*.txt"))
        relevant = 0
        print(f"\n{culture.name}: {len(roles)} profiles")
        for role in sorted(roles):
            assigned = fields(role).get("goal", [])
            cooks = [g for g in assigned if g in goals and goals[g][0] == "genericcooking"]
            tend = [g for g in assigned if g in goals and goals[g][0] == "generictendfurnace"]
            if not cooks and not tend:
                continue
            relevant += 1
            uncovered = [g for g in cooks if not any(
                scope(goals[g][1]) == scope(goals[t][1]) for t in tend)]
            print(f"  {role.stem}: cooking={cooks}; maintenance={tend}")
            if uncovered:
                print(f"    REVIEW partner/workplace coverage: {uncovered}")
        print(f"  {relevant} cooking/maintenance profiles")
    print("\nREVIEW is informational: another resident may tend the same oven.")

class FurnaceProductionWiring(unittest.TestCase):
    def test_fishing_checks_stock_at_selection_and_execution(self):
        src = read(JAVA / "goal/GoalFish.java")
        self.assertIn("if (!hasStockRoom(villager, b)) continue;", src)
        self.assertIn("!hasStockRoom(villager, destination)", src)
        self.assertIn("home.countGoods(item) + villager.countInv(item)", src)
        self.assertIn("stock >= entry.getValue()", src)
        priority = src.split("public int priority(", 1)[1].split("@Override", 1)[0]
        self.assertNotIn("getGoalBuildingDest()", priority)
        self.assertNotIn("getDestination(villager)", priority)

    def test_maintenance_collects_independently_and_preserves_remainder(self):
        src = read(JAVA / "goal/generic/GoalGenericTendFurnace.java")
        destination = src.split("getDestination(MillVillager", 1)[1].split("getHeldItemsTravelling", 1)[0]
        self.assertLess(destination.index("hasCollectableOutput"), destination.index("needsFuel"))
        action = src.split("public boolean performAction(", 1)[1]
        self.assertLess(action.index("collectOutput"), action.index("needsFuel"))
        self.assertIn("remainder.shrink(stored)", src)
        self.assertIn("if (stored <= 0) return output", src)
        self.assertIn("firepit.outputs.getStackInSlot(i)", src)
        self.assertIn("furnace.getItem(2)", src)
        self.assertIn("if (output.isEmpty() || output.hasTag()) return false", src)

    def test_workplace_balance_only_for_matching_local_input(self):
        base = read(JAVA / "goal/Goal.java")
        src = read(JAVA / "goal/generic/GoalGenericCooking.java")
        self.assertIn("if (!passesResourceBalance(villager))", base)
        self.assertIn("!this.townHallGoal", src)
        self.assertIn("this.balanceOutput[0].matches(this.itemToCook)", src)
        self.assertIn("raw += destination.countGoods(input)", src)
        self.assertIn("cooked += destination.countGoods(output)", src)
        self.assertIn("!passesWorkplaceBalance(villager, dest)", src)
        self.assertIn("GoalGenericTendFurnace.canStoreOutput", src)

    def test_tavern_role_and_same_version_migration(self):
        role = DATA / "cultures/byzantines/villagers/artisans/hostesse_byzantine.txt"
        data = fields(role)
        self.assertEqual(data["goal"].count("tendfurnace"), 1)
        for wood in ("wood", "wood_pine", "wood_birch", "wood_jungle", "wood_acacia", "wood_darkoak"):
            self.assertIn(f"{wood},4", data["requiredgood"])
        src = read(JAVA / "deployer/ContentDeployer.java")
        self.assertIn("ContentBundleUpdater.update(bundled, installed", src)
        updater = read(JAVA / "deployer/ContentBundleUpdater.java")
        self.assertIn("millenaire-content-backups", updater)
        self.assertIn('"cultures", "goals", "villagerconfig"', updater)

if __name__ == "__main__":
    audit()
    unittest.main(argv=["furnace"], verbosity=2)
