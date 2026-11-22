import org.millenaire.common.village.VillageTerrainColumn;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import java.util.*;import java.util.function.IntFunction;
public class VillageTerrainColumnCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Set<Block> extra=new HashSet<>();
 static final VillageTerrainColumn.Rules RULES=new VillageTerrainColumn.Rules(){
  public boolean ground(Block b){return b==Blocks.STONE||b==Blocks.DIRT||b==Blocks.DIRT_PATH;}
  public boolean solid(Block b){return b.defaultBlockState().getMaterial().isSolid();}
  public boolean liquid(Block b){return b instanceof LiquidBlock;}
  public boolean forbidden(Block b){return b==Blocks.CHEST||b==Blocks.WATER||b==Blocks.LAVA;}
  public boolean dangerous(Block b){return b==Blocks.LAVA;}
  public boolean water(Block b){return b==Blocks.WATER;}
  public boolean fence(Block b){return b instanceof FenceBlock;}
  public boolean path(Block b){return b==Blocks.DIRT_PATH;}
  public boolean tree(BlockState s){return s.is(Blocks.OAK_LOG);}
  public boolean explicitlyForbidden(Block b){return extra.contains(b);}
 };
 static VillageTerrainColumn.Result scan(Map<Integer,BlockState> column,int baseline,int min,int max){return VillageTerrainColumn.scan(y->{check(y>=min&&y<max,"World height read out of range: "+y);return column.getOrDefault(y,Blocks.AIR.defaultBlockState());},baseline,min,max,RULES);}
 public static void main(String[] args){
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();Map<Integer,BlockState> c=new HashMap<>();
  for(int ground:new int[]{-64,-40,0,64,300}){c.clear();c.put(ground,Blocks.DIRT.defaultBlockState());var result=scan(c,ground+1,-64,320);check(result.top()==ground+1&&result.groundFound()&&result.clearance()==3&&!result.danger()&&!result.forbidden(),"Surface height and clearance "+ground);}
  c.clear();var empty=scan(c,-40,-64,320);check(!empty.groundFound()&&empty.clearance()==0,"Empty column is not buildable ground");
  c.put(-43,Blocks.DIRT.defaultBlockState());c.put(-42,Blocks.WATER.defaultBlockState());c.put(-41,Blocks.WATER.defaultBlockState());var water=scan(c,-40,-64,320);check(water.top()==-41&&water.water()&&water.forbidden()&&water.clearance()==0,"Negative-height liquid sentinel and obstruction");
  c.put(-42,Blocks.LAVA.defaultBlockState());c.put(-41,Blocks.LAVA.defaultBlockState());var lava=scan(c,-40,-64,320);check(lava.danger()&&lava.clearance()==0,"Dangerous liquid");
  c.clear();c.put(63,Blocks.DIRT.defaultBlockState());c.put(66,Blocks.OAK_PLANKS.defaultBlockState());check(scan(c,64,-64,320).clearance()==2,"Two-block headroom under wooden ceiling");
  c.put(67,Blocks.CHEST.defaultBlockState());check(scan(c,64,-64,320).forbidden(),"Protected block above prevents building");
  c.clear();c.put(63,Blocks.DIRT.defaultBlockState());c.put(64,Blocks.OAK_LOG.defaultBlockState());var stump=scan(c,64,-64,320);check(stump.top()==65&&stump.tree(),"Standing on a tree stump");
  c.clear();c.put(63,Blocks.DIRT_PATH.defaultBlockState());check(scan(c,64,-64,320).path(),"Path surface");extra.add(Blocks.DIRT_PATH);var forbiddenSoil=scan(c,64,-64,320);check(forbiddenSoil.danger()&&forbiddenSoil.clearance()==0,"Configured forbidden soil compared to the correct block");extra.clear();
  c.clear();c.put(319,Blocks.DIRT.defaultBlockState());check(!scan(c,320,-64,320).groundFound(),"No standable surface beyond the ceiling");
  check(!scan(c,1000,-64,320).groundFound(),"Out-of-range village baseline handled without invalid reads");
  System.out.println("PASS: actual terrain column scanner: -64 floor, negative/zero/high surfaces, empty columns, water/lava, headroom, protected blocks, stump/path flags, forbidden soil and world bounds. Classification rules are fixtures; live chunk loading, scheduling and village pathfinding untested.");
 }
}