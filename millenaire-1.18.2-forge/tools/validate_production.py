#!/usr/bin/env python3
"""Run the production acceptance checks once; optional full Forge build."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/org/millenaire/common"

def run(*args):
    print("\n>", " ".join(map(str,args)), flush=True)
    subprocess.run(list(map(str,args)), cwd=ROOT, check=True)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build", action="store_true", help="Also run the full Gradle build")
    args = parser.parse_args()
    run(sys.executable,"-m","unittest","discover","-s","tools/tests","-p","test_*.py","-v")
    with tempfile.TemporaryDirectory() as classes:
        run("javac","--release","17","-d",classes,
            JAVA / "buildingplan/ProductionSourceMigrationPolicy.java",
            ROOT / "tools/tests/ProductionSourceMigrationSmoke.java")
        run("java","-cp",classes,"ProductionSourceMigrationSmoke")
        run("javac","--release","17","-d",classes,
            JAVA / "village/VillageProgressionRules.java",
            ROOT / "tools/tests/VillageProgressionRulesSmoke.java")
        run("java","-cp",classes,"org.millenaire.common.village.VillageProgressionRulesSmoke")
        run("javac","--release","17","-d",classes,
            JAVA / "buildingplan/BuildingSiteSearch.java",
            ROOT / "tools/tests/SiteAssessmentSmoke.java")
        run("java","-cp",classes,"org.millenaire.common.buildingplan.SiteAssessmentSmoke")
        run("javac","--release","17","-d",classes,
            JAVA / "goal/BrickProductionPolicy.java",
            ROOT / "tools/tests/BrickProductionPolicySmoke.java")
        run("java","-cp",classes,"BrickProductionPolicySmoke")
        run("javac","--release","17","-d",classes,
            JAVA / "block/BrickDryingPolicy.java",
            ROOT / "tools/tests/BrickDryingPolicySmoke.java")
        run("java","-cp",classes,"BrickDryingPolicySmoke")
        run("javac","--release","17","-d",classes,
            JAVA / "buildingplan/DoorPairRules.java",
            JAVA / "village/PenAllocationRules.java",
            ROOT / "tools/tests/VillageInteractionRulesSmoke.java")
        run("java","-cp",classes,"VillageInteractionRulesSmoke")
        run("javac","--release","17","-d",classes,
            JAVA / "goal/generic/SeedSurplusPolicy.java",
            ROOT / "tools/tests/SeedSurplusSmoke.java")
        run("java","-cp",classes,"SeedSurplusSmoke")
        run("javac","--release","17","-d",classes,
            JAVA / "world/VillageGenerationProbe.java",
            ROOT / "tools/tests/VillageGenerationProbeSmoke.java")
        run("java","-cp",classes,"org.millenaire.common.world.VillageGenerationProbeSmoke")
        run("javac","--release","17","-d",classes,
            JAVA / "world/VillagePreparationPolicy.java",
            ROOT / "tools/tests/VillagePreparationPolicySmoke.java")
        run("java","-cp",classes,"org.millenaire.common.world.VillagePreparationPolicySmoke")
    from audit_production import audit, DEFAULT_DATA, DEFAULT_JAVA
    result = audit(DEFAULT_DATA, DEFAULT_JAVA, True)
    output = ROOT / "tools/reports/production_audit.json"
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("\nAudit summary:",json.dumps(result["summary"],ensure_ascii=False),flush=True)
    if not all(c["png_scanned"] for c in result["cultures"].values()):
        raise SystemExit("Pillow required for full source-marker validation.")
    if result["summary"]["findings"].get("ERROR",0):
        raise SystemExit("Production audit has ERROR findings.")
    if args.build:
        run(*(["cmd","/c","gradlew.bat","build"] if os.name=="nt" else ["bash","gradlew","build"]))
    print("\nPASS: production validation complete. REVIEW findings are not automatically defects.")

if __name__ == "__main__":
    main()
