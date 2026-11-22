import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.properties.*;import org.millenaire.common.buildingplan.DoorPlacement;
public class DoorHalvesCheck{
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();int n=0;
 for(var state:Blocks.OAK_DOOR.getStateDefinition().getPossibleStates())for(var rotation:Rotation.values())for(boolean invert:new boolean[]{false,true}){
  var rotated=state.rotate(rotation);var lower=DoorPlacement.half(rotated,invert,DoubleBlockHalf.LOWER);var upper=DoorPlacement.half(rotated,invert,DoubleBlockHalf.UPPER);
  if(!lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER).equals(upper))throw new AssertionError("mismatched halves");
  if(lower.getValue(DoorBlock.FACING)!=rotated.getValue(DoorBlock.FACING)||lower.getValue(DoorBlock.OPEN)!=rotated.getValue(DoorBlock.OPEN)||lower.getValue(DoorBlock.POWERED)!=rotated.getValue(DoorBlock.POWERED))throw new AssertionError("changed plan state");
  if(lower.getValue(DoorBlock.HINGE)!=(invert?DoorHingeSide.RIGHT:rotated.getValue(DoorBlock.HINGE)))throw new AssertionError("wrong hinge");n++;
 }System.out.println("PASS: "+n+" actual vanilla door states/rotations/inversion cases preserve facing, opening and power with matching halves. World placement integration not covered.");}}
