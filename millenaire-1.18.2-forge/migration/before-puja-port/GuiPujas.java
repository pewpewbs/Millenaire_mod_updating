/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.resources.I18n
 *  net.minecraft.client.util.ITooltipFlag
 *  net.minecraft.client.util.ITooltipFlag$TooltipFlags
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.client.event.GuiContainerEvent$DrawForeground
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.fml.client.config.GuiUtils
 *  net.minecraftforge.fml.common.eventhandler.Event
 *  org.lwjgl.opengl.GL11
 */
package org.millenaire.client.gui;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.eventbus.api.Event;
import org.lwjgl.opengl.GL11;
import org.millenaire.client.network.ClientSender;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.ui.ContainerPuja;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;

public class GuiPujas
extends AbstractContainerScreen {
    private static final ResourceLocation texturePujas = new ResourceLocation("millenaire", "textures/gui/pujas.png");
    private static final ResourceLocation textureSacrifices = new ResourceLocation("millenaire", "textures/gui/mayansacrifices.png");
    private final Building temple;
    private final Player player;
    private final Method drawSlotInventory;
    private final Method drawItemStackInventory;

    public GuiPujas(Player player, Building temple) {
        super((AbstractContainerMenu)new ContainerPuja(player, temple));
        this.imageHeight = 188;
        this.temple = temple;
        this.player = player;
        if (MillConfigValues.LogPujas >= 3) {
            MillLog.debug((Object)this, "Opening shrine GUI");
        }
        this.drawSlotInventory = MillCommonUtilities.getDrawSlotInventoryMethod(this);
        this.drawItemStackInventory = MillCommonUtilities.getDrawItemStackInventoryMethod(this);
    }

    protected void func_146976_a(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        if (this.temple.pujas != null && this.temple.pujas.type == 1) {
            this.minecraft.getTextureManager().func_110577_a(textureSacrifices);
        } else {
            this.minecraft.getTextureManager().func_110577_a(texturePujas);
        }
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.func_73729_b(x, y, 0, 0, this.imageWidth, this.imageHeight);
        if (this.temple.pujas != null) {
            int linePos = 0;
            int colPos = 0;
            for (int cp = 0; cp < this.temple.pujas.getTargets().size(); ++cp) {
                if (this.temple.pujas.currentTarget == this.temple.pujas.getTargets().get(cp)) {
                    this.func_73729_b(x + this.getTargetXStart() + colPos * this.getButtonWidth(), y + this.getTargetYStart() + this.getButtonHeight() * linePos, this.temple.pujas.getTargets().get((int)cp).startXact, this.temple.pujas.getTargets().get((int)cp).startYact, this.getButtonWidth(), this.getButtonHeight());
                } else {
                    this.func_73729_b(x + this.getTargetXStart() + colPos * this.getButtonWidth(), y + this.getTargetYStart() + this.getButtonHeight() * linePos, this.temple.pujas.getTargets().get((int)cp).startX, this.temple.pujas.getTargets().get((int)cp).startY, this.getButtonWidth(), this.getButtonHeight());
                }
                if (++colPos < this.getNbPerLines()) continue;
                colPos = 0;
                ++linePos;
            }
            int progress = this.temple.pujas.getPujaProgressScaled(13);
            this.func_73729_b(x + 27, y + 39 + 13 - progress, 176, 13 - progress, 15, progress);
            progress = this.temple.pujas.getOfferingProgressScaled(16);
            this.func_73729_b(x + 84, y + 63 + 16 - progress, 176, 47 - progress, 19, progress);
        }
    }

    protected void func_146979_b(int mouseX, int mouseY) {
        if (this.temple.pujas.type == 1) {
            this.font.func_78276_b(LanguageUtilities.string("sacrifices.offering"), 8, 6, 0x404040);
            this.font.func_78276_b(LanguageUtilities.string("sacrifices.panditfee"), 8, 75, 0x404040);
        } else {
            this.font.func_78276_b(LanguageUtilities.string("pujas.offering"), 8, 6, 0x404040);
            this.font.func_78276_b(LanguageUtilities.string("pujas.panditfee"), 8, 75, 0x404040);
        }
        this.font.func_78276_b(I18n.get((String)"container.inventory", (Object[])new Object[0]), 8, this.imageHeight - 94 + 2, 0x404040);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        int k;
        this.func_146276_q_();
        int i = this.leftPos;
        int j = this.topPos;
        this.func_146976_a(partialTicks, mouseX, mouseY);
        GlStateManager.func_179101_C();
        Lighting.func_74518_a();
        GlStateManager.func_179140_f();
        GlStateManager.func_179097_i();
        try {
            for (k = 0; k < this.field_146292_n.size(); ++k) {
                Button guibutton = (Button)this.field_146292_n.get(k);
                guibutton.func_191745_a(this.minecraft, i, j, partialTicks);
            }
        }
        catch (Exception e) {
            MillLog.printException("Exception in button rendering: ", e);
        }
        Lighting.func_74520_c();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)i, (float)j, (float)0.0f);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179091_B();
        k = 240;
        int l = 240;
        OpenGlHelper.func_77475_a((int)OpenGlHelper.field_77476_b, (float)240.0f, (float)240.0f);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        Slot hoveredSlot = null;
        for (int i1 = 0; i1 < this.menu.slots.size(); ++i1) {
            Slot slot1 = (Slot)this.menu.slots.get(i1);
            this.drawSlotInventory(slot1);
            if (!this.getIsMouseOverSlot(slot1, mouseX, mouseY)) continue;
            hoveredSlot = slot1;
            GlStateManager.func_179140_f();
            GlStateManager.func_179097_i();
            int j1 = hoveredSlot.x;
            int k1 = hoveredSlot.y;
            GlStateManager.func_179135_a((boolean)true, (boolean)true, (boolean)true, (boolean)false);
            this.func_73733_a(j1, k1, j1 + 16, k1 + 16, -2130706433, -2130706433);
            GlStateManager.func_179135_a((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            GlStateManager.func_179145_e();
            GlStateManager.func_179126_j();
        }
        Lighting.func_74518_a();
        this.func_146979_b(mouseX, mouseY);
        Lighting.func_74520_c();
        MinecraftForge.EVENT_BUS.post((Event)new GuiContainerEvent.DrawForeground((AbstractContainerScreen)this, mouseX, mouseY));
        Inventory inventoryplayer = this.minecraft.player.inventory;
        if (inventoryplayer.func_70445_o() != null) {
            try {
                this.drawItemStackInventory.invoke((Object)this, inventoryplayer.func_70445_o(), i - 240 - 8, j - 240 - 8, null);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                MillLog.printException(e);
            }
        }
        GlStateManager.func_179121_F();
        GlStateManager.func_179145_e();
        GlStateManager.func_179126_j();
        Lighting.func_74519_b();
        if (inventoryplayer.func_70445_o().isEmpty() && hoveredSlot != null) {
            ArrayList<String> list = null;
            ItemStack itemstack = null;
            if (hoveredSlot.hasItem()) {
                itemstack = hoveredSlot.getItem();
                list = itemstack.getTooltipLines((Player)this.minecraft.player, (TooltipFlag)(this.minecraft.options.advancedItemTooltips ? TooltipFlag.TooltipFlags.ADVANCED : TooltipFlag.TooltipFlags.NORMAL));
                this.renderToolTipCustom(itemstack, mouseX, mouseY, list);
            } else if (hoveredSlot instanceof ContainerPuja.OfferingSlot) {
                list = new ArrayList<String>();
                list.add("\u00a76" + LanguageUtilities.string("pujas.offeringslot"));
                list.add("\u00a77" + LanguageUtilities.string("pujas.offeringslot2"));
                this.renderToolTipCustom(itemstack, mouseX, mouseY, list);
            } else if (hoveredSlot instanceof ContainerPuja.MoneySlot) {
                list = new ArrayList();
                list.add("\u00a76" + LanguageUtilities.string("pujas.moneyslot"));
                this.renderToolTipCustom(itemstack, mouseX, mouseY, list);
            } else if (hoveredSlot instanceof ContainerPuja.ToolSlot) {
                list = new ArrayList();
                list.add("\u00a76" + LanguageUtilities.string("pujas.toolslot"));
                this.renderToolTipCustom(itemstack, mouseX, mouseY, list);
            }
        }
        int startx = (this.width - this.imageWidth) / 2;
        int starty = (this.height - this.imageHeight) / 2;
        if (this.temple.pujas != null) {
            int linePos = 0;
            int colPos = 0;
            for (int cp = 0; cp < this.temple.pujas.getTargets().size(); ++cp) {
                if (mouseX > startx + this.getTargetXStart() + colPos * this.getButtonWidth() && mouseX < startx + this.getTargetXStart() + (colPos + 1) * this.getButtonWidth() && mouseY > starty + this.getTargetYStart() + this.getButtonHeight() * linePos && mouseY < starty + this.getTargetYStart() + this.getButtonHeight() * (linePos + 1)) {
                    String s = LanguageUtilities.string(this.temple.pujas.getTargets().get((int)cp).mouseOver);
                    int stringlength = this.font.width(s);
                    this.func_73733_a(mouseX + 5, mouseY - 3, mouseX + stringlength + 10, mouseY + 8 + 3, -1073741824, -1073741824);
                    this.font.func_78276_b(s, mouseX + 8, mouseY, 0xF0F0F0);
                }
                if (++colPos < this.getNbPerLines()) continue;
                colPos = 0;
                ++linePos;
            }
        }
    }

    public void drawSlotInventory(Slot slot) {
        try {
            this.drawSlotInventory.invoke((Object)this, slot);
        }
        catch (Exception e) {
            MillLog.printException("Exception when trying to access drawSlotInventory", e);
        }
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

    private boolean getIsMouseOverSlot(Slot slot, int i, int j) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        return (i -= k) >= slot.x - 1 && i < slot.x + 16 + 1 && (j -= l) >= slot.y - 1 && j < slot.y + 16 + 1;
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

    protected void func_73864_a(int x, int y, int par3) throws IOException {
        super.func_73864_a(x, y, par3);
        int startx = (this.width - this.imageWidth) / 2;
        int starty = (this.height - this.imageHeight) / 2;
        if (this.temple.pujas != null) {
            int linePos = 0;
            int colPos = 0;
            for (int cp = 0; cp < this.temple.pujas.getTargets().size(); ++cp) {
                if (x > startx + this.getTargetXStart() + colPos * this.getButtonWidth() && x < startx + this.getTargetXStart() + (colPos + 1) * this.getButtonWidth() && y > starty + this.getTargetYStart() + this.getButtonHeight() * linePos && y < starty + this.getTargetYStart() + this.getButtonHeight() * (linePos + 1)) {
                    ClientSender.pujasChangeEnchantment(this.player, this.temple, cp);
                }
                if (++colPos < this.getNbPerLines()) continue;
                colPos = 0;
                ++linePos;
            }
        }
    }

    protected void renderToolTipCustom(ItemStack stack, int x, int y, List<String> customToolTip) {
        if (stack == null) {
            stack = ItemStack.EMPTY;
        }
        Font font = stack.getItem().getFontRenderer(stack);
        GuiUtils.preItemToolTip((ItemStack)stack);
        this.drawHoveringText(customToolTip, x, y, font == null ? this.font : font);
        GuiUtils.postItemToolTip();
    }
}

