import org.millenaire.common.goal.generic.SeedSurplusPolicy;
public class SeedSurplusSmoke {
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        check(SeedSurplusPolicy.plantingReserve(0)==0,"no field");
        check(SeedSurplusPolicy.plantingReserve(4)==16,"small field floor");
        check(SeedSurplusPolicy.plantingReserve(48)==96,"two sowings");
        check(SeedSurplusPolicy.chickenFeedReserve(5)==10,"chicken reserve");
        check(SeedSurplusPolicy.allowedDrop(1,0,16,0,32)==1,"empty stock");
        check(SeedSurplusPolicy.allowedDrop(1,16,16,32,32)==0,"at cap");
        check(SeedSurplusPolicy.allowedDrop(1,15,16,500,32)==1,"local shortage");
        check(SeedSurplusPolicy.allowedDrop(1,50,16,31,32)==1,"village shortage");
        check(SeedSurplusPolicy.allowedDrop(8,14,16,40,32)==2,"clip multiplier");
        check(SeedSurplusPolicy.allowedDrop(8,14,16,27,32)==5,"larger deficit");
        check(SeedSurplusPolicy.allowedDrop(1,1000,16,1000,32)==0,"legacy surplus");
        check(SeedSurplusPolicy.allowedDrop(0,0,16,0,32)==0,"no award");
        System.out.println("PASS: 12 seed stock policy checks (not gameplay tests)");
    }
}
