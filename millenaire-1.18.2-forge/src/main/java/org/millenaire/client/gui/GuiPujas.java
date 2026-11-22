package org.millenaire.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.common.ui.ContainerPuja;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.village.Building;
import java.util.List;

public class GuiPujas extends AbstractContainerScreen<ContainerPuja> {
    private static final ResourceLocation texturePujas = new ResourceLocation("millenaire", "textures/gui/pujas.png");
    private static final ResourceLocation textureSacrifices = new ResourceLocation("millenaire", "textures/gui/mayansacrifices.png");
    private final Building temple;

    public GuiPujas(Player player, Building temple) {
        this(new ContainerPuja(player, temple), player.getInventory(), temple.pujas.getDisplayName());
    }
    public GuiPujas(ContainerPuja menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.temple = menu.shrine.temple;
        this.imageHeight = 188;
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTicks);
        renderTooltip(pose, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem()) {
            List<Component> help = null;
            if (hoveredSlot instanceof ContainerPuja.OfferingSlot) help = List.of(
                    new TextComponent("§6" + LanguageUtilities.string("pujas.offeringslot")),
                    new TextComponent("§7" + LanguageUtilities.string("pujas.offeringslot2")));
            else if (hoveredSlot instanceof ContainerPuja.MoneySlot) help = List.of(new TextComponent("§6" + LanguageUtilities.string("pujas.moneyslot")));
            else if (hoveredSlot instanceof ContainerPuja.ToolSlot) help = List.of(new TextComponent("§6" + LanguageUtilities.string("pujas.toolslot")));
            if (help != null) renderComponentTooltip(pose, help, mouseX, mouseY);
        }
        int target = targetAt(mouseX, mouseY);
        if (target >= 0) renderTooltip(pose, new TextComponent(LanguageUtilities.string(temple.pujas.getTargets().get(target).mouseOver)), mouseX, mouseY);
    }

    private int targetAt(double mouseX, double mouseY) {
        var targets = temple.pujas.getTargets();
        for (int index = 0; index < targets.size(); index++) {
            int x = leftPos + getTargetXStart() + index % getNbPerLines() * getButtonWidth();
            int y = topPos + getTargetYStart() + index / getNbPerLines() * getButtonHeight();
            if (mouseX >= x && mouseX < x + getButtonWidth() && mouseY >= y && mouseY < y + getButtonHeight()) return index;
        }
        return -1;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int target = targetAt(mouseX, mouseY);
        if (button == 0 && target >= 0 && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, target);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void renderBg(PoseStack pose, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (this.temple.pujas != null && this.temple.pujas.type == 1) {
            RenderSystem.setShaderTexture(0, textureSacrifices);
        } else {
            RenderSystem.setShaderTexture(0, texturePujas);
        }
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.blit(pose, x, y, 0, 0, this.imageWidth, this.imageHeight);
        if (this.temple.pujas != null) {
            int linePos = 0;
            int colPos = 0;
            for (int cp = 0; cp < this.temple.pujas.getTargets().size(); ++cp) {
                if (this.temple.pujas.currentTarget == this.temple.pujas.getTargets().get(cp)) {
                    this.blit(pose, x + this.getTargetXStart() + colPos * this.getButtonWidth(), y + this.getTargetYStart() + this.getButtonHeight() * linePos, this.temple.pujas.getTargets().get((int)cp).startXact, this.temple.pujas.getTargets().get((int)cp).startYact, this.getButtonWidth(), this.getButtonHeight());
                } else {
                    this.blit(pose, x + this.getTargetXStart() + colPos * this.getButtonWidth(), y + this.getTargetYStart() + this.getButtonHeight() * linePos, this.temple.pujas.getTargets().get((int)cp).startX, this.temple.pujas.getTargets().get((int)cp).startY, this.getButtonWidth(), this.getButtonHeight());
                }
                if (++colPos < this.getNbPerLines()) continue;
                colPos = 0;
                ++linePos;
            }
            int progress = this.temple.pujas.getPujaProgressScaled(13);
            this.blit(pose, x + 27, y + 39 + 13 - progress, 176, 13 - progress, 15, progress);
            progress = this.temple.pujas.getOfferingProgressScaled(16);
            this.blit(pose, x + 84, y + 63 + 16 - progress, 176, 47 - progress, 19, progress);
        }
    }

    @Override protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        if (this.temple.pujas.type == 1) {
            this.font.draw(pose, LanguageUtilities.string("sacrifices.offering"), 8, 6, 0x404040);
            this.font.draw(pose, LanguageUtilities.string("sacrifices.panditfee"), 8, 75, 0x404040);
        } else {
            this.font.draw(pose, LanguageUtilities.string("pujas.offering"), 8, 6, 0x404040);
            this.font.draw(pose, LanguageUtilities.string("pujas.panditfee"), 8, 75, 0x404040);
        }
        this.font.draw(pose, I18n.get((String)"container.inventory", (Object[])new Object[0]), 8, this.imageHeight - 94 + 2, 0x404040);
    }

    private int getButtonHeight() {
        if (this.temple.pujas == null) {
            return 0;
        }
        if (this.temple.pujas.type == 0) {
            return 17;
        }
        if (this.temple.pujas.type == 1) {
            return 20;
        }
        return 0;
    }

    private int getButtonWidth() {
        if (this.temple.pujas == null) {
            return 0;
        }
        if (this.temple.pujas.type == 0) {
            return 46;
        }
        if (this.temple.pujas.type == 1) {
            return 20;
        }
        return 0;
    }

    private int getNbPerLines() {
        if (this.temple.pujas == null) {
            return 1;
        }
        if (this.temple.pujas.type == 0) {
            return 1;
        }
        if (this.temple.pujas.type == 1) {
            return 3;
        }
        return 1;
    }

    private int getTargetXStart() {
        if (this.temple.pujas == null) {
            return 0;
        }
        if (this.temple.pujas.type == 0) {
            return 118;
        }
        if (this.temple.pujas.type == 1) {
            return 110;
        }
        return 0;
    }

    private int getTargetYStart() {
        if (this.temple.pujas == null) {
            return 0;
        }
        if (this.temple.pujas.type == 0) {
            return 22;
        }
        if (this.temple.pujas.type == 1) {
            return 22;
        }
        return 0;
    }

}
