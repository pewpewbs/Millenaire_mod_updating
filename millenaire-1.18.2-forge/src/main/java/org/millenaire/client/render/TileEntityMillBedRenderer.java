package org.millenaire.client.render;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.*;
import org.millenaire.common.block.MillBlocks;
/** These beds use their block models; the old empty tile renderer is replaced by layer registration. */
@Mod.EventBusSubscriber(modid="millenaire",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class TileEntityMillBedRenderer {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> { ItemBlockRenderTypes.setRenderLayer(MillBlocks.BED_STRAW,RenderType.cutoutMipped()); ItemBlockRenderTypes.setRenderLayer(MillBlocks.BED_CHARPOY,RenderType.cutoutMipped()); });
    }
}
