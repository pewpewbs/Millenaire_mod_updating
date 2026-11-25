package org.millenaire.common.world;

import java.util.HashSet;

public final class VillagePreparationPolicySmoke {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        for (int x : new int[]{-123,0,234}) for(int z : new int[]{-99,0,123}) {
            int radius = VillagePreparationPolicy.radius(80);
            var cells = VillagePreparationPolicy.cells(x,z,radius);
            check(radius==7,"default radius");
            check(cells.size()==256,"16x16 area");
            check(new HashSet<>(cells).size()==cells.size(),"duplicates");
            check(cells.get(0).equals(new VillagePreparationPolicy.Cell(x,z)),"centre first");
            for(int i=-radius;i<=radius+1;i++) for(int j=-radius;j<=radius+1;j++)
                check(cells.contains(new VillagePreparationPolicy.Cell(x+i,z+j)),"missing chunk");
        }
        check(VillagePreparationPolicy.cells(0,0,10).size()==484,"maximum");
        check(VillagePreparationPolicy.cells(0,0,11).isEmpty(),"oversized");
        check(VillagePreparationPolicy.nearby(-5,-5,3,3,8),"negative spacing");
        check(!VillagePreparationPolicy.nearby(-5,-5,4,3,8),"outside spacing");
        check(VillagePreparationPolicy.interval(20,0)==2,"normal");
        check(VillagePreparationPolicy.interval(40,0)==10,"slow");
        check(VillagePreparationPolicy.interval(54,0)==10,"slow high");
        check(VillagePreparationPolicy.interval(55,0)==0,"overload");
        check(VillagePreparationPolicy.interval(20,15_000_000)==10,"world budget");
        check(VillagePreparationPolicy.interval(20,30_000_000)==0,"world busy");
        check(VillagePreparationPolicy.interval(Float.NaN,0)==0,"invalid mspt");
        System.out.println("VillagePreparationPolicy: "+checks+" checks passed");
    }
}
