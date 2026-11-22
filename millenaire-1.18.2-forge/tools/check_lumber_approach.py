from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
out=Path('work/lumber-approach-check').resolve();out.mkdir(exist_ok=True)
src=out/'LumberCheck.java'
src.write_text('''import org.millenaire.common.pathing.atomicstryker.*;
import net.minecraft.world.level.pathfinder.*;
public class LumberCheck extends AStarWorker {
 boolean accepts(int x,int y,int z,AStarNode target,int height){setup(null,null,target,new AStarConfig(true,false,false,false,true,4,height));return isCoordsEnd(x,y,z,target);}
 public static void main(String[] args){
  LumberCheck w=new LumberCheck();int count=0;
  for(int height:new int[]{20,60})for(int origin:new int[]{-100,0,100}){
   AStarNode target=new AStarNode(origin,80,origin);
   for(int x=origin-6;x<=origin+6;x++)for(int z=origin-6;z<=origin+6;z++){
    boolean ok=w.accepts(x,64,z,target,height);
    if(ok!=(Math.abs(x-origin)<=4&&Math.abs(z-origin)<=4))throw new AssertionError("acceptance");
    if(ok && Math.hypot(x+0.5-origin,z+0.5-origin)>=8)throw new AssertionError("out of reach");count++;
   }
  }
  if(!w.accepts(6,64,6,new AStarNode(2,80,2),60)||Math.hypot(6.5,6.5)<8)throw new AssertionError("old offset");
  AS_PathEntity p=new AS_PathEntity(new Node[]{new Node(1,64,1),new Node(2,64,1)});
  if(!p.canReach()||p.isDone()||p.getNodeCount()!=2||p.getEndNode().x!=2)throw new AssertionError("adaptation");
  p.advance();p.advance();if(!p.isDone())throw new AssertionError("completion");
  System.out.println("PASS: "+count+" lumber tolerance checks, old out-of-range offset reproduced, navigation path adaptation and completion checked.");
 }
}''')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(out),str(src)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'LumberCheck'],check=True)
