package org.millenaire.client.gui.text;

public final class SitePreparationOverlaySmoke {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        String hint="X: -320..-311, Z: -605..-594";
        var r=SitePreparationOverlay.project("ui.site.prepare",hint,-330,-620,90,90);
        check(new SitePreparationOverlay.Rect(10,15,20,27).equals(r),"inclusive world bounds");
        check(new SitePreparationOverlay.Rect(0,0,1,1).equals(
                SitePreparationOverlay.project("ui.site.prepare","X: 0..0, Z: 0..0",0,0,1,1)),"one cell");
        check(new SitePreparationOverlay.Rect(0,0,5,5).equals(
                SitePreparationOverlay.project("ui.site.prepare","X: -2..9, Z: -3..9",0,0,5,5)),"clip all edges");
        check(SitePreparationOverlay.project("ui.site.nolayout",hint,-330,-620,90,90)==null,"no layout no marker");
        check(SitePreparationOverlay.project("ui.site.review",hint,-330,-620,90,90)==null,"review no marker");
        check(SitePreparationOverlay.project("ui.lackingresources",hint,-330,-620,90,90)==null,"other statuses no marker");
        check(SitePreparationOverlay.project("ui.site.prepare",null,0,0,5,5)==null,"missing hint");
        check(SitePreparationOverlay.project("ui.site.prepare","X: 3..1, Z: 0..4",0,0,5,5)==null,"reversed hint");
        check(SitePreparationOverlay.project("ui.site.prepare","X: 9999999999..9999999999, Z: 0..4",0,0,5,5)==null,"malformed coordinates");
        check(SitePreparationOverlay.project("ui.site.prepare",hint,0,0,5,5)==null,"outside map");
        check(SitePreparationOverlay.visible(0)&&SitePreparationOverlay.visible(699),"red phase");
        check(!SitePreparationOverlay.visible(700)&&!SitePreparationOverlay.visible(1399),"transparent phase");
        check(SitePreparationOverlay.visible(1400),"repeat");
        System.out.println("PASS: site preparation overlay checks");
    }
}
