"""Regression wiring for the site-preparation map marker."""
from pathlib import Path
import unittest

SRC=Path(__file__).resolve().parents[2]/"src/main/java/org/millenaire"

class SitePreparationOverlayWiring(unittest.TestCase):
    def test_draw_after_terrain_before_residents(self):
        s=(SRC/"client/gui/text/GuiPanelParchment.java").read_text(encoding="utf-8")
        scope=s.split("private void drawVillageMap(",1)[1].split("public void render(",1)[0]
        self.assertLess(scope.index("this.drawScaledRect(x, lastZ, x + 1, minfo.width"),scope.index("SitePreparationOverlay.project"))
        self.assertLess(scope.index("SitePreparationOverlay.project"),scope.index("for (MillVillager villager"))
        self.assertIn("SitePreparationOverlay.visible(net.minecraft.Util.getMillis())",scope)
        self.assertIn("this.townHall.buildingGoalSiteHint,minfo.mapStartX,minfo.mapStartZ",scope)
        self.assertIn("SitePreparationOverlay.RED",scope)
    def test_pushes_only_when_marker_changes(self):
        s=(SRC/"common/village/Building.java").read_text(encoding="utf-8")
        self.assertIn("!java.util.Objects.equals(oldSite,newSite)||!java.util.Objects.equals(oldSiteGoal,newSiteGoal)",s)
        self.assertIn("profile.buildingsSent.containsKey(this.pos)",s)
        self.assertIn("this.sendBuildingPacket(player,false)",s)

if __name__=="__main__":
    unittest.main()
