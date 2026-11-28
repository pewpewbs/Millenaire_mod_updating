import org.millenaire.common.goal.generic.FurnaceBatchPolicy;

public final class FurnaceBatchPolicySmoke {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(!FurnaceBatchPolicy.inputDue(9,64), "do not refill early");
        check(FurnaceBatchPolicy.inputDue(8,64), "refill at eight");
        check(FurnaceBatchPolicy.inputDue(0,1), "single-stack inputs are usable");
        check(FurnaceBatchPolicy.inputDue(8,16), "small stack low water");
        check(FurnaceBatchPolicy.capacity(64,16)==16, "respect item limit");
        check(FurnaceBatchPolicy.minimumBatch(16, 8)==8, "respect small batch capacity");
        check(!FurnaceBatchPolicy.outputDue(1,64,true), "single cooked item does not interrupt work");
        check(!FurnaceBatchPolicy.outputDue(31,64,true), "wait for batch");
        check(FurnaceBatchPolicy.outputDue(32,64,true), "collect ready batch");
        check(FurnaceBatchPolicy.outputDue(1,64,false), "collect final or stalled remainder");
        check(FurnaceBatchPolicy.outputDue(16,16,true), "collect full small slot");
        check(!FurnaceBatchPolicy.outputDue(0,64,false), "no empty output trip");
        check(!FurnaceBatchPolicy.fuelDue(5,64), "do not refill fuel early");
        check(FurnaceBatchPolicy.fuelDue(4,64), "refill low fuel");
        check(!FurnaceBatchPolicy.fuelDue(0,3), "do not consume log without four slots");
        System.out.println("FurnaceBatchPolicySmoke passed");
    }
}
