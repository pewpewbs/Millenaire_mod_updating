package org.millenaire.common.block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
public class BlockMillSandstoneDecorated extends BlockMillSandstone {
    public BlockMillSandstoneDecorated(String name) { super(name); }
    @Override @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.cutout()); }
}
