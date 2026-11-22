package org.millenaire.client.gui;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.Level;
import org.millenaire.common.block.BlockLockedChest;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.ui.ContainerLockedChest;

public class GuiLockedChest extends AbstractContainerScreen<ContainerLockedChest> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("minecraft","textures/gui/container/generic_54.png");
    public GuiLockedChest(ContainerLockedChest menu,Inventory inventory,Component title) { super(menu,inventory,title);imageHeight=114+menu.getRows()*18;inventoryLabelY=imageHeight-94; }
    public static GuiLockedChest createGUI(Level level,int x,int y,int z,Player player) {
        if (!(level.getBlockEntity(new BlockPos(x,y,z)) instanceof TileEntityLockedChest chest) || level.isClientSide && !chest.loaded) return null;
        return new GuiLockedChest(BlockLockedChest.createContainer(level,x,y,z,player),player.getInventory(),chest.getDisplayName());
    }
    @Override public void render(PoseStack pose,int mouseX,int mouseY,float partial) { renderBackground(pose);super.render(pose,mouseX,mouseY,partial);renderTooltip(pose,mouseX,mouseY); }
    @Override protected void renderBg(PoseStack pose,float partial,int mouseX,int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShaderTexture(0,TEXTURE);
        int upper=menu.getRows()*18+17;blit(pose,leftPos,topPos,0,0,imageWidth,upper);blit(pose,leftPos,topPos+upper,0,126,imageWidth,96);
    }
    @Override public boolean mouseClicked(double x,double y,int button) { return !menu.isLocked() && super.mouseClicked(x,y,button); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if (!menu.isLocked()) return super.keyPressed(key,scan,modifiers);
        if (key==256 || minecraft.options.keyInventory.matches(key,scan)) { minecraft.player.closeContainer();return true; }
        return false;
    }
}
