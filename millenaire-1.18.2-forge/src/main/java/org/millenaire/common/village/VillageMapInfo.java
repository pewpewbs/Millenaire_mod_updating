/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockLog
 *  net.minecraft.init.Blocks
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 *  net.minecraft.world.chunk.Chunk
 */
package org.millenaire.common.village;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import org.millenaire.common.forge.Mill;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.ThreadSafeUtilities;
import org.millenaire.common.village.BuildingLocation;

public class VillageMapInfo
implements Cloneable {
    private static final int MAP_MARGIN = 5;
    private static final int BUILDING_MARGIN = 5;
    private static final int VALID_HEIGHT_DIFF = 10;
    public static final int UPDATE_FREQUENCY = 1000;
    public int length = 0;
    public int width = 0;
    public int chunkStartX = 0;
    public int chunkStartZ = 0;
    public int mapStartX = 0;
    public int mapStartZ = 0;
    public int yBaseline = 0;
    public short[][] topGround;
    public short[][] spaceAbove;
    public boolean[][] danger;
    public BuildingLocation[][] buildingLocRef;
    public boolean[][] canBuild;
    public boolean[][] buildingForbidden;
    public boolean[][] water;
    public boolean[][] tree;
    public boolean[][] buildTested = null;
    public boolean[][] topAdjusted;
    public boolean[][] path;
    public int frequency = 10;
    private List<BuildingLocation> buildingLocations = new ArrayList<BuildingLocation>();
    public Level world;
    public int lastUpdatedX;
    public int lastUpdatedZ;
    private int updateCounter;

    public static BuildingLocation[][] blArrayDeepClone(BuildingLocation[][] source) {
        BuildingLocation[][] target = new BuildingLocation[source.length][];
        for (int i = 0; i < source.length; ++i) {
            target[i] = (BuildingLocation[])source[i].clone();
        }
        return target;
    }

    public static boolean[][] booleanArrayDeepClone(boolean[][] source) {
        boolean[][] target = new boolean[source.length][];
        for (int i = 0; i < source.length; ++i) {
            target[i] = (boolean[])source[i].clone();
        }
        return target;
    }

    public static short[][] shortArrayDeepClone(short[][] source) {
        short[][] target = new short[source.length][];
        for (int i = 0; i < source.length; ++i) {
            target[i] = (short[])source[i].clone();
        }
        return target;
    }

    public void addBuildingLocationToMap(BuildingLocation bl) {
        if (MillConfigValues.LogWorldInfo >= 1) {
            MillLog.major(this, "Registering building location: " + bl);
        }
        this.buildingLocations.add(bl);
        int sx = Math.max(bl.minxMargin - this.mapStartX, 0);
        int sz = Math.max(bl.minzMargin - this.mapStartZ, 0);
        int ex = Math.min(bl.maxxMargin - this.mapStartX, this.length);
        int ez = Math.min(bl.maxzMargin - this.mapStartZ, this.width);
        for (int i = sx; i < ex; ++i) {
            for (int j = sz; j < ez; ++j) {
                this.buildingLocRef[i][j] = bl;
            }
        }
    }

    public VillageMapInfo clone() throws CloneNotSupportedException {
        VillageMapInfo o = (VillageMapInfo)super.clone();
        o.topGround = VillageMapInfo.shortArrayDeepClone(this.topGround);
        o.spaceAbove = VillageMapInfo.shortArrayDeepClone(this.spaceAbove);
        o.danger = VillageMapInfo.booleanArrayDeepClone(this.danger);
        o.buildingLocRef = VillageMapInfo.blArrayDeepClone(this.buildingLocRef);
        o.canBuild = VillageMapInfo.booleanArrayDeepClone(this.canBuild);
        o.buildingForbidden = VillageMapInfo.booleanArrayDeepClone(this.buildingForbidden);
        o.water = VillageMapInfo.booleanArrayDeepClone(this.water);
        o.tree = VillageMapInfo.booleanArrayDeepClone(this.tree);
        o.path = VillageMapInfo.booleanArrayDeepClone(this.path);
        o.topAdjusted = this.topAdjusted == null ? null : booleanArrayDeepClone(this.topAdjusted);
        o.buildTested = this.buildTested == null ? null : booleanArrayDeepClone(this.buildTested);
        o.buildingLocations = new ArrayList<BuildingLocation>();
        o.buildingLocations.addAll(this.buildingLocations);
        return o;
    }

    private void createWorldInfo(List<BuildingLocation> locations, int pstartX, int pstartZ, int endX, int endZ) throws MillLog.MillenaireException {
        if (MillConfigValues.LogWorldInfo >= 2) {
            MillLog.minor(this, "Creating world info: " + pstartX + "/" + pstartZ + "/" + endX + "/" + endZ);
        }
        this.chunkStartX = pstartX >> 4;
        this.chunkStartZ = pstartZ >> 4;
        this.mapStartX = this.chunkStartX << 4;
        this.mapStartZ = this.chunkStartZ << 4;
        this.length = ((endX >> 4) + 1 << 4) - this.mapStartX;
        this.width = ((endZ >> 4) + 1 << 4) - this.mapStartZ;
        this.frequency = (int)Math.max(1000.0 / (double)(this.length * this.width / 256), 10.0);
        if (this.frequency == 0) {
            throw new MillLog.MillenaireException("Null frequency in createWorldInfo.");
        }
        if (MillConfigValues.LogWorldInfo >= 1) {
            MillLog.major(this, "Creating world info: " + this.mapStartX + "/" + this.mapStartZ + "/" + this.length + "/" + this.width);
        }
        this.topGround = new short[this.length][this.width];
        this.spaceAbove = new short[this.length][this.width];
        this.danger = new boolean[this.length][this.width];
        this.buildingLocRef = new BuildingLocation[this.length][this.width];
        this.buildingForbidden = new boolean[this.length][this.width];
        this.canBuild = new boolean[this.length][this.width];
        this.buildTested = new boolean[this.length][this.width];
        this.water = new boolean[this.length][this.width];
        this.tree = new boolean[this.length][this.width];
        this.path = new boolean[this.length][this.width];
        this.topAdjusted = new boolean[this.length][this.width];
        this.buildingLocations = new ArrayList<BuildingLocation>();
        for (int i = 0; i < this.length; ++i) {
            for (int j = 0; j < this.width; ++j) {
                this.buildingLocRef[i][j] = null;
                this.canBuild[i][j] = false;
            }
        }
        for (BuildingLocation location : locations) {
            this.addBuildingLocationToMap(location);
        }
        for (int i = 0; i < this.length; i += 16) {
            for (int j = 0; j < this.width; j += 16) {
                try {
                    this.updateChunk(i, j);
                    continue;
                }
                catch (ThreadSafeUtilities.ChunkAccessException e) {
                    MillLog.error(this, "Chunk access violation while updating map info.");
                }
            }
        }
        this.lastUpdatedX = 0;
        this.lastUpdatedZ = 0;
    }

    public List<BuildingLocation> getBuildingLocations() {
        return this.buildingLocations;
    }

    public boolean isConstructionForbiddenHere(Point p) {
        if (p.getiX() < this.mapStartX || p.getiZ() < this.mapStartZ || p.getiX() >= this.mapStartX + this.length || p.getiZ() >= this.mapStartZ + this.width) {
            return false;
        }
        return this.buildingForbidden[p.getiX() - this.mapStartX][p.getiZ() - this.mapStartZ];
    }

    public void removeBuildingLocation(BuildingLocation bl) {
        for (BuildingLocation l : this.buildingLocations) {
            if (!l.isLocationSamePlace(bl)) continue;
            this.buildingLocations.remove(l);
            break;
        }
        int sx = Math.max(bl.minxMargin - this.mapStartX, 0);
        int sz = Math.max(bl.minzMargin - this.mapStartZ, 0);
        int ex = Math.min(bl.maxxMargin - this.mapStartX, this.length);
        int ez = Math.min(bl.maxzMargin - this.mapStartZ, this.width);
        for (int i = sx; i < ex; ++i) {
            for (int j = sz; j < ez; ++j) {
                this.buildingLocRef[i][j] = null;
            }
        }
    }

    public boolean update(Level world, List<BuildingLocation> locations, Point centre, int radius) throws MillLog.MillenaireException {
        Level previousWorld = this.world;
        int previousBaseline = this.yBaseline;
        this.world = world;
        this.yBaseline = centre.getiY();
        int startX = centre.getiX();
        int startZ = centre.getiZ();
        int endX = centre.getiX();
        int endZ = centre.getiZ();
        BuildingLocation blStartX = null;
        BuildingLocation blStartZ = null;
        BuildingLocation blEndX = null;
        BuildingLocation blEndZ = null;
        for (BuildingLocation location : locations) {
            if (location == null) continue;
            if (location.pos.getiX() - location.length / 2 < startX) {
                startX = location.pos.getiX() - location.length / 2;
                blStartX = location;
            }
            if (location.pos.getiX() + location.length / 2 > endX) {
                endX = location.pos.getiX() + location.length / 2;
                blEndX = location;
            }
            if (location.pos.getiZ() - location.width / 2 < startZ) {
                startZ = location.pos.getiZ() - location.width / 2;
                blStartZ = location;
            }
            if (location.pos.getiZ() + location.width / 2 <= endZ) continue;
            endZ = location.pos.getiZ() + location.width / 2;
            blEndZ = location;
        }
        if (MillConfigValues.LogWorldInfo >= 1) {
            MillLog.major(this, "WorldInfo Centre: " + centre);
            if (startX - 5 < centre.getiX() - radius - 5) {
                MillLog.major(this, "Pushing startX down by " + (startX - 5 - (centre.getiX() - radius - 5)) + " due to " + blStartX);
            } else {
                MillLog.major(this, "Using default value of " + (centre.getiX() - radius - 5) + " for startX");
            }
            if (startZ - 5 < centre.getiZ() - radius - 5) {
                MillLog.major(this, "Pushing startZ down by " + (startZ - 5 - (centre.getiZ() - radius - 5)) + " due to " + blStartZ);
            } else {
                MillLog.major(this, "Using default value of " + (centre.getiZ() - radius - 5) + " for startZ");
            }
            if (endX + 5 > centre.getiX() + radius + 5) {
                MillLog.major(this, "Pushing endX up by " + (endX + 5 - (centre.getiX() + radius + 5)) + " due to " + blEndX);
            } else {
                MillLog.major(this, "Using default value of " + (centre.getiX() + radius + 5) + " for endX");
            }
            if (endZ + 5 > centre.getiZ() + radius + 5) {
                MillLog.major(this, "Pushing endZ up by " + (endZ + 5 - (centre.getiZ() + radius + 5)) + " due to " + blEndZ);
            } else {
                MillLog.major(this, "Using default value of " + (centre.getiZ() + radius + 5) + " for endZ");
            }
        }
        startX = Math.min(startX - 5, centre.getiX() - radius - 5);
        startZ = Math.min(startZ - 5, centre.getiZ() - radius - 5);
        endX = Math.max(endX + 5, centre.getiX() + radius + 5);
        endZ = Math.max(endZ + 5, centre.getiZ() + radius + 5);
        if (MillConfigValues.LogWorldInfo >= 1) {
            MillLog.major(this, "Values: " + startX + "/" + startZ + "/" + endX + "/" + endZ);
        }
        int chunkStartXTemp = startX >> 4;
        int chunkStartZTemp = startZ >> 4;
        int mapStartXTemp = chunkStartXTemp << 4;
        int mapStartZTemp = chunkStartZTemp << 4;
        int lengthTemp = ((endX >> 4) + 1 << 4) - mapStartXTemp;
        int widthTemp = ((endZ >> 4) + 1 << 4) - mapStartZTemp;
        if (MillConfigValues.LogWorldInfo >= 1) {
            MillLog.major(this, "Values after chunks: " + mapStartXTemp + "/" + mapStartZTemp + "/" + (mapStartXTemp + lengthTemp) + "/" + (mapStartZTemp + widthTemp));
        }
        if (lengthTemp != this.length || widthTemp != this.width || mapStartXTemp != this.mapStartX || mapStartZTemp != this.mapStartZ || previousWorld != world || previousBaseline != this.yBaseline) {
            this.createWorldInfo(locations, startX, startZ, endX, endZ);
            return true;
        }
        for (BuildingLocation[] row : this.buildingLocRef) java.util.Arrays.fill(row, null);
        this.buildingLocations = new ArrayList<BuildingLocation>();
        for (BuildingLocation location : locations) {
            this.addBuildingLocationToMap(location);
        }
        this.updateNextChunk();
        return false;
    }

    private static final VillageTerrainColumn.Rules TERRAIN_RULES = new VillageTerrainColumn.Rules() {
        public boolean ground(Block b) { return BlockItemUtilities.isBlockGround(b); }
        public boolean solid(Block b) { return BlockItemUtilities.isBlockSolid(b); }
        public boolean liquid(Block b) { return BlockItemUtilities.isBlockLiquid(b); }
        public boolean forbidden(Block b) { return BlockItemUtilities.isBlockForbidden(b); }
        public boolean dangerous(Block b) { return BlockItemUtilities.isBlockDangerous(b); }
        public boolean water(Block b) { return BlockItemUtilities.isBlockWater(b); }
        public boolean fence(Block b) { return BlockItemUtilities.isFence(b); }
        public boolean path(Block b) { return BlockItemUtilities.isPath(b); }
        public boolean tree(BlockState state) { return state.is(net.minecraft.tags.BlockTags.LOGS); }
        public boolean explicitlyForbidden(Block b) { return MillConfigValues.forbiddenBlocks.contains(b); }
    };

    // Cache classifications only within one synchronous chunk scan.
    private static final class ScanRules implements VillageTerrainColumn.Rules {
        private final java.util.IdentityHashMap<Block,Integer> flags=new java.util.IdentityHashMap<>();
        private int flags(Block b){
            Integer known=flags.get(b);
            if(known!=null)return known;
            int v=(TERRAIN_RULES.ground(b)?1:0)|(TERRAIN_RULES.solid(b)?2:0)
                |(TERRAIN_RULES.liquid(b)?4:0)|(TERRAIN_RULES.forbidden(b)?8:0)
                |(TERRAIN_RULES.dangerous(b)?16:0)|(TERRAIN_RULES.water(b)?32:0)
                |(TERRAIN_RULES.fence(b)?64:0)|(TERRAIN_RULES.path(b)?128:0)
                |(TERRAIN_RULES.explicitlyForbidden(b)?256:0);
            flags.put(b,v);return v;
        }
        public boolean ground(Block b){return (flags(b)&1)!=0;}
        public boolean solid(Block b){return (flags(b)&2)!=0;}
        public boolean liquid(Block b){return (flags(b)&4)!=0;}
        public boolean forbidden(Block b){return (flags(b)&8)!=0;}
        public boolean dangerous(Block b){return (flags(b)&16)!=0;}
        public boolean water(Block b){return (flags(b)&32)!=0;}
        public boolean fence(Block b){return (flags(b)&64)!=0;}
        public boolean path(Block b){return (flags(b)&128)!=0;}
        public boolean explicitlyForbidden(Block b){return (flags(b)&256)!=0;}
        public boolean tree(BlockState state){return TERRAIN_RULES.tree(state);}
    }

    private void updateChunk(int startX, int startZ) throws ThreadSafeUtilities.ChunkAccessException {
        if (startX < 0 || startZ < 0 || startX + 16 > length || startZ + 16 > width) return;
        int cx = (mapStartX + startX) >> 4;
        int cz = (mapStartZ + startZ) >> 4;
        // All live chunk access runs on its world thread. This scan never requests generation.
        if (world instanceof net.minecraft.server.level.ServerLevel server && !server.getServer().isSameThread())
            throw new IllegalStateException("Village terrain must be read on the server thread");
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if (!world.hasChunk(cx + dx, cz + dz)) return;
        var available = world instanceof net.minecraft.server.level.ServerLevel server
            ? server.getChunkSource().getChunkNow(cx, cz)
            : world.getChunkSource().getChunk(cx, cz, net.minecraft.world.level.chunk.ChunkStatus.FULL, false);
        if (!(available instanceof LevelChunk chunk)) return;
        ScanRules rules=new ScanRules();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY=world.getMinBuildHeight(),maxY=world.getMaxBuildHeight();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int mx = startX + x, mz = startZ + z;
            int wx = mapStartX + mx, wz = mapStartZ + mz;
            var column = VillageTerrainColumn.scan(y -> chunk.getBlockState(pos.set(wx, y, wz)),
                yBaseline, minY, maxY, rules);
            topGround[mx][mz] = (short)column.top();
            spaceAbove[mx][mz] = (short)column.clearance();
            buildingForbidden[mx][mz] = column.forbidden();
            danger[mx][mz] = column.danger();
            water[mx][mz] = column.water();
            tree[mx][mz] = column.tree();
            path[mx][mz] = column.path();
            topAdjusted[mx][mz] = false;
            canBuild[mx][mz] = column.groundFound() && !column.danger() && !column.forbidden()
                && buildingLocRef[mx][mz] == null && column.top() > yBaseline - 10 && column.top() < yBaseline + 10;
            if (buildingLocRef[mx][mz] != null) {
                topGround[mx][mz] = (short)buildingLocRef[mx][mz].pos.y;
                spaceAbove[mx][mz] = 3;
            }
        }
    }

    /** 1 = valid, 0 = loaded terrain changed, -1 = cannot safely evaluate now. */
    public int validateReservedSite(BuildingLocation site, org.millenaire.common.buildingplan.BuildingPlan plan, Point centre) {
        if(world==null||topGround==null||plan==null)return -1;
        int x=site.pos.getiX()-mapStartX,z=site.pos.getiZ()-mapStartZ;
        int a=plan.length+plan.areaToClearLengthBefore+plan.areaToClearLengthAfter+2;
        int d=plan.width+plan.areaToClearWidthBefore+plan.areaToClearWidthAfter+2;
        int hx=(site.orientation%2==0?a:d)/2,hz=(site.orientation%2==0?d:a)/2;
        if(x-hx<0||z-hz<0||x+hx>=length||z+hz>=width)return 0;
        try {
            VillageMapInfo fresh=this.clone();
            fresh.buildingLocations.removeIf(l->l.isSameLocation(site));
            List<BuildingLocation> others=new ArrayList<>(fresh.buildingLocations);
            fresh.buildingLocations.clear();
            for(BuildingLocation[] row:fresh.buildingLocRef)java.util.Arrays.fill(row,null);
            for(BuildingLocation l:others)fresh.addBuildingLocationToMap(l);
            for(int ix=(x-hx)/16;ix<=(x+hx)/16;ix++)for(int iz=(z-hz)/16;iz<=(z+hz)/16;iz++){
                int cx=chunkStartX+ix,cz=chunkStartZ+iz;
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                    if(!world.hasChunk(cx+dx,cz+dz))return -1;
                if(world instanceof net.minecraft.server.level.ServerLevel server
                    && server.getChunkSource().getChunkNow(cx,cz)==null)return -1;
                fresh.updateChunk(ix*16,iz*16);
            }
            var result=plan.testSpot(fresh,null,centre,x,z,new java.util.Random(0L),
                Math.floorMod(site.orientation-plan.buildingOrientation,4),false);
            return result.location!=null&&result.location.pos.getiY()==site.pos.getiY()?1:0;
        }catch(CloneNotSupportedException|ThreadSafeUtilities.ChunkAccessException error){
            MillLog.printException(error);return -1;
        }
    }

    public void updateNextChunk() {
        if (world == null || length == 0 || width == 0) return;
        updateCounter = (updateCounter + 1) % Math.max(1, frequency);
        if (updateCounter != 0) return;
        if (++lastUpdatedX * 16 >= length) { lastUpdatedX = 0; lastUpdatedZ++; }
        if (lastUpdatedZ * 16 >= width) lastUpdatedZ = 0;
        int x = lastUpdatedX << 4, z = lastUpdatedZ << 4;
        Level expectedWorld = world;
        short[][] expectedMap = topGround;
        Mill.proxy.executeWorldTask(expectedWorld, () -> {
            if (world != expectedWorld || topGround != expectedMap) return;
            try { updateChunk(x, z); }
            catch (ThreadSafeUtilities.ChunkAccessException error) { MillLog.printException(error); }
        });
    }
}
