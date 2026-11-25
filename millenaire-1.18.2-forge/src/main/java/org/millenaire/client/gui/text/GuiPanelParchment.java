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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
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
    private BuildingLocation tooltipLocation;
    private int tooltipLevel;
    private boolean tooltipConstruction;
    private String tooltipTitle;
    private String tooltipLanguage;
    private long tooltipRefreshNanos;
    private List<BuildingMapTooltip.Line> tooltipDetails = List.of();
    private boolean showBuildingTooltip;
    private BuildingLocation hoveredLocation,pinnedLocation;
    private boolean hoveredConstruction,tooltipPinned;
    private int hoverX,hoverY,panelLeft,panelTop,panelWidth,panelHeight,pinLeft,pinTop,closeX,closeY,closeSize;
    private boolean panelVisible;
    private final java.util.Map<ResourceLocation,Boolean> portraitResources=new java.util.HashMap<>();
    private boolean portraitAvailable(ResourceLocation texture){
        return texture!=null&&portraitResources.computeIfAbsent(texture,t->minecraft.getResourceManager().hasResource(t));
    }
    private record TooltipText(FormattedCharSequence text, int color, int y,String detail) {}
    private record TooltipIcon(ItemStack stack,int x,int y,String detail,List<BuildingMapTooltip.RecipeView> recipes,boolean action,BuildingMapTooltip.Portrait portrait) {
        TooltipIcon(ItemStack stack,int x,int y,String detail,List<BuildingMapTooltip.RecipeView> recipes,boolean action){
            this(stack,x,y,detail,recipes,action,null);
        }
    }
    private record TooltipLayout(List<TooltipText> text, List<TooltipIcon> icons, int width, int height) {}
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
        boolean constructionHover = false;
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
                constructionHover = true;
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
                constructionHover = false;
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
        hoveredLocation=locHover;hoveredConstruction=constructionHover;hoverX=i;hoverY=j;
        if(pinnedLocation!=null){
            BuildingLocation found=null;
            boolean constructing=false;
            for(ConstructionIP cip:constructionsIP)if(cip.getBuildingLocation()!=null&&cip.getBuildingLocation().isSameLocation(pinnedLocation)){
                found=cip.getBuildingLocation();constructing=true;break;
            }
            if(found==null)for(BuildingLocation location:locations)if(location.isSameLocation(pinnedLocation)){found=location;break;}
            pinnedLocation=found;
            if(found!=null){locHover=found;constructionHover=constructing;villagerHover=null;playerHover=null;}
        }
        if (villagerHover != null) {
            boolean gameString;
            org.millenaire.common.goal.Goal activeGoal = org.millenaire.common.goal.Goal.goals.get(villagerHover.goalKey);
            String taskLabel = activeGoal == null ? "" : activeGoal.gameName(villagerHover);
            int stringlength = Math.max(this.font.width(taskLabel), this.font.width(villagerHover.func_70005_c_()));
            stringlength = Math.max(stringlength, this.font.width(villagerHover.getNativeOccupationName()));
            boolean bl = gameString = villagerHover.getGameOccupationName(this.player.getName().getString()) != null && villagerHover.getGameOccupationName(this.player.getName().getString()).length() > 0;
            if (gameString) {
                stringlength = Math.max(stringlength, this.font.width(villagerHover.getGameOccupationName(this.player.getName().getString())));
                this.drawGradient(i + 10 - 3 - xStart, j + 10 - 3 - yStart, i + 10 + stringlength + 3 - xStart, j + 10 + (taskLabel.isEmpty() ? 33 : 44) - yStart, -1073741824, -1073741824);
                this.font.draw(framePose,villagerHover.func_70005_c_(), i + 10 - xStart, j + 10 - yStart, 0x909090);
                this.font.draw(framePose,villagerHover.getNativeOccupationName(), i + 10 - xStart, j + 10 - yStart + 11, 0x909090);
                this.font.draw(framePose,villagerHover.getGameOccupationName(this.player.getName().getString()), i + 10 - xStart, j + 10 - yStart + 22, 0x909090);
            } else {
                this.drawGradient(i + 10 - 3 - xStart, j + 10 - 3 - yStart, i + 10 + stringlength + 3 - xStart, j + 10 + (taskLabel.isEmpty() ? 22 : 33) - yStart, -1073741824, -1073741824);
                this.font.draw(framePose,villagerHover.func_70005_c_(), i + 10 - xStart, j + 10 - yStart, 0x909090);
                this.font.draw(framePose,villagerHover.getNativeOccupationName(), i + 10 - xStart, j + 10 - yStart + 11, 0x909090);
            }
            if (!taskLabel.isEmpty()) this.font.draw(framePose, taskLabel, i + 10 - xStart,
                    j + 10 - yStart + (gameString ? 33 : 22), 0xB0B060);
        } else if (playerHover != null) {
            int stringlength = this.font.width(playerHover.getName().getString());
            this.drawGradient(i + 10 - 3 - xStart, j + 10 - 3 - yStart, i + 10 + stringlength + 3 - xStart, j + 10 + 11 - yStart, -1073741824, -1073741824);
            this.font.draw(framePose,playerHover.getName().getString(), i + 10 - xStart, j + 10 - yStart, 0x909090);
        } else if (locHover != null) {
            Building building = locHover.getBuilding(this.townHall.world);
            boolean unreachable = building != null && this.townHall.regionMapper != null
                    && !building.isReachableFromRegion(this.townHall.regionMapper.thRegion);
            // A synchronized plan may be temporarily missing while opening the map.
            String title = locHover.getIBuildingPlan() == null ? locHover.planKey : locHover.getNativeName();
            if (unreachable) title += " - " + LanguageUtilities.string("panels.unreachablebuilding");
            long now = System.nanoTime();
            String language = org.millenaire.common.config.MillConfigValues.effective_language;
            if (tooltipLocation != locHover || tooltipLevel != locHover.level
                    || tooltipConstruction != constructionHover || tooltipPinned!=(pinnedLocation!=null)
                    || !java.util.Objects.equals(tooltipTitle, title)
                    || !java.util.Objects.equals(tooltipLanguage, language)
                    || now - tooltipRefreshNanos >= 500_000_000L) {
                tooltipDetails = BuildingMapTooltip.describe(locHover, this.townHall, title, constructionHover,true);
                tooltipLocation = locHover;
                tooltipLevel = locHover.level;
                tooltipConstruction = constructionHover;
                tooltipPinned=pinnedLocation!=null;
                tooltipTitle = title;
                tooltipLanguage = language;
                tooltipRefreshNanos = now;
            }
            // ItemRenderer uses screen coordinates, not the translated parchment pose.
            // Defer the complete tooltip until GuiText has rendered the map and widgets.
            showBuildingTooltip = true;
        }






    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        showBuildingTooltip = false;
        panelVisible=false;
        if(pageNum!=0){pinnedLocation=null;hoveredLocation=null;}
        super.render(pose, mouseX, mouseY, partialTicks);
        if (showBuildingTooltip && !tooltipDetails.isEmpty()) {
            drawBuildingTooltip(pose, mouseX, mouseY);
        }
    }

    @Override public boolean mouseClicked(double x,double y,int button){
        if(mapType==VILLAGE_MAP&&pageNum==0&&button==0){
            if(pinnedLocation!=null){
                if(x>=closeX&&x<closeX+closeSize&&y>=closeY&&y<closeY+closeSize){
                    pinnedLocation=null;tooltipRefreshNanos=0;panelVisible=false;
                }
                return true;
            }
            if(panelVisible&&hoveredLocation!=null&&Math.abs(x-hoverX)<=3&&Math.abs(y-hoverY)<=3){
                pinnedLocation=hoveredLocation;pinLeft=panelLeft;pinTop=panelTop;
                tooltipRefreshNanos=0;return true;
            }
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE&&pinnedLocation!=null){
            pinnedLocation=null;tooltipRefreshNanos=0;return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }

    private TooltipLayout layoutBuildingTooltip(int maxWidth) {
        List<TooltipText> texts = new ArrayList<>();
        List<TooltipIcon> icons = new ArrayList<>();
        int usedWidth = 0;
        int y = 0; // keep the title at the top; reserve space beside it, not above it
        int columns = Math.max(1, maxWidth / 18);
        for (var line : tooltipDetails) {
            if (!line.icons().isEmpty()) y += 3;
            if(!line.text().isEmpty()){
                // Both hover and pinned layouts reserve the same header space.
                // A wrapped first title keeps its entire text clear of the button.
                int headerReserve=y<12?16:0;
                for (var part : font.split(new net.minecraft.network.chat.TextComponent(line.text()), Math.max(1,maxWidth-headerReserve))) {
                    texts.add(new TooltipText(part, line.color(), y,line.detail()));
                    usedWidth = Math.max(usedWidth, font.width(part)+(y<12?16:0));
                    y += 11;
                }
            }
            if(!line.portraits().isEmpty()){
                int count=line.portraits().size();
                for(int n=0;n<count;n++){
                    var portrait=line.portraits().get(n);
                    icons.add(new TooltipIcon(ItemStack.EMPTY,(n%columns)*18,y+(n/columns)*18,portrait.detail(),List.of(),true,portrait));
                }
                usedWidth=Math.max(usedWidth,Math.min(columns,count)*18);
                y+=((count+columns-1)/columns)*18;
            }
            if (!line.icons().isEmpty()) {
                int count = line.icons().size();
                for (int n = 0; n < count; n++) {
                    int x = (n % columns) * 18;
                    icons.add(new TooltipIcon(line.icons().get(n),x,y+(n/columns)*18,
                        n<line.iconDetails().size()?line.iconDetails().get(n):null,
                        n<line.recipes().size()?line.recipes().get(n):List.of(),line.action()));
                }
                usedWidth = Math.max(usedWidth, Math.min(columns, count) * 18);
                y += ((count + columns - 1) / columns) * 18;
            }
        }
        return new TooltipLayout(texts,icons,Math.max(usedWidth,60),y);
    }

    private void drawBuildingTooltip(PoseStack pose, int mouseX, int mouseY) {
        int availableWidth = Math.max(18, this.width - 20);
        int availableHeight = Math.max(1, this.height - 20);
        TooltipLayout layout = layoutBuildingTooltip(Math.min(240, availableWidth));
        if (layout.height() > availableHeight && availableWidth > 240)
            layout = layoutBuildingTooltip(availableWidth);
        // In unusually small windows or very large assortments, scale both
        // the text and the actual item models by the same factor.
        float fit = Math.min(1.0f, availableHeight / (float)Math.max(1, layout.height()));
        int w = (int)Math.ceil(layout.width() * fit);
        int h = (int)Math.ceil(layout.height() * fit);
        int left = mouseX + 12 + w + 5 <= this.width
                ? mouseX + 12 : mouseX - w - 12;
        left = Math.max(5, Math.min(left, this.width - w - 5));
        int top = Math.max(5, Math.min(mouseY + 8, this.height - h - 5));
        if(pinnedLocation!=null){left=Math.max(5,Math.min(pinLeft,this.width-w-5));top=Math.max(5,Math.min(pinTop,this.height-h-5));}
        panelLeft=left;panelTop=top;panelWidth=w;panelHeight=h;panelVisible=true;
        closeSize=Math.max(1,(int)Math.ceil(12*fit));closeX=left+w-closeSize;closeY=top;

        pose.pushPose();
        try {
            pose.translate(left, top, 200);
            pose.scale(fit, fit, 1);
            fillGradient(pose, -3, -3, layout.width() + 3, layout.height() + 3,
                    0xF0101010, 0xF0101010);
            for(var row:layout.text())font.draw(pose,row.text(),0,row.y(),row.color());
            if(pinnedLocation!=null){
                fill(pose,layout.width()-12,0,layout.width(),12,0xff563232);
                String cross="×";
                font.draw(pose,cross,layout.width()-12+(12-font.width(cross))/2f,(12-font.lineHeight)/2f,0xffffff);
            }
        } finally {
            pose.popPose();
        }

        // Vanilla's inventory renderer draws the real item model, including
        // custom Millenaire assets, resource packs and NBT-dependent variants.
        if (layout.icons().isEmpty()) {
            if(pinnedLocation!=null)drawPinnedDetail(layout,mouseX,mouseY,fit);
            return;
        }
        PoseStack modelView = RenderSystem.getModelViewStack();
        float oldOffset = itemRenderer.blitOffset;
        modelView.pushPose();
        try {
            modelView.translate(left, top, 0);
            modelView.scale(fit, fit, 1);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            RenderSystem.enableDepthTest();
            itemRenderer.blitOffset = 300.0f;
            for (var icon : layout.icons()){
                if(icon.portrait()!=null){
                    var texture=icon.portrait().texture();
                    if(!portraitAvailable(texture))
                        itemRenderer.renderAndDecorateItem(new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD),icon.x(),icon.y());
                }else itemRenderer.renderAndDecorateItem(icon.stack(), icon.x(), icon.y());
            }
        } finally {
            itemRenderer.blitOffset = oldOffset;
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
        }
        // All Millenaire humanoid models use the 64x32 skin layout. UVs
        // are normalized, so higher-resolution resource pack skins work too.
        pose.pushPose();
        try{
            pose.translate(left,top,350);pose.scale(fit,fit,1);
            RenderSystem.enableBlend();
            for(var icon:layout.icons()){
                if(icon.portrait()==null)continue;
                var texture=icon.portrait().texture();
                if(!portraitAvailable(texture))continue;
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0,texture);
                RenderSystem.setShaderColor(1,1,1,1);
                blit(pose,icon.x(),icon.y(),16,16,8,8,8,8,64,32);
                blit(pose,icon.x(),icon.y(),16,16,40,8,8,8,64,32);
            }
        }finally{RenderSystem.disableBlend();pose.popPose();}
        if(pinnedLocation!=null)drawPinnedDetail(layout,mouseX,mouseY,fit);
    }

    private void drawPinnedDetail(TooltipLayout layout,int mouseX,int mouseY,float fit){
        if(fit<=0)return;
        float x=(mouseX-panelLeft)/fit,y=(mouseY-panelTop)/fit;
        if(x<0||x>layout.width()||y<0||y>layout.height())return;
        for(var icon:layout.icons())if(x>=icon.x()&&x<icon.x()+16&&y>=icon.y()&&y<icon.y()+16){
            drawMapDetail(icon,mouseX,mouseY);return;
        }
        for(var line:layout.text())if(line.detail()!=null&&y>=line.y()&&y<line.y()+11){
            drawMapDetail(new TooltipIcon(ItemStack.EMPTY,0,0,line.detail(),List.of(),true),mouseX,mouseY);
            return;
        }
    }

    /** Draw the secondary tooltip after every primary item, at a higher Z.
     * Real stacks with counts; no fabricated recipe for unknown outputs. */
    private void drawMapDetail(TooltipIcon icon,int mouseX,int mouseY){
        List<FormattedCharSequence> text=new ArrayList<>();
        int maxWidth=Math.max(40,Math.min(170,width-18));
        if(!icon.action()&&!icon.stack().isEmpty())
            text.addAll(font.split(icon.stack().getHoverName(),maxWidth));
        if(icon.detail()!=null&&!icon.detail().isBlank()){
            String detail=icon.detail();
            if(!icon.recipes().isEmpty()&&!icon.action())detail="";
            for(String part:detail.split("; "))if(!part.isBlank())
                text.addAll(font.split(new net.minecraft.network.chat.TextComponent(part),maxWidth));
        }
        List<TooltipIcon> recipeIcons=new ArrayList<>();
        List<int[]> arrows=new ArrayList<>();
        int y=text.size()*11,w=40;
        for(var line:text)w=Math.max(w,font.width(line));
        int columns=Math.max(2,maxWidth/20);
        for(var recipe:icon.recipes()){
            int n=0;y+=3;
            for(var stack:recipe.inputs()){
                recipeIcons.add(new TooltipIcon(stack,(n%columns)*20,y+(n/columns)*20,null,List.of(),false));n++;
            }
            arrows.add(new int[]{(n%columns)*20,y+(n/columns)*20+5});n++;
            for(var stack:recipe.outputs()){
                recipeIcons.add(new TooltipIcon(stack,(n%columns)*20,y+(n/columns)*20,null,List.of(),false));n++;
            }
            w=Math.max(w,Math.min(n,columns)*20);y+=((n+columns-1)/columns)*20;
        }
        if(y==0)return;
        float fit=Math.min(1f,Math.min((width-12)/(float)w,(height-12)/(float)y));
        int sw=(int)Math.ceil(w*fit),sh=(int)Math.ceil(y*fit);
        int left=mouseX+12+sw+5<=width?mouseX+12:mouseX-sw-12;
        left=Math.max(5,Math.min(left,width-sw-5));
        int top=Math.max(5,Math.min(mouseY+8,height-sh-5));
        framePose.pushPose();
        try{
            framePose.translate(left,top,600);framePose.scale(fit,fit,1);
            fillGradient(framePose,-4,-4,w+4,y+4,0xff160f20,0xff160f20);
            fill(framePose,-4,-4,w+4,-3,0xff754c99);
            for(int i=0;i<text.size();i++)font.draw(framePose,text.get(i),0,i*11,0xffffff);
            for(int[] arrow:arrows)font.draw(framePose,"→",arrow[0]+3,arrow[1],0xffffff);
        }finally{framePose.popPose();}
        if(recipeIcons.isEmpty())return;
        PoseStack model=RenderSystem.getModelViewStack();float old=itemRenderer.blitOffset;
        model.pushPose();
        try{
            model.translate(left,top,0);model.scale(fit,fit,1);RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();RenderSystem.enableDepthTest();itemRenderer.blitOffset=700;
            for(var item:recipeIcons)itemRenderer.renderAndDecorateItem(item.stack(),item.x(),item.y());
            // Use the vanilla inventory overlay, with an explicit count even
            // for one item. Keep the same model-view transform as the icons.
            // Render every model first so another model cannot cover a count.
            for(var item:recipeIcons)itemRenderer.renderGuiItemDecorations(font,item.stack(),item.x(),item.y(),
                    item.stack().getCount()>1?Integer.toString(item.stack().getCount()):null);
        }finally{itemRenderer.blitOffset=old;model.popPose();RenderSystem.applyModelViewMatrix();Lighting.setupFor3DItems();}
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
        portraitResources.clear();
        this.textBook = this.bookManager.adjustTextBookLineLength(this.textBook);
        if (this.mapType == 1 && this.townHall != null) {
            ClientSender.requestMapInfo(this.townHall);
        }
    }

}

