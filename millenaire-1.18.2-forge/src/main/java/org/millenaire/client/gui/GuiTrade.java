package org.millenaire.client.gui;

import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import org.millenaire.client.gui.text.GuiText;
import org.millenaire.client.gui.text.GuiTravelBook;
import org.millenaire.client.network.ClientSender;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.ui.ContainerTrade;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.UserProfile;

public class GuiTrade extends AbstractContainerScreen<ContainerTrade> {
    private final Building building;
    private final MillVillager merchant;
    private final Player player;
    private final UserProfile profile;
    private final ContainerTrade container;
    private int sellingRow, buyingRow;
    private final ResourceLocation background = new ResourceLocation("millenaire", "textures/gui/trade.png");

    public GuiTrade(Player player, Building building) { this(new ContainerTrade(player, building), player.getInventory(), new TextComponent(building.getNativeBuildingName())); }
    public GuiTrade(Player player, MillVillager merchant) { this(new ContainerTrade(player, merchant), player.getInventory(), merchant.getName()); }
    public GuiTrade(ContainerTrade menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        container = menu; building = menu.getBuilding(); merchant = menu.getMerchant(); player = inventory.player;
        profile = (building != null ? building.mw : merchant.mw).getProfile(player);
        imageWidth = 248; imageHeight = 222;
        updateRows(true); updateRows(false);
    }

    @Override protected void init() {
        super.init();
        if (building != null) addRenderableWidget(new Button(leftPos + imageWidth - 21, topPos + 5, 15, 20, new TextComponent("?"), button -> {
            var book = new GuiTravelBook(player);
            book.setCallingScreen(this);
            book.jumpToDetails(building.culture, GuiText.GuiButtonReference.RefType.CULTURE, null, false);
            minecraft.setScreen(book);
        }));
    }

