import org.millenaire.client.render.*;
import net.minecraft.client.model.geom.ModelPart;
import java.util.Random;
public class VillagerModelCheck {
 static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
 public static void main(String[] args){
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  for(float inflation:new float[]{0,0.5F,1})for(float offset:new float[]{0,2}){
   ModelMillVillager[] models={new ModelFemaleSymmetrical(inflation,offset),new ModelFemaleAsymmetrical(inflation,offset)};
   for(var m:models){
    ModelPart[] parts={m.head,m.hat,m.body,m.body.getChild("breast"),m.rightArm,m.leftArm,m.rightLeg,m.leftLeg};
    float[][] sizes={{8,8,8},{8,8,8},{7,12,3},{7,4,2},{3,12,3},{3,12,3},{4,12,4},{4,12,4}};
    for(int i=0;i<parts.length;i++){var c=parts[i].getRandomCube(new Random(0));check(c.maxX-c.minX==sizes[i][0]&&c.maxY-c.minY==sizes[i][1]&&c.maxZ-c.minZ==sizes[i][2],"Cube dimensions "+i);}
    check(m.head.y==offset&&m.body.y==offset&&m.body.getChild("breast").y==offset,"Body pivots");
    check(m.rightArm.x==-5&&m.leftArm.x==5&&m.leftArm.y==2+offset,"Arm pivots");
    check(m.rightLeg.x==-2&&m.leftLeg.x==2&&m.leftLeg.y==12+offset,"Leg pivots");
    check(m.body.getAllParts().count()==2,"Breast remains attached to torso");
   }
  }
  check(!new ModelMillVillager().head.isEmpty(),"Male mesh");
  System.out.println("PASS: male mesh; both female meshes at three inflations and two offsets; eight cube dimensions, pivots and torso child attachment. Rendering and entity animation not game-tested; entity class is a fixture.");
 }
}