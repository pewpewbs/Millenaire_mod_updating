from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin');o=Path('work/scheduler27-check').resolve();o.mkdir(exist_ok=True)
s=o/'Scheduler27Check.java';s.write_text(r'''package org.millenaire.common.pathing.atomicstryker;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class Scheduler27Check {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void waitFor(java.util.function.BooleanSupplier condition)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!condition.getAsBoolean()){if(System.nanoTime()>end)throw new AssertionError("timeout");Thread.sleep(5);}}
 static class Receiver implements IAStarPathedEntity {List<AStarNode> result;int found,failed;public void onFoundPath(List<AStarNode> p){result=p;found++;}public void onNoPathAvailable(){failed++;}}
 static class Planner extends AStarPathPlannerJPS {
  ConcurrentLinkedQueue<Runnable> callbacks=new ConcurrentLinkedQueue<>();List<AStarWorker> workers=new CopyOnWriteArrayList<>();boolean firstFails;
  Planner(Receiver r,boolean jps){super(null,r,jps);}
  protected void dispatch(Runnable r){callbacks.add(r);}
  protected AStarWorker createWorker(boolean classic){var worker=new AStarWorker(this){public ArrayList<AStarNode> getPath(AStarNode start,AStarNode end,boolean mode){if(firstFails&&!classic)return null;return new ArrayList<>(List.of(new AStarNode(end.x,end.y,end.z)));}};workers.add(worker);return worker;}
  void drain(){Runnable r;while((r=callbacks.poll())!=null)r.run();}
 }
 public static void main(String[]args)throws Exception {
  var config=new AStarConfig(true,false,false,false,false);var receiver=new Receiver();var p=new Planner(receiver,false);
  p.getPath(new AStarNode(0,0,0),new AStarNode(10,0,0),config);waitFor(()->!p.callbacks.isEmpty());
  ok(p.isBusy()&&receiver.found==0,"result waits for server delivery");
  p.getPath(new AStarNode(0,0,0),new AStarNode(20,0,0),config);waitFor(()->p.callbacks.size()==2);
  p.drain();ok(receiver.found==1&&receiver.result.get(0).x==20,"obsolete completion discarded");ok(!p.isBusy(),"busy cleared after delivery");
  p.getPath(new AStarNode(0,0,0),new AStarNode(30,0,0),config);waitFor(()->!p.callbacks.isEmpty());p.stopPathSearch(true);p.drain();ok(receiver.found==1&&receiver.failed==0,"cancelled pending callback discarded");
  var r2=new Receiver();var fallback=new Planner(r2,true);fallback.firstFails=true;fallback.getPath(new AStarNode(0,0,0),new AStarNode(5,0,0),config);
  waitFor(()->!fallback.callbacks.isEmpty());fallback.drain();waitFor(()->!fallback.callbacks.isEmpty());fallback.drain();ok(r2.found==1&&r2.failed==0&&fallback.workers.size()==2,"one classic fallback");
  CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);AtomicInteger active=new AtomicInteger(),max=new AtomicInteger();
  Runnable block=()->{int a=active.incrementAndGet();max.accumulateAndGet(a,Math::max);entered.countDown();try{release.await();}catch(InterruptedException cancelled){Thread.currentThread().interrupt();}finally{active.decrementAndGet();}};
  var tasks=new ArrayList<FutureTask<Void>>();tasks.add(PathSearchExecutor.submit(block));tasks.add(PathSearchExecutor.submit(block));ok(entered.await(5,TimeUnit.SECONDS),"two running workers");
  for(int i=0;i<64;i++){var f=PathSearchExecutor.submit(block);ok(f!=null,"bounded queue accepts capacity");tasks.add(f);}
  ok(PathSearchExecutor.submit(block)==null,"overload rejected without caller running search");ok(PathSearchExecutor.metrics()[3]==64&&max.get()==2,"queue/thread bounds");
  for(var f:tasks)PathSearchExecutor.cancel(f);release.countDown();waitFor(()->active.get()==0&&PathSearchExecutor.metrics()[3]==0);
  ok(max.get()<=2,"never exceeded two workers");
  System.out.println("PASS stale completion, cancellation, server delivery, single fallback, 2 workers + 64 queue, rejection and cancellation drain; metrics="+Arrays.toString(PathSearchExecutor.metrics()));
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
raise SystemExit(subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'org.millenaire.common.pathing.atomicstryker.Scheduler27Check']).returncode)