    private static String problem(Slot slot) {
        if (slot instanceof ContainerTrade.TradeSlot trade) return trade.isProblem();
        if (slot instanceof ContainerTrade.MerchantSlot trade) return trade.isProblem();
        return null;
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTicks);
        pose.pushPose();
        try {
            pose.translate(leftPos, topPos, 300);
            for (Slot slot : menu.slots) if (slot.isActive() && problem(slot) != null)
                fill(pose, slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80000000);
        } finally { pose.popPose(); }
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.isActive() && hoveredSlot.hasItem()) {
            var stack = hoveredSlot.getItem();
            var tooltip = new ArrayList<Component>(getTooltipFromItem(stack));
            if (hoveredSlot instanceof ContainerTrade.TradeSlot trade) {
                int price = trade.sellingSlot ? trade.good.getCalculatedSellingPrice(building, player) : trade.good.getCalculatedBuyingPrice(building, player);
                if (trade.sellingSlot || !profile.donationActivated) {
                    tooltip.add(new TextComponent("\u00a7" + Integer.toHexString(MillCommonUtilities.getPriceColourMC(price)) + MillCommonUtilities.getShortPrice(price)));
                    tooltip.add(new TextComponent(LanguageUtilities.string("ui.repgain", "" + price)));
                } else {
                    tooltip.add(new TextComponent("\u00a76" + LanguageUtilities.string("ui.donatinggoods")));
                    tooltip.add(new TextComponent(LanguageUtilities.string("ui.repgain", "" + (long)price * 4)));
                }
            } else if (hoveredSlot instanceof ContainerTrade.MerchantSlot trade) {
                int price = trade.good.getCalculatedSellingPrice(merchant);
                tooltip.add(new TextComponent("\u00a7" + Integer.toHexString(MillCommonUtilities.getPriceColourMC(price)) + MillCommonUtilities.getShortPrice(price)));
            }
            String problem = problem(hoveredSlot);
            if (problem != null) tooltip.add(new TextComponent("\u00a74" + problem));
            renderTooltip(pose, tooltip, stack.getTooltipImage(), mouseX, mouseY, stack);
        }
        int dx = mouseX - leftPos, dy = mouseY - topPos;
        if (building != null && dy >= 122 && dy < 138 && dx >= 8 && dx < 40)
            renderTooltip(pose, new TextComponent(LanguageUtilities.string(dx < 24 ? "ui.trade_buying" : "ui.trade_donation")), mouseX, mouseY);
    }

    @Override protected void slotClicked(Slot slot, int id, int button, ClickType type) {
        // Inventory cells are a read-only view here; offer icons dispatch purchases explicitly.
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double dx = mouseX - leftPos, dy = mouseY - topPos;
        if (button == 0) {
            if (dy >= 68 && dy <= 74 && dx >= 216 && dx <= 240) {
                sellingRow = Math.max(0, Math.min(Math.max(0, container.nbRowSelling - 2), sellingRow + (dx < 228 ? -1 : 1)));
                updateRows(true); return true;
            }
            if (dy >= 122 && dy <= 127 && dx >= 216 && dx <= 240) {
                buyingRow = Math.max(0, Math.min(Math.max(0, container.nbRowBuying - 2), buyingRow + (dx < 228 ? -1 : 1)));
                updateRows(false); return true;
            }
            if (building != null && dy >= 122 && dy < 138 && dx >= 8 && dx < 40) {
                boolean donation = dx >= 24;
                if (profile.donationActivated != donation) {
                    profile.donationActivated = donation;
                    ClientSender.playerToggleDonation(player, donation);
                }
                return true;
            }
        }
        if ((button == 0 || button == 1) && minecraft.gameMode != null) {
            for (Slot slot : menu.slots) {
                if (!(slot instanceof ContainerTrade.TradeSlot || slot instanceof ContainerTrade.MerchantSlot) || !slot.isActive()) continue;
                if (dx >= slot.x && dx < slot.x + 16 && dy >= slot.y && dy < slot.y + 16) {
                    minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, button,
                            hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP, player);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateRows(boolean selling) {
        int pos = 0, row = selling ? sellingRow : buyingRow;
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot old = menu.slots.get(index), replacement;
            int x = 8 + pos % 13 * 18;
            int y = (selling ? 32 : 86) + (pos / 13 - row) * 18;
            if (pos / 13 < row || pos / 13 > row + 1) x = -1000;
            if (old instanceof ContainerTrade.TradeSlot trade) {
                if (trade.sellingSlot != selling) continue;
                replacement = new ContainerTrade.TradeSlot(trade.building, trade.player, trade.sellingSlot, trade.good, x, y);
            } else if (old instanceof ContainerTrade.MerchantSlot trade && selling) {
                replacement = new ContainerTrade.MerchantSlot(trade.merchant, trade.player, trade.good, x, y);
            } else continue;
            replacement.index = old.index;
            menu.slots.set(index, replacement);
            pos++;
        }
    }

    @Override protected void renderBg(PoseStack pose, float f, int i, int j) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, this.background);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.blit(pose, x, y, 0, 0, this.imageWidth, this.imageHeight);
        if (this.sellingRow == 0) {
            this.blit(pose, x + 216, y + 68, 5, 5, 11, 7);
        }
        if (this.buyingRow == 0) {
            this.blit(pose, x + 216, y + 122, 5, 5, 11, 7);
        }
        if (this.sellingRow >= this.container.nbRowSelling - 2) {
            this.blit(pose, x + 230, y + 68, 5, 5, 11, 7);
        }
        if (this.buyingRow >= this.container.nbRowBuying - 2) {
            this.blit(pose, x + 230, y + 122, 5, 5, 11, 7);
        }
        if (!this.profile.donationActivated) {
            this.blit(pose, x + 8, y + 122, 0, 238, 16, 16);
            this.blit(pose, x + 8 + 16, y + 122, 16, 222, 16, 16);
        } else {
            this.blit(pose, x + 8, y + 122, 0, 222, 16, 16);
            this.blit(pose, x + 8 + 16, y + 122, 16, 238, 16, 16);
        }
    }

    @Override protected void renderLabels(PoseStack pose, int x, int y) {
        if (this.building != null) {
            this.font.draw(pose, this.building.getNativeBuildingName(), 8, 6, 0x404040);
            this.font.draw(pose, LanguageUtilities.string("ui.wesell") + ":", 8, 22, 0x404040);
            this.font.draw(pose, LanguageUtilities.string("ui.webuy") + ":", 8, 76, 0x404040);
        } else {
            this.font.draw(pose, this.merchant.func_70005_c_() + ": " + this.merchant.getNativeOccupationName(), 8, 6, 0x404040);
            this.font.draw(pose, LanguageUtilities.string("ui.isell") + ":", 8, 22, 0x404040);
        }
        this.font.draw(pose, LanguageUtilities.string("ui.inventory"), 44, this.imageHeight - 96 + 2, 0x404040);
    }

}
