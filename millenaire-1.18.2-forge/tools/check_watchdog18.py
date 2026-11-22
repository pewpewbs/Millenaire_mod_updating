from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge'); j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
o=Path('work/watchdog18-check').resolve();o.mkdir(exist_ok=True)
s=o/'WatchdogCheck.java'
s.write_text('''import org.millenaire.common.pathing.MovementWatchdog;
import static org.millenaire.common.pathing.MovementWatchdog.Action.*;
class WatchdogCheck {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  var w=new MovementWatchdog();
  check(w.sample(0,"home",0,0,0)==NONE);
  for(int t=1;t<240;t++) check(w.sample(t,"home",0,0,0)==(t==80||t==160?REPATH:NONE));
  check(w.sample(240,"home",0,0,0)==RECOVER);
  check(w.sample(241,"home",0,0,0)==NONE);
  w.reset();
  // Walking away from the destination to go around a wall is still progress.
  for(int t=0;t<1000;t++)check(w.sample(t,"fort",-t*0.03,0,0)==NONE);
  w.reset();w.sample(1000,"a",0,0,0);
  check(w.sample(1080,"b",0,0,0)==NONE);
  check(w.sample(100,"b",0,0,0)==NONE);
  w.reset();check(w.sample(9000,"b",0,0,0)==NONE);
  System.out.println("PASS stationary repath/recovery, moving detour, changed destination, reset and backwards clock");
 }
}''',encoding='utf-8')
subprocess.run([str(j/'javac.exe'),'-d',str(o),str(r/'src/main/java/org/millenaire/common/pathing/MovementWatchdog.java'),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o),'WatchdogCheck'],check=True)
