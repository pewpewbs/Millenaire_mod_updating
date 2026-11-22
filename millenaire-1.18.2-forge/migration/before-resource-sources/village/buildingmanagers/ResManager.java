/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockCocoa
 *  net.minecraft.block.BlockOldLog
 *  net.minecraft.block.BlockPlanks$EnumType
 *  net.minecraft.block.BlockSapling
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.network.PacketBuffer
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.world.World
 */
package org.millenaire.common.village.buildingmanagers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.block.BlockOldLog;
import org.millenaire.common.block.WoodVariant;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.millenaire.common.block.BlockMillSapling;
import org.millenaire.common.block.BlockSilkWorm;
import org.millenaire.common.block.BlockSnailSoil;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.network.StreamReadWrite;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

public class ResManager {
    public CopyOnWriteArrayList<Point> brickspot = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> chests = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> fishingspots = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> sugarcanesoils = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> healingspots = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> furnaces = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> firepits = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> brewingStands = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> signs = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> banners = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> cultureBanners = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<CopyOnWriteArrayList<Point>> sources = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<BlockState> sourceTypes = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<CopyOnWriteArrayList<Point>> spawns = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<ResourceLocation> spawnTypes = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<CopyOnWriteArrayList<Point>> mobSpawners = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<ResourceLocation> mobSpawnerTypes = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<CopyOnWriteArrayList<Point>> soils = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<ResourceLocation> soilTypes = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> stalls = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> woodspawn = new CopyOnWriteArrayList();
    public ConcurrentHashMap<Point, String> woodspawnTypes = new ConcurrentHashMap();
    public CopyOnWriteArrayList<Point> netherwartsoils = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> silkwormblock = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> snailsoilblock = new CopyOnWriteArrayList();
    public CopyOnWriteArrayList<Point> dispenderUnknownPowder = new CopyOnWriteArrayList();
    private Point sleepingPos = null;
    private Point sellingPos = null;
    private Point craftingPos = null;
    private Point defendingPos = null;
    private Point shelterPos = null;
    private Point pathStartPos = null;
    private Point leasurePos = null;
    private final Building building;

    public ResManager(Building b) {
        this.building = b;
    }

    public void addMobSpawnerPoint(ResourceLocation type, Point p) {
        if (!this.mobSpawnerTypes.contains(type)) {
            CopyOnWriteArrayList<Point> spawnsPoint = new CopyOnWriteArrayList<Point>();
            spawnsPoint.add(p);
            this.mobSpawners.add(spawnsPoint);
            this.mobSpawnerTypes.add(type);
        } else {
            for (int i = 0; i < this.mobSpawnerTypes.size(); ++i) {
                if (!this.mobSpawnerTypes.get(i).equals((Object)type) || this.mobSpawners.get(i).contains(p)) continue;
                this.mobSpawners.get(i).add(p);
            }
        }
    }

    public void addSoilPoint(ResourceLocation type, Point p) {
        if (!this.soilTypes.contains(type)) {
            CopyOnWriteArrayList<Point> spawnsPoint = new CopyOnWriteArrayList<Point>();
            spawnsPoint.add(p);
            this.soils.add(spawnsPoint);
            this.soilTypes.add(type);
        } else {
            for (int i = 0; i < this.soilTypes.size(); ++i) {
                if (!this.soilTypes.get(i).equals((Object)type) || this.soils.get(i).contains(p)) continue;
                this.soils.get(i).add(p);
            }
        }
    }

    public void addSourcePoint(BlockState blockState, Point p) {
        if (!this.sourceTypes.contains(blockState)) {
            CopyOnWriteArrayList<Point> spawnsPoint = new CopyOnWriteArrayList<Point>();
            spawnsPoint.add(p);
            this.sources.add(spawnsPoint);
            this.sourceTypes.add(blockState);
        } else {
            for (int i = 0; i < this.sourceTypes.size(); ++i) {
                if (!this.sourceTypes.get(i).equals(blockState) || this.sources.get(i).contains(p)) continue;
                this.sources.get(i).add(p);
            }
        }
    }

