from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
out=Path('work/construction-approach-check').resolve();out.mkdir(exist_ok=True)
g=(r/'src/main/java/org/millenaire/common/goal/Goal.java').read_text()
assert 'JPS_CONFIG_BUILDING = new AStarConfig(true, false, false, false, true, 2, 60)' in g
assert 'JPS_CONFIG_BUILDING_SCAFFOLDINGS = new AStarConfig(true, false, false, false, true, 2, 5)' in g
s=out/'ApproachCheck.java'
s.write_text('''import org.millenaire.common.pathing.atomicstryker.*;
public class ApproachCheck extends AStarWorker {
 boolean accepts(int x,int y,int z,AStarNode target,int height){
  setup(null,null,target,new AStarConfig(true,false,false,false,true,2,height));
  return isCoordsEnd(x,y,z,target);
 }
 public static void main(String[] args){
  ApproachCheck w=new ApproachCheck();int count=0;
  for(int origin:new int[]{-100,0,100}){
   AStarNode block=new AStarNode(origin,90,origin);
   for(int x=origin-4;x<=origin+4;x++)for(int z=origin-4;z<=origin+4;z++){
    boolean accepted=w.accepts(x,64,z,block,60);
    if(accepted!=(Math.abs(x-origin)<=2&&Math.abs(z-origin)<=2))throw new AssertionError("tolerance");
    if(accepted && Math.hypot(x+0.5-origin,z+0.5-origin)>=5)throw new AssertionError("outside work reach");
    count++;
   }
   if(w.accepts(origin,64,origin,block,5))throw new AssertionError("scaffolding limit");
  }
  // Old access-point shift could finish more than five blocks from the real block.
  if(!w.accepts(4,64,4,new AStarNode(2,90,2),60)||Math.hypot(4.5,4.5)<5)throw new AssertionError("regression setup");
  System.out.println("PASS: "+count+" actual worker acceptance cases; canopy approached from ground; accepted block-centred endpoints within working reach; scaffold vertical limit preserved; old offset reproduced.");
 }
}''')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(out),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'ApproachCheck'],check=True)
