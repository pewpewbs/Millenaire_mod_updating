import org.millenaire.common.village.PenAllocationRules;
import org.millenaire.common.buildingplan.DoorPairRules;
public class VillageInteractionRulesSmoke {
    static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    public static void main(String[] args){
        int checks=0;
        int[][] facing={{0,-1},{1,0},{0,1},{-1,0}};
        for(int[] f:facing)for(boolean left:new boolean[]{false,true}){
            int x=DoorPairRules.partnerX(f[0],f[1],left),z=DoorPairRules.partnerZ(f[0],f[1],left);
            check(Math.abs(x)+Math.abs(z)==1&&x*f[0]+z*f[1]==0,"adjacent lateral leaf");checks++;
            check(x==-DoorPairRules.partnerX(f[0],f[1],!left)&&z==-DoorPairRules.partnerZ(f[0],f[1],!left),"reciprocal hinge");checks++;
            check(DoorPairRules.isRight(f[0],f[1],x,z)==left,"outside hinge");checks++;
        }
        check(PenAllocationRules.score(1,1,1)>PenAllocationRules.score(0,3,0),"complete isolated pair first");checks++;
        check(PenAllocationRules.score(0,1,0)>PenAllocationRules.score(2,3,0),"start empty pen before overfill");checks++;
        check(PenAllocationRules.score(2,4,0)>PenAllocationRules.score(2,2,0),"relative capacity");checks++;
        check(PenAllocationRules.score(2,2,0)>PenAllocationRules.score(2,2,1),"prefer unused marker");checks++;
        System.out.println("PASS: "+checks+" pure door geometry and pen scoring checks; not gameplay tests");
    }
}
