import org.millenaire.common.pathing.atomicstryker.*;import net.minecraft.world.level.pathfinder.Node;import java.util.*;
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
}