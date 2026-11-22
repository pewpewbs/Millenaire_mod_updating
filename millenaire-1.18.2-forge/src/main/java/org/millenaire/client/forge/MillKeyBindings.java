package org.millenaire.client.forge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.ClientRegistry;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="millenaire",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MillKeyBindings {
    private static final String CATEGORY="key.category.millenaire";
    public static final KeyMapping MENU=new KeyMapping("key.menu",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_M,CATEGORY);
    public static final KeyMapping VILLAGES=new KeyMapping("key.villages",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,CATEGORY);
    public static final KeyMapping ESCORTS=new KeyMapping("key.escorts",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,CATEGORY);
    private static boolean registered;
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(MillKeyBindings::register); }
    public static void register() {
        if(registered)return;
        ClientRegistry.registerKeyBinding(MENU);ClientRegistry.registerKeyBinding(VILLAGES);ClientRegistry.registerKeyBinding(ESCORTS);
        registered=true;
    }
}
