import org.millenaire.client.forge.MillKeyBindings;import net.minecraft.client.KeyMapping;import com.mojang.blaze3d.platform.InputConstants;import net.minecraftforge.client.settings.KeyConflictContext;import org.lwjgl.glfw.GLFW;
public class ClientKeyCheck {
 public static void main(String[] args){
  var keys=new KeyMapping[]{MillKeyBindings.MENU,MillKeyBindings.VILLAGES,MillKeyBindings.ESCORTS};int[] codes={GLFW.GLFW_KEY_M,GLFW.GLFW_KEY_V,GLFW.GLFW_KEY_G};
  for(int i=0;i<keys.length;i++)if(keys[i].getDefaultKey().getValue()!=codes[i] || keys[i].getKeyConflictContext()!=KeyConflictContext.IN_GAME || !keys[i].getCategory().equals("key.category.millenaire"))throw new AssertionError("Defaults or context");
  var key=MillKeyBindings.ESCORTS;key.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K));if(!key.matches(GLFW.GLFW_KEY_K,0) || key.matches(GLFW.GLFW_KEY_G,0))throw new AssertionError("Keyboard remapping");
  key.setKey(InputConstants.Type.MOUSE.getOrCreate(3));if(!key.matchesMouse(3) || key.matches(GLFW.GLFW_KEY_G,0))throw new AssertionError("Mouse remapping");key.setKey(key.getDefaultKey());
  System.out.println("PASS: M/V/G GLFW defaults, in-game conflict contexts, category, keyboard and mouse remapping. Client setup registration, hardware input and server actions not exercised.");
 }
}
