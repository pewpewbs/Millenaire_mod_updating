/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.ResourceLocation
 *  org.lwjgl.opengl.GL11
 */
package org.millenaire.client.gui.text;

import java.io.IOException;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.client.book.BookManager;
import org.millenaire.client.book.TextBook;
import org.millenaire.client.book.TextLine;
import org.millenaire.client.book.TextPage;
import org.millenaire.client.gui.text.GuiText;
import org.millenaire.common.utilities.LanguageUtilities;

public class GuiHelp
extends GuiText {
    public static final int NB_CHAPTERS = 13;
    int helpDisplayed = 1;
    ResourceLocation background = new ResourceLocation("millenaire", "textures/gui/help.png");

    public GuiHelp() {
        this.bookManager = new BookManager(256, 224, 200, 180, 36, new GuiText.FontRendererGUIWrapper(this));
    }

    public TextBook convertAdjustHelpText(List<List<String>> baseText) {
        TextBook adjustedBook = new TextBook();
        for (List<String> page : baseText) {
            TextPage newPage = new TextPage();
            for (String s : page) {
                newPage.addLine(new TextLine(s, true));
            }
            adjustedBook.addPage(newPage);
        }
        return this.bookManager.adjustTextBookLineLength(adjustedBook);
    }

    @Override
    protected void customDrawBackground(int mouseX, int mouseY, float f) {
        int xStart = (this.width - this.getXSize()) / 2;
        int yStart = (this.height - this.getYSize()) / 2;


        for (int i = 0; i < 7; ++i) {
            int extraFirstRow;
            if (this.helpDisplayed - 1 != i) {
                extraFirstRow = i == 0 ? 1 : 0;
                this.drawGradient(xStart, yStart - extraFirstRow + 32 * i + 1, xStart + 32, yStart + 32 * i + 32, -1610612736, -1610612736);
            }
            if (this.helpDisplayed - 8 == i) continue;
            extraFirstRow = i == 0 ? 1 : 0;
            this.drawGradient(xStart + 224, yStart - extraFirstRow + 32 * i + 1, xStart + 32 + 224, yStart + 32 * i + 32, -1610612736, -1610612736);
        }


    }

    @Override
    protected void customDrawScreen(int mouseX, int mouseY, float f) {
        int stringlength;
        int pos;
        int xStart = (this.width - this.getXSize()) / 2;
        int yStart = (this.height - this.getYSize()) / 2;


        if ((mouseX -= xStart) > 0 && mouseX < 32 && (pos = (mouseY -= yStart) / 32) >= 0 && pos < 13) {
            stringlength = this.font.width(LanguageUtilities.string("help.tab_" + (pos + 1)));
            this.drawGradient(mouseX + 10 - 3, mouseY + 10 - 3, mouseX + 10 + stringlength + 3, mouseY + 10 + 14, -1073741824, -1073741824);
            this.font.draw(framePose,LanguageUtilities.string("help.tab_" + (pos + 1)), mouseX + 10, mouseY + 10, 0x909090);
        }
        if (mouseX > 224 && mouseX < 256 && (pos = mouseY / 32) >= 0 && pos < 6) {
            stringlength = this.font.width(LanguageUtilities.string("help.tab_" + (pos + 8)));
            this.drawGradient(mouseX + 10 - 3, mouseY + 10 - 3, mouseX + 10 + stringlength + 3, mouseY + 10 + 14, -1073741824, -1073741824);
            this.font.draw(framePose,LanguageUtilities.string("help.tab_" + (pos + 8)), mouseX + 10, mouseY + 10, 0x909090);
        }


    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public ResourceLocation getPNGPath() {
        return this.background;
    }

    @Override
    public void initData() {
        List<List<String>> baseText = LanguageUtilities.getHelp(this.helpDisplayed);
        if (baseText != null) {
            this.textBook = this.convertAdjustHelpText(baseText);
        } else {
            this.textBook = new TextBook();
            TextPage page = new TextPage();
            page.addLine("Il n'y a malheuresement pas d'aide disponible dans votre langue.");
            page.addLine("");
            page.addLine("Unfortunately there is no help available in your language.");
            this.textBook.addPage(page);
            this.textBook = this.bookManager.adjustTextBookLineLength(this.textBook);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int k) {
        if (k!=0) return super.mouseClicked(mouseX,mouseY,k);
        int pos;
        int xStart = (this.width - this.getXSize()) / 2;
        int yStart = (this.height - this.getYSize()) / 2;
        int ai = (int)mouseX - xStart;
        int aj = (int)mouseY - yStart;
        if (aj > this.getYSize() - 14 && aj < this.getYSize()) {
            if (ai > 36 && ai < 64) {
                this.decrementPage();
            } else if (ai > this.getXSize() - 64 && ai < this.getXSize() - 36) {
                this.incrementPage();
            }
        }
        if (ai > 0 && ai < 32 && (pos = aj / 32) >= 0 && pos < 13) {
            this.pageNum = 0;
            if (++pos != this.helpDisplayed) {
                this.helpDisplayed = pos;
                this.initData();
                this.buttonPagination();
                return true;
            }
        }
        if (ai > 224 && ai < 256 && (pos = aj / 32) >= 0 && pos < 6) {
            this.pageNum = 0;
            if ((pos += 8) != this.helpDisplayed) {
                this.helpDisplayed = pos;
                this.initData();
                this.buttonPagination();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, k);
    }
}

