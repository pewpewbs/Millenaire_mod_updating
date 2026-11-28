import java.util.*;
import org.millenaire.common.buildingplan.BuildingSiteSearch;
public class BuildingSiteSearchCheck {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args){
        Set<String> seen=new HashSet<>();
        BuildingSiteSearch.find(2,2,0,2,5,5,(x,z)->{check(seen.add(x+","+z),"duplicate");return null;});
        check(seen.size()==25,"all centres");
        check("ok".equals(BuildingSiteSearch.find(0,0,0,0,1,1,(x,z)->"ok")),"radius zero");
        check("corner".equals(BuildingSiteSearch.find(2,2,1,2,5,5,(x,z)->x==4&&z==4?"corner":null)),"outer corner");
        seen.clear();
        BuildingSiteSearch.find(0,0,0,3,3,3,(x,z)->{check(seen.add(x+","+z),"duplicate clipped");return null;});
        check(seen.size()==9,"map bounds");
        for(int p=0;p<4;p++){
            int[] o=BuildingSiteSearch.orientations(-1,-1,p);
            check(o.length==4&&o[0]==p&&Arrays.stream(o).distinct().count()==4,"rotations");
            check(Arrays.equals(BuildingSiteSearch.orientations(-1,p,0),new int[]{p}),"fixed");
            check(Arrays.equals(BuildingSiteSearch.orientations(p,(p+1)%4,0),new int[]{p}),"explicit");
        }
        check(BuildingSiteSearch.failures(new int[]{0,2,4,0,1,0,3,0,0}).equals("OUTSIDE_MAP=2 OCCUPIED=4 TERRAIN=1 UNREACHABLE=3"),"reasons");
        System.out.println("PASS: site enumeration, bounds, rotations and diagnostics");
    }
}
