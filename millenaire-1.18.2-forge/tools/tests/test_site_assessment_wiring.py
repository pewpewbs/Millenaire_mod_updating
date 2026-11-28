"""Verify the site result is propagated to the selected project and its UI."""
from pathlib import Path
import unittest

JAVA = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"

class SiteAssessmentWiring(unittest.TestCase):
    def test_selected_project_is_retained_and_retried(self):
        s=(JAVA/"village/Building.java").read_text(encoding="utf-8")
        failure=s.split("var assessment=this.winfo.lastSiteAssessment;",1)[1].split("} else if (!canAffordProject)",1)[0]
        self.assertIn("assessment.issue()",failure)
        self.assertIn("assessment.hint()",failure)
        self.assertIn("projectSiteCooldown.defer",failure)
        self.assertNotIn("this.buildingGoal=null",failure)
        self.assertIn("if(this.hasSiteIssue())return false;",s)
        self.assertIn("!projectHandled && project != null && !this.hasSiteIssue()",s)
        self.assertIn("if(!this.hasSiteIssue())this.buildingGoalIssue = \"ui.lackingresources\";",s)
        self.assertIn("if(status==0)this.buildingGoalLocation=null;",s)
    def test_site_checks_preserve_constraints(self):
        s=(JAVA/"buildingplan/BuildingPlan.java").read_text(encoding="utf-8")
        a=s.split("private BuildingSiteSearch.Assessment assessFailedSite(",1)[1].split("public BuildingBlock[] getBuildingPoints",1)[0]
        for marker in ("winfo.buildingLocRef", "winfo.surveyed", "winfo.buildingForbidden",
                       "winfo.danger", "winfo.water", "farFromTag", "closeToTag",
                       "areaToClearLengthBefore", "areaToClearWidthBefore", "fixedOrientation"):
            self.assertIn(marker,a)
        self.assertIn("NO_LAYOUT_SPACE",a)
    def test_hint_is_synchronized_and_displayed(self):
        b=(JAVA/"village/Building.java").read_text(encoding="utf-8")
        p=(JAVA/"village/buildingmanagers/PanelContentGenerator.java").read_text(encoding="utf-8")
        self.assertIn("building.buildingGoalSiteHint = StreamReadWrite.readNullableString(ds)",b)
        self.assertIn("StreamReadWrite.writeNullableString(this.buildingGoalSiteHint, data)",b)
        self.assertIn("page.addLine(townHall.buildingGoalSiteHint)",p)
        self.assertIn("page.addLine(house.buildingGoalSiteHint)",p)

if __name__ == "__main__":
    unittest.main()
