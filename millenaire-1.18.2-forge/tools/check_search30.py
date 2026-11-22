from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin');o=Path('work/search30-check').resolve();o.mkdir(exist_ok=True);s=o/'OpenCheck.java'
s.write_text('''package org.millenaire.common.pathing.atomicstryker;
import java.util.*;
class OpenCheck {
 public static void main(String[] args){
  Random random=new Random(30);AStarOpenSet queue=new AStarOpenSet();Map<Integer,Integer> reference=new HashMap<>();
  for(int i=0;i<30000;i++){
   int key=random.nextInt(3000),cost=random.nextInt(50000);
   if(random.nextInt(4)!=0){queue.offer(new AStarNode(key,-64,30000000,cost,null));reference.merge(key,cost,Math::min);}
   else if(!reference.isEmpty()){int expected=Collections.min(reference.values());AStarNode actual=queue.poll();if(actual==null||actual.getG()!=expected||!Objects.equals(reference.remove(actual.x),actual.getG()))throw new AssertionError("heap order/index mismatch");}
  }
  int last=-1;while(!queue.isEmpty()){AStarNode n=queue.poll();if(n.getG()<last||!Objects.equals(reference.remove(n.x),n.getG()))throw new AssertionError("drain");last=n.getG();}
  if(!reference.isEmpty()||queue.peek()!=null||queue.poll()!=null)throw new AssertionError("empty");
  queue.offer(new AStarNode(1,0,0,500,null));queue.offer(new AStarNode(2,0,0,100,null));
  if(!queue.improve(new AStarNode(1,0,0,1,null),null)||queue.poll().x!=1)throw new AssertionError("decrease key must reorder");
  if(queue.improve(new AStarNode(9,0,0,0,null),null))throw new AssertionError("missing candidate");
  System.out.println("PASS 30000 randomized open-set operations, duplicate coordinates, negative/large coordinates, decreased priority, drain and empty reuse");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'org.millenaire.common.pathing.atomicstryker.OpenCheck'],check=True)
