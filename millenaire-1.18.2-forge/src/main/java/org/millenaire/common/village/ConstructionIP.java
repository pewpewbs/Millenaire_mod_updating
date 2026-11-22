/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.common.village;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import org.millenaire.common.buildingplan.BuildingBlock;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;

public class ConstructionIP {
    private final Building townHall;
    private BuildingBlock[] bblocks = null;
    private int bblocksPos = 0;
    private final int id;
    private BuildingLocation buildingLocation;
    private boolean bblocksChanged = false;
    private final boolean wallConstruction;
    private MillVillager builder = null;

    public ConstructionIP(Building th, int id, boolean wallConstruction) {
        this.townHall = th;
        this.id = id;
        this.wallConstruction = wallConstruction;
    }

    public boolean areBlocksLeft() {
        if (this.bblocks == null) {
            return false;
        }
        return this.bblocksPos < this.bblocks.length;
    }

    public void clearBblocks() {
        this.townHall.markSaveNeeded();
        this.bblocksChanged = true;
        this.bblocks = null;
    }

    public void clearBuildingLocation() {
        this.townHall.markSaveNeeded();
        this.buildingLocation = null;
    }

    public BuildingBlock[] getBblocks() {
        return this.bblocks;
    }

    public int getBblocksPos() {
        return this.bblocksPos;
    }

    public MillVillager getBuilder() {
        return this.builder;
    }

    public BuildingLocation getBuildingLocation() {
        return this.buildingLocation;
    }

    public BuildingBlock getCurrentBlock() {
        if (this.bblocks == null) {
            return null;
        }
        if (this.bblocksPos >= this.bblocks.length) {
            return null;
        }
        return this.bblocks[this.bblocksPos];
    }

    public int getId() {
        return this.id;
    }

    public void incrementBblockPos() {
        this.townHall.markSaveNeeded();
        ++this.bblocksPos;
        if (!this.areBlocksLeft()) {
            this.bblocks = null;
            this.bblocksPos = 0;
            this.bblocksChanged = true;
        }
    }

    public boolean isBblocksChanged() {
        return this.bblocksChanged;
    }

    public boolean isWallConstruction() {
        return this.wallConstruction;
    }

    public void readBblocks() {
        File file = new File(MillCommonUtilities.getBuildingsDir(this.townHall.world),
            this.townHall.getPos().getPathString() + "_bblocks_" + this.id + ".bin");
        try {
            if (file.exists()) {
                BuildingBlock[] loaded = org.millenaire.common.buildingplan.SavedPlanReader.blocks(file.toPath(), this.bblocksPos);
                this.bblocks = loaded;
            } else if (this.bblocksPos != 0) throw new IOException("Missing construction plan with nonzero progress");
        } catch (Exception failure) {
            this.townHall.protectPlanFiles(file, failure);
        }
    }

    public void setBblockPos(int pos) {
        this.bblocksPos = pos;
    }

    public void setBuilder(MillVillager builder) {
        this.builder = builder;
    }

    public void setBuildingLocation(BuildingLocation buildingLocation) {
        this.buildingLocation = buildingLocation;
    }

    public void startNewConstruction(BuildingLocation bl, BuildingBlock[] bblocks) {
        this.townHall.markSaveNeeded();
        this.buildingLocation = bl;
        this.bblocks = bblocks;
        this.bblocksPos = 0;
        this.bblocksChanged = true;
        if (this.townHall.winfo != null) {
            this.townHall.winfo.addBuildingLocationToMap(bl);
        }
    }

    public void writeBblocks() {
        this.townHall.requirePlanFilesWritable();
        File dir = MillCommonUtilities.getBuildingsDir(this.townHall.world);
        java.nio.file.Path target = new File(dir, this.townHall.getPos().getPathString()+"_bblocks_"+this.id+".bin").toPath();
        BuildingBlock[] blocks = this.getBblocks();
        try {
            if (blocks == null) org.millenaire.common.utilities.AtomicSaveFile.delete(target);
            else org.millenaire.common.utilities.AtomicSaveFile.write(target, stream -> {
                DataOutputStream ds = new DataOutputStream(stream);
                ds.writeInt(blocks.length);
                for (BuildingBlock block : blocks) {
                    ds.writeInt(block.p.getiX()); ds.writeShort(block.p.getiY()); ds.writeInt(block.p.getiZ());
                    block.writeState(ds);
                }
                ds.flush();
            });
            org.millenaire.common.utilities.AtomicSaveFile.retryIfFailed(() -> this.bblocksChanged = true);
            this.bblocksChanged = false;
        } catch (IOException error) { throw new java.io.UncheckedIOException("Cannot save construction blocks", error); }
    }
}
