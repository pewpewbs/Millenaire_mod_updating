from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
cp=(r/'build/migration-classpath.txt').read_text().strip()
cp+=';'+ ';'.join(str(p) for p in (r/'.gradle-user-home/caches/forge_gradle/maven_downloader/org/lwjgl').rglob('*natives-windows.jar'))
out=r/'build/client-key-names-check';out.mkdir(exist_ok=True)
test=r/'tools/LegacyKeyNamesCheck.java'
test.write_text('''import org.millenaire.client.forge.LegacyKeyNames;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
public class LegacyKeyNamesCheck {
 static void check(String name,int expected) {
  int actual=LegacyKeyNames.parse(name);
  if(actual!=expected)throw new AssertionError(name+": "+actual+" != "+expected);
 }
 public static void main(String[] args)throws Exception {
  check("M",GLFW.GLFW_KEY_M);check(" v ",GLFW.GLFW_KEY_V);
  check("LCONTROL",GLFW.GLFW_KEY_LEFT_CONTROL);check("RMENU",GLFW.GLFW_KEY_RIGHT_ALT);
  check("RETURN",GLFW.GLFW_KEY_ENTER);check("BACK",GLFW.GLFW_KEY_BACKSPACE);
  check("PRIOR",GLFW.GLFW_KEY_PAGE_UP);check("NEXT",GLFW.GLFW_KEY_PAGE_DOWN);
  check("NUMPADENTER",GLFW.GLFW_KEY_KP_ENTER);check("NUMPADEQUALS",GLFW.GLFW_KEY_KP_EQUAL);
  for(int i=0;i<=9;i++)check("NUMPAD"+i,GLFW.GLFW_KEY_KP_0+i);
  for(int i=1;i<=25;i++)check("F"+i,GLFW.GLFW_KEY_F1+i-1);
  check(null,-1);check("",-1);check("NONE",-1);check("bad-key",-1);
  check("key.keyboard.unknown",-1);
  int count=0;
  for(var f:GLFW.class.getFields())if(f.getName().startsWith("GLFW_KEY_")&&!f.getName().equals("GLFW_KEY_LAST")) {
   int code=f.getInt(null);
   check(InputConstants.Type.KEYSYM.getOrCreate(code).getName(),code);count++;
  }
  System.out.println("PASS: legacy key aliases, numpad/function keys, invalid names and "+count+" GLFW key save/load roundtrips. No physical input or client startup test.");
 }
}
''',encoding='utf-8')
subprocess.run([str(j/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(out),str(test),str(r/'src/main/java/org/millenaire/client/forge/LegacyKeyNames.java')],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'LegacyKeyNamesCheck'],check=True)
