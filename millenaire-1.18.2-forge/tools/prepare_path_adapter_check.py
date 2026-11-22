from pathlib import Path
r=Path(__file__).resolve().parents[1];f=r/'tools/fixtures/path-adapter';f.mkdir(parents=True,exist_ok=True)
files={'BlockMillWall':'package org.millenaire.common.block;public class BlockMillWall extends net.minecraft.world.level.block.WallBlock { public BlockMillWall(){super(Properties.of(net.minecraft.world.level.material.Material.STONE));}}','BlockItemUtilities':'package org.millenaire.common.utilities;public class BlockItemUtilities {public static boolean isFence(net.minecraft.world.level.block.Block b){return false;}public static boolean isWoodenDoor(net.minecraft.world.level.block.Block b){return false;}public static boolean isFenceGate(net.minecraft.world.level.block.Block b){return false;}}','BlockStateUtilities':'package org.millenaire.common.utilities;public class BlockStateUtilities {}','ThreadSafeUtilities':'''package org.millenaire.common.utilities;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.*;public class ThreadSafeUtilities {public static class ChunkAccessException extends Exception {}public static BlockState getBlockState(Level w,int x,int y,int z)throws ChunkAccessException{throw new AssertionError("Unexpected world access");}public static Block getBlock(Level w,int x,int y,int z)throws ChunkAccessException{throw new AssertionError("Unexpected world access");}public static boolean isBlockPassable(Block b,Level w,int x,int y,int z)throws ChunkAccessException{throw new AssertionError("Unexpected world access");}}'''}
for n,s in files.items():(f/(n+'.java')).write_text(s,encoding='utf-8')
(r/'tools/PathAdapterCheck.java').write_text('''import org.millenaire.common.pathing.atomicstryker.*;import net.minecraft.world.level.pathfinder.Node;import java.util.*;
public class PathAdapterCheck {
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var config=new AStarConfig(false,false,false,false,false);
  var input=List.of(new AStarNode(3,-40,0),new AStarNode(2,-40,0),new AStarNode(1,-40,0));
  var p=AStarStatic.translateAStarPathtoPathEntity(null,input,config);
  check(input.size()==3&&p.getNodeCount()==3&&p.getNode(0).x==1&&p.getEndNode().x==3,"Both endpoints and unmodified input");
  check(p.canReach()&&p.getTarget().getX()==3&&p.getTarget().getY()==-40,"Target and negative height");
  p.advance();check(p.getCurrentTargetPathPoint().x==2&&p.getPreviousTargetPathPoint().x==1,"Vanilla advancement");p.advancePathIndex();check(p.getCurrentTargetPathPoint().x==3,"Custom advancement stays synchronized");p.advance();p.advance();check(p.isDone()&&p.getNextNodeIndex()==3&&p.getCurrentTargetPathPoint()==null,"Completion bounded");
  p.setNextNodeIndex(-10);check(p.getNextNodeIndex()==0&&p.getPastTargetPathPoint(1)==null&&p.getFuturePathPoint(-1)==null,"Invalid index/lookahead guards");
  p.replaceNode(1,new Node(9,-40,0));check(p.getNextTargetPathPoint().x==9,"Navigation follows replaced node");p.truncateNodes(1);check(p.getNextTargetPathPoint()==null&&p.getNodeCount()==1,"Truncatable mutable route");
  var single=AStarStatic.translateAStarPathtoPathEntity(null,List.of(new AStarNode(-2,-20,5)),config);check(single.getNodeCount()==1,"Singleton preserved");
  var empty=AStarStatic.translateAStarPathtoPathEntity(null,List.of(),config);check(empty.isDone()&&!empty.canReach()&&empty.getCurrentTargetPathPoint()==null,"Empty route");
  var point=new AS_PathPoint(1,2,3);point.setIndex(7);point.setTotalPathDistance(8);point.setDistanceToNext(9);point.setDistanceToTarget(10);point.setPrevious(p.getNode(0));check(point.heapIdx==7&&point.g==8&&point.h==9&&point.f==10&&point.cameFrom==p.getNode(0),"Named node fields");
  System.out.println("PASS: actual path adapters and straight-route conversion; endpoints, immutable input, negative height, advancement, replacement, truncation, singleton/empty routes and node fields. World collision reads are fixtures; threaded pathfinding and live navigation untested.");
 }
}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/path-adapter-check';o.mkdir(exist_ok=True);q=lambda x:'"'+str(x).replace('\\','/')+'"'
a=['-encoding','UTF-8','-cp',cp,'-d',o,r/'tools/PathAdapterCheck.java']+[r/('src/main/java/org/millenaire/common/pathing/atomicstryker/'+n+'.java') for n in ['AS_PathEntity','AS_PathPoint','AStarStatic','AStarNode','AStarConfig']]+list(f.glob('*.java'))
(r/'build/path-adapter-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8');(r/'build/path-adapter-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'PathAdapterCheck'])),encoding='utf-8')
