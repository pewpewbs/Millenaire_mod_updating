/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.util.ResourceLocation
 *  org.lwjgl.input.Keyboard
 */
package org.millenaire.client.gui.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.resources.ResourceLocation;

import org.millenaire.client.book.BookManager;
import org.millenaire.client.book.TextBook;
import org.millenaire.client.book.TextLine;
import org.millenaire.client.forge.ClientProxy;
import org.millenaire.client.gui.text.GuiText;
import org.millenaire.client.network.ClientSender;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;

public class GuiHire
extends GuiText {
    private static final int REPUTATION_NEEDED = 4096;
    public static final int BUTTON_CLOSE = 0;
    public static final int BUTTON_HIRE = 1;
    public static final int BUTTON_EXTEND = 2;
    public static final int BUTTON_RELEASE = 3;
    private final MillVillager villager;
    private final Player player;
    ResourceLocation background = new ResourceLocation("millenaire", "textures/gui/quest.png");

    public GuiHire(Player player, MillVillager villager) {
        this.villager = villager;
        this.player = player;
        this.bookManager = new BookManager(256, 220, 160, 240, new GuiText.FontRendererGUIWrapper(this));
    }

    @Override
    protected void onButtonPressed(Button guibutton) throws IOException {
        if (!guibutton.active) {
            return;
        }
        if (guibutton instanceof GuiText.MillGuiButton) {
            if (buttonId(guibutton) == 0) {
                this.minecraft.setScreen(null);
                
            } else if (buttonId(guibutton) == 1) {
                ClientSender.hireHire(this.player, this.villager);
                this.refreshContent();
            } else if (buttonId(guibutton) == 2) {
                ClientSender.hireExtend(this.player, this.villager);
                this.refreshContent();
            } else if (buttonId(guibutton) == 3) {
                ClientSender.hireRelease(this.player, this.villager);
                this.refreshContent();
            }
        }
        super.onButtonPressed(guibutton);
    }

    @Override
    public void buttonPagination() {
        super.buttonPagination();
        int xStart = (this.width - this.getXSize()) / 2;
        int yStart = (this.height - this.getYSize()) / 2;
        if (this.villager.hiredBy != null) {
            if (MillCommonUtilities.countMoney((Container)this.player.getInventory()) >= this.villager.getHireCost(this.player)) {
                this.addMillButton(new GuiText.MillGuiButton(2, xStart + this.getXSize() / 2 - 100, yStart + this.getYSize() - 40, 63, 20, LanguageUtilities.string("hire.extend")));
            }
            this.addMillButton(new GuiText.MillGuiButton(3, xStart + this.getXSize() / 2 - 32, yStart + this.getYSize() - 40, 64, 20, LanguageUtilities.string("hire.release")));
            this.addMillButton(new GuiText.MillGuiButton(0, xStart + this.getXSize() / 2 + 37, yStart + this.getYSize() - 40, 63, 20, LanguageUtilities.string("hire.close")));
        } else {
            if (this.villager.getTownHall().getReputation(this.player) >= 4096 && MillCommonUtilities.countMoney((Container)this.player.getInventory()) >= this.villager.getHireCost(this.player)) {
                this.addMillButton(new GuiText.MillGuiButton(1, xStart + this.getXSize() / 2 - 100, yStart + this.getYSize() - 40, 95, 20, LanguageUtilities.string("hire.hire")));
            }
            this.addMillButton(new GuiText.MillGuiButton(0, xStart + this.getXSize() / 2 + 5, yStart + this.getYSize() - 40, 95, 20, LanguageUtilities.string("hire.close")));
        }
    }

    @Override
    protected void customDrawBackground(int i, int j, float f) {
    }

    @Override
    protected void customDrawScreen(int i, int j, float f) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private TextBook getData() {
        ArrayList<TextLine> text = new ArrayList<TextLine>();
        text.add(new TextLine(this.villager.func_70005_c_() + ", " + this.villager.getNativeOccupationName(), "\u00a71", new GuiText.GuiButtonReference(this.villager.vtype)));
        text.add(new TextLine());
        if (this.villager.hiredBy != null) {
            text.add(new TextLine(LanguageUtilities.string("hire.hiredvillager", "" + Math.round((this.villager.hiredUntil - this.villager.level.getDayTime()) / 1000L), ClientProxy.KB_ESCORTS.getTranslatedKeyMessage().getString())));
        } else if (this.villager.getTownHall().getReputation(this.player) >= 4096) {
            text.add(new TextLine(LanguageUtilities.string("hire.hireablevillager")));
        } else {
            text.add(new TextLine(LanguageUtilities.string("hire.hireablevillagernoreputation")));
        }
        text.add(new TextLine());
        text.add(new TextLine(LanguageUtilities.string("hire.health") + ": " + (double)this.villager.getHealth() * 0.5 + "/" + (double)this.villager.getMaxHealth() * 0.5));
        text.add(new TextLine(LanguageUtilities.string("hire.strength") + ": " + this.villager.getAttackStrength()));
        text.add(new TextLine(LanguageUtilities.string("hire.cost") + ": " + MillCommonUtilities.getShortPrice(this.villager.getHireCost(this.player))));
        ArrayList<List<TextLine>> ftext = new ArrayList<List<TextLine>>();
        ftext.add(text);
        return this.bookManager.convertAndAdjustLines(ftext);
    }

    @Override
    public ResourceLocation getPNGPath() {
        return this.background;
    }

    @Override
    public void initData() {
        this.refreshContent();
    }

    private void refreshContent() {
        this.textBook = this.getData();
        this.buttonPagination();
    }
}

