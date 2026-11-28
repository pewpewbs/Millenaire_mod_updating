import org.millenaire.common.goal.BrickProductionPolicy;

public final class BrickProductionPolicySmoke {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        var empty = BrickProductionPolicy.demand(0, 0, 0, 0, 0, 0);
        check(empty.shortage() == 64 && !empty.urgent(), "reserve");
        check(empty.priority(false) > empty.priority(true), "refill drying area before collecting");
        check(empty.shouldForm(63) && !empty.shouldForm(64) && empty.shouldGather(), "drying work in progress");
        var full = BrickProductionPolicy.demand(0, 64, 0, 0, 0, 0);
        check(!full.shouldGather() && !full.shouldForm(0), "stop when stocked");
        var urgent = BrickProductionPolicy.demand(200, 80, 0, 0, 0, 0);
        check(urgent.shortage() == 184 && urgent.constructionShortage() == 120, "pending project");
        check(urgent.priority(false) > urgent.priority(true) && urgent.priority(false) < 900, "delivery and construction precedence");
        var carried = BrickProductionPolicy.demand(200, 80, 120, 0, 0, 0);
        check(carried.shortage() == 64 && !carried.urgent(), "offsite stock not duplicated");
        var fired = BrickProductionPolicy.demand(100, 100, 0, 80, 20, 10);
        check(fired.shortage() == 114 && fired.constructionShortage() == 50, "fired brick input");
        var covered = BrickProductionPolicy.demand(100, 164, 0, 80, 20, 60);
        check(!covered.shouldGather(), "already fired stock");
        check(BrickProductionPolicy.demand(100000, 0, 0, 0, 0, 0).priority(true) < 1000, "bounded boost");
        var cultureReserve = BrickProductionPolicy.demand(0, 64, 0, 0, 0, 0, 2048);
        check(cultureReserve.shortage() == 1984 && cultureReserve.shouldForm(0), "culture stock target");
        check(!BrickProductionPolicy.demand(0, 2048, 0, 0, 0, 0, 2048).shouldGather(), "stop at culture target");
        check(!BrickProductionPolicy.deliveryDue(7,7,100,false,true),"finish urgent batch");
        check(BrickProductionPolicy.deliveryDue(8,8,100,false,true),"urgent batch");
        check(BrickProductionPolicy.deliveryDue(3,3,3,false,true),"last needed bricks");
        check(BrickProductionPolicy.deliveryDue(3,3,100,false,false),"no more dry bricks");
        check(BrickProductionPolicy.deliveryDue(32,32,0,false,true),"ordinary batch");
        check(BrickProductionPolicy.deliveryDue(1,1,0,true,true),"finished fired brick");
        check(!BrickProductionPolicy.deliveryDue(0,32,0,true,false),"empty cargo");
        System.out.println("BrickProductionPolicySmoke: 19 checks passed");
    }
}
