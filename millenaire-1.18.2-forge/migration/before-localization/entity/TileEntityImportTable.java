/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.BlockWallSign
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.NetworkManager
 *  net.minecraft.network.play.server.SPacketUpdateTileEntity
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntitySign
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.text.TextComponentString
 */
package org.millenaire.common.entity;

import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import org.millenaire.client.network.ClientSender;
import org.millenaire.common.buildingplan.BuildingImportExport;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;

public class TileEntityImportTable
extends BlockEntity {
    public TileEntityImportTable(net.minecraft.core.BlockPos pos, BlockState state) {
        super(MillBlockEntities.IMPORT_TABLE, pos, state);
    }

    private String buildingKey = null;
    private int variation = 0;
    private int upgradeLevel = 0;
    private int length;
    private int width;
    private int startingLevel = -1;
    private int orientation = 0;
    private boolean exportSnow = false;
    private boolean importMockBlocks = true;
    private boolean autoconvertToPreserveGround = true;
    private boolean exportRegularChests = false;
    private Point parentTablePos = null;

    public void activate(Player player) {
        if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == MillItems.SUMMONING_WAND) {
            if (player.level.isClientSide && this.buildingKey != null) {
                MillLog.temp((Object)this, "Activating. Building key: " + this.buildingKey);
                ClientSender.importTableImportBuildingPlan(player, new Point(this.worldPosition), BuildingImportExport.EXPORT_DIR, this.buildingKey, false, this.variation, this.upgradeLevel, this.orientation, this.importMockBlocks);
            } else {
                this.sendUpdates();
            }
        } else if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == MillItems.NEGATION_WAND) {
            if (player.level.isClientSide && this.buildingKey != null) {
                BuildingImportExport.importTableExportBuildingPlan(player.level, this, this.getUpgradeLevel());
            } else {
                this.sendUpdates();
            }
        } else if (!player.level.isClientSide) {
            this.sendUpdates();
            ServerSender.displayImportTableGUI(player, new Point(this.getBlockPos()));
        }
    }

    public boolean autoconvertToPreserveGround() {
        return this.autoconvertToPreserveGround;
    }

    public boolean exportRegularChests() {
        return this.exportRegularChests;
    }

    public boolean exportSnow() {
        return this.exportSnow;
    }

    public String getBuildingKey() {
        return this.buildingKey;
    }

    public int getLength() {
        return this.length;
    }

    public int getOrientation() {
        return this.orientation;
    }

    public Point getParentTablePos() {
        return this.parentTablePos;
    }

    public Point getPosPoint() {
        return new Point(this.worldPosition);
    }

    public int getStartingLevel() {
        return this.startingLevel;
    }

    private BlockState getState() {
        return this.level.getBlockState(this.worldPosition);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    public int getUpgradeLevel() {
        return this.upgradeLevel;
    }

    public int getVariation() {
        return this.variation;
    }

    public int getWidth() {
        return this.width;
    }

    public boolean importMockBlocks() {
        return this.importMockBlocks;
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
    }

    @Override public void load(CompoundTag compound) {
        super.load(compound);
        this.buildingKey = compound.getString("buildingKey");
        this.variation = compound.getInt("variation");
        this.length = compound.getInt("length");
        this.width = compound.getInt("width");
        this.upgradeLevel = compound.getInt("upgradeLevel");
        this.startingLevel = compound.getInt("startingLevel");
        this.orientation = compound.getInt("orientation");
        this.exportSnow = compound.getBoolean("exportSnow");
        this.importMockBlocks = compound.getBoolean("importMockBlocks");
        this.autoconvertToPreserveGround = compound.getBoolean("autoconvertToPreserveGround");
        this.exportRegularChests = compound.getBoolean("exportRegularChests");
        this.parentTablePos = Point.read(compound, "parentTablePos");
    }

    private void sendUpdates() {
        this.level.sendBlockUpdated(this.worldPosition, this.getState(), this.getState(), 3);
        this.setChanged();
    }

    public void setAutoconvertToPreserveGround(boolean autoconvertToPreserveGround) {
        this.autoconvertToPreserveGround = autoconvertToPreserveGround;
    }

    public void setBuildingKey(String buildingKey) {
        this.buildingKey = buildingKey;
    }

    public void setExportRegularChests(boolean exportRegularChests) {
        this.exportRegularChests = exportRegularChests;
    }

    public void setExportSnow(boolean exportSnow) {
        this.exportSnow = exportSnow;
    }

    public void setImportMockBlocks(boolean importMockBlocks) {
        this.importMockBlocks = importMockBlocks;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public void setParentTablePos(Point parentTablePos) {
        this.parentTablePos = parentTablePos;
    }

    public void setStartingLevel(int startingLevel) {
        this.startingLevel = startingLevel;
    }

    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

    private void updateAttachedSign() {
        Point signPos = new Point(this.worldPosition).getRelative(-1.0, 0.0, 0.0);
        signPos.setBlockState(this.level, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.WEST));
        SignBlockEntity sign = signPos.getSign(this.level);
        if (sign == null) return;
        sign.setMessage(0, this.buildingKey != null ? new TextComponent(this.buildingKey + "_" + (char)(65 + this.variation) + this.upgradeLevel) : new TextComponent(""));
        sign.setMessage(1, new TextComponent("Start level: " + this.startingLevel));
        sign.setMessage(2, new TextComponent(this.length + "x" + this.width));
        sign.setChanged();
        BlockState iblockstate = this.level.getBlockState(sign.getBlockPos());
        this.level.sendBlockUpdated(sign.getBlockPos(), iblockstate, iblockstate, 3);
    }

    public void updatePlan(String buildingKey, int length, int width, int variation, int level, int startLevel, Player player) {
        this.buildingKey = buildingKey;
        MillLog.temp((Object)this, "updatePlan : Updating buildingKey to: " + buildingKey);
        this.length = length;
        this.width = width;
        this.variation = variation;
        this.upgradeLevel = level;
        this.startingLevel = startLevel;
        this.updateAttachedSign();
        if (player != null) {
            this.sendUpdates();
        }
    }

    public void updateSettings(int upgradeLevel, int orientation, int startingLevel, boolean exportSnow, boolean importMockBlocks, boolean autoconvertToPreserveGround, boolean exportRegularChests, Player player) {
        this.upgradeLevel = upgradeLevel;
        this.orientation = orientation;
        this.startingLevel = startingLevel;
        this.exportSnow = exportSnow;
        this.importMockBlocks = importMockBlocks;
        this.autoconvertToPreserveGround = autoconvertToPreserveGround;
        this.exportRegularChests = exportRegularChests;
        this.updateAttachedSign();
        if (player != null) {
            this.sendUpdates();
        }
    }

    @Override protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        if (this.buildingKey != null) {
            compound.putString("buildingKey", this.buildingKey);
        }
        compound.putInt("variation", this.variation);
        compound.putInt("length", this.length);
        compound.putInt("width", this.width);
        compound.putInt("upgradeLevel", this.upgradeLevel);
        compound.putInt("startingLevel", this.startingLevel);
        compound.putInt("orientation", this.orientation);
        compound.putBoolean("exportSnow", this.exportSnow);
        compound.putBoolean("importMockBlocks", this.importMockBlocks);
        compound.putBoolean("autoconvertToPreserveGround", this.autoconvertToPreserveGround);
        compound.putBoolean("exportRegularChests", this.exportRegularChests);
        if (this.parentTablePos != null) {
            this.parentTablePos.write(compound, "parentTablePos");
        }
    }
}

