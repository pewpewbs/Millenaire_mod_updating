package org.millenaire.client.forge;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.item.LegacyItemStacks;

@Mod.EventBusSubscriber(modid = "millenaire", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LegacyVariantModels {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.<org.millenaire.common.ui.ContainerTrade,org.millenaire.client.gui.GuiTrade>register(org.millenaire.common.ui.MillMenus.TRADE, org.millenaire.client.gui.GuiTrade::new);
            net.minecraft.client.gui.screens.MenuScreens.<org.millenaire.common.ui.ContainerPuja,org.millenaire.client.gui.GuiPujas>register(org.millenaire.common.ui.MillMenus.PUJA, org.millenaire.client.gui.GuiPujas::new);
            net.minecraft.client.gui.screens.MenuScreens.<org.millenaire.common.ui.ContainerLockedChest,org.millenaire.client.gui.GuiLockedChest>register(org.millenaire.common.ui.MillMenus.LOCKED_CHEST,org.millenaire.client.gui.GuiLockedChest::new);
            net.minecraft.client.gui.screens.MenuScreens.<org.millenaire.common.ui.firepit.ContainerFirePit, org.millenaire.client.gui.GuiFirePit>register(org.millenaire.common.ui.MillMenus.FIRE_PIT, org.millenaire.client.gui.GuiFirePit::new);
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> item instanceof org.millenaire.common.item.ItemMillenaireBow).forEach(item -> {
                    ItemProperties.register(item,new ResourceLocation("pull"),(stack,level,entity,seed) -> entity!=null && entity.getUseItem()==stack?(stack.getUseDuration()-entity.getUseItemRemainingTicks())/20.0f:0.0f);
                    ItemProperties.register(item,new ResourceLocation("pulling"),(stack,level,entity,seed) -> entity!=null && entity.isUsingItem() && entity.getUseItem()==stack?1.0f:0.0f);
                });
            ItemProperties.registerGeneric(new ResourceLocation("millenaire", "variant"),
                (stack, level, entity, seed) -> LegacyItemStacks.metadata(stack));
            ItemBlockRenderTypes.setRenderLayer(MillBlocks.SOD, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(MillBlocks.ICE_BRICK, RenderType.translucent());
            net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues().stream().filter(block -> block instanceof org.millenaire.common.block.BlockFruitLeaves).forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped()));
            net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues().stream().filter(block -> block instanceof org.millenaire.common.block.BlockMillSapling || block instanceof org.millenaire.common.block.BlockMillCrops || block instanceof org.millenaire.common.block.IBlockPath || block instanceof org.millenaire.common.block.BlockOrientedSlabDoubleDecorated).forEach(block -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(block, net.minecraft.client.renderer.RenderType.cutout()));

        });
    }
    @SubscribeEvent public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(org.millenaire.common.entity.MillBlockEntities.LOCKED_CHEST,org.millenaire.client.render.TileEntityLockedChestRenderer::new);
        event.registerBlockEntityRenderer(org.millenaire.common.entity.MillBlockEntities.FIRE_PIT, org.millenaire.client.render.TESRFirePit::new);
        event.registerBlockEntityRenderer(org.millenaire.common.entity.MillBlockEntities.MOCK_BANNER, org.millenaire.client.render.TESRMockBanner::new);
        event.registerBlockEntityRenderer(org.millenaire.common.entity.MillBlockEntities.PANEL, org.millenaire.client.render.TESRPanel::new);
    }
}
