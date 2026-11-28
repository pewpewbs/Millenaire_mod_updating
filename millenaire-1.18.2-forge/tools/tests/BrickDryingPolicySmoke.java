import java.util.Random;
import org.millenaire.common.block.BrickDryingPolicy;
public final class BrickDryingPolicySmoke {
    public static void main(String[] args) {
        int[] expected = {379,341,303};
        for(int level=0;level<3;level++){
            if(BrickDryingPolicy.meanSeconds(level)!=expected[level])throw new AssertionError("mean seconds");
            Random random=new Random(42);
            long count=0;
            for(int i=0;i<100000;i++){
                int progress=0;
                while(progress<3){progress=BrickDryingPolicy.advance(progress,level,random);count++;}
            }
            double seconds=count/100000.0*4096/60;
            if(Math.abs(seconds-expected[level])>4)throw new AssertionError("simulation "+level+": "+seconds);
        }
        Random skipped = new Random() {
            @Override public int nextInt(int bound) { return bound - 1; }
        };
        for(int level=0;level<3;level++)
            for(int progress=0;progress<3;progress++)
                if(BrickDryingPolicy.advance(progress,level,skipped)!=progress)
                    throw new AssertionError("global slowdown must gate every level");
        System.out.println("BrickDryingPolicySmoke passed");
    }
}
