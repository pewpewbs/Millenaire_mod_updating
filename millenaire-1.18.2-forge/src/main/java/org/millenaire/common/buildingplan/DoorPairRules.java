package org.millenaire.common.buildingplan;

/** Horizontal geometry in world X/Z. Minecraft NORTH is (0,-1). */
public final class DoorPairRules {
    private DoorPairRules() {}
    public static int partnerX(int facingX,int facingZ,boolean leftHinge){
        return leftHinge?-facingZ:facingZ;
    }
    public static int partnerZ(int facingX,int facingZ,boolean leftHinge){
        return leftHinge?facingX:-facingX;
    }
    public static boolean isRight(int facingX,int facingZ,int deltaX,int deltaZ){
        return deltaX==partnerX(facingX,facingZ,true)&&deltaZ==partnerZ(facingX,facingZ,true);
    }
}
