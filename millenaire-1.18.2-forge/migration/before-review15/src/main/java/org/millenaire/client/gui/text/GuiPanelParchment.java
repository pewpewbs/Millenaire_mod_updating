/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.BufferBuilder
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.GlStateManager$DestFactor
 *  net.minecraft.client.renderer.GlStateManager$SourceFactor
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.ChunkPos
 *  net.minecraft.world.World
 *  net.minecraftforge.common.ForgeChunkManager
 *  org.lwjgl.opengl.GL11
 */
package org.millenaire.client.gui.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.millenaire.client.gui.ChunkMapSnapshot;
import java.util.concurrent.CompletableFuture;
import org.millenaire.client.book.BookManager;
import org.millenaire.client.book.TextBook;
import org.millenaire.client.gui.DisplayActions;
import org.millenaire.client.gui.text.GuiText;
import org.millenaire.client.network.ClientSender;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.ui.MillMapInfo;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.ThreadSafeUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.ConstructionIP;
import org.millenaire.common.world.MillWorldData;

public class GuiPanelParchment
extends GuiText {
    public static final int VILLAGE_MAP = 1;
    public static final int CHUNK_MAP = 2;
    public static final int chunkMapSizeInBlocks = 1280;
    private boolean isParchment = false;
    private int mapType = 0;
    private ChunkMapSnapshot chunkSnapshot;
    private CompletableFuture<ChunkMapSnapshot> chunkRequest;
    private long nextChunkRefresh;
    private Building townHall = null;
    private final Player player;
    ResourceLocation backgroundParchment = new ResourceLocation("millenaire", "textures/gui/parchment.png");
    ResourceLocation backgroundPanel = new ResourceLocation("millenaire", "textures/gui/panel.png");
    private final float targetHeight = 180.0f;
    private float scaledStartX;
    private float scaledStartY;
    private float scaleFactor;
    Tesselator tessellator = Tesselator.getInstance();
    BufferBuilder bufferbuilder = this.tessellator.getBuilder();

    public GuiPanelParchment(Player player, Building townHall, TextBook textBook, int mapType, boolean isParchment) {
        this.mapType = mapType;
        this.townHall = townHall;
        this.isParchment = isParchment;
        this.player = player;
        this.textBook = textBook;
        this.bookManager = new BookManager(204, 220, 190, 195, new GuiText.FontRendererGUIWrapper(this));
    }

    public GuiPanelParchment(Player player, TextBook textBook, Building townHall, int mapType, boolean isParchment) {
        this.mapType = mapType;
        this.townHall = townHall;
        this.isParchment = isParchment;
        this.player = player;
        this.textBook = textBook;
        this.bookManager = new BookManager(204, 220, 190, 195, new GuiText.FontRendererGUIWrapper(this));
    }

    @Override
    protected void onButtonPressed(Button guibutton) throws IOException {
        if (guibutton instanceof GuiText.MillGuiButton) {
            GuiText.MillGuiButton gb = (GuiText.MillGuiButton)guibutton;
            if (buttonId(gb) == 2000) {
                DisplayActions.displayHelpGUI();
            } else if (buttonId(gb) == 3000) {
                DisplayActions.displayChunkGUI(this.player, this.player.level);
            } else if (buttonId(gb) == 4000) {
                DisplayActions.displayConfigGUI();
            } else if (buttonId(gb) == 5000) {
                DisplayActions.displayTravelBookGUI(this.player);
            }
        }
        super.onButtonPressed(guibutton);
    }

    @Override
    protected void customDrawBackground(int i, int j, float f) {
    }

    @Override
    public void customDrawScreen(int i, int j, float f) {
        try {
            if (this.mapType == 1 && this.pageNum == 0 && this.townHall != null && this.townHall.mapInfo != null) {
                this.drawVillageMap(i, j);
            } else if (this.mapType == 2 && this.pageNum == 0) {
                this.drawChunkMap(i, j);
            }
        }
        catch (Exception e) {
            MillLog.printException("Exception while rendering map: ", e);
        }
    }

    private void drawChunkMap(int mouseX,int mouseY) {
        var server=minecraft.getSingleplayerServer();
        if(server==null)return;
        if(chunkRequest!=null && chunkRequest.isDone()) {
            try { chunkSnapshot=chunkRequest.join(); }
            catch(java.util.concurrent.CompletionException error) { MillLog.printException("Could not read chunk overview",error); }
            chunkRequest=null;
        }
        long now=net.minecraft.Util.getMillis();
        if(chunkRequest==null && now>=nextChunkRefresh) {
            chunkRequest=ChunkMapSnapshot.request(server,player.level.dimension(),player.chunkPosition().x,player.chunkPosition().z);
            nextChunkRefresh=now+2000;
        }
        if(chunkSnapshot==null || !chunkSnapshot.dimension().equals(player.level.dimension()))return;
        int left=(getXSize()-160)/2,top=(getYSize()-160)/2;
        int windowX=(width-getXSize())/2,windowY=(height-getYSize())/2;
        int localX=mouseX-windowX-left,localY=mouseY-windowY-top;
        boolean inside=localX>=0 && localX<160 && localY>=0 && localY<160;
        int hoverX=chunkSnapshot.startX()+Math.floorDiv(localX,2),hoverZ=chunkSnapshot.startZ()+Math.floorDiv(localY,2);
        List<String> labels=new ArrayList<>();
        drawGradient(left-2,top-2,left+162,top+162,0x20000000,0x20000000);
        for(int dx=0;dx<ChunkMapSnapshot.SIZE;dx++)for(int dz=0;dz<ChunkMapSnapshot.SIZE;dz++) {
            int x=chunkSnapshot.startX()+dx,z=chunkSnapshot.startZ()+dz;long key=ChunkPos.asLong(x,z);
            int px=left+dx*2,py=top+dz*2;
            boolean hover=inside && x==hoverX && z==hoverZ;
            if(chunkSnapshot.saved().contains(key)) {
                drawPixel(px,py,chunkSnapshot.loaded().contains(key)?-1073676544:-1057030144);
                if(hover)labels.add(LanguageUtilities.string("chunk.chunkcoords",x+"/"+z));
            }
            for(var area:chunkSnapshot.villages())if(area.contains(x,z)) {
                drawPixel(px+1,py+1,area.lone()?-258408295:-268435201);
                if(hover)labels.add(LanguageUtilities.string("chunk.village",area.name()));
            }
            if(chunkSnapshot.forced().contains(key)) {
                drawPixel(px,py+1,-251658241);
                if(hover)labels.add(LanguageUtilities.string("chunk.chunkforced"));
            }
        }
        if(!labels.isEmpty()) {
            int textWidth=labels.stream().mapToInt(font::width).max().orElse(0);
            int x=mouseX-windowX+10,y=mouseY-windowY;
            drawGradient(x-3,y-3,x+textWidth+3,y+11*labels.size(),-1073741824,-1073741824);
            for(int n=0;n<labels.size();n++)font.draw(framePose,labels.get(n),x,y+n*11,0x909090);
        }
    }

    private void drawPixel(int x, int y, int colour) {
        this.drawGradient(x, y, x + 1, y + 1, colour, colour);
    }

    private void drawScaledRect(int left, int top, int right, int bottom, int color) {
        int alpha = color >> 24 & 0xFF;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        this.bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        this.bufferbuilder.vertex(framePose.last().pose(), (this.scaledStartX + (float)right * this.scaleFactor), (this.scaledStartY + (float)top * this.scaleFactor), (float)getBlitOffset()).color(red, green, blue, alpha).endVertex();
        this.bufferbuilder.vertex(framePose.last().pose(), (this.scaledStartX + (float)left * this.scaleFactor), (this.scaledStartY + (float)top * this.scaleFactor), (float)getBlitOffset()).color(red, green, blue, alpha).endVertex();
        this.bufferbuilder.vertex(framePose.last().pose(), (this.scaledStartX + (float)left * this.scaleFactor), (this.scaledStartY + (float)bottom * this.scaleFactor), (float)getBlitOffset()).color(red, green, blue, alpha).endVertex();
        this.bufferbuilder.vertex(framePose.last().pose(), (this.scaledStartX + (float)right * this.scaleFactor), (this.scaledStartY + (float)bottom * this.scaleFactor), (float)getBlitOffset()).color(red, green, blue, alpha).endVertex();
        this.tessellator.end();
        RenderSystem.disableBlend();
    }

    private void drawVillageMap(int i, int j) {
        int xStart = (this.width - this.getXSize()) / 2;
        int yStart = (this.height - this.getYSize()) / 2;
        MillMapInfo minfo = this.townHall.mapInfo;


        if (minfo.width<=0 || minfo.length<=0) return;
        // Construction overlays must not erase the terrain in the shared map data.
        byte[][] displayData = Arrays.stream(minfo.data).map(byte[]::clone).toArray(byte[][]::new);
        this.scaleFactor = 180.0f / (float)minfo.width;
        this.scaledStartX = ((float)this.getXSize() - (float)minfo.length * this.scaleFactor) / 2.0f;
        this.scaledStartY = ((float)this.getYSize() - (float)minfo.width * this.scaleFactor) / 2.0f;





        this.drawScaledRect(-2, -2, minfo.length + 2, minfo.width + 2, 0x20000000);
        BuildingLocation locHover = null;
        MillVillager villagerHover = null;
        Player playerHover = null;
        List<BuildingLocation> locations = this.townHall.getLocations();
        List<ConstructionIP> constructionsIP = this.townHall.getConstructionsInProgress();
        for (ConstructionIP cip : constructionsIP) {
            if (cip.getBuildingLocation() == null) continue;
            BuildingLocation bl = cip.getBuildingLocation();
            int left = Math.max(0, bl.minx - minfo.mapStartX);
            int top = Math.max(0, bl.minz - minfo.mapStartZ);
            int right = Math.min(minfo.length - 1, bl.maxx + 1 - minfo.mapStartX);
            int bottom = Math.min(minfo.width - 1, bl.maxz + 1 - minfo.mapStartZ);
            if (left >= right || top >= bottom) continue;
            float screenLeft = (float)xStart + this.scaledStartX + (float)left * this.scaleFactor;
            float screenRight = (float)xStart + this.scaledStartX + (float)right * this.scaleFactor;
            float screenTop = (float)yStart + this.scaledStartY + (float)top * this.scaleFactor;
            float screenBottom = (float)yStart + this.scaledStartY + (float)bottom * this.scaleFactor;
            if ((float)i >= screenLeft && (float)i <= screenRight && (float)j >= screenTop && (float)j <= screenBottom) {
                locHover = bl;
            }
            this.drawScaledRect(left, top, right, bottom, 0x40FF00FF);
            for (int x = left; x < right; ++x) {
                Arrays.fill(displayData[x], top, bottom, (byte)11);
            }
        }
        for (BuildingLocation bl : locations) {
            if (bl.isSubBuildingLocation) continue;
            int left = Math.max(0, bl.minx - minfo.mapStartX);
            int top = Math.max(0, bl.minz - minfo.mapStartZ);
            int right = Math.min(minfo.length - 1, bl.maxx + 1 - minfo.mapStartX);
            int bottom = Math.min(minfo.width - 1, bl.maxz + 1 - minfo.mapStartZ);
            if (left >= right || top >= bottom) continue;
            float screenLeft = (float)xStart + this.scaledStartX + (float)left * this.scaleFactor;
            float screenRight = (float)xStart + this.scaledStartX + (float)right * this.scaleFactor;
            float screenTop = (float)yStart + this.scaledStartY + (float)top * this.scaleFactor;
            float screenBottom = (float)yStart + this.scaledStartY + (float)bottom * this.scaleFactor;
            if ((float)i >= screenLeft && (float)i <= screenRight && (float)j >= screenTop && (float)j <= screenBottom) {
                locHover = bl;
            }
            if (bl.level < 0) {
                this.drawScaledRect(left, top, right, bottom, 0x40000060);
            } else {
                this.drawScaledRect(left, top, right, bottom, 0x400000FF);
            }
            for (int x = left; x < right; ++x) {
                Arrays.fill(displayData[x], top, bottom, (byte)11);
            }
        }
        for (int x = 0; x < minfo.length; ++x) {
            int lastColour = 0;
            int lastZ = 0;
            for (int z = 0; z < minfo.width; ++z) {
                int colour = 0;
                byte groundType = displayData[x][z];
                colour = groundType == 11 ? 0 : (groundType == 1 ? -1439682305 : (groundType == 2 ? 0x40FF0000 : (groundType == 3 ? 0x40FFFF00 : (groundType == 4 ? 1090486336 : (groundType == 5 ? 0x1000FF00 : (groundType == 10 ? 0x40808080 : (groundType == 6 ? 1090474064 : (groundType == 7 ? Integer.MIN_VALUE : (groundType == 8 ? 1083834265 : 0x4000FF00)))))))));
                if (z == 0) {
                    lastColour = colour;
                    continue;
                }
                if (colour == lastColour) continue;
                if (lastColour != 0) {
                    this.drawScaledRect(x, lastZ, x + 1, z, lastColour);
                }
                lastColour = colour;
                lastZ = z;
            }
            if (lastColour == 0) continue;
            this.drawScaledRect(x, lastZ, x + 1, minfo.width, lastColour);
        }
        for (MillVillager villager : this.townHall.getKnownVillagers()) {
            int mapPosX = (int)(villager.getX() - (double)minfo.mapStartX);
            int mapPosZ = (int)(villager.getZ() - (double)minfo.mapStartZ);
            if (mapPosX <= 0 || mapPosZ <= 0 || mapPosX >= minfo.length || mapPosZ >= minfo.width) continue;
            if (villager.isBaby()) {
                this.drawScaledRect(mapPosX - 1, mapPosZ - 1, mapPosX + 1, mapPosZ + 1, -1593835776);
            } else if (villager.getRecord() != null && villager.getRecord().raidingVillage) {
                this.drawScaledRect(mapPosX - 1, mapPosZ - 1, mapPosX + 1, mapPosZ + 1, -1610612736);
            } else if (villager.gender == 1) {
                this.drawScaledRect(mapPosX - 1, mapPosZ - 1, mapPosX + 1, mapPosZ + 1, -1610547201);
            } else {
                this.drawScaledRect(mapPosX - 1, mapPosZ - 1, mapPosX + 1, mapPosZ + 1, -1593901056);
            }
            int screenPosX = (int)((float)xStart + this.scaledStartX + (float)mapPosX * this.scaleFactor);
            int screenPosY = (int)((float)yStart + this.scaledStartY + (float)mapPosZ * this.scaleFactor);
            if (screenPosX <= i - 2 || screenPosX >= i + 2 || screenPosY <= j - 2 || screenPosY >= j + 2) continue;
            villagerHover = villager;
        }
        int mapPosX = (int)(this.player.getX() - (double)minfo.mapStartX);
        int mapPosZ = (int)(this.player.getZ() - (double)minfo.mapStartZ);
        if (mapPosX > 0 && mapPosZ > 0 && mapPosX < minfo.length && mapPosZ < minfo.width) {
            this.drawScaledRect(mapPosX - 1, mapPosZ - 1, mapPosX + 2, mapPosZ + 2, -1593835521);
            int screenPosX = (int)((float)xStart + this.scaledStartX + (float)mapPosX * this.scaleFactor);
            int screenPosY = (int)((float)yStart + this.scaledStartY + (float)mapPosZ * this.scaleFactor);
            if (screenPosX > i - 2 && screenPosX < i + 3 && screenPosY > j - 2 && screenPosY < j + 3) {
                playerHover = this.player;
            }
        }
        if (villagerHover != null) {
            boolean gameString;
            int stringlength = this.font.width(villagerHover.func_70005_c_());
            stringlength = Math.max(stringlength, this.font.width(villagerHover.getNativeOccupationName()));
            boolean bl = gameString = villagerHover.getGameOccupationName(this.player.getName().getString()) != null && villagerHover.getGameOccupationName(this.player.getName().getString()).length() > 0;
            if (gameString) {
                stringlength = Math.max(stringlength, this.font.width(villagerHover.getGameOccupationName(this.player.getName().getString())));
                this.drawGradient(i + 10 - 3 - xStart, j + 10 - 3 - yStart, i + 10 + stringlength + 3 - xStart, j + 10 + 33 - yStart, -1073741824, -1073741824);
                this.font.draw(framePose,villagerHover.func_70005_c_(), i + 10 - xStart, j + 10 - yStart, 0x909090);
                this.font.draw(framePose,villagerHover.getNativeOccupationName(), i + 10 - xStart, j + 10 - yStart + 11, 0x909090);
                this.font.draw(framePose,villagerHover.getGameOccupationName(this.player.getName().getString()), i + 10 - xStart, j + 10 - yStart + 22, 0x909090);
            } else {
                this.drawGradient(i + 10 - 3 - xStart, j + 10 - 3 - yStart, i + 10 + stringlength + 3 - xStart, j + 10 + 22 - yStart, -1073741824, -1073741824);
                this.font.draw(framePose,villagerHover.func_70005_c_(), i + 10 - xStart, j + 10 - yStart, 0x909090);
                this.font.draw(framePose,villagerHover.getNativeOccupationName(), i + 10 - xStart, j + 10 - yStart + 11, 0x909090);
            }
        } else if (playerHover != null) {
            int stringlength = this.font.width(playerHover.getName().getString());
            this.drawGradient(i + 10 - 3 - xStart, j + 10 - 3 - yStart, i + 10 + stringlength + 3 - xStart, j + 10 + 11 - yStart, -1073741824, -1073741824);
            this.font.draw(framePose,playerHover.getName().getString(), i + 10 - xStart, j + 10 - yStart, 0x909090);
        } else if (locHover != null) {
            boolean gameString;
            String nativeString;
            int stringlength;
            boolean unreachable;
            Building b = locHover.getBuilding(this.townHall.world);
            boolean bl = unreachable = b != null && this.townHall.regionMapper != null && !b.isReachableFromRegion(this.townHall.regionMapper.thRegion);
            if (unreachable) {
                stringlength = this.font.width(locHover.getNativeName() + " - " + LanguageUtilities.string("panels.unreachablebuilding"));
                nativeString = locHover.getNativeName() + " - " + LanguageUtilities.string("panels.unreachablebuilding");
            } else {
                stringlength = this.font.width(locHover.getNativeName());
                nativeString = locHover.getNativeName();
            }
            int nblines = 1;
            boolean bl2 = gameString = locHover.getGameName() != null && locHover.getGameName().length() > 0;
            if (gameString) {
                stringlength = Math.max(stringlength, this.font.width(locHover.getGameName()));
                ++nblines;
            }
            List<String> effects = locHover.getBuildingEffects(this.townHall.world);
            nblines += effects.size();
            for (String s : effects) {
                stringlength = Math.max(stringlength, this.font.width(s));
            }
            this.drawGradient(i - 3 - xStart, j - 3 - yStart, i + stringlength + 3 - xStart, j + 11 * nblines - yStart, -1073741824, -1073741824);
            this.font.draw(framePose,nativeString, i - xStart, j - yStart, 0x909090);
            int pos = 1;
            if (gameString) {
                this.font.draw(framePose,locHover.getGameName(), i - xStart, j - yStart + 11, 0x909090);
                ++pos;
            }
            for (String s : effects) {
                this.font.draw(framePose,s, i - xStart, j - yStart + 11 * pos, 0x909090);
                ++pos;
            }
        }






    }

    @Override
    public ResourceLocation getPNGPath() {
        if (this.isParchment) {
            return this.backgroundParchment;
        }
        return this.backgroundPanel;
    }

    @Override
    public void initData() {
        this.textBook = this.bookManager.adjustTextBookLineLength(this.textBook);
        if (this.mapType == 1 && this.townHall != null) {
            ClientSender.requestMapInfo(this.townHall);
        }
    }

}

