import org.millenaire.common.goal.BrickProductionPolicy;

public final class BrickProductionPolicySmoke {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        var empty = BrickProductionPolicy.demand(0, 0, 0, 0, 0, 0);
        check(empty.shortage() == 128 && !empty.urgent(), "reserve");
        check(empty.priority(false) > empty.priority(true), "refill drying area before collecting");
        check(empty.shouldForm(127) && !empty.shouldForm(128) && empty.shouldGather(), "drying work in progress");
        var full = BrickProductionPolicy.demand(0, 128, 0, 0, 0, 0);
        check(!full.shouldGather() && !full.shouldForm(0), "stop when stocked");
        var urgent = BrickProductionPolicy.demand(200, 80, 0, 0, 0, 0);
        check(urgent.shortage() == 248 && urgent.constructionShortage() == 120, "pending project");
        check(urgent.priority(false) > urgent.priority(true) && urgent.priority(false) < 900, "delivery and construction precedence");
        var carried = BrickProductionPolicy.demand(200, 80, 120, 0, 0, 0);
        check(carried.shortage() == 128 && !carried.urgent(), "offsite stock not duplicated");
        var fired = BrickProductionPolicy.demand(100, 100, 0, 80, 20, 10);
        check(fired.shortage() == 178 && fired.constructionShortage() == 50, "fired brick input");
        var covered = BrickProductionPolicy.demand(100, 228, 0, 80, 20, 60);
        check(!covered.shouldGather(), "already fired stock");
        check(BrickProductionPolicy.demand(100000, 0, 0, 0, 0, 0).priority(true) < 1000, "bounded boost");
        int[] reserves = {128, 256, 384, 512, 512};
        for (int level = 0; level < reserves.length; level++) {
            int reserve = BrickProductionPolicy.reserveForCentreLevel(level);
            check(reserve == reserves[level], "reserve at centre level " + level);
            check(!BrickProductionPolicy.demand(0, reserve, 0, 0, 0, 0, reserve).shouldGather(), "stop at level reserve " + level);
        }
        check(BrickProductionPolicy.reserveForCentreLevel(-1) == 128, "unbuilt or missing centre fallback");
        check(BrickProductionPolicy.reserveForCentreLevel(Integer.MAX_VALUE) == BrickProductionPolicy.MAX_RESERVE, "reserve capped");
        var project = BrickProductionPolicy.demand(600, 0, 0, 0, 0, 0, 256);
        check(project.shortage() == 856 && project.constructionShortage() == 600, "project cost plus level reserve");
        check(!BrickProductionPolicy.demand(600, 856, 0, 0, 0, 0, 256).shouldGather(), "project and reserve covered");
        check(!BrickProductionPolicy.demand(600, 824, 32, 0, 0, 0, 256).shouldGather(), "carried batch counts toward reserve");
        check(BrickProductionPolicy.demand(0, 2048, 0, 0, 0, 0, 128).shortage() == 0, "existing surplus retained");
        check(!BrickProductionPolicy.deliveryDue(7,false),"small raw batch");
        check(!BrickProductionPolicy.deliveryDue(8,false),"no urgent eight-brick batch");
        check(!BrickProductionPolicy.deliveryDue(16,false),"half raw batch");
        check(!BrickProductionPolicy.deliveryDue(31,false),"finish raw batch");
        check(BrickProductionPolicy.deliveryDue(32,false),"full raw batch");
        check(BrickProductionPolicy.deliveryDue(1,true),"finished fired brick");
        check(!BrickProductionPolicy.deliveryDue(0,true),"empty cargo");
        System.out.println("BrickProductionPolicySmoke: 33 checks passed");
    }
}
