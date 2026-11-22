package org.millenaire.client.gui;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.ui.firepit.ContainerFirePit;

public class GuiFirePit extends AbstractContainerScreen<ContainerFirePit> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("millenaire", "textures/gui/firepit.png");
    private static final int[][] ARROWS = {{77, 22, 23, 31, 8}, {71, 28, 37, 14, 16}, {77, 42, 23, 31, 8}};
    private final TileEntityFirePit firePit;
    public GuiFirePit(ContainerFirePit menu, Inventory inventory, Component title) {
        super(menu, inventory, title); firePit = menu.getFirePit(); imageWidth = 176; imageHeight = 175; inventoryLabelY = imageHeight - 94;
    }
    public GuiFirePit(Player player, TileEntityFirePit pit) { this(new ContainerFirePit(player, pit), player.getInventory(), pit.getDisplayName()); }
    @Override protected void renderBg(PoseStack pose, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.setShaderTexture(0, TEXTURE);
        blit(pose, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (firePit.getBurnTime() > 0) {
            int total = firePit.getTotalBurnTime() == 0 ? 200 : firePit.getTotalBurnTime();
            int burn = Math.min(13, firePit.getBurnTime() * 13 / total);
            blit(pose, leftPos + 81, topPos + 54 + 12 - burn, imageWidth, 12 - burn, 14, burn + 1);
        }
        for (int i = 0; i < 3; i++) {
            int[] data = ARROWS[i]; int progress = Math.min(data[2], firePit.getCookTime(i) * data[2] / 200);
            blit(pose, leftPos + data[0], topPos + data[1], imageWidth, data[3], progress, data[4]);
        }
    }
    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        renderBackground(pose); super.render(pose, mouseX, mouseY, partialTicks); renderTooltip(pose, mouseX, mouseY);
    }
}
