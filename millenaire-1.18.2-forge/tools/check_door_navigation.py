from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
cp=(r/'build/migration-classpath.txt').read_text().strip();out=r/'build/door-navigation-check';out.mkdir(exist_ok=True)
test=r/'tools/DoorHalvesCheck.java'
test.write_text('''import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.properties.*;import org.millenaire.common.buildingplan.DoorPlacement;
public class DoorHalvesCheck{
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();int n=0;
 for(var state:Blocks.OAK_DOOR.getStateDefinition().getPossibleStates())for(var rotation:Rotation.values())for(boolean invert:new boolean[]{false,true}){
  var rotated=state.rotate(rotation);var lower=DoorPlacement.half(rotated,invert,DoubleBlockHalf.LOWER);var upper=DoorPlacement.half(rotated,invert,DoubleBlockHalf.UPPER);
  if(!lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER).equals(upper))throw new AssertionError("mismatched halves");
  if(lower.getValue(DoorBlock.FACING)!=rotated.getValue(DoorBlock.FACING)||lower.getValue(DoorBlock.OPEN)!=rotated.getValue(DoorBlock.OPEN)||lower.getValue(DoorBlock.POWERED)!=rotated.getValue(DoorBlock.POWERED))throw new AssertionError("changed plan state");
  if(lower.getValue(DoorBlock.HINGE)!=(invert?DoorHingeSide.RIGHT:rotated.getValue(DoorBlock.HINGE)))throw new AssertionError("wrong hinge");n++;
 }System.out.println("PASS: "+n+" actual vanilla door states/rotations/inversion cases preserve facing, opening and power with matching halves. World placement integration not covered.");}}
''',encoding='utf-8')
subprocess.run([str(j/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(out),str(test),str(r/'src/main/java/org/millenaire/common/buildingplan/DoorPlacement.java')],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'DoorHalvesCheck'],check=True)
subprocess.run([str(Path('C:/Users/sviat/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe')),'work/prepare_simple_navigation_check.py'],check=True)
p=r/'tools/SimpleNavigationCheck.java';s=p.read_text()
s=s.replace('boolean shortcut;int probes;Vec3 lastTarget;','boolean shortcut;int probes,stuckChecks;Vec3 lastTarget;\n  protected void doStuckDetection(Vec3 position){stuckChecks++;}')
s=s.replace('check(p.getNextNodeIndex()==1,"Negative-coordinate waypoint reached");','check(p.getNextNodeIndex()==1,"Negative-coordinate waypoint reached");check(nav.stuckChecks==1,"Stuck detection invoked after path following");')
s=s.replace('PathFinder finder(){return createPathFinder(100);}', 'PathFinder finder(){return createPathFinder(100);}\n  void timer(int n){lastStuckCheck=n;} int timer(){return lastStuckCheck;} double speed(){return speedModifier;}')
s=s.replace('nav.shortcut=false;p=path();', 'p=path(new Node(5,0,0));nav.route(p);nav.timer(37);check(nav.moveTo(p,.8)&&nav.timer()==37&&nav.speed()==.8,"Reusing active route preserves stuck timer and updates speed");nav.shortcut=false;p=path();')
p.write_text(s,encoding='utf-8')
subprocess.run([str(j/'javac.exe'),'@'+str(r/'build/simple-navigation-javac.args')],check=True)
subprocess.run([str(j/'java.exe'),'@'+str(r/'build/simple-navigation-java.args')],check=True)
