/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.NetworkManager
 *  net.minecraft.network.play.server.SPacketUpdateTileEntity
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package org.millenaire.common.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.client.book.BookManager;
import org.millenaire.client.book.TextBook;
import org.millenaire.client.gui.text.GuiText;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.buildingmanagers.PanelContentGenerator;

public class TileEntityPanel
extends BlockEntity {
    public static final int MAP_VILLAGE_MAP = 1;
    public static final int ETAT_CIVIL = 1;
    public static final int CONSTRUCTIONS = 2;
    public static final int PROJECTS = 3;
    public static final int CONTROLLED_PROJECTS = 4;
    public static final int HOUSE = 5;
    public static final int RESOURCES = 6;
    public static final int ARCHIVES = 7;
    public static final int VILLAGE_MAP = 8;
    public static final int MILITARY = 9;
    public static final int INN_TRADE_GOODS = 10;
    public static final int INN_VISITORS = 11;
    public static final int MARKET_MERCHANTS = 12;
    public static final int CONTROLLED_MILITARY = 13;
    public static final int VISITORS = 14;
    public static final int WALLS = 15;
    public static final int MARVEL_DONATIONS = 20;
    public static final int MARVEL_RESOURCES = 21;
    public List<PanelUntranslatedLine> untranslatedLines = new ArrayList<PanelUntranslatedLine>();
    public List<PanelDisplayLine> displayLines = new ArrayList<PanelDisplayLine>();
    public Point buildingPos = null;
    public long villager_id = 0L;
    public int panelType = 0;
    public ResourceLocation texture = null;

    public TileEntityPanel(net.minecraft.core.BlockPos pos, BlockState state) { super(MillBlockEntities.PANEL,pos,state); }
    public TextBook getFullText(Player player) {
        if (this.panelType == 0 || this.buildingPos == null) {
            return null;
        }
        Building building = Mill.clientWorld.getBuilding(this.buildingPos);
        if (this.panelType == 1) {
            return PanelContentGenerator.generateEtatCivil(building);
        }
        if (this.panelType == 2) {
            return PanelContentGenerator.generateConstructions(building);
        }
        if (this.panelType == 3) {
            return PanelContentGenerator.generateProjects(player, building);
        }
        if (this.panelType == 5) {
            return PanelContentGenerator.generateHouse(building);
        }
        if (this.panelType == 7) {
            return PanelContentGenerator.generateArchives(building, this.villager_id);
        }
        if (this.panelType == 6) {
            return PanelContentGenerator.generateResources(building);
        }
        if (this.panelType == 8) {
            return PanelContentGenerator.generateVillageMap(building);
        }
        if (this.panelType == 9) {
            return PanelContentGenerator.generateMilitary(building);
        }
        if (this.panelType == 10) {
            return PanelContentGenerator.generateInnGoods(building);
        }
        if (this.panelType == 11) {
            return PanelContentGenerator.generateInnVisitors(building);
        }
        if (this.panelType == 12) {
            return PanelContentGenerator.generateVisitors(building, true);
        }
        if (this.panelType == 14) {
            return PanelContentGenerator.generateVisitors(building, false);
        }
        if (this.panelType == 15) {
            return PanelContentGenerator.generateWalls(player, building);
        }
        if (this.panelType == 20) {
            return building.getMarvelManager().generateDonationPanelText();
        }
        if (this.panelType == 21) {
            return building.getMarvelManager().generateResourcesPanelText();
        }
        return null;
    }

    public int getMapType() {
        if (this.panelType == 8) {
            return 1;
        }
        return 0;
    }

    private BlockState getState() {
        return this.level.getBlockState(this.worldPosition);
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }

    @Override public void load(CompoundTag compound) {
        super.load(compound);
        this.texture = null; this.displayLines.clear();
        this.buildingPos = Point.read(compound, "buildingPos");
        this.panelType = compound.getInt("panelType");
        this.villager_id = compound.getLong("villager_id");
        if (compound.contains("texture")) {
            String textureString = compound.getString("texture");
            this.texture = new ResourceLocation(textureString);
        }
        int pos = 0;
        this.untranslatedLines.clear();
        while (compound.contains("Lines_" + pos)) {
            this.untranslatedLines.add(PanelUntranslatedLine.readFromNBT(compound.getCompound("Lines_" + pos)));
            ++pos;
        }
    }


    private String translatedLines_cutLines(Font fontrenderer, String text, int maxLength) {
        if (fontrenderer.width(text) > maxLength) {
            while (!text.isEmpty() && fontrenderer.width(text + "...") > maxLength) {
                text = text.substring(0, text.length() - 1);
            }
            text = text + "...";
        }
        return text;
    }

    @OnlyIn(value=Dist.CLIENT)
    public void translateLines(Font fontrenderer) {
        this.displayLines.clear();
        int nbExtraLines = 0;
        for (PanelUntranslatedLine line : this.untranslatedLines) {
            PanelDisplayLine displayLine = new PanelDisplayLine();
            displayLine.leftIcon = line.leftIcon;
            displayLine.middleIcon = line.middleIcon;
            displayLine.rightIcon = line.rightIcon;
            displayLine.fullLine = LanguageUtilities.string(line.fullLine);
            displayLine.leftColumn = LanguageUtilities.string(line.leftColumn);
            displayLine.rightColumn = LanguageUtilities.string(line.rightColumn);
            displayLine.centerLine = line.centerLine;
            int maxLength = 80;
            if (displayLine.leftIcon.getItem() != Items.AIR) {
                maxLength = 62;
            }
            displayLine.leftColumn = this.translatedLines_cutLines(fontrenderer, displayLine.leftColumn, 32);
            displayLine.rightColumn = this.translatedLines_cutLines(fontrenderer, displayLine.rightColumn, 32);
            List<String> splitStrings = BookManager.splitStringByLength(new GuiText.FontRendererWrapped(fontrenderer), displayLine.fullLine, maxLength);
            displayLine.fullLine = splitStrings.isEmpty() ? "" : splitStrings.get(0);
            this.displayLines.add(displayLine);
            if (splitStrings.size() <= 1 || this.untranslatedLines.size() + nbExtraLines + 1 >= 8) continue;
            PanelDisplayLine extraDisplayLine = new PanelDisplayLine();
            extraDisplayLine.fullLine = splitStrings.get(1);
            this.displayLines.add(extraDisplayLine);
            ++nbExtraLines;
        }
    }

    public void triggerUpdate() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        try {
            if (this.buildingPos != null) {
                this.buildingPos.write(compound, "buildingPos");
            }
            compound.putInt("panelType", this.panelType);
            compound.putLong("villager_id", this.villager_id);
            if (this.texture != null) {
                compound.putString("texture", this.texture.toString());
            }
            for (int i = 0; i < this.untranslatedLines.size(); ++i) {
                compound.put("Lines_" + i, (Tag)this.untranslatedLines.get(i).writeToNBT(new CompoundTag()));
            }
        }
        catch (Exception e) {
            MillLog.printException("Error writing panel", e);
        }
    }

    public static class PanelUntranslatedLine {
        private String[] fullLine = new String[]{""};
        private String[] leftColumn = new String[]{""};
        public String[] rightColumn = new String[]{""};
        public ItemStack leftIcon = ItemStack.EMPTY;
        public ItemStack middleIcon = ItemStack.EMPTY;
        public ItemStack rightIcon = ItemStack.EMPTY;
        public boolean centerLine = true;

        public static PanelUntranslatedLine readFromNBT(CompoundTag compound) {
            PanelUntranslatedLine line = new PanelUntranslatedLine();
            line.fullLine = PanelUntranslatedLine.readText(compound, "fullLine");
            line.leftColumn = PanelUntranslatedLine.readText(compound, "leftColumn");
            line.rightColumn = PanelUntranslatedLine.readText(compound, "rightColumn");
            line.leftIcon = ItemStack.of(compound.getCompound("leftIcon"));
            line.middleIcon = ItemStack.of(compound.getCompound("middleIcon"));
            line.rightIcon = ItemStack.of(compound.getCompound("rightIcon"));
            line.centerLine = compound.getBoolean("centerLine");
            return line;
        }

        private static String[] readText(CompoundTag compound, String key) {
            ArrayList<String> lineFragment = new ArrayList<String>();
            int i = 0;
            while (compound.contains(key + "_" + i)) {
                lineFragment.add(compound.getString(key + "_" + i));
                ++i;
            }
            return lineFragment.toArray(new String[0]);
        }

        private static void writeText(CompoundTag compound, String[] text, String key) {
            for (int i = 0; i < text.length; ++i) {
                compound.putString(key + "_" + i, text[i]);
            }
        }

        public void setFullLine(String[] fullLine) {
            this.fullLine = fullLine;
            for (int i = 0; i < this.fullLine.length; ++i) {
                if (this.fullLine[i] != null) continue;
                this.fullLine[i] = "";
            }
        }

        public void setLeftColumn(String[] leftColumn) {
            this.leftColumn = leftColumn;
            for (int i = 0; i < this.leftColumn.length; ++i) {
                if (this.leftColumn[i] != null) continue;
                this.leftColumn[i] = "";
            }
        }

        public void setRightColumn(String[] rightColumn) {
            this.rightColumn = rightColumn;
            for (int i = 0; i < this.rightColumn.length; ++i) {
                if (this.rightColumn[i] != null) continue;
                this.rightColumn[i] = "";
            }
        }

        public CompoundTag writeToNBT(CompoundTag compound) {
            PanelUntranslatedLine.writeText(compound, this.fullLine, "fullLine");
            PanelUntranslatedLine.writeText(compound, this.leftColumn, "leftColumn");
            PanelUntranslatedLine.writeText(compound, this.rightColumn, "rightColumn");
            compound.put("leftIcon", (Tag)this.leftIcon.save(new CompoundTag()));
            compound.put("middleIcon", (Tag)this.middleIcon.save(new CompoundTag()));
            compound.put("rightIcon", (Tag)this.rightIcon.save(new CompoundTag()));
            compound.putBoolean("centerLine", this.centerLine);
            return compound;
        }
    }

    public static class PanelPacketInfo {
        public Point pos;
        public Point buildingPos;
        public String[][] lines;
        public long villager_id;
        public int panelType;

        public PanelPacketInfo(Point pos, String[][] lines, Point buildingPos, int panelType, long village_id) {
            this.pos = pos;
            this.lines = lines;
            this.buildingPos = buildingPos;
            this.panelType = panelType;
            this.villager_id = village_id;
        }
    }

    public static class PanelDisplayLine {
        public String fullLine = "";
        public String leftColumn = "";
        public String rightColumn = "";
        public ItemStack leftIcon = ItemStack.EMPTY;
        public ItemStack middleIcon = ItemStack.EMPTY;
        public ItemStack rightIcon = ItemStack.EMPTY;
        public boolean centerLine = true;
    }
}