    public void addSpawnPoint(ResourceLocation type, Point p) {
        if (!this.spawnTypes.contains(type)) {
            CopyOnWriteArrayList<Point> spawnsPoint = new CopyOnWriteArrayList<Point>();
            spawnsPoint.add(p);
            this.spawns.add(spawnsPoint);
            this.spawnTypes.add(type);
        } else {
            for (int i = 0; i < this.spawnTypes.size(); ++i) {
                if (!this.spawnTypes.get(i).equals((Object)type) || this.spawns.get(i).contains(p)) continue;
                this.spawns.get(i).add(p);
            }
        }
    }

    public HashMap<InvItem, Integer> getChestsContent() {
        HashMap<InvItem, Integer> contents = new HashMap<InvItem, Integer>();
        for (Point p : this.chests) {
            TileEntityLockedChest chest = p.getMillChest(this.building.world);
            if (chest == null) continue;
            for (int i = 0; i < chest.getContainerSize(); ++i) {
                ItemStack stack = chest.getItem(i);
                if (stack == null || stack.getItem() == Items.AIR) continue;
                InvItem key = InvItem.createInvItem(stack);
                if (stack == null) continue;
                if (contents.containsKey(key)) {
                    contents.put(key, stack.getCount() + contents.get(key));
                    continue;
                }
                contents.put(key, stack.getCount());
            }
        }
        return contents;
    }

    public Point getCocoaHarvestLocation() {
        for (int i = 0; i < this.soilTypes.size(); ++i) {
            if (!this.soilTypes.get(i).equals((Object)Mill.CROP_CACAO)) continue;
            for (Point p : this.soils.get(i)) {
                BlockState state = p.getBlockActualState(this.building.world);
                if (state.getBlock() != Blocks.COCOA || (Integer)state.getValue(CocoaBlock.AGE) < 2) continue;
                return p;
            }
        }
        return null;
    }

    public Point getCocoaPlantingLocation() {
        for (int i = 0; i < this.soilTypes.size(); ++i) {
            if (!this.soilTypes.get(i).equals((Object)Mill.CROP_CACAO)) continue;
            for (Point p : this.soils.get(i)) {
                if (p.getBlock(this.building.world) != Blocks.AIR) continue;
                if (p.getNorth().getBlock(this.building.world) == Blocks.field_150364_r && this.isBlockJungleWood(p.getNorth().getBlockActualState(this.building.world))) {
                    return p;
                }
                if (p.getEast().getBlock(this.building.world) == Blocks.field_150364_r && this.isBlockJungleWood(p.getEast().getBlockActualState(this.building.world))) {
                    return p;
                }
                if (p.getSouth().getBlock(this.building.world) == Blocks.field_150364_r && this.isBlockJungleWood(p.getSouth().getBlockActualState(this.building.world))) {
                    return p;
                }
                if (p.getWest().getBlock(this.building.world) != Blocks.field_150364_r || !this.isBlockJungleWood(p.getWest().getBlockActualState(this.building.world))) continue;
                return p;
            }
        }
        return null;
    }

    public Point getCraftingPos() {
        if (this.craftingPos != null) {
            return this.craftingPos;
        }
        if (this.sellingPos != null) {
            return this.sellingPos;
        }
        return this.sleepingPos;
    }

    public Point getDefendingPos() {
        if (this.defendingPos != null) {
            return this.defendingPos;
        }
        if (this.sellingPos != null) {
            return this.sellingPos;
        }
        return this.sleepingPos;
    }

    public Point getEmptyBrickLocation() {
        if (this.brickspot.size() == 0) {
            return null;
        }
        for (int i = 0; i < this.brickspot.size(); ++i) {
            Point p = this.brickspot.get(i);
            if (WorldUtilities.getBlock(this.building.world, p) != Blocks.AIR) continue;
            return p;
        }
        return null;
    }

