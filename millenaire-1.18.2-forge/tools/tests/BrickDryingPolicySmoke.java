import java.util.Random;
import org.millenaire.common.block.BrickDryingPolicy;
public final class BrickDryingPolicySmoke {
    public static void main(String[] args) {
        int[] expected = {190,171,152};
        for(int level=0;level<3;level++){
            if(BrickDryingPolicy.meanSeconds(level)!=expected[level])throw new AssertionError("mean seconds");
            Random random=new Random(42);
            long count=0;
            for(int i=0;i<100000;i++){
                int progress=0;
                while(progress<3){progress=BrickDryingPolicy.advance(progress,level,random);count++;}
            }
            double seconds=count/100000.0*4096/60;
            if(Math.abs(seconds-expected[level])>2)throw new AssertionError("simulation "+level+": "+seconds);
        }
        Random old=new Random(123), now=new Random(123);
        for(int i=0;i<1000;i++){
            int progress=i%3, next=progress+1;
            if(next<2&&old.nextBoolean())next++;
            if(BrickDryingPolicy.advance(progress,1,now)!=next)throw new AssertionError("original middle level");
        }
        System.out.println("BrickDryingPolicySmoke passed");
    }
}
