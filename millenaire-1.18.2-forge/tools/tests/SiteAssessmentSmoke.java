package org.millenaire.common.buildingplan;
public final class SiteAssessmentSmoke {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        var index=new BuildingSiteSearch.FootprintIndex(9,9,(x,z)->x==6&&z==6?1:x==2&&z==2?2:x==4&&z==4?4:0);
        check(index.inspect(0,0,1,1,0,0)==null);
        check(index.inspect(5,5,1,1,0,0)==null);
        check(index.inspect(3,3,1,1,0,0).status()==BuildingSiteSearch.SiteStatus.PROTECTED_OR_UNCERTAIN);
        var candidate=index.inspect(4,4,1,1,100,-100);
        check(candidate.status()==BuildingSiteSearch.SiteStatus.PREPARE_TERRAIN);
        check(candidate.work()==1);
        check(candidate.hint().equals("X: 103..105, Z: -97..-95"));
        check(BuildingSiteSearch.prefer(index.inspect(2,2,1,1,0,0),candidate)==candidate);
        check(BuildingSiteSearch.prefer(candidate,null)==candidate);
        System.out.println("PASS: site assessment checks");
    }
}
