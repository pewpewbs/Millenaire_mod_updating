/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiLabel
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.util.ITooltipFlag
 *  net.minecraft.client.util.ITooltipFlag$TooltipFlags
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.ClickType
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
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.eventbus.api.Event;
import org.lwjgl.opengl.GL11;
import org.millenaire.client.gui.text.GuiText;
import org.millenaire.client.gui.text.GuiTravelBook;
import org.millenaire.client.network.ClientSender;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.ui.ContainerTrade;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.UserProfile;

public class GuiTrade
extends AbstractContainerScreen {
    private static final int DONATION_BUTTON_Y = 122;
    private static final int DONATION_BUTTON_X = 8;
    private Building building;
    private MillVillager merchant;
    private final Player player;
    private final UserProfile profile;
    private int sellingRow = 0;
    private int buyingRow = 0;
    private final ContainerTrade container;
    private final Method drawSlotInventory;
    private final Method drawItemStackInventory;
    ResourceLocation background = new ResourceLocation("millenaire", "textures/gui/trade.png");

    public GuiTrade(Player player, Building building) {
        super((AbstractContainerMenu)new ContainerTrade(player, building));
        this.drawSlotInventory = MillCommonUtilities.getDrawSlotInventoryMethod(this);
        this.drawItemStackInventory = MillCommonUtilities.getDrawItemStackInventoryMethod(this);
        this.container = (ContainerTrade)this.menu;
        this.building = building;
        this.player = player;
        this.profile = building.mw.getProfile(player);
        this.imageHeight = 222;
        this.imageWidth = 248;
        this.updateRows(false, 0, 0);
        this.updateRows(true, 0, 0);
    }

    public GuiTrade(Player player, MillVillager merchant) {
        super((AbstractContainerMenu)new ContainerTrade(player, merchant));
        this.drawSlotInventory = MillCommonUtilities.getDrawSlotInventoryMethod(this);
        this.drawItemStackInventory = MillCommonUtilities.getDrawItemStackInventoryMethod(this);
        this.container = (ContainerTrade)this.menu;
        this.merchant = merchant;
        this.player = player;
        this.profile = merchant.mw.getProfile(player);
        this.imageHeight = 222;
        this.imageWidth = 248;
        this.updateRows(false, 0, 0);
        this.updateRows(true, 0, 0);
    }

    protected void func_146284_a(Button button) throws IOException {
        if (button instanceof GuiText.MillGuiButton) {
            Culture culture = this.building != null ? this.building.culture : this.merchant.getCulture();
            GuiTravelBook guiTravelBook = new GuiTravelBook((Player)Minecraft.getInstance().player);
            guiTravelBook.setCallingScreen((Screen)this);
            guiTravelBook.jumpToDetails(culture, GuiText.GuiButtonReference.RefType.CULTURE, null, false);
            Minecraft.getInstance().setScreen((Screen)guiTravelBook);
            return;
        }
        super.func_146284_a(button);
    }

    protected void func_146976_a(float f, int i, int j) {
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.minecraft.getTextureManager().func_110577_a(this.background);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.func_73729_b(x, y, 0, 0, this.imageWidth, this.imageHeight);
        if (this.sellingRow == 0) {
            this.func_73729_b(x + 216, y + 68, 5, 5, 11, 7);
        }
        if (this.buyingRow == 0) {
            this.func_73729_b(x + 216, y + 122, 5, 5, 11, 7);
        }
        if (this.sellingRow >= this.container.nbRowSelling - 2) {
            this.func_73729_b(x + 230, y + 68, 5, 5, 11, 7);
        }
        if (this.buyingRow >= this.container.nbRowBuying - 2) {
            this.func_73729_b(x + 230, y + 122, 5, 5, 11, 7);
        }
        if (!this.profile.donationActivated) {
            this.func_73729_b(x + 8, y + 122, 0, 238, 16, 16);
            this.func_73729_b(x + 8 + 16, y + 122, 16, 222, 16, 16);
        } else {
            this.func_73729_b(x + 8, y + 122, 0, 222, 16, 16);
            this.func_73729_b(x + 8 + 16, y + 122, 16, 238, 16, 16);
        }
    }

    protected void func_146979_b(int x, int y) {
        if (this.building != null) {
            this.font.func_78276_b(this.building.getNativeBuildingName(), 8, 6, 0x404040);
            this.font.func_78276_b(LanguageUtilities.string("ui.wesell") + ":", 8, 22, 0x404040);
            this.font.func_78276_b(LanguageUtilities.string("ui.webuy") + ":", 8, 76, 0x404040);
        } else {
            this.font.func_78276_b(this.merchant.func_70005_c_() + ": " + this.merchant.getNativeOccupationName(), 8, 6, 0x404040);
            this.font.func_78276_b(LanguageUtilities.string("ui.isell") + ":", 8, 22, 0x404040);
        }
        this.font.func_78276_b(LanguageUtilities.string("ui.inventory"), 44, this.imageHeight - 96 + 2, 0x404040);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        int i;
        this.func_146276_q_();
        this.func_146976_a(partialTicks, mouseX, mouseY);
        GlStateManager.func_179101_C();
        Lighting.func_74518_a();
        GlStateManager.func_179140_f();
        GlStateManager.func_179097_i();
        for (i = 0; i < this.field_146292_n.size(); ++i) {
            ((Button)this.field_146292_n.get(i)).func_191745_a(this.minecraft, mouseX, mouseY, partialTicks);
        }
        for (int j = 0; j < this.field_146293_o.size(); ++j) {
            ((GuiLabel)this.field_146293_o.get(j)).func_146159_a(this.minecraft, mouseX, mouseY);
        }
        i = this.leftPos;
        int j = this.topPos;
        Lighting.func_74520_c();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)i, (float)j, (float)0.0f);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179091_B();
        Slot hoveredSlot = null;
        String currentProblemString = null;
        int k = 240;
        int l = 240;
        OpenGlHelper.func_77475_a((int)OpenGlHelper.field_77476_b, (float)240.0f, (float)240.0f);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        for (int i1 = 0; i1 < this.menu.slots.size(); ++i1) {
            ContainerTrade.MerchantSlot tslot;
            int l1;
            int j1;
            Slot slot = (Slot)this.menu.slots.get(i1);
            if (slot.isActive()) {
                try {
                    this.drawSlotInventory.invoke((Object)this, slot);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    MillLog.printException(e);
                }
            }
            String problem = null;
            if (slot instanceof ContainerTrade.TradeSlot) {
                ContainerTrade.TradeSlot tslot2 = (ContainerTrade.TradeSlot)slot;
                problem = tslot2.isProblem();
                if (problem != null) {
                    GlStateManager.func_179140_f();
                    GlStateManager.func_179097_i();
                    j1 = slot.x;
                    l1 = slot.y;
                    this.func_73733_a(j1, l1, j1 + 16, l1 + 16, Integer.MIN_VALUE, Integer.MIN_VALUE);
                    GlStateManager.func_179145_e();
                    GlStateManager.func_179126_j();
                }
            } else if (slot instanceof ContainerTrade.MerchantSlot && (problem = (tslot = (ContainerTrade.MerchantSlot)slot).isProblem()) != null) {
                GlStateManager.func_179140_f();
                GlStateManager.func_179097_i();
                j1 = slot.x;
                l1 = slot.y;
                this.func_73733_a(j1, l1, j1 + 16, l1 + 16, Integer.MIN_VALUE, Integer.MIN_VALUE);
                GlStateManager.func_179145_e();
                GlStateManager.func_179126_j();
            }
            if (!this.getIsMouseOverSlot(slot, mouseX, mouseY)) continue;
            hoveredSlot = slot;
            currentProblemString = problem;
            GlStateManager.func_179140_f();
            GlStateManager.func_179097_i();
            int j12 = slot.x;
            int k1 = slot.y;
            GlStateManager.func_179135_a((boolean)true, (boolean)true, (boolean)true, (boolean)false);
            this.func_73733_a(j12, k1, j12 + 16, k1 + 16, -2130706433, -2130706433);
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
                this.drawItemStackInventory.invoke((Object)this, inventoryplayer.func_70445_o(), i - 240 - 8, j - 240 - 8, currentProblemString);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                MillLog.printException(e);
            }
        }
        GlStateManager.func_179121_F();
        GlStateManager.func_179145_e();
        GlStateManager.func_179126_j();
        Lighting.func_74519_b();
        if (inventoryplayer.func_70445_o().getItem() == Items.AIR && hoveredSlot != null && hoveredSlot.hasItem()) {
            Slot tslot;
            if (hoveredSlot instanceof ContainerTrade.TradeSlot) {
                tslot = (ContainerTrade.TradeSlot)hoveredSlot;
                int price = 0;
                String priceText = null;
                int priceColour = 0;
                price = tslot.sellingSlot ? tslot.good.getCalculatedSellingPrice(this.building, this.player) : tslot.good.getCalculatedBuyingPrice(this.building, this.player);
                priceText = MillCommonUtilities.getShortPrice(price);
                priceColour = MillCommonUtilities.getPriceColourMC(price);
                ItemStack itemstack = hoveredSlot.getItem();
                try {
                    List list = itemstack.getTooltipLines((Player)this.minecraft.player, (TooltipFlag)(this.minecraft.options.advancedItemTooltips ? TooltipFlag.TooltipFlags.ADVANCED : TooltipFlag.TooltipFlags.NORMAL));
                    if (((ContainerTrade.TradeSlot)hoveredSlot).sellingSlot || !this.profile.donationActivated) {
                        list.add("\u00a7" + Integer.toHexString(priceColour) + priceText);
                        list.add(LanguageUtilities.string("ui.repgain", "" + price));
                    } else {
                        list.add("\u00a76" + LanguageUtilities.string("ui.donatinggoods"));
                        list.add(LanguageUtilities.string("ui.repgain", "" + price * 4));
                    }
                    if (currentProblemString != null) {
                        list.add("\u00a74" + currentProblemString);
                    }
                    this.renderToolTipCustom(itemstack, mouseX, mouseY, list);
                }
                catch (Exception e) {
                    MillLog.printException("Exception when rendering tooltip for stack: " + itemstack, e);
                }
            } else if (hoveredSlot instanceof ContainerTrade.MerchantSlot) {
                tslot = (ContainerTrade.MerchantSlot)hoveredSlot;
                String price = MillCommonUtilities.getShortPrice(tslot.good.getCalculatedSellingPrice(this.merchant));
                int priceColour = MillCommonUtilities.getPriceColourMC(tslot.good.getCalculatedSellingPrice(this.merchant));
                ItemStack itemstack = hoveredSlot.getItem();
                List list = itemstack.getTooltipLines((Player)this.minecraft.player, (TooltipFlag)(this.minecraft.options.advancedItemTooltips ? TooltipFlag.TooltipFlags.ADVANCED : TooltipFlag.TooltipFlags.NORMAL));
                list.add("\u00a7" + Integer.toHexString(priceColour) + price);
                if (currentProblemString != null) {
                    list.add("\u00a74" + currentProblemString);
                }
                this.renderToolTipCustom(itemstack, mouseX, mouseY, list);
            } else {
                ItemStack itemstack = hoveredSlot.getItem();
                this.func_146285_a(itemstack, mouseX, mouseY);
            }
        }
        int startx = (this.width - this.imageWidth) / 2;
        int starty = (this.height - this.imageHeight) / 2;
        int dx = mouseX - startx;
        int dy = mouseY - starty;
        if (dy >= 122 && dy <= 138) {
            if (dx >= 8 && dx <= 24) {
                String toolTip = LanguageUtilities.string("ui.trade_buying");
                int stringlength = this.font.width(toolTip);
                this.func_73733_a(mouseX + 5, mouseY - 3, mouseX + stringlength + 10, mouseY + 8 + 3, -1073741824, -1073741824);
                this.font.func_78276_b(toolTip, mouseX + 8, mouseY, 0xFFFFFF);
            } else if (dx >= 24 && dx <= 40) {
                String toolTip = LanguageUtilities.string("ui.trade_donation");
                int stringlength = this.font.width(toolTip);
                this.func_73733_a(mouseX + 5, mouseY - 3, mouseX + stringlength + 10, mouseY + 8 + 3, -1073741824, -1073741824);
                this.font.func_78276_b(toolTip, mouseX + 8, mouseY, 0xFFFFFF);
            }
        }
        this.func_191948_b(mouseX, mouseY);
    }

    private boolean getIsMouseOverSlot(Slot slot, int i, int j) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        return (i -= k) >= slot.x - 1 && i < slot.x + 16 + 1 && (j -= l) >= slot.y - 1 && j < slot.y + 16 + 1;
    }

    protected void slotClicked(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        if (slotIn != null && !(slotIn instanceof ContainerTrade.TradeSlot) && !(slotIn instanceof ContainerTrade.MerchantSlot)) {
            return;
        }
        if (slotIn == null || slotIn.getItem() == null) {
            return;
        }
        super.slotClicked(slotIn, slotId, mouseButton, type);
    }

    public void func_73866_w_() {
        super.func_73866_w_();
        if (this.building != null) {
            int xStart = (this.width - this.imageWidth) / 2;
            int yStart = (this.height - this.imageHeight) / 2;
            this.field_146292_n.add(new GuiText.MillGuiButton(0, xStart + this.imageWidth - 21, yStart + 5, 15, 20, "?"));
        }
    }

    protected void func_73864_a(int x, int y, int clickType) throws IOException {
        if (clickType == 0) {
            int startx = (this.width - this.imageWidth) / 2;
            int starty = (this.height - this.imageHeight) / 2;
            int dx = x - startx;
            int dy = y - starty;
            if (dy >= 68 && dy <= 74) {
                if (dx >= 216 && dx <= 226) {
                    if (this.sellingRow > 0) {
                        --this.sellingRow;
                        this.updateRows(true, 1, this.sellingRow);
                    }
                } else if (dx >= 230 && dx <= 240 && this.sellingRow < this.container.nbRowSelling - 2) {
                    ++this.sellingRow;
                    this.updateRows(true, -1, this.sellingRow);
                }
            } else if (dy >= 122 && dy <= 127) {
                if (dx >= 216 && dx <= 226) {
                    if (this.buyingRow > 0) {
                        --this.buyingRow;
                        this.updateRows(false, 1, this.buyingRow);
                    }
                } else if (dx >= 230 && dx <= 240 && this.buyingRow < this.container.nbRowBuying - 2) {
                    ++this.buyingRow;
                    this.updateRows(false, -1, this.buyingRow);
                }
            }
            if (dy >= 122 && dy <= 138) {
                if (dx >= 8 && dx <= 24) {
                    if (this.profile.donationActivated) {
                        this.profile.donationActivated = false;
                        ClientSender.playerToggleDonation(this.player, this.profile.donationActivated);
                    }
                } else if (dx >= 24 && dx <= 40 && !this.profile.donationActivated) {
                    this.profile.donationActivated = true;
                    ClientSender.playerToggleDonation(this.player, this.profile.donationActivated);
                }
            }
        }
        super.func_73864_a(x, y, clickType);
    }

    protected void renderToolTipCustom(ItemStack stack, int x, int y, List<String> customToolTip) {
        Font font = stack.getItem().getFontRenderer(stack);
        GuiUtils.preItemToolTip((ItemStack)stack);
        this.drawHoveringText(customToolTip, x, y, font == null ? this.font : font);
        GuiUtils.postItemToolTip();
    }

    private void updateRows(boolean selling, int change, int row) {
        int pos = 0;
        for (Object o : this.container.slots) {
            Slot slot = (Slot)o;
            if (slot instanceof ContainerTrade.TradeSlot) {
                ContainerTrade.TradeSlot tradeSlot = (ContainerTrade.TradeSlot)slot;
                if (tradeSlot.sellingSlot != selling) continue;
                tradeSlot.y += 18 * change;
                if (pos / 13 < row || pos / 13 > row + 1) {
                    if (tradeSlot.x > 0) {
                        tradeSlot.x -= 1000;
                    }
                } else if (tradeSlot.x < 0) {
                    tradeSlot.x += 1000;
                }
                ++pos;
                continue;
            }
            if (!(slot instanceof ContainerTrade.MerchantSlot) || !selling) continue;
            ContainerTrade.MerchantSlot merchantSlot = (ContainerTrade.MerchantSlot)slot;
            merchantSlot.y += 18 * change;
            if (pos / 13 < row || pos / 13 > row + 1) {
                if (merchantSlot.x > 0) {
                    merchantSlot.x -= 1000;
                }
            } else if (merchantSlot.x < 0) {
                merchantSlot.x += 1000;
            }
            ++pos;
        }
    }
}

