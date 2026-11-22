import org.millenaire.client.forge.LegacyKeyNames;
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