    public Point getFullBrickLocation() {
        if (this.brickspot.size() == 0) {
            return null;
        }
        for (int i = 0; i < this.brickspot.size(); ++i) {
            Point p = this.brickspot.get(i);
            if (WorldUtilities.getBlockState(this.building.world, p) != MillBlocks.BS_MUD_BRICK) continue;
            return p;
        }
        return null;
    }

    public Point getLeasurePos() {
        if (this.leasurePos != null) {
            return this.leasurePos;
        }
        return this.getSellingPos();
    }

    public int getNbEmptyBrickLocation() {
        if (this.brickspot.size() == 0) {
            return 0;
        }
        int nb = 0;
        for (int i = 0; i < this.brickspot.size(); ++i) {
            Point p = this.brickspot.get(i);
            if (WorldUtilities.getBlock(this.building.world, p) != Blocks.AIR) continue;
            ++nb;
        }
        return nb;
    }

    public int getNbFullBrickLocation() {
        if (this.brickspot.size() == 0) {
            return 0;
        }
        int nb = 0;
        for (int i = 0; i < this.brickspot.size(); ++i) {
            Point p = this.brickspot.get(i);
            if (WorldUtilities.getBlockState(this.building.world, p) != MillBlocks.BS_MUD_BRICK) continue;
            ++nb;
        }
        return nb;
    }

