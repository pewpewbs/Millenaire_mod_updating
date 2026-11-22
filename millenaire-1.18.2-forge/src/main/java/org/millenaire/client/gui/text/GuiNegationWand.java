/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.ResourceLocation
 */
package org.millenaire.client.gui.text;

import java.io.IOException;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.client.book.BookManager;
import org.millenaire.client.book.TextBook;
import org.millenaire.client.book.TextPage;
import org.millenaire.client.gui.text.GuiText;
import org.millenaire.client.network.ClientSender;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.village.Building;

public class GuiNegationWand
extends GuiText {
    private final Building th;
    private final Player player;
    ResourceLocation background = new ResourceLocation("millenaire", "textures/gui/quest.png");

    public GuiNegationWand(Player player, Building th) {
        this.th = th;
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
                ClientSender.negationWand(this.player, this.th);
            }
            this.minecraft.setScreen(null);
            
        }
        super.onButtonPressed(guibutton);
    }

    @Override
    public void buttonPagination() {
        super.buttonPagination();
        int xStart = (this.width - this.getXSize()) / 2;
        int yStart = (this.height - this.getYSize()) / 2;
        this.addMillButton(new GuiText.MillGuiButton(1, xStart + this.getXSize() / 2 - 100, yStart + this.getYSize() - 40, 95, 20, LanguageUtilities.string("negationwand.cancel")));
        this.addMillButton(new GuiText.MillGuiButton(0, xStart + this.getXSize() / 2 + 5, yStart + this.getYSize() - 40, 95, 20, LanguageUtilities.string("negationwand.confirm")));
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

    @Override
    public ResourceLocation getPNGPath() {
        return this.background;
    }

    @Override
    public void initData() {
        this.textBook = new TextBook();
        TextPage page = new TextPage();
        page.addLine(LanguageUtilities.string("negationwand.confirmmessage", this.th.villageType.name));
        this.textBook.addPage(page);
        this.textBook = this.bookManager.adjustTextBookLineLength(this.textBook);
        this.pageNum = 0;
    }
}

