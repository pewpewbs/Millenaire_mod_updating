package org.millenaire.common.world;

import java.util.HashMap;
import java.util.Map;

public final class VillageGenerationProbeSmoke {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static boolean original(int sx,int sz,int ex,int ez,
            java.util.function.BiPredicate<Integer,Integer> generated) {
        for(int x=sx>>4;x<=(ex>>4)+1;x++)
            for(int z=sz>>4;z<=(ez>>4)+1;z++)
                if(!generated.test(x,z))return false;
        return true;
    }

    public static void main(String[] args) {
        Map<String,Integer> calls=new HashMap<>();
        java.util.function.BiPredicate<Integer,Integer> terrain =
            (x,z) -> !(x==-3 && z==2) && !(x==6 && z==-4);
        var probe=new VillageGenerationProbe((x,z)->{
            calls.merge(x+":"+z,1,Integer::sum);
            return terrain.test(x,z);
        });
        int count=0;
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++) {
            int sx=x*16-31,sz=z*16-31,ex=x*16+32,ez=z*16+32;
            check(probe.checkArea(sx,sz,ex,ez)==original(sx,sz,ex,ez,terrain),
                    "Changed area decision at "+x+","+z);
            count++;
        }
        check(calls.values().stream().allMatch(n->n==1),"Repeated expensive check");
        check(probe.uniqueChecks()==calls.size(),"Incorrect unique probe count");
        check(probe.cacheHits()>0,"No overlapping checks reused");
        var border=new VillageGenerationProbe((x,z)->x!=-1||z!=0);
        check(!border.checkArea(-16,-16,-1,-1),"Lost extra end chunk at negative coordinate");
        boolean[] available={false};
        var first=new VillageGenerationProbe((x,z)->available[0]);
        check(!first.checkArea(0,0,0,0),"Expected missing chunk");
        available[0]=true;
        var next=new VillageGenerationProbe((x,z)->available[0]);
        check(next.checkArea(0,0,0,0),"Missing cache leaked into next attempt");
        System.out.println("VillageGenerationProbe: "+count+" area comparisons passed, "
                +probe.uniqueChecks()+" unique checks, "+probe.cacheHits()+" reused checks; 6 additional checks passed.");
    }
}
