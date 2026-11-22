from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
out=Path('work/path-worker-check').resolve();out.mkdir(exist_ok=True)
src=out/'PathWorkerCheck.java'
src.write_text('''import java.util.ArrayList;
import org.millenaire.common.pathing.atomicstryker.*;
import org.millenaire.common.utilities.ThreadSafeUtilities;
public class PathWorkerCheck {
 static class Planner extends AStarPathPlannerJPS {
  int found,failed; boolean throwCallback;
  Planner(){super(null,null,false);}
  public void onFoundPath(ArrayList<AStarNode> path){found++;if(throwCallback)throw new IllegalStateException("callback");}
  public void onNoPathAvailable(){failed++;if(throwCallback)throw new IllegalStateException("callback");}
 }
 static class Worker extends AStarWorker {
  int mode; Worker(Planner p,int m){super(p);mode=m;setup(null,null,null,new AStarConfig(false,false,false,false,false));}
  public ArrayList<AStarNode> getPath(AStarNode a,AStarNode b,boolean c)throws ThreadSafeUtilities.ChunkAccessException {
   if(!isBusy()||!isRunning)throw new AssertionError("flags during search");
   if(mode==2)throw new ThreadSafeUtilities.ChunkAccessException("test",0,0);
   if(mode==3)throw new IllegalArgumentException("test");
   return mode==0 ? new ArrayList<>() : null;
  }
 }
 public static void main(String[] args){
  for(int mode=0;mode<4;mode++)for(boolean callbackThrows:new boolean[]{false,true}){
   Planner p=new Planner();p.throwCallback=callbackThrows;Worker w=new Worker(p,mode);
   boolean caught=false;try{w.run();}catch(IllegalStateException ex){caught=true;}
   if(caught!=callbackThrows||p.found!=(mode==0?1:0)||p.failed!=(mode==0?0:1)||w.isBusy()||w.isRunning)throw new AssertionError("mode "+mode);
  }
  System.out.println("PASS: actual AStarWorker, success/null/chunk error/runtime error, exactly one callback, flags cleared even if callback throws (8 cases).");
 }
}''')
# Isolate logging from mod bootstrap; path worker and planner remain actual compiled classes.
stub=out/'stubs/org/millenaire/common/utilities/MillLog.java';stub.parent.mkdir(parents=True,exist_ok=True)
stub.write_text('''package org.millenaire.common.utilities; public class MillLog {
 public static void error(Object o,String s){} public static int printException(String s,Throwable t){return 0;}
 public static void printException(Throwable t){} public static void minor(Object o,String s){}
}''')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(out),str(stub),str(src)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'PathWorkerCheck'],check=True)
