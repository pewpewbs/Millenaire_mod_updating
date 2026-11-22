/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.pathfinding.PathPoint
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityBrewingStand
 *  net.minecraft.tileentity.TileEntityDispenser
 *  net.minecraft.tileentity.TileEntityFurnace
 *  net.minecraft.tileentity.TileEntitySign
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 */
package org.millenaire.common.utilities;

import java.util.Arrays;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.entity.TileEntityImportTable;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.entity.TileEntityPanel;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.pathing.atomicstryker.RegionMapper;
import org.millenaire.common.utilities.IntPoint;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.WorldUtilities;

public class Point {
    public final double x;
    public final double y;
    public final double z;

    public static final Point read(CompoundTag nbttagcompound, String label) {
        double x = nbttagcompound.getDouble(label + "_xCoord");
        double y = nbttagcompound.getDouble(label + "_yCoord");
        double z = nbttagcompound.getDouble(label + "_zCoord");
        if (x == 0.0 && y == 0.0 && z == 0.0) {
            return null;
        }
        return new Point(x, y, z);
    }

    public Point(AStarNode node) {
        this.x = node.x;
        this.y = node.y;
        this.z = node.z;
    }

    public Point(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    public Point(double i, double j, double k) {
        this.x = i;
        this.y = j;
        this.z = k;
    }

    public Point(Entity ent) {
        this.x = ent.getX();
        this.y = ent.getY();
        this.z = ent.getZ();
    }

    public Point(Node pp) {
        this.x = pp.x;
        this.y = pp.y;
        this.z = pp.z;
    }

    public Point(String s) {
        String[] scoord = s.split("/");
        this.x = Double.parseDouble(scoord[0]);
        this.y = Double.parseDouble(scoord[1]);
        this.z = Double.parseDouble(scoord[2]);
    }

    public String approximateDistanceLongString(Point p) {
        int dist = (int)this.distanceTo(p);
        if (dist < 950) {
            return dist / 100 * 100 + " " + LanguageUtilities.string("other.metre");
        }
        if ((dist = Math.round(dist / 500)) % 2 == 0) {
            return dist / 2 + " " + LanguageUtilities.string("other.kilometre");
        }
        return (dist - 1) / 2 + LanguageUtilities.string("other.andhalf") + " " + LanguageUtilities.string("other.kilometre");
    }

    public String approximateDistanceShortString(Point p) {
        int dist = (int)this.distanceTo(p);
        if (dist < 950) {
            return dist / 100 * 100 + "m";
        }
        if ((dist /= 500) % 2 == 0) {
            return dist / 2 + "km";
        }
        return (dist - 1) / 2 + ".5 km";
    }

    public String directionTo(Point p) {
        return this.directionTo(p, false);
    }

    public String directionTo(Point p, boolean prefixed) {
        String direction;
        String prefix = prefixed ? "other.tothe" : "other.";
        int xdist = Mth.floor((double)(p.x - this.x));
        int zdist = Mth.floor((double)(p.z - this.z));
        if ((double)Math.abs(xdist) > (double)Math.abs(zdist) * 0.6 && (double)Math.abs(xdist) < (double)Math.abs(zdist) * 1.4 || (double)Math.abs(zdist) > (double)Math.abs(xdist) * 0.6 && (double)Math.abs(zdist) < (double)Math.abs(xdist) * 1.4) {
            direction = zdist > 0 ? prefix + "south-" : prefix + "north-";
            direction = xdist > 0 ? direction + "east" : direction + "west";
        } else {
            direction = Math.abs(xdist) > Math.abs(zdist) ? (xdist > 0 ? prefix + "east" : prefix + "west") : (zdist > 0 ? prefix + "south" : prefix + "north");
        }
        return direction;
    }

    public String directionToShort(Point p) {
        String direction;
        int xdist = Mth.floor((double)(p.x - this.x));
        int zdist = Mth.floor((double)(p.z - this.z));
        if ((double)Math.abs(xdist) > (double)Math.abs(zdist) * 0.6 && (double)Math.abs(xdist) < (double)Math.abs(zdist) * 1.4 || (double)Math.abs(zdist) > (double)Math.abs(xdist) * 0.6 && (double)Math.abs(zdist) < (double)Math.abs(xdist) * 1.4) {
            direction = zdist > 0 ? LanguageUtilities.string("other.south_short") : LanguageUtilities.string("other.north_short");
            direction = xdist > 0 ? direction + LanguageUtilities.string("other.east_short") : direction + LanguageUtilities.string("other.west_short");
        } else {
            direction = Math.abs(xdist) > Math.abs(zdist) ? (xdist > 0 ? LanguageUtilities.string("other.east_short") : LanguageUtilities.string("other.west_short")) : (zdist > 0 ? LanguageUtilities.string("other.south_short") : LanguageUtilities.string("other.north_short"));
        }
        return direction;
    }

    public String distanceDirectionShort(Point p) {
        return LanguageUtilities.string("other.directionshort", this.directionToShort(p), "" + (int)this.horizontalDistanceTo(p) + "m");
    }

    public double distanceTo(double px, double py, double pz) {
        double d = px - this.x;
        double d1 = py - this.y;
        double d2 = pz - this.z;
        return Math.sqrt((double)(d * d + d1 * d1 + d2 * d2));
    }

    public double distanceTo(Entity e) {
        return this.distanceTo(e.getX(), e.getY(), e.getZ());
    }

    public double distanceTo(Point p) {
        if (p == null) {
            return -1.0;
        }
        return this.distanceTo(p.x, p.y, p.z);
    }

    public double distanceToSquared(double px, double py, double pz) {
        double d = px - this.x;
        double d1 = py - this.y;
        double d2 = pz - this.z;
        return d * d + d1 * d1 + d2 * d2;
    }

    public double distanceToSquared(Entity e) {
        return this.distanceToSquared(e.getX(), e.getY(), e.getZ());
    }

    public double distanceToSquared(Node pp) {
        return this.distanceToSquared(pp.x, pp.y, pp.z);
    }

    public double distanceToSquared(Point p) {
        return this.distanceToSquared(p.x, p.y, p.z);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Point)) {
            return false;
        }
        Point p = (Point)o;
        return p.x == this.x && p.y == this.y && p.z == this.z;
    }

    public Point getAbove() {
        return new Point(this.x, this.y + 1.0, this.z);
    }

    public Point getBelow() {
        return new Point(this.x, this.y - 1.0, this.z);
    }

    public Block getBlock(Level world) {
        return world.getBlockState(this.getBlockPos()).getBlock();
    }

    public BlockState getBlockActualState(Level world) {
        return world.getBlockState(this.getBlockPos());
    }

    public BlockPos getBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public BrewingStandBlockEntity getBrewingStand(Level world) {
        BlockEntity ent = world.getBlockEntity(this.getBlockPos());
        if (ent != null && ent instanceof BrewingStandBlockEntity) {
            return (BrewingStandBlockEntity)ent;
        }
        return null;
    }

    public String getChunkString() {
        return this.getChunkX() + "/" + this.getChunkZ();
    }

    public int getChunkX() {
        return this.getiX() >> 4;
    }

    public int getChunkZ() {
        return this.getiZ() >> 4;
    }

    public DispenserBlockEntity getDispenser(Level world) {
        BlockEntity ent = world.getBlockEntity(this.getBlockPos());
        if (ent != null && ent instanceof DispenserBlockEntity) {
            return (DispenserBlockEntity)ent;
        }
        return null;
    }

    public Point getEast() {
        return new Point(this.x + 1.0, this.y, this.z);
    }

    public TileEntityFirePit getFirePit(Level world) {
        BlockEntity ent = world.getBlockEntity(this.getBlockPos());
        if (ent != null && ent instanceof TileEntityFirePit) {
            return (TileEntityFirePit)ent;
        }
        return null;
    }

    public FurnaceBlockEntity getFurnace(Level world) {
        BlockEntity ent = world.getBlockEntity(this.getBlockPos());
        if (ent != null && ent instanceof FurnaceBlockEntity) {
            return (FurnaceBlockEntity)ent;
        }
        return null;
    }

    public TileEntityImportTable getImportTable(Level world) {
        BlockEntity ent = world.getBlockEntity(this.getBlockPos());
        if (ent != null && ent instanceof TileEntityImportTable) {
            return (TileEntityImportTable)ent;
        }
        return null;
    }

    public IntPoint getIntPoint() {
        return new IntPoint(this);
    }

    public String getIntString() {
        return this.getiX() + "/" + this.getiY() + "/" + this.getiZ();
    }

    public int getiX() {
        return Mth.floor((double)this.x);
    }

    public int getiY() {
        return Mth.floor((double)this.y);
    }

    public int getiZ() {
        return Mth.floor((double)this.z);
    }

    public int getMeta(Level world) {
        BlockState state = world.getBlockState(this.getBlockPos());
        return state.getBlock().func_176201_c(state);
    }

    public TileEntityLockedChest getMillChest(Level world) {
        BlockEntity ent = world.getBlockEntity(this.getBlockPos());
        if (ent != null && ent instanceof TileEntityLockedChest) {
            return (TileEntityLockedChest)ent;
        }
        return null;
    }

    public List<Point> getNeightbours() {
        return Arrays.asList(this.getAbove(), this.getBelow(), this.getNorth(), this.getEast(), this.getSouth(), this.getWest());
    }

    public Point getNorth() {
        return new Point(this.x, this.y, this.z - 1.0);
    }

    public RegionMapper.Point2D getP2D() {
        return new RegionMapper.Point2D(this.getiX(), this.getiZ());
    }

    public TileEntityPanel getPanel(Level world) {
        BlockEntity ent = world.getBlockEntity(this.getBlockPos());
        if (ent != null && ent instanceof TileEntityPanel) {
            return (TileEntityPanel)ent;
        }
        return null;
    }

    public Node getPathPoint() {
        return new Node(this.getiX(), this.getiY(), this.getiZ());
    }

    public String getPathString() {
        return this.getiX() + "_" + this.getiY() + "_" + this.getiZ();
    }

    public Point getRelative(double dx, double dy, double dz) {
        return new Point(this.x + dx, this.y + dy, this.z + dz);
    }

    public SignBlockEntity getSign(Level world) {
        BlockEntity ent = world.getBlockEntity(this.getBlockPos());
        if (ent != null && ent instanceof SignBlockEntity) {
            return (SignBlockEntity)ent;
        }
        return null;
    }

    public Point getSouth() {
        return new Point(this.x, this.y, this.z + 1.0);
    }

    public BlockEntity getTileEntity(Level world) {
        return world.getBlockEntity(this.getBlockPos());
    }

    public Point getWest() {
        return new Point(this.x - 1.0, this.y, this.z);
    }

    public int hashCode() {
        return (int)(this.x + (double)((int)this.y << 8) + (double)((int)this.z << 16));
    }

    public double horizontalDistanceTo(BlockPos bp) {
        return this.horizontalDistanceTo(bp.getX(), bp.getZ());
    }

    public double horizontalDistanceTo(double px, double pz) {
        double d = px - this.x;
        double d2 = pz - this.z;
        return Math.sqrt((double)(d * d + d2 * d2));
    }

    public double horizontalDistanceTo(Entity e) {
        return this.horizontalDistanceTo(e.getX(), e.getZ());
    }

    public double horizontalDistanceTo(Node p) {
        if (p == null) {
            return 0.0;
        }
        return this.horizontalDistanceTo(p.x, p.z);
    }

    public double horizontalDistanceTo(Point p) {
        if (p == null) {
            return 0.0;
        }
        return this.horizontalDistanceTo(p.x, p.z);
    }

    public double horizontalDistanceToSquared(double px, double pz) {
        double d = px - this.x;
        double d2 = pz - this.z;
        return d * d + d2 * d2;
    }

    public double horizontalDistanceToSquared(Entity e) {
        return this.horizontalDistanceToSquared(e.getX(), e.getZ());
    }

    public double horizontalDistanceToSquared(Point p) {
        return this.horizontalDistanceToSquared(p.x, p.z);
    }

    public boolean isBlockPassable(Level world) {
        return !this.getBlock(world).defaultBlockState().getMaterial().isSolid();
    }

    public boolean sameBlock(Node p) {
        if (p == null) {
            return false;
        }
        return this.getiX() == p.x && this.getiY() == p.y && this.getiZ() == p.z;
    }

    public boolean sameBlock(Point p) {
        if (p == null) {
            return false;
        }
        return this.getiX() == p.getiX() && this.getiY() == p.getiY() && this.getiZ() == p.getiZ();
    }

    public void setBlock(Level world, Block block, int meta, boolean notify, boolean sound) {
        WorldUtilities.setBlockAndMetadata(world, this, block, meta, notify, sound);
    }

    public void setBlockState(Level world, BlockState state) {
        world.setBlockAndUpdate(this.getBlockPos(), state);
    }

    public int squareRadiusDistance(Point p) {
        return (int)Math.max(Math.abs(this.x - p.x), Math.abs(this.z - p.z));
    }

    public String toString() {
        return Math.round(this.x * 100.0) / 100L + "/" + Math.round(this.y * 100.0) / 100L + "/" + Math.round(this.z * 100.0) / 100L;
    }

    public void write(CompoundTag nbttagcompound, String label) {
        nbttagcompound.putDouble(label + "_xCoord", this.x);
        nbttagcompound.putDouble(label + "_yCoord", this.y);
        nbttagcompound.putDouble(label + "_zCoord", this.z);
    }
}

