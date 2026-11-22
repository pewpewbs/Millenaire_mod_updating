/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.texture.TextureUtil
 *  net.minecraft.client.shader.Framebuffer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.World
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 */
package org.millenaire.client.book;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Screenshot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.millenaire.client.book.BookManager;
import org.millenaire.client.book.BookManagerTravelBook;
import org.millenaire.client.book.TextBook;
import org.millenaire.client.book.TextLine;
import org.millenaire.client.book.TextPage;
import org.millenaire.client.gui.text.GuiTravelBook;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.culture.VillagerType;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.VillagerRecord;

public class TravelBookExporter {
    private static Map<String, ItemStack> itemsToRender = new HashMap<String, ItemStack>();
    private static final String EOL = "\n";

    private static String escapeQuotes(String label) {
        label = label.replaceAll("'", "''");
        label = label.replaceAll("\"", "\\\\\"");
        return label;
    }

    private static void exportAllBuildings(BookManagerTravelBook travelBookManager, BufferedWriter writer, Gson gson, String language) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        boolean firstValues = true;
        writer.write("DELETE from encyclopediadata WHERE type='buildings' AND language='" + language + "';" + EOL);
        for (Culture culture : Culture.ListCultures) {
            for (BuildingPlanSet planSet : culture.ListPlanSets) {
                if (!planSet.getFirstStartingPlan().travelBookDisplay) continue;
                if (firstValues) {
                    firstValues = false;
                }
                TextBook book = travelBookManager.getBookBuildingDetail(culture, planSet.key, null);
                String json = TravelBookExporter.escapeQuotes(gson.toJson((Object)new ExportBook(book)));
                String label = planSet.getNameNativeAndTranslated();
                label = TravelBookExporter.escapeQuotes(label);
                String categoryLabel = TravelBookExporter.escapeQuotes(culture.getCultureString("travelbook_category." + planSet.getFirstStartingPlan().travelBookCategory));
                String itemref = culture.key + "-buildings-" + planSet.key + "-" + language;
                writer.write("INSERT INTO encyclopediadata (itemref,type,language,culture,category,categorylabel,itemkey,label,iconkey,data) VALUES \n");
                writer.write("('" + itemref + "','buildings','" + language + "','" + culture.key + "','" + planSet.getFirstStartingPlan().travelBookCategory + "','" + categoryLabel + "','" + planSet.key + "','" + label + "','" + TravelBookExporter.getIconKey(planSet.getIcon()) + "','" + json + "');" + EOL);
            }
        }
        writer.write(";\n");
    }

    private static void exportAllCultures(BookManagerTravelBook travelBookManager, BufferedWriter writer, Gson gson, String language) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        boolean firstValues = true;
        writer.write("DELETE from encyclopediadata WHERE type='cultures' AND language='" + language + "';" + EOL);
        for (Culture culture : Culture.ListCultures) {
            if (firstValues) {
                firstValues = false;
            }
            TextBook book = travelBookManager.getBookCultureForJSONExport(culture, null);
            String json = TravelBookExporter.escapeQuotes(gson.toJson((Object)new ExportBook(book)));
            String label = culture.getNameTranslated();
            label = TravelBookExporter.escapeQuotes(label);
            String itemref = culture.key + "-cultures-" + culture.key + "-" + language;
            writer.write("INSERT INTO encyclopediadata (itemref,type,language,culture,category,itemkey,label,iconkey,data) VALUES \n");
            writer.write("('" + itemref + "','cultures','" + language + "','" + culture.key + "',NULL,'" + culture.key + "','" + label + "','" + TravelBookExporter.getIconKey(culture.getIcon()) + "','" + json + "');" + EOL);
        }
        writer.write(";\n");
    }

    private static void exportAllTradeGoods(BookManagerTravelBook travelBookManager, BufferedWriter writer, Gson gson, String language) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        boolean firstValues = true;
        writer.write("DELETE from encyclopediadata WHERE type='tradegoods' AND language='" + language + "';" + EOL);
        for (Culture culture : Culture.ListCultures) {
            for (TradeGood tradeGood : culture.goodsList) {
                if (!tradeGood.travelBookDisplay || tradeGood.travelBookCategory.equals("foreigntrade")) continue;
                if (firstValues) {
                    firstValues = false;
                }
                TextBook book = travelBookManager.getBookTradeGoodDetail(culture, tradeGood.key, null);
                String json = TravelBookExporter.escapeQuotes(gson.toJson((Object)new ExportBook(book)));
                String label = tradeGood.getName();
                label = TravelBookExporter.escapeQuotes(label);
                String categoryLabel = TravelBookExporter.escapeQuotes(culture.getCultureString("travelbook_category." + tradeGood.travelBookCategory));
                String itemref = culture.key + "-tradegoods-" + tradeGood.key + "-" + language;
                writer.write("INSERT INTO encyclopediadata (itemref,type,language,culture,category,categorylabel,itemkey,label,iconkey,data) VALUES \n");
                writer.write("('" + itemref + "','tradegoods','" + language + "','" + culture.key + "','" + tradeGood.travelBookCategory + "','" + categoryLabel + "','" + tradeGood.key + "','" + label + "','" + TravelBookExporter.getIconKey(tradeGood.getIcon()) + "','" + json + "');" + EOL);
            }
        }
        writer.write(";\n");
    }

    private static void exportAllVillagers(BookManagerTravelBook travelBookManager, BufferedWriter writer, Gson gson, String language) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        writer.write("DELETE from encyclopediadata WHERE type='villagers' AND language='" + language + "';" + EOL);
        boolean firstValues = true;
        for (Culture culture : Culture.ListCultures) {
            for (VillagerType vtype : culture.listVillagerTypes) {
                if (!vtype.travelBookDisplay) continue;
                if (firstValues) {
                    firstValues = false;
                }
                TextBook book = travelBookManager.getBookVillagerDetail(culture, vtype.key, null);
                String json = TravelBookExporter.escapeQuotes(gson.toJson((Object)new ExportBook(book)));
                String label = vtype.getNameNativeAndTranslated();
                label = TravelBookExporter.escapeQuotes(label);
                String categoryLabel = TravelBookExporter.escapeQuotes(culture.getCultureString("travelbook_category." + vtype.travelBookCategory));
                String itemref = culture.key + "-villagers-" + vtype.key + "-" + language;
                writer.write("INSERT INTO encyclopediadata (itemref,type,language,culture,category,categorylabel,itemkey,label,iconkey,data) VALUES \n");
                writer.write("('" + itemref + "','villagers','" + language + "','" + culture.key + "','" + vtype.travelBookCategory + "','" + categoryLabel + "','" + vtype.key + "','" + label + "','" + TravelBookExporter.getIconKey(vtype.getIcon()) + "','" + json + "');" + EOL);
            }
        }
        writer.write(";\n");
    }

    private static void exportAllVillages(BookManagerTravelBook travelBookManager, BufferedWriter writer, Gson gson, String language) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        boolean firstValues = true;
        writer.write("DELETE from encyclopediadata WHERE type='villages' AND language='" + language + "';" + EOL);
        for (Culture culture : Culture.ListCultures) {
            for (VillageType vtype : culture.listVillageTypes) {
                if (!vtype.travelBookDisplay) continue;
                if (firstValues) {
                    firstValues = false;
                }
                TextBook book = travelBookManager.getBookVillageDetail(culture, vtype.key, null);
                String json = TravelBookExporter.escapeQuotes(gson.toJson((Object)new ExportBook(book)));
                String label = vtype.getNameNativeAndTranslated();
                label = TravelBookExporter.escapeQuotes(label);
                String itemref = culture.key + "-villages-" + vtype.key + "-" + language;
                writer.write("INSERT INTO encyclopediadata (itemref,type,language,culture,category,itemkey,label,iconkey,data) VALUES \n");
                writer.write("('" + itemref + "','villages','" + language + "','" + culture.key + "',NULL,'" + vtype.key + "','" + label + "','" + TravelBookExporter.getIconKey(vtype.getIcon()) + "','" + json + "');" + EOL);
            }
        }
        writer.write(";\n");
    }

    private static BufferedImage renderExport(int width, int height, Runnable draw) {
        RenderSystem.assertOnRenderThread();
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget previous = minecraft.getMainRenderTarget();
        Matrix4f projection = RenderSystem.getProjectionMatrix().copy();
        var modelView = RenderSystem.getModelViewStack();
        float oldOffset = minecraft.getItemRenderer().blitOffset;
        RenderTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        modelView.pushPose();
        try {
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(Matrix4f.orthographic(0, width / 2.0F, height / 2.0F, 0, 1000, 3000));
            modelView.setIdentity();
            modelView.translate(0, 0, -2000);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableDepthTest();
            Lighting.setupFor3DItems();
            draw.run();
            try (NativeImage pixels = Screenshot.takeScreenshot(target)) {
                BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    int abgr = pixels.getPixelRGBA(x, y);
                    int argb = (abgr & 0xff00ff00) | ((abgr & 0xff) << 16) | ((abgr >>> 16) & 0xff);
                    result.setRGB(x, y, argb);
                }
                return result;
            }
        } finally {
            minecraft.getItemRenderer().blitOffset = oldOffset;
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection);
            target.destroyBuffers();
            previous.bindWrite(true);
            Lighting.setupFor3DItems();
        }
    }

    private static void writeExport(BufferedImage image, File file) throws IOException {
        java.nio.file.Files.createDirectories(file.toPath().getParent());
        if (!ImageIO.write(image, "png", file)) throw new IOException("No PNG writer for " + file);
    }

    private static void exportItemStack(ItemStack stack) throws IOException {
        BufferedImage image = renderExport(32, 32, () -> {
            Minecraft.getInstance().getItemRenderer().blitOffset = 100.0F;
            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, 0, 0);
        });
        File file = new File(new File(MillCommonUtilities.getMillenaireCustomContentDir(), "item picts"), getIconKey(stack) + ".png");
        writeExport(image, file);
    }

    public static void exportItemStacks() {
        for (ItemStack stack : itemsToRender.values()) {
            try {
                TravelBookExporter.exportItemStack(stack);
            }
            catch (IOException e) {
                MillLog.printException(e);
            }
        }
        MillLog.major(null, "Exported " + itemsToRender.size() + " icons.");
    }

    public static void exportTravelBookData() {
        BookManagerTravelBook travelBookManager = new BookManagerTravelBook(50000, 50000, 50000, 50000, new BookManager.FontRendererMock());
        File dir = new File(MillCommonUtilities.getMillenaireCustomContentDir(), "jsonexports");
        dir.mkdirs();
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        try {
            String language = MillConfigValues.effective_language;
            if (language.contains("_")) {
                language = language.split("_")[0];
            }
            File file = new File(dir, "travelbook_" + language + ".sql");
            BufferedWriter writer = MillCommonUtilities.getWriter(file);
            TravelBookExporter.exportAllCultures(travelBookManager, writer, gson, language);
            TravelBookExporter.exportAllVillagers(travelBookManager, writer, gson, language);
            TravelBookExporter.exportAllBuildings(travelBookManager, writer, gson, language);
            TravelBookExporter.exportAllVillages(travelBookManager, writer, gson, language);
            TravelBookExporter.exportAllTradeGoods(travelBookManager, writer, gson, language);
            writer.close();
            MillLog.warning(null, "Exported travel book data to SQL for language: " + language);
        }
        catch (IOException e) {
            MillLog.printException(e);
        }
    }

    public static void exportVillagerPicture(VillagerType villagerType, boolean mainPageExport) throws IOException {
        MillCommonUtilities.initRandom(villagerType.key.hashCode());
        VillagerRecord villagerRecord = VillagerRecord.createVillagerRecord(villagerType.culture, villagerType.key, Mill.getMillWorld((Level)Minecraft.getInstance().level), null, null, null, null, -1L, true);
        MillVillager mockVillager = MillVillager.createMockVillager(villagerRecord, (Level)Minecraft.getInstance().level);
        if (!mainPageExport) {
            mockVillager.heldItem = villagerType.getTravelBookHeldItem();
            mockVillager.heldItemOffHand = villagerType.getTravelBookHeldItemOffHand();
            mockVillager.travelBookMockVillager = true;
        }
        BufferedImage image = renderExport(600, 440,
            () -> GuiTravelBook.drawEntityOnScreen(200, 200, 100, 20.0F, 0.0F, mockVillager));
        int left = mainPageExport ? 256 : 195;
        int width = mainPageExport ? 280 : 320;
        BufferedImage cropped = image.getSubimage(left, 0, width, 420);
        String name = mockVillager.vtype.culture.key + "_" + mockVillager.vtype.key + (mainPageExport ? "_main" : "") + ".png";
        writeExport(cropped, new File(new File(MillCommonUtilities.getMillenaireCustomContentDir(), "villagers"), name));
    }

    public static void exportVillagerPictures(Level world) {
        int nb = 0;
        for (Culture culture : Culture.ListCultures) {
            for (VillagerType villagerType : culture.listVillagerTypes) {
                if (villagerType.travelBookDisplay) {
                    try {
                        TravelBookExporter.exportVillagerPicture(villagerType, false);
                        ++nb;
                    }
                    catch (Exception e) {
                        MillLog.printException(e);
                    }
                }
                if (!villagerType.travelBookMainCultureVillager) continue;
                try {
                    TravelBookExporter.exportVillagerPicture(villagerType, true);
                    ++nb;
                }
                catch (Exception e) {
                    MillLog.printException(e);
                }
            }
        }
        MillLog.major(null, "Exported " + nb + " villager pictures.");
    }

    private static String getIconKey(ItemStack stack) {
        if (stack == null) {
            return "";
        }
        String key = stack.getDamageValue() > 0 ? stack.getItem().getRegistryName().toString().replaceAll(":", "_") + "_" + stack.getDamageValue() : stack.getItem().getRegistryName().toString().replaceAll(":", "_");
        if (!itemsToRender.containsKey(key)) {
            itemsToRender.put(key, stack);
        }
        return key;
    }

    private static class ExportPage {
        public List<ExportLine> lines = new ArrayList<ExportLine>();

        public ExportPage(TextPage page) {
            for (TextLine line : page.getLines()) {
                if (line.columns != null && this.lines.size() > 0 && this.lines.get((int)(this.lines.size() - 1)).columns != null && !line.exportTwoColumns) {
                    ExportLine previousLine = this.lines.get(this.lines.size() - 1);
                    ExportLine newLine = new ExportLine(line);
                    previousLine.columns.addAll(newLine.columns);
                    continue;
                }
                if (line.columns == null && (line.text == null || line.text.trim().length() <= 0)) continue;
                this.lines.add(new ExportLine(line));
            }
        }
    }

    private static class ExportLine {
        public String style = "";
        public String text = "";
        public String referenceButtonCulture = null;
        public String referenceButtonType = null;
        public String referenceButtonKey = null;
        public String referenceButtonLabel = null;
        public String referenceButtonIconKey = null;
        public String iconKey = null;
        public String iconLabel = null;
        public List<ExportLine> columns = null;
        public Boolean exportTwoColumns = null;
        public String specialTag = null;

        public ExportLine(TextLine textLine) {
            this.text = textLine.text;
            this.specialTag = textLine.exportSpecialTag;
            if (textLine.style != null) {
                this.style = textLine.style.equals(TextLine.ITALIC) ? "subheader" : (textLine.style.equals("\u00a71") ? "header" : textLine.style);
            }
            if (textLine.referenceButton != null) {
                this.referenceButtonCulture = textLine.referenceButton.culture.key;
                switch (textLine.referenceButton.type) {
                    case BUILDING_DETAIL: {
                        this.referenceButtonType = "buildings";
                        break;
                    }
                    case CULTURE: {
                        this.referenceButtonType = "cultures";
                        break;
                    }
                    case TRADE_GOOD_DETAIL: {
                        this.referenceButtonType = "tradegoods";
                        break;
                    }
                    case VILLAGE_DETAIL: {
                        this.referenceButtonType = "villages";
                        break;
                    }
                    case VILLAGER_DETAIL: {
                        this.referenceButtonType = "villagers";
                    }
                }
                this.referenceButtonKey = textLine.referenceButton.key;
                this.referenceButtonIconKey = TravelBookExporter.getIconKey(textLine.referenceButton.getIcon());
                this.referenceButtonLabel = textLine.referenceButton.getIconFullLegendExport();
            }
            if (textLine.icons != null && textLine.icons.size() > 0) {
                this.iconKey = TravelBookExporter.getIconKey(textLine.icons.get(0));
                if (textLine.iconExtraLegends != null && textLine.iconExtraLegends.size() > 0 && textLine.iconExtraLegends.get(0) != null) {
                    this.iconLabel = textLine.iconExtraLegends.get(0);
                } else if (textLine.displayItemLegend()) {
                    this.iconLabel = textLine.icons.get(0).getHoverName().getString();
                }
            }
            if (textLine.columns != null) {
                this.columns = new ArrayList<ExportLine>();
                for (TextLine col : textLine.columns) {
                    this.columns.add(new ExportLine(col));
                }
                if (textLine.exportTwoColumns) {
                    this.exportTwoColumns = Boolean.TRUE;
                }
            }
        }
    }

    private static class ExportBook {
        public List<ExportPage> pages = new ArrayList<ExportPage>();

        public ExportBook(TextBook book) {
            for (TextPage page : book.getPages()) {
                this.pages.add(new ExportPage(page));
            }
        }
    }
}

