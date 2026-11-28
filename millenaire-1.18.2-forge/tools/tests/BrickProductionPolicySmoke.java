import org.millenaire.common.goal.BrickProductionPolicy;

public final class BrickProductionPolicySmoke {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        var empty = BrickProductionPolicy.demand(0, 0, 0, 0, 0, 0);
        check(empty.shortage() == 64 && !empty.urgent(), "reserve");
        check(empty.priority(true) > empty.priority(false), "collect before shape");
        check(empty.shouldForm(63) && !empty.shouldForm(64) && empty.shouldGather(), "drying work in progress");
        var full = BrickProductionPolicy.demand(0, 64, 0, 0, 0, 0);
        check(!full.shouldGather() && !full.shouldForm(0), "stop when stocked");
        var urgent = BrickProductionPolicy.demand(200, 80, 0, 0, 0, 0);
        check(urgent.shortage() == 184 && urgent.constructionShortage() == 120, "pending project");
        check(urgent.priority(true) > urgent.priority(false) && urgent.priority(true) < 1000, "construction precedence");
        var carried = BrickProductionPolicy.demand(200, 80, 120, 0, 0, 0);
        check(carried.shortage() == 64 && !carried.urgent(), "offsite stock not duplicated");
        var fired = BrickProductionPolicy.demand(100, 100, 0, 80, 20, 10);
        check(fired.shortage() == 114 && fired.constructionShortage() == 50, "fired brick input");
        var covered = BrickProductionPolicy.demand(100, 164, 0, 80, 20, 60);
        check(!covered.shouldGather(), "already fired stock");
        check(BrickProductionPolicy.demand(100000, 0, 0, 0, 0, 0).priority(true) < 1000, "bounded boost");
        System.out.println("BrickProductionPolicySmoke: 10 checks passed");
    }
}
