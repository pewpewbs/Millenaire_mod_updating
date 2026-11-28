/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiTextField
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.RenderItem
 *  net.minecraft.client.util.ITooltipFlag
 *  net.minecraft.client.util.ITooltipFlag$TooltipFlags
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.TextFormatting
 *  org.lwjgl.opengl.GL11
 */
package org.millenaire.client.gui.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;

import org.millenaire.client.book.BookManager;
import org.millenaire.client.book.TextBook;
import org.millenaire.client.book.TextLine;
import org.millenaire.client.gui.text.GuiTravelBook;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.culture.VillagerType;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillLog;

public abstract class GuiText
extends Screen {
    private static final ResourceLocation ICONS_TEXTURE = new ResourceLocation("millenaire", "textures/gui/icons.png");
    public static final String WHITE = "<white>";
    public static final String YELLOW = "<yellow>";
    public static final String PINK = "<pink>";
    public static final String LIGHTRED = "<lightred>";
    public static final String CYAN = "<cyan>";
    public static final String LIGHTGREEN = "<lightgreen>";
    public static final String BLUE = "<blue>";
    public static final String DARKGREY = "<darkgrey>";
    public static final String LIGHTGREY = "<lightgrey>";
    public static final String ORANGE = "<orange>";
    public static final String PURPLE = "<purple>";
    public static final String DARKRED = "<darkred>";
    public static final String LIGHTBLUE = "<lightblue>";
    public static final String DARKGREEN = "<darkgreen>";
    public static final String DARKBLUE = "<darkblue>";
    public static final String BLACK = "<black>";
    private Screen callingScreen = null;
    protected int pageNum = 0;
    protected TextBook textBook = null;
    protected BookManager bookManager = null;
    List<MillGuiTextField> textFields = new ArrayList<MillGuiTextField>();
    protected final List<Button> buttons = new ArrayList<>();
    protected PoseStack framePose;
    protected GuiText() { super(TextComponent.EMPTY); }
    protected void addMillButton(Button button) {
        if (button instanceof ActionButton action) action.handler = pressed -> {
            try { onButtonPressed(pressed); }
            catch (IOException error) { MillLog.printException("Button action in " + this,error); }
        };
        buttons.add(button); addRenderableWidget(button);
    }
    protected void clearMillWidgets() { setFocused(null); clearWidgets(); buttons.clear(); textFields.clear(); }
    protected static int buttonId(Button button) { return button instanceof MillGuiButton mill ? mill.id : -1; }
    public static class ActionButton extends Button {
        private java.util.function.Consumer<Button> handler = button -> {};
        protected ActionButton(int x,int y,int width,int height,Component label) {
            super(x,y,width,height,label,button -> ((ActionButton)button).handler.accept(button));
        }
    }
    protected void drawGradient(int x1,int y1,int x2,int y2,int c1,int c2) {
        fillGradient(framePose,x1,y1,x2,y2,c1,c2);
    }
    protected void drawTexture(int x,int y,int u,int v,int w,int h) { blit(framePose,x,y,u,v,w,h); }
    protected void bindTexture(ResourceLocation texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0,texture); RenderSystem.setShaderColor(1,1,1,1);
    }

    protected void onButtonPressed(Button button) throws IOException {
        if (button instanceof GuiButtonReference) {
            GuiButtonReference refButton = (GuiButtonReference)button;
            GuiTravelBook guiTravelBook = new GuiTravelBook((Player)Minecraft.getInstance().player);
            guiTravelBook.setCallingScreen(this);
            guiTravelBook.jumpToDetails(refButton.culture, refButton.type, refButton.key, false);
            Minecraft.getInstance().setScreen((Screen)guiTravelBook);
        }
    }

    public void buttonPagination() {
        int elementsId = 0;
        try {
            if (this.textBook == null) {
                return;
            }
            this.pageNum = Math.max(0,Math.min(this.pageNum,Math.max(0,this.textBook.nbPages()-1)));
            int xStart = (this.width - this.getXSize()) / 2;
            int yStart = (this.height - this.getYSize()) / 2;
            this.clearMillWidgets();
            int vpos = 6;
            if (this.pageNum < this.textBook.nbPages()) {
                for (int cp = 0; cp < this.getTextHeight() && cp < this.textBook.getPage(this.pageNum).getNbLines(); ++cp) {
                    TextLine line = this.textBook.getPage(this.pageNum).getLine(cp);
                    int totalButtonWidth = this.getLineSizeInPx() - 20;
                    if (line.buttons != null) {
                        if (line.buttons.length == 1) {
                            if (line.buttons[0] != null) {
                                line.buttons[0].x = xStart + this.getXSize() / 2 - totalButtonWidth / 2;
                                line.buttons[0].setWidth(totalButtonWidth);
                            }
                        } else if (line.buttons.length == 2) {
                            int buttonWidth = totalButtonWidth / 2 - 5;
                            if (line.buttons[0] != null) {
                                line.buttons[0].x = xStart + this.getXSize() / 2 - totalButtonWidth / 2;
                                line.buttons[0].setWidth(buttonWidth);
                            }
                            if (line.buttons[1] != null) {
                                line.buttons[1].x = xStart + this.getXSize() / 2 + 5;
                                line.buttons[1].setWidth(buttonWidth);
                            }
                        } else if (line.buttons.length == 3) {
                            int buttonWidth = totalButtonWidth / 3 - 10;
                            if (line.buttons[0] != null) {
                                line.buttons[0].x = xStart + this.getXSize() / 2 - totalButtonWidth / 2;
                                line.buttons[0].setWidth(buttonWidth);
                            }
                            if (line.buttons[1] != null) {
                                line.buttons[1].x = xStart + this.getXSize() / 2 - totalButtonWidth / 2 + buttonWidth + 10;
                                line.buttons[1].setWidth(buttonWidth);
                            }
                            if (line.buttons[2] != null) {
                                line.buttons[2].x = xStart + this.getXSize() / 2 - totalButtonWidth / 2 + buttonWidth * 2 + 20;
                                line.buttons[2].setWidth(buttonWidth);
                            }
                        }
                        for (int i = 0; i < line.buttons.length; ++i) {
                            if (line.buttons[i] == null) continue;
                            line.buttons[i].y = yStart + vpos;
                            line.buttons[i].setHeight(20);
                            this.addMillButton(line.buttons[i]);
                        }
                    } else if (line.referenceButton != null) {
                        line.referenceButton.setWidth(20);
                        line.referenceButton.setHeight(20);
                        line.referenceButton.y = yStart + vpos;
                        line.referenceButton.x = xStart + 6 + line.getLineMarginLeft();
                        this.addMillButton(line.referenceButton);
                    } else if (line.textField != null) {
                        MillGuiTextField textField = new MillGuiTextField(elementsId++, this.font, xStart + this.getXSize() / 2 + 40, yStart + vpos, 95, 20, line.textField.fieldKey);
                        textField.setValue(line.textField.getValue());
                        textField.setMaxLength(line.textField.getMaxLength());
                        textField.setTextColor(-1);
                        line.textField = textField;
                        line.textField.setTextColor(-1);
                        line.textField.setBordered(false);
                        this.textFields.add(textField);
                        this.addRenderableWidget(textField);
                        textField.setResponder(value -> handleTextFieldPress(textField));
                    }
                    if (line.columns != null) {
                        int lineSize = this.getLineSizeInPx() - line.getTextMarginLeft() - line.getLineMarginLeft() - line.getLineMarginRight();
                        int colSize = (lineSize - (line.columns.length - 1) * 10) / line.columns.length;
                        for (int col = 0; col < line.columns.length; ++col) {
                            TextLine column = line.columns[col];
                            int colXStart = col * (colSize + 10) + line.getLineMarginLeft();
                            if (column.referenceButton == null) continue;
                            column.referenceButton.setWidth(20);
                            column.referenceButton.setHeight(20);
                            column.referenceButton.y = yStart + vpos;
                            column.referenceButton.x = xStart + colXStart + 6 + column.getLineMarginLeft();
                            this.addMillButton(column.referenceButton);
                        }
                    }
                    vpos += line.getLineHeight();
                }
            }
        }
        catch (Exception e) {
            MillLog.printException("Exception while doing button pagination in GUI " + (Object)((Object)this), e);
        }
    }

    protected void closeGui() {
        if (this.callingScreen != null) {
            Minecraft.getInstance().setScreen(this.callingScreen);
        } else {
            this.minecraft.setScreen(null);

        }
    }

    protected void closeWindow() {
        this.minecraft.setScreen(null);

    }

    protected abstract void customDrawBackground(int var1, int var2, float var3);

    protected abstract void customDrawScreen(int var1, int var2, float var3);

    public void decrementPage() {
        if (this.textBook == null) {
            return;
        }
        if (this.pageNum > 0) {
            --this.pageNum;
        }
        this.buttonPagination();
    }

    public boolean isPauseScreen() {
        return false;
    }

    protected void drawHoveringText(List<String> lines,int x,int y,Font tooltipFont) {
        if (!lines.isEmpty()) renderComponentTooltip(framePose,lines.stream().map(TextComponent::new).map(c -> (Component)c).toList(),x,y,tooltipFont);
    }

    protected void drawItemStackTooltip(ItemStack stack,int x,int y,boolean showItem,String extra) {
        List<Component> lines=new ArrayList<>();
        if (showItem) lines.addAll(getTooltipFromItem(stack));
        if (extra!=null) for (String line:BookManager.splitStringByLength(new FontRendererWrapped(font),extra,150)) lines.add(new TextComponent(line));
        if (!lines.isEmpty()) renderComponentTooltip(framePose,lines,x,y,stack);
    }

    private record HoverItem(ItemStack stack, String legend, boolean showItem) {}
    private HoverItem drawLineIcons(TextLine line,int x,int y,int mouseX,int mouseY) {
        HoverItem hovered=null;
        if (line.icons==null) return null;
        for (int i=0;i<line.icons.size();i++) {
            ItemStack icon=line.icons.get(i); int iconX=x+18*i;
            if (icon==null || icon.isEmpty()) continue;
            // Non-reference items still deserve the same visible slot frame.
            fill(framePose,iconX-1,y-1,iconX+17,y+17,0xff303030);
            fill(framePose,iconX,y,iconX+16,y+16,0xff999999);
            itemRenderer.renderAndDecorateItem(icon,iconX,y);
            if (mouseX>=iconX && mouseX<iconX+16 && mouseY>=y && mouseY<y+16) {
                String legend=line.iconExtraLegends!=null && i<line.iconExtraLegends.size() ? line.iconExtraLegends.get(i) : null;
                hovered=new HoverItem(icon,legend,line.displayItemLegend());
            }
        }
        return hovered;
    }
    private void drawTextLine(TextLine line,int x,int y) {
        if (line.shadow) font.drawShadow(framePose,line.style+line.text,x+line.getTextMarginLeft(),y+line.getTextMarginTop(),0x101010);
        else font.draw(framePose,line.style+line.text,x+line.getTextMarginLeft(),y+line.getTextMarginTop(),0x101010);
    }
    private void drawSpecialIcon(SpecialIcon icon,int x,int y) {
        if (icon!=null) { bindTexture(ICONS_TEXTURE); blit(framePose,x,y,icon.xpos,icon.ypos,16,16); }
    }

    @Override public void render(PoseStack pose,int mouseX,int mouseY,float partialTicks) {
        framePose=pose;
        try {
            if (textBook==null) { initData(); buttonPagination(); }
            if (textBook==null) return;
            pageNum=Math.max(0,Math.min(pageNum,Math.max(0,textBook.nbPages()-1)));
            renderBackground(pose);
            int left=(width-getXSize())/2, top=(height-getYSize())/2;
            bindTexture(getPNGPath()); blit(pose,left,top,0,0,getXSize(),getYSize());
            customDrawBackground(mouseX,mouseY,partialTicks);
            pose.pushPose();
            try {
                pose.translate(left,top,0);
                int y=6;
                if (pageNum<textBook.nbPages()) for (int n=0;n<getTextHeight() && n<textBook.getPage(pageNum).getNbLines();n++) {
                    TextLine line=textBook.getPage(pageNum).getLine(n);
                    drawTextLine(line,getTextXStart()+line.getLineMarginLeft(),y);
                    if (line.columns!=null) {
                        int lineWidth=getLineSizeInPx()-line.getTextMarginLeft()-line.getLineMarginLeft()-line.getLineMarginRight();
                        int colWidth=(lineWidth-(line.columns.length-1)*10)/line.columns.length;
                        for (int c=0;c<line.columns.length;c++) drawTextLine(line.columns[c],getTextXStart()+c*(colWidth+10)+line.getLineMarginLeft(),y);
                    }
                    y+=line.getLineHeight();
                }
                font.draw(pose,(textBook.nbPages()==0 ? 0 : pageNum+1)+"/"+getNbPage(),getXSize()/2-10,getYSize()-10,0x101010);
                customDrawScreen(mouseX,mouseY,partialTicks);
            } finally { pose.popPose(); }
            super.render(pose,mouseX,mouseY,partialTicks);
            HoverItem hovered=null; GuiButtonReference reference=null;
            Lighting.setupFor3DItems(); RenderSystem.enableDepthTest();
            int y=top+6;
            if (pageNum<textBook.nbPages()) for (int n=0;n<getTextHeight() && n<textBook.getPage(pageNum).getNbLines();n++) {
                TextLine line=textBook.getPage(pageNum).getLine(n);
                HoverItem hit=drawLineIcons(line,left+getTextXStart()+line.getLineMarginLeft(),y,mouseX,mouseY);
                if (hit!=null) hovered=hit;
                if (line.columns!=null) {
                    int lineWidth=getLineSizeInPx()-line.getTextMarginLeft()-line.getLineMarginLeft()-line.getLineMarginRight();
                    int colWidth=(lineWidth-(line.columns.length-1)*10)/line.columns.length;
                    for (int c=0;c<line.columns.length;c++) {
                        hit=drawLineIcons(line.columns[c],left+getTextXStart()+c*(colWidth+10)+line.getLineMarginLeft(),y,mouseX,mouseY);
                        if (hit!=null) hovered=hit;
                    }
                }
                y+=line.getLineHeight();
            }
            for (Button button:buttons) {
                if (!button.visible) continue;
                if (button instanceof MillGuiButton mill) {
                    if (mill.itemStackIconLeft!=null) itemRenderer.renderAndDecorateItem(mill.itemStackIconLeft,mill.x+4,mill.y+2);
                    if (mill.itemStackIconRight!=null) itemRenderer.renderAndDecorateItem(mill.itemStackIconRight,mill.x+mill.getWidth()-20,mill.y+2);
                    drawSpecialIcon(mill.specialIconLeft,mill.x+4,mill.y+2);
                    drawSpecialIcon(mill.specialIconRight,mill.x+mill.getWidth()-20,mill.y+2);
                } else if (button instanceof GuiButtonReference ref) {
                    ItemStack icon=ref.getIcon();
                    if (icon!=null) itemRenderer.renderAndDecorateItem(icon,ref.x+2,ref.y+2);
                    if (ref.isMouseOver(mouseX,mouseY)) reference=ref;
                }
            }
            if (hovered!=null) drawItemStackTooltip(hovered.stack(),mouseX,mouseY,hovered.showItem(),hovered.legend());
            if (reference!=null) drawHoveringText(BookManager.splitStringByLength(new FontRendererWrapped(font),this instanceof GuiTravelBook ? reference.getIconName() : reference.getIconFullLegend(),150),reference.x+15,reference.y,font);
        } catch (Exception error) { MillLog.printException("Exception rendering " + this,error); }
        finally { if (itemRenderer!=null) itemRenderer.blitOffset=0; }
    }

    public Screen getCallingScreen() {
        return this.callingScreen;
    }

    private final int getLineSizeInPx() {
        return this.bookManager.getLineSizeInPx();
    }

    protected int getNbPage() {
        return this.textBook.nbPages();
    }

    public abstract ResourceLocation getPNGPath();

    public final int getTextHeight() {
        return this.bookManager.getTextHeight();
    }

    public final int getTextXStart() {
        return this.bookManager.getTextXStart();
    }

    public final int getXSize() {
        return this.bookManager.getXSize();
    }

    public final int getYSize() {
        return this.bookManager.getYSize();
    }

    protected void handleTextFieldPress(MillGuiTextField textField) {
    }

    public void incrementPage() {
        if (this.textBook == null) {
            return;
        }
        if (this.pageNum < this.getNbPage() - 1) {
            ++this.pageNum;
        }
        this.buttonPagination();
    }

    public abstract void initData();

    @Override protected void init() {
        initData(); buttonPagination();
    }
    @Override public void tick() { for (MillGuiTextField field:textFields) field.tick(); }
    @Override public void onClose() { closeGui(); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if (key==GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        for (MillGuiTextField field:textFields) field.setFocus(field.isMouseOver(x,y));
        int left=(width-getXSize())/2,top=(height-getYSize())/2;
        if (button==0 && y-top>getYSize()-14 && y-top<getYSize()) {
            if (x-left>0 && x-left<33) { decrementPage(); return true; }
            if (x-left>getXSize()-33 && x-left<getXSize()) { incrementPage(); return true; }
        }
        return super.mouseClicked(x,y,button);
    }

    public void setCallingScreen(Screen callingScreen) {
        this.callingScreen = callingScreen;
    }

    public static enum SpecialIcon {
        PLUS(0, 0),
        MINUS(16, 0);

        public int xpos;
        public int ypos;

        private SpecialIcon(int xpos, int ypos) {
            this.xpos = xpos;
            this.ypos = ypos;
        }
    }

    public static class MillGuiTextField
    extends EditBox {
        public final String fieldKey;
        private int maximumLength=32;
        @Override public void setMaxLength(int length) { super.setMaxLength(length); maximumLength=length; }
        public int getMaxLength() { return maximumLength; }

        public MillGuiTextField(int id, Font par1FontRenderer, int x, int y, int par5Width, int par6Height, String fieldKey) {
            super(par1FontRenderer,x,y,par5Width,par6Height,new TextComponent(fieldKey));
            this.fieldKey = fieldKey;
        }
    }

    public static class MillGuiButton
    extends ActionButton {
        public final int id;
        public static final int HELPBUTTON = 2000;
        public static final int CHUNKBUTTON = 3000;
        public static final int CONFIGBUTTON = 4000;
        public static final int TRAVELBOOKBUTTON = 5000;
        public ItemStack itemStackIconLeft = null;
        public SpecialIcon specialIconLeft = null;
        public ItemStack itemStackIconRight = null;
        public SpecialIcon specialIconRight = null;

        public MillGuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String label) {
            super(x,y,widthIn,heightIn,new TextComponent(label));
            this.id=buttonId;
        }

        public MillGuiButton(int id,int x,int y,String label) { this(id,x,y,200,20,label); }

        public MillGuiButton(String label, int id) {
            this(id,0,0,0,0,label);
        }

        public MillGuiButton(String label, int id, ItemStack icon) {
            this(id,0,0,0,0,label);
            this.itemStackIconLeft = icon;
        }

        public MillGuiButton(String label, int id, ItemStack iconLeft, ItemStack iconRight) {
            this(id,0,0,0,0,label);
            this.itemStackIconLeft = iconLeft;
            this.itemStackIconRight = iconRight;
        }

        public MillGuiButton(String label, int id, SpecialIcon icon) {
            this(id,0,0,0,0,label);
            this.specialIconLeft = icon;
        }

        public int getHeight() {
            return this.height;
        }

        public int getWidth() {
            return this.width;
        }

        public void setHeight(int h) {
            this.height = h;
        }
    }

    public static class GuiButtonReference
    extends ActionButton {
        public Culture culture;
        public RefType type;
        public String key;

        public GuiButtonReference(BuildingPlanSet planSet) {
            super(0,0,0,0,TextComponent.EMPTY);
            if (planSet == null) {
                MillLog.printException(new Exception("Tried creating a ref button to a null planSet."));
            } else {
                this.culture = planSet.culture;
            }
            this.type = RefType.BUILDING_DETAIL;
            this.key = planSet.key;
        }

        public GuiButtonReference(Culture culture) {
            super(0,0,0,0,TextComponent.EMPTY);
            this.culture = culture;
            this.type = RefType.CULTURE;
            this.key = null;
        }

        public GuiButtonReference(Culture culture, RefType type, String key) {
            super(0,0,0,0,TextComponent.EMPTY);
            this.culture = culture;
            this.type = type;
            this.key = key;
        }

        public GuiButtonReference(TradeGood tradeGood) {
            super(0,0,0,0,TextComponent.EMPTY);
            this.culture = tradeGood.culture;
            this.type = RefType.TRADE_GOOD_DETAIL;
            this.key = tradeGood.key;
        }

        public GuiButtonReference(VillagerType villagerType) {
            super(0,0,0,0,TextComponent.EMPTY);
            this.culture = villagerType.culture;
            this.type = RefType.VILLAGER_DETAIL;
            this.key = villagerType.key;
        }

        public GuiButtonReference(VillageType villageType) {
            super(0,0,0,0,TextComponent.EMPTY);
            if (villageType.lonebuilding) {
                this.culture = villageType.culture;
                this.type = RefType.BUILDING_DETAIL;
                this.key = villageType.centreBuilding.key;
            } else {
                this.culture = villageType.culture;
                this.type = RefType.VILLAGE_DETAIL;
                this.key = villageType.key;
            }
        }

        public ItemStack getIcon() {
            if (this.type == RefType.BUILDING_DETAIL) {
                return this.culture.getBuildingPlanSet(this.key).getIcon();
            }
            if (this.type == RefType.VILLAGER_DETAIL) {
                return this.culture.getVillagerType(this.key).getIcon();
            }
            if (this.type == RefType.VILLAGE_DETAIL) {
                return this.culture.getVillageType(this.key).getIcon();
            }
            if (this.type == RefType.TRADE_GOOD_DETAIL) {
                return this.culture.getTradeGood(this.key).getIcon();
            }
            if (this.type == RefType.CULTURE) {
                return this.culture.getIcon();
            }
            return null;
        }

        public String getIconFullLegend() {
            return LanguageUtilities.string("travelbook.reference_button", this.getIconName());
        }

        public String getIconFullLegendExport() {
            return LanguageUtilities.string("travelbook.reference_button_export", this.getIconNameTranslated());
        }

        public String getIconName() {
            if (this.type == RefType.BUILDING_DETAIL) {
                return this.culture.getBuildingPlanSet(this.key).getNameNative();
            }
            if (this.type == RefType.VILLAGER_DETAIL) {
                return this.culture.getVillagerType((String)this.key).name;
            }
            if (this.type == RefType.VILLAGE_DETAIL) {
                return this.culture.getVillageType((String)this.key).name;
            }
            if (this.type == RefType.TRADE_GOOD_DETAIL) {
                return this.culture.getTradeGood(this.key).getName();
            }
            if (this.type == RefType.CULTURE) {
                return this.culture.getAdjectiveTranslated();
            }
            return null;
        }

        public String getIconNameTranslated() {
            if (this.type == RefType.BUILDING_DETAIL) {
                return this.culture.getBuildingPlanSet(this.key).getNameNativeAndTranslated();
            }
            if (this.type == RefType.VILLAGER_DETAIL) {
                return this.culture.getVillagerType(this.key).getNameNativeAndTranslated();
            }
            if (this.type == RefType.VILLAGE_DETAIL) {
                return this.culture.getVillageType(this.key).getNameNativeAndTranslated();
            }
            if (this.type == RefType.TRADE_GOOD_DETAIL) {
                return this.culture.getTradeGood(this.key).getName();
            }
            if (this.type == RefType.CULTURE) {
                return this.culture.getAdjectiveTranslated();
            }
            return null;
        }

        public void setHeight(int h) {
            this.height = h;
        }

        public static enum RefType {
            BUILDING_DETAIL,
            VILLAGER_DETAIL,
            VILLAGE_DETAIL,
            TRADE_GOOD_DETAIL,
            CULTURE;

        }
    }

    public static class FontRendererWrapped
    implements BookManager.IFontRendererWrapper {
        private final Font fontRenderer;

        public FontRendererWrapped(Font fontRenderer) {
            this.fontRenderer = fontRenderer;
        }

        @Override
        public int getStringWidth(String text) {
            return this.fontRenderer.width(text);
        }

        @Override
        public boolean isAvailable() {
            return this.fontRenderer != null;
        }
    }

    public static class FontRendererGUIWrapper
    implements BookManager.IFontRendererWrapper {
        private final GuiText gui;

        public FontRendererGUIWrapper(GuiText gui) {
            this.gui = gui;
        }

        @Override
        public int getStringWidth(String text) {
            return this.gui.font.width(text);
        }

        @Override
        public boolean isAvailable() {
            return this.gui.font != null;
        }
    }
}