    public int getNbNetherWartHarvestLocation() {
        if (this.netherwartsoils.size() == 0) {
            return 0;
        }
        int nb = 0;
        for (int i = 0; i < this.netherwartsoils.size(); ++i) {
            Point p = this.netherwartsoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.NETHER_WART || WorldUtilities.getBlockMeta(this.building.world, p.getAbove()) < 3) continue;
            ++nb;
        }
        return nb;
    }

    public int getNbNetherWartPlantingLocation() {
        if (this.netherwartsoils.size() == 0) {
            return 0;
        }
        int nb = 0;
        for (int i = 0; i < this.netherwartsoils.size(); ++i) {
            Point p = this.netherwartsoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.AIR) continue;
            ++nb;
        }
        return nb;
    }

    public int getNbSilkWormHarvestLocation() {
        if (this.silkwormblock.size() == 0) {
            return 0;
        }
        int nb = 0;
        for (int i = 0; i < this.silkwormblock.size(); ++i) {
            Point p = this.silkwormblock.get(i);
            if (WorldUtilities.getBlockState(this.building.world, p) != MillBlocks.SILK_WORM.defaultBlockState().setValue(BlockSilkWorm.PROGRESS, (BlockSilkWorm.EnumType.SILKWORMFULL))) continue;
            ++nb;
        }
        return nb;
    }

    public int getNbSnailSoilHarvestLocation() {
        if (this.snailsoilblock.size() == 0) {
            return 0;
        }
        int nb = 0;
        for (int i = 0; i < this.snailsoilblock.size(); ++i) {
            Point p = this.snailsoilblock.get(i);
            if (WorldUtilities.getBlockState(this.building.world, p) != MillBlocks.SNAIL_SOIL.defaultBlockState().setValue(BlockSnailSoil.PROGRESS, (BlockSnailSoil.EnumType.SNAIL_SOIL_FULL))) continue;
            ++nb;
        }
        return nb;
    }

    public int getNbSugarCaneHarvestLocation() {
        if (this.sugarcanesoils.size() == 0) {
            return 0;
        }
        int nb = 0;
        for (int i = 0; i < this.sugarcanesoils.size(); ++i) {
            Point p = this.sugarcanesoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getRelative(0.0, 2.0, 0.0)) != Blocks.SUGAR_CANE) continue;
            ++nb;
        }
        return nb;
    }

    public int getNbSugarCanePlantingLocation() {
        if (this.sugarcanesoils.size() == 0) {
            return 0;
        }
        int nb = 0;
        for (int i = 0; i < this.sugarcanesoils.size(); ++i) {
            Point p = this.sugarcanesoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.AIR) continue;
            ++nb;
        }
        return nb;
    }

    public Point getNetherWartsHarvestLocation() {
        Point p;
        int start;
        int i;
        if (this.netherwartsoils.size() == 0) {
            return null;
        }
        for (i = start = MillCommonUtilities.randomInt(this.netherwartsoils.size()); i < this.netherwartsoils.size(); ++i) {
            p = this.netherwartsoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.NETHER_WART || WorldUtilities.getBlockMeta(this.building.world, p.getAbove()) != 3) continue;
            return p;
        }
        for (i = 0; i < start; ++i) {
            p = this.netherwartsoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.NETHER_WART || WorldUtilities.getBlockMeta(this.building.world, p.getAbove()) != 3) continue;
            return p;
        }
        return null;
    }

    public Point getNetherWartsPlantingLocation() {
        Point p;
        int start;
        int i;
        if (this.netherwartsoils.size() == 0) {
            return null;
        }
        for (i = start = MillCommonUtilities.randomInt(this.netherwartsoils.size()); i < this.netherwartsoils.size(); ++i) {
            p = this.netherwartsoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.AIR || WorldUtilities.getBlock(this.building.world, p) != Blocks.SOUL_SAND) continue;
            return p;
        }
        for (i = 0; i < start; ++i) {
            p = this.netherwartsoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.AIR || WorldUtilities.getBlock(this.building.world, p) != Blocks.SOUL_SAND) continue;
            return p;
        }
        return null;
    }

    public Point getPathStartPos() {
        if (this.pathStartPos != null) {
            return this.pathStartPos;
        }
        return this.getSellingPos();
    }

    public Point getPlantingLocation() {
        for (Point p : this.woodspawn) {
            Block block = WorldUtilities.getBlock(this.building.world, p);
            if (block != Blocks.AIR && block != Blocks.SNOW && (!BlockItemUtilities.isBlockDecorativePlant(block) || block instanceof SaplingBlock || block instanceof BlockMillSapling)) continue;
            return p;
        }
        return null;
    }

    public String getPlantingLocationType(Point target) {
        return this.woodspawnTypes.get(target);
    }

    public Point getSellingPos() {
        if (this.sellingPos != null) {
            return this.sellingPos;
        }
        return this.sleepingPos;
    }

    public Point getShelterPos() {
        if (this.shelterPos != null) {
            return this.shelterPos;
        }
        return this.sleepingPos;
    }

    public Point getSilkwormHarvestLocation() {
        Point p;
        int start;
        int i;
        if (this.silkwormblock.size() == 0) {
            return null;
        }
        for (i = start = MillCommonUtilities.randomInt(this.silkwormblock.size()); i < this.silkwormblock.size(); ++i) {
            p = this.silkwormblock.get(i);
            if (WorldUtilities.getBlockState(this.building.world, p) != MillBlocks.SILK_WORM.defaultBlockState().setValue(BlockSilkWorm.PROGRESS, (BlockSilkWorm.EnumType.SILKWORMFULL))) continue;
            return p;
        }
        for (i = 0; i < start; ++i) {
            p = this.silkwormblock.get(i);
            if (WorldUtilities.getBlockState(this.building.world, p) != MillBlocks.SILK_WORM.defaultBlockState().setValue(BlockSilkWorm.PROGRESS, (BlockSilkWorm.EnumType.SILKWORMFULL))) continue;
            return p;
        }
        return null;
    }

    public Point getSleepingPos() {
        return this.sleepingPos;
    }

    public Point getSnailSoilHarvestLocation() {
        Point p;
        int start;
        int i;
        if (this.snailsoilblock.size() == 0) {
            return null;
        }
        for (i = start = MillCommonUtilities.randomInt(this.snailsoilblock.size()); i < this.snailsoilblock.size(); ++i) {
            p = this.snailsoilblock.get(i);
            if (WorldUtilities.getBlockState(this.building.world, p) != MillBlocks.SNAIL_SOIL.defaultBlockState().setValue(BlockSnailSoil.PROGRESS, (BlockSnailSoil.EnumType.SNAIL_SOIL_FULL))) continue;
            return p;
        }
        for (i = 0; i < start; ++i) {
            p = this.snailsoilblock.get(i);
            if (WorldUtilities.getBlockState(this.building.world, p) != MillBlocks.SNAIL_SOIL.defaultBlockState().setValue(BlockSnailSoil.PROGRESS, (BlockSnailSoil.EnumType.SNAIL_SOIL_FULL))) continue;
            return p;
        }
        return null;
    }

    public List<Point> getSoilPoints(ResourceLocation type) {
        for (int i = 0; i < this.soilTypes.size(); ++i) {
            if (!this.soilTypes.get(i).equals((Object)type)) continue;
            return this.soils.get(i);
        }
        return null;
    }

    public Point getSugarCaneHarvestLocation() {
        Point p;
        int start;
        int i;
        if (this.sugarcanesoils.size() == 0) {
            return null;
        }
        for (i = start = MillCommonUtilities.randomInt(this.sugarcanesoils.size()); i < this.sugarcanesoils.size(); ++i) {
            p = this.sugarcanesoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getRelative(0.0, 2.0, 0.0)) != Blocks.SUGAR_CANE) continue;
            return p;
        }
        for (i = 0; i < start; ++i) {
            p = this.sugarcanesoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getRelative(0.0, 2.0, 0.0)) != Blocks.SUGAR_CANE) continue;
            return p;
        }
        return null;
    }

    public Point getSugarCanePlantingLocation() {
        Point p;
        int start;
        int i;
        if (this.sugarcanesoils.size() == 0) {
            return null;
        }
        for (i = start = MillCommonUtilities.randomInt(this.sugarcanesoils.size()); i < this.sugarcanesoils.size(); ++i) {
            p = this.sugarcanesoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.AIR) continue;
            return p;
        }
        for (i = 0; i < start; ++i) {
            p = this.sugarcanesoils.get(i);
            if (WorldUtilities.getBlock(this.building.world, p.getAbove()) != Blocks.AIR) continue;
            return p;
        }
        return null;
    }

    private boolean isBlockJungleWood(BlockState state) {
        return state.is(Blocks.JUNGLE_LOG);
    }

    public void readDataStream(FriendlyByteBuf ds) throws IOException {
        this.chests = StreamReadWrite.readPointList(ds);
        this.furnaces = StreamReadWrite.readPointList(ds);
        this.firepits = StreamReadWrite.readPointList(ds);
        this.signs = StreamReadWrite.readPointList(ds);
        this.stalls = StreamReadWrite.readPointList(ds);
        this.banners = StreamReadWrite.readPointList(ds);
        this.cultureBanners = StreamReadWrite.readPointList(ds);
        for (Point p : this.chests) {
            TileEntityLockedChest chest = p.getMillChest(this.building.mw.world);
            if (chest == null) continue;
            chest.buildingPos = this.building.getPos();
        }
    }

    public void readFromNBT(CompoundTag nbttagcompound) {
        Point p;
        ListTag nbttaglist2;
        CopyOnWriteArrayList<Point> v;
        Point p2;
        CompoundTag nbttagcompound1;
        int i;
        this.sleepingPos = Point.read(nbttagcompound, "spawnPos");
        this.sellingPos = Point.read(nbttagcompound, "sellingPos");
        this.craftingPos = Point.read(nbttagcompound, "craftingPos");
        this.defendingPos = Point.read(nbttagcompound, "defendingPos");
        this.shelterPos = Point.read(nbttagcompound, "shelterPos");
        this.pathStartPos = Point.read(nbttagcompound, "pathStartPos");
        this.leasurePos = Point.read(nbttagcompound, "leasurePos");
        if (this.sleepingPos == null) {
            this.sleepingPos = this.building.getPos().getAbove();
        }
        ListTag nbttaglist = nbttagcompound.getList("chests", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null || this.chests.contains(p2)) continue;
            this.chests.add(p2);
        }
        nbttaglist = nbttagcompound.getList("furnaces", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.furnaces.add(p2);
        }
        nbttaglist = nbttagcompound.getList("firepits", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.firepits.add(p2);
        }
        nbttaglist = nbttagcompound.getList("brewingStands", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.brewingStands.add(p2);
        }
        nbttaglist = nbttagcompound.getList("banners", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.banners.add(p2);
        }
        nbttaglist = nbttagcompound.getList("culturebanners", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.cultureBanners.add(p2);
        }
        nbttaglist = nbttagcompound.getList("signs", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            this.signs.add(p2);
        }
        nbttaglist = nbttagcompound.getList("netherwartsoils", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.netherwartsoils.add(p2);
        }
        nbttaglist = nbttagcompound.getList("silkwormblock", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.silkwormblock.add(p2);
        }
        nbttaglist = nbttagcompound.getList("snailsoilblock", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.snailsoilblock.add(p2);
        }
        nbttaglist = nbttagcompound.getList("sugarcanesoils", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.sugarcanesoils.add(p2);
        }
        nbttaglist = nbttagcompound.getList("fishingspots", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.fishingspots.add(p2);
        }
        nbttaglist = nbttagcompound.getList("healingspots", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.healingspots.add(p2);
        }
        nbttaglist = nbttagcompound.getList("stalls", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.stalls.add(p2);
        }
        nbttaglist = nbttagcompound.getList("woodspawn", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.woodspawn.add(p2);
            String type = nbttagcompound1.getString("type");
            if (type == null) continue;
            this.woodspawnTypes.put(p2, type);
        }
        nbttaglist = nbttagcompound.getList("brickspot", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            p2 = Point.read(nbttagcompound1, "pos");
            if (p2 == null) continue;
            this.brickspot.add(p2);
        }
        nbttaglist = nbttagcompound.getList("spawns", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            ResourceLocation spawnType = new ResourceLocation(nbttagcompound1.getString("type"));
            this.spawnTypes.add(spawnType);
            CopyOnWriteArrayList<Point> v2 = new CopyOnWriteArrayList<Point>();
            ListTag nbttaglist22 = nbttagcompound1.getList("points", 10);
            for (int j = 0; j < nbttaglist22.size(); ++j) {
                CompoundTag nbttagcompound2 = nbttaglist22.getCompound(j);
                Point p3 = Point.read(nbttagcompound2, "pos");
                if (p3 == null) continue;
                v2.add(p3);
                if (MillConfigValues.LogHybernation < 2) continue;
                MillLog.minor(this, "Loaded spawn point: " + p3);
            }
            this.spawns.add(v2);
            if (MillConfigValues.LogHybernation < 2) continue;
            MillLog.minor(this, "Loaded " + v2.size() + " spawn points for " + this.spawnTypes.get(i));
        }
        nbttaglist = nbttagcompound.getList("mobspawns", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            this.mobSpawnerTypes.add(new ResourceLocation(nbttagcompound1.getString("type")));
            v = new CopyOnWriteArrayList<Point>();
            nbttaglist2 = nbttagcompound1.getList("points", 10);
            for (int j = 0; j < nbttaglist2.size(); ++j) {
                CompoundTag nbttagcompound2 = nbttaglist2.getCompound(j);
                p = Point.read(nbttagcompound2, "pos");
                if (p == null) continue;
                v.add(p);
                if (MillConfigValues.LogHybernation < 2) continue;
                MillLog.minor(this, "Loaded spawn point: " + p);
            }
            this.mobSpawners.add(v);
            if (MillConfigValues.LogHybernation < 2) continue;
            MillLog.minor(this, "Loaded " + v.size() + " mob spawn points for " + this.spawnTypes.get(i));
        }
        nbttaglist = nbttagcompound.getList("sources", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            if (nbttagcompound1.contains("block_rl")) {
                Block block = Block.func_149684_b((String)nbttagcompound1.getString("block_rl"));
                int meta = nbttagcompound1.getInt("block_meta");
                BlockState blockState = block.func_176203_a(meta);
                this.sourceTypes.add(blockState);
            } else {
                int blockId = nbttagcompound1.getInt("type");
                this.sourceTypes.add(Block.func_149729_e((int)blockId).defaultBlockState());
            }
            v = new CopyOnWriteArrayList();
            nbttaglist2 = nbttagcompound1.getList("points", 10);
            for (int j = 0; j < nbttaglist2.size(); ++j) {
                CompoundTag nbttagcompound2 = nbttaglist2.getCompound(j);
                p = Point.read(nbttagcompound2, "pos");
                if (p == null) continue;
                v.add(p);
                if (MillConfigValues.LogHybernation < 3) continue;
                MillLog.debug(this, "Loaded source point: " + p);
            }
            this.sources.add(v);
            if (MillConfigValues.LogHybernation < 1) continue;
            MillLog.debug(this, "Loaded " + v.size() + " sources points for " + this.sourceTypes.get(i).toString());
        }
        nbttaglist = nbttagcompound.getList("genericsoils", 10);
        for (i = 0; i < nbttaglist.size(); ++i) {
            nbttagcompound1 = nbttaglist.getCompound(i);
            ResourceLocation type = new ResourceLocation(nbttagcompound1.getString("type"));
            nbttaglist2 = nbttagcompound1.getList("points", 10);
            for (int j = 0; j < nbttaglist2.size(); ++j) {
                CompoundTag nbttagcompound2 = nbttaglist2.getCompound(j);
                p = Point.read(nbttagcompound2, "pos");
                if (p == null) continue;
                this.addSoilPoint(type, p);
            }
        }
        for (Point p4 : this.chests) {
            if (!this.building.world.func_190526_b(p4.getiX() / 16, p4.getiZ() / 16) || p4.getMillChest(this.building.world) == null) continue;
            p4.getMillChest((Level)this.building.world).buildingPos = this.building.getPos();
        }
    }

    public void sendBuildingPacket(FriendlyByteBuf data) throws IOException {
        StreamReadWrite.writePointList(this.chests, data);
        StreamReadWrite.writePointList(this.furnaces, data);
        StreamReadWrite.writePointList(this.firepits, data);
        StreamReadWrite.writePointList(this.signs, data);
        StreamReadWrite.writePointList(this.stalls, data);
        StreamReadWrite.writePointList(this.banners, data);
        StreamReadWrite.writePointList(this.cultureBanners, data);
    }

    public void setCraftingPos(Point p) {
        this.craftingPos = p;
    }

    public void setDefendingPos(Point p) {
        this.defendingPos = p;
    }

    public void setLeasurePos(Point p) {
        this.leasurePos = p;
    }

    public void setPathStartPos(Point p) {
        this.pathStartPos = p;
    }

    public void setSellingPos(Point p) {
        this.sellingPos = p;
    }

    public void setShelterPos(Point p) {
        this.shelterPos = p;
    }

    public void setSleepingPos(Point p) {
        this.sleepingPos = p;
    }

    public void writeToNBT(CompoundTag nbttagcompound) {
        CompoundTag nbttagcompound2;
        ListTag nbttaglist2;
        CompoundTag nbttagcompound1;
        int i;
        CompoundTag nbttagcompound12;
        if (this.sleepingPos != null) {
            this.sleepingPos.write(nbttagcompound, "spawnPos");
        }
        if (this.sellingPos != null) {
            this.sellingPos.write(nbttagcompound, "sellingPos");
        }
        if (this.craftingPos != null) {
            this.craftingPos.write(nbttagcompound, "craftingPos");
        }
        if (this.defendingPos != null) {
            this.defendingPos.write(nbttagcompound, "defendingPos");
        }
        if (this.shelterPos != null) {
            this.shelterPos.write(nbttagcompound, "shelterPos");
        }
        if (this.pathStartPos != null) {
            this.pathStartPos.write(nbttagcompound, "pathStartPos");
        }
        if (this.leasurePos != null) {
            this.leasurePos.write(nbttagcompound, "leasurePos");
        }
        ListTag nbttaglist = new ListTag();
        for (Point p : this.signs) {
            nbttagcompound12 = new CompoundTag();
            if (p != null) {
                p.write(nbttagcompound12, "pos");
            } else {
                nbttagcompound12.putDouble("pos_xCoord", 0.0);
                nbttagcompound12.putDouble("pos_yCoord", 0.0);
                nbttagcompound12.putDouble("pos_zCoord", 0.0);
            }
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("signs", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.netherwartsoils) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("netherwartsoils", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.silkwormblock) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("silkwormblock", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.snailsoilblock) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("snailsoilblock", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.sugarcanesoils) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("sugarcanesoils", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.fishingspots) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("fishingspots", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.healingspots) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("healingspots", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.stalls) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("stalls", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.woodspawn) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            if (this.woodspawnTypes.containsKey(p)) {
                nbttagcompound12.putString("type", this.woodspawnTypes.get(p));
            }
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("woodspawn", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.brickspot) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("brickspot", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (i = 0; i < this.spawns.size(); ++i) {
            nbttagcompound1 = new CompoundTag();
            nbttagcompound1.putString("type", this.spawnTypes.get(i).toString());
            nbttaglist2 = new ListTag();
            for (Point p : this.spawns.get(i)) {
                nbttagcompound2 = new CompoundTag();
                p.write(nbttagcompound2, "pos");
                nbttaglist2.add((Tag)nbttagcompound2);
            }
            nbttagcompound1.put("points", (Tag)nbttaglist2);
            nbttaglist.add((Tag)nbttagcompound1);
        }
        nbttagcompound.put("spawns", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (i = 0; i < this.soilTypes.size(); ++i) {
            nbttagcompound1 = new CompoundTag();
            nbttagcompound1.putString("type", this.soilTypes.get(i).toString());
            nbttaglist2 = new ListTag();
            for (Point p : this.soils.get(i)) {
                nbttagcompound2 = new CompoundTag();
                p.write(nbttagcompound2, "pos");
                nbttaglist2.add((Tag)nbttagcompound2);
            }
            nbttagcompound1.put("points", (Tag)nbttaglist2);
            nbttaglist.add((Tag)nbttagcompound1);
        }
        nbttagcompound.put("genericsoils", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (i = 0; i < this.mobSpawners.size(); ++i) {
            nbttagcompound1 = new CompoundTag();
            nbttagcompound1.putString("type", this.mobSpawnerTypes.get(i).toString());
            nbttaglist2 = new ListTag();
            for (Point p : this.mobSpawners.get(i)) {
                nbttagcompound2 = new CompoundTag();
                p.write(nbttagcompound2, "pos");
                nbttaglist2.add((Tag)nbttagcompound2);
            }
            nbttagcompound1.put("points", (Tag)nbttaglist2);
            nbttaglist.add((Tag)nbttagcompound1);
        }
        nbttagcompound.put("mobspawns", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (i = 0; i < this.sources.size(); ++i) {
            nbttagcompound1 = new CompoundTag();
            nbttagcompound1.putString("block_rl", this.sourceTypes.get(i).getBlock().getRegistryName().toString());
            nbttagcompound1.putInt("block_meta", this.sourceTypes.get(i).getBlock().func_176201_c(this.sourceTypes.get(i)));
            nbttaglist2 = new ListTag();
            for (Point p : this.sources.get(i)) {
                nbttagcompound2 = new CompoundTag();
                p.write(nbttagcompound2, "pos");
                nbttaglist2.add((Tag)nbttagcompound2);
            }
            nbttagcompound1.put("points", (Tag)nbttaglist2);
            nbttaglist.add((Tag)nbttagcompound1);
        }
        nbttagcompound.put("sources", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.chests) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("chests", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.furnaces) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("furnaces", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.firepits) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("firepits", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.brewingStands) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("brewingStands", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.banners) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("banners", (Tag)nbttaglist);
        nbttaglist = new ListTag();
        for (Point p : this.cultureBanners) {
            nbttagcompound12 = new CompoundTag();
            p.write(nbttagcompound12, "pos");
            nbttaglist.add((Tag)nbttagcompound12);
        }
        nbttagcompound.put("culturebanners", (Tag)nbttaglist);
    }
}

