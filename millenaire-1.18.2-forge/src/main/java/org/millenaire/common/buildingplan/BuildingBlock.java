/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockBed
 *  net.minecraft.block.BlockChest
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.BlockDoor$EnumDoorHalf
 *  net.minecraft.block.BlockDoor$EnumHingePosition
 *  net.minecraft.block.BlockDoublePlant
 *  net.minecraft.block.BlockDoublePlant$EnumBlockHalf
 *  net.minecraft.block.BlockFlowerPot
 *  net.minecraft.block.BlockFlowerPot$EnumFlowerType
 *  net.minecraft.block.BlockFurnace
 *  net.minecraft.block.BlockHorizontal
 *  net.minecraft.block.BlockLeaves
 *  net.minecraft.block.BlockLiquid
 *  net.minecraft.block.BlockLog
 *  net.minecraft.block.BlockOldLeaf
 *  net.minecraft.block.BlockOldLog
 *  net.minecraft.block.BlockPlanks$EnumType
 *  net.minecraft.block.BlockTorch
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.JsonToNBT
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTException
 *  net.minecraft.nbt.NBTTagInt
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityDispenser
 *  net.minecraft.tileentity.TileEntityFlowerPot
 *  net.minecraft.tileentity.TileEntityMobSpawner
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.world.World
 *  net.minecraft.world.gen.feature.WorldGenBirchTree
 *  net.minecraft.world.gen.feature.WorldGenCanopyTree
 *  net.minecraft.world.gen.feature.WorldGenSavannaTree
 *  net.minecraft.world.gen.feature.WorldGenTaiga2
 *  net.minecraft.world.gen.feature.WorldGenTrees
 */
package org.millenaire.common.buildingplan;

import java.io.DataInputStream;
import java.io.IOException;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import org.millenaire.common.block.WoodVariant;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import org.millenaire.common.utilities.LegacyFlowerPots;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.millenaire.common.block.IBlockPath;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.block.mock.MockBlockBannerHanging;
import org.millenaire.common.block.mock.MockBlockBannerStanding;
import org.millenaire.common.entity.EntityWallDecoration;
import org.millenaire.common.entity.TileEntityMockBanner;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.ItemMockBanner;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.PathUtilities;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.WorldGenAppleTree;
import org.millenaire.common.world.WorldGenCherry;
import org.millenaire.common.world.WorldGenOliveTree;
import org.millenaire.common.world.WorldGenPistachio;
import org.millenaire.common.world.WorldGenSakura;

public class BuildingBlock {
    public static byte TAPESTRY = 1;
    public static byte OAKSPAWN = (byte)2;
    public static byte PINESPAWN = (byte)3;
    public static byte BIRCHSPAWN = (byte)4;
    public static byte INDIANSTATUE = (byte)5;
    public static byte PRESERVEGROUNDDEPTH = (byte)6;
    public static byte CLEARTREE = (byte)7;
    public static byte MAYANSTATUE = (byte)8;
    public static byte SPAWNERSKELETON = (byte)9;
    public static byte SPAWNERZOMBIE = (byte)10;
    public static byte SPAWNERSPIDER = (byte)11;
    public static byte SPAWNERCAVESPIDER = (byte)12;
    public static byte SPAWNERCREEPER = (byte)13;
    public static byte DISPENDERUNKNOWNPOWDER = (byte)14;
    public static byte JUNGLESPAWN = (byte)15;
    public static byte INVERTED_DOOR = (byte)16;
    public static byte CLEARGROUND = (byte)17;
    public static byte BYZANTINEICONSMALL = (byte)18;
    public static byte BYZANTINEICONMEDIUM = (byte)19;
    public static byte BYZANTINEICONLARGE = (byte)20;
    public static byte PRESERVEGROUNDSURFACE = (byte)21;
    public static byte SPAWNERBLAZE = (byte)22;
    public static byte ACACIASPAWN = (byte)23;
    public static byte DARKOAKSPAWN = (byte)24;
    public static byte TORCHGUESS = (byte)25;
    public static byte CHESTGUESS = (byte)26;
    public static byte FURNACEGUESS = (byte)27;
    public static byte CLEARGROUNDOUTSIDEBUILDING = (byte)28;
    public static byte HIDEHANGING = (byte)29;
    public static byte APPLETREESPAWN = (byte)30;
    public static byte CLEARGROUNDBORDER = (byte)31;
    public static byte OLIVETREESPAWN = (byte)32;
    public static byte PISTACHIOTREESPAWN = (byte)33;
    public static byte WALLCARPETSMALL = (byte)40;
    public static byte WALLCARPETMEDIUM = (byte)41;
    public static byte WALLCARPETLARGE = (byte)42;
    public static byte CHERRYTREESPAWN = (byte)43;
    public static byte SAKURATREESPAWN = (byte)43;
    public Block block;
    private byte meta;
    public final Point p;
    private BlockState blockState;
    public byte special;

    public BuildingBlock(Point p, Block block, int meta) {
        this.p = p;
        this.block = block;
        this.meta = (byte)meta;
        this.blockState = BuildingBlockStateIO.fromMarker(block, meta);
        this.special = 0;
    }

    public BuildingBlock(Point p, DataInputStream ds) throws IOException {
        this.p = p;
        BuildingBlockStateIO.Entry entry = BuildingBlockStateIO.read(ds);
        this.blockState = entry.state();
        this.block = this.blockState.getBlock();
        this.meta = entry.markerMetadata();
        this.special = entry.special();
    }

    public BuildingBlock(Point p, BlockState bs) {
        this.p = p;
        this.block = bs.getBlock();
        this.meta = BuildingBlockStateIO.markerMetadata(bs);
        this.blockState = bs;
        this.special = 0;
    }

    public BuildingBlock(Point p, int special) {
        this.p = p;
        this.block = Blocks.AIR;
        this.meta = 0;
        this.special = (byte)special;
        this.blockState = Blocks.AIR.defaultBlockState();
    }

    public boolean alreadyDone(Level world) {
        if (this.special != 0) {
            return false;
        }
        return world.getBlockState(this.p.getBlockPos()).equals(this.blockState);
    }

    public boolean build(Level world, Building townHall, boolean worldGeneration, boolean wandimport) {
        boolean blockSet = false;
        try {
            boolean playSound;
            boolean notifyBlocks = true;
            boolean bl = playSound = !worldGeneration && !wandimport;
            if (this.block instanceof DoorBlock && (this.special == 0 || this.special == INVERTED_DOOR)) {
                blockSet = DoorPlacement.place(world, this.p.getBlockPos(), this.blockState, this.special == INVERTED_DOOR);
            } else if (this.special == 0) {
                blockSet = this.buildNormalBlock(world, townHall, wandimport, true, playSound);
            } else if (this.special == PRESERVEGROUNDDEPTH || this.special == PRESERVEGROUNDSURFACE) {
                blockSet = this.buildPreserveGround(world, worldGeneration, true, playSound);
            } else if (this.special == CLEARTREE) {
                blockSet = this.buildClearTree(world, worldGeneration, true, playSound);
            } else if (this.special == CLEARGROUND || this.special == CLEARGROUNDOUTSIDEBUILDING || this.special == CLEARGROUNDBORDER) {
                blockSet = this.buildClearGround(world, worldGeneration, wandimport, true, playSound);
            } else if (this.special == TAPESTRY || this.special == INDIANSTATUE || this.special == MAYANSTATUE || this.special == BYZANTINEICONSMALL || this.special == BYZANTINEICONMEDIUM || this.special == BYZANTINEICONLARGE || this.special == HIDEHANGING || this.special == WALLCARPETSMALL || this.special == WALLCARPETMEDIUM || this.special == WALLCARPETLARGE) {
                blockSet = this.buildPicture(world);
            } else if (this.special == OAKSPAWN || this.special == PINESPAWN || this.special == BIRCHSPAWN || this.special == JUNGLESPAWN || this.special == ACACIASPAWN || this.special == DARKOAKSPAWN || this.special == APPLETREESPAWN || this.special == OLIVETREESPAWN || this.special == PISTACHIOTREESPAWN || this.special == CHERRYTREESPAWN || this.special == SAKURATREESPAWN) {
                blockSet = this.buildTreeSpawn(world, worldGeneration);
            } else if (this.special == SPAWNERSKELETON) {
                WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.SPAWNER, 0);
                SpawnerBlockEntity tileentitymobspawner = (SpawnerBlockEntity)this.p.getTileEntity(world);
                tileentitymobspawner.getSpawner().setEntityId(EntityType.SKELETON);
                tileentitymobspawner.setChanged();
                blockSet = true;
            } else if (this.special == SPAWNERZOMBIE) {
                WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.SPAWNER, 0);
                SpawnerBlockEntity tileentitymobspawner = (SpawnerBlockEntity)this.p.getTileEntity(world);
                tileentitymobspawner.getSpawner().setEntityId(EntityType.ZOMBIE);
                tileentitymobspawner.setChanged();
                blockSet = true;
            } else if (this.special == SPAWNERSPIDER) {
                WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.SPAWNER, 0);
                SpawnerBlockEntity tileentitymobspawner = (SpawnerBlockEntity)this.p.getTileEntity(world);
                tileentitymobspawner.getSpawner().setEntityId(EntityType.SPIDER);
                tileentitymobspawner.setChanged();
                blockSet = true;
            } else if (this.special == SPAWNERCAVESPIDER) {
                WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.SPAWNER, 0);
                SpawnerBlockEntity tileentitymobspawner = (SpawnerBlockEntity)this.p.getTileEntity(world);
                tileentitymobspawner.getSpawner().setEntityId(EntityType.CAVE_SPIDER);
                tileentitymobspawner.setChanged();
                blockSet = true;
            } else if (this.special == SPAWNERCREEPER) {
                WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.SPAWNER, 0);
                SpawnerBlockEntity tileentitymobspawner = (SpawnerBlockEntity)this.p.getTileEntity(world);
                tileentitymobspawner.getSpawner().setEntityId(EntityType.CREEPER);
                tileentitymobspawner.setChanged();
                blockSet = true;
            } else if (this.special == SPAWNERBLAZE) {
                WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.SPAWNER, 0);
                SpawnerBlockEntity tileentitymobspawner = (SpawnerBlockEntity)this.p.getTileEntity(world);
                tileentitymobspawner.getSpawner().setEntityId(EntityType.BLAZE);
                tileentitymobspawner.setChanged();
                blockSet = true;
            } else if (this.special == DISPENDERUNKNOWNPOWDER) {
                WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.DISPENSER, 0);
                DispenserBlockEntity dispenser = this.p.getDispenser(world);
                MillCommonUtilities.putItemsInChest((Container)dispenser, MillItems.UNKNOWN_POWDER, 2);
                blockSet = true;
            } else if (this.special == FURNACEGUESS) {
                Direction facing = this.guessChestFurnaceFacing(world, this.p);
                BlockState furnaceBS = Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, facing);
                world.setBlockAndUpdate(this.p.getBlockPos(), furnaceBS);
                blockSet = true;
            } else if (this.special == CHESTGUESS) {
                Direction facing = this.guessChestFurnaceFacing(world, this.p);
                BlockState chestBS = MillBlocks.LOCKED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
                world.setBlockAndUpdate(this.p.getBlockPos(), chestBS);
                blockSet = true;
            } else if (this.special == TORCHGUESS) {
                BlockState torch = Blocks.TORCH.defaultBlockState();
                if (!torch.canSurvive(world, this.p.getBlockPos())) {
                    torch = null;
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockState wall = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, direction);
                        if (wall.canSurvive(world, this.p.getBlockPos())) { torch = wall; break; }
                    }
                }
                blockSet = torch != null && world.setBlockAndUpdate(this.p.getBlockPos(), torch);

            }
        }
        catch (Exception e) {
            MillLog.printException("Exception in BuildingBlock.build():", e);
        }
        return blockSet;
    }

    private boolean buildClearGround(Level world, boolean worldGeneration, boolean wandimport, boolean notifyBlocks, boolean playSound) {
        BlockState blockStateBelow;
        boolean shouldSetBlock = false;
        boolean shouldSetBlockBelow = false;
        Block existingBlock = WorldUtilities.getBlock(world, this.p);
        BlockState targetBlockState = null;
        BlockState targetBelowBlockState = null;
        if ((!wandimport || existingBlock != Blocks.OAK_SIGN && existingBlock != MillBlocks.IMPORT_TABLE) && !BlockItemUtilities.isBlockDecorativePlant(existingBlock)) {
            if (this.special == CLEARGROUNDBORDER && !(existingBlock instanceof LeavesBlock) && existingBlock != Blocks.AIR) {
                if (this.p.getEast().getBlock(world) instanceof LiquidBlock || this.p.getWest().getBlock(world) instanceof LiquidBlock || this.p.getNorth().getBlock(world) instanceof LiquidBlock || this.p.getSouth().getBlock(world) instanceof LiquidBlock) {
                    blockStateBelow = WorldUtilities.getBlockState(world, this.p.getBelow());
                    targetBlockState = WorldUtilities.getBlockStateValidGround(blockStateBelow, true);
                    if (targetBlockState == null) {
                        targetBlockState = Blocks.DIRT.defaultBlockState();
                    }
                    if (existingBlock != targetBlockState.getBlock()) {
                        shouldSetBlock = true;
                    }
                } else if (existingBlock != Blocks.AIR) {
                    targetBlockState = Blocks.AIR.defaultBlockState();
                    shouldSetBlock = true;
                }
            } else if (existingBlock != Blocks.AIR && (this.special != CLEARGROUNDOUTSIDEBUILDING && this.special != CLEARGROUNDBORDER || !(existingBlock instanceof LeavesBlock))) {
                targetBlockState = Blocks.AIR.defaultBlockState();
                shouldSetBlock = true;
            }
        }
        blockStateBelow = WorldUtilities.getBlockState(world, this.p.getBelow());
        targetBelowBlockState = WorldUtilities.getBlockStateValidGround(blockStateBelow, true);
        if (worldGeneration && targetBelowBlockState == Blocks.DIRT.defaultBlockState()) {
            targetBelowBlockState = Blocks.GRASS_BLOCK.defaultBlockState();
            shouldSetBlockBelow = true;
        } else if (targetBlockState != null && (targetBlockState != Blocks.DIRT.defaultBlockState() || blockStateBelow.getBlock() != Blocks.GRASS_BLOCK)) {
            shouldSetBlock = true;
        }
        if (shouldSetBlock) {
            WorldUtilities.setBlockstate(world, this.p, targetBlockState, notifyBlocks, playSound);
        }
        if (shouldSetBlockBelow) {
            WorldUtilities.setBlockstate(world, this.p.getBelow(), targetBelowBlockState, notifyBlocks, playSound);
        }
        return shouldSetBlock || shouldSetBlockBelow;
    }

    private boolean buildClearTree(Level world, boolean worldGeneration, boolean notifyBlocks, boolean playSound) {
        Block block = WorldUtilities.getBlock(world, this.p);
        if (world.getBlockState(this.p.getBlockPos()).is(BlockTags.LOGS)) {
            WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.AIR, 0, notifyBlocks, playSound);
            BlockState blockStateBelow = WorldUtilities.getBlockState(world, this.p.getBelow());
            BlockState targetBlockState = WorldUtilities.getBlockStateValidGround(blockStateBelow, true);
            if (worldGeneration && targetBlockState != null && targetBlockState.getBlock() == Blocks.DIRT) {
                WorldUtilities.setBlock(world, this.p.getBelow(), (Block)Blocks.GRASS_BLOCK, notifyBlocks, playSound);
            } else if (targetBlockState != null && (targetBlockState != Blocks.DIRT.defaultBlockState() || block != Blocks.GRASS_BLOCK)) {
                WorldUtilities.setBlockstate(world, this.p.getBelow(), targetBlockState, notifyBlocks, playSound);
            }
            return true;
        }
        return false;
    }

    private boolean buildNormalBlock(Level world, Building townHall, boolean wandimport, boolean notifyBlocks, boolean playSound) {
        boolean blockSet;
        block65: {
            BlockEntity bannerEntity;
            Object bannerBlock;
            BlockState bs;
            Direction facing;
            blockSet = false;
            if (this.block instanceof DoorBlock) {
                if (this.blockState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    WorldUtilities.setBlockAndMetadata(world, this.p.getAbove(), Blocks.AIR, 0, notifyBlocks, playSound);
                }
            } else if (this.block instanceof BedBlock) {
                facing = (Direction)this.blockState.getValue(HorizontalDirectionalBlock.FACING);
                if (facing == Direction.EAST) {
                    WorldUtilities.setBlockAndMetadata(world, this.p.getWest(), Blocks.AIR, 0, notifyBlocks, playSound);
                } else if (facing == Direction.SOUTH) {
                    WorldUtilities.setBlockAndMetadata(world, this.p.getNorth(), Blocks.AIR, 0, notifyBlocks, playSound);
                } else if (facing == Direction.WEST) {
                    WorldUtilities.setBlockAndMetadata(world, this.p.getEast(), Blocks.AIR, 0, notifyBlocks, playSound);
                } else if (facing == Direction.NORTH) {
                    WorldUtilities.setBlockAndMetadata(world, this.p.getSouth(), Blocks.AIR, 0, notifyBlocks, playSound);
                }
            }
            if (!wandimport || this.block != Blocks.AIR || WorldUtilities.getBlock(world, this.p) != Blocks.OAK_SIGN) {
                Block existingBlock = WorldUtilities.getBlock(world, this.p);
                if (this.block == Blocks.AIR) {
                    if (!BlockItemUtilities.isBlockDecorativePlant(existingBlock)) {
                        WorldUtilities.setBlockstate(world, this.p, this.blockState, notifyBlocks, playSound);
                        blockSet = true;
                    }
                } else if (this.block instanceof FlowerPotBlock) {
                    WorldUtilities.setBlockstate(world, this.p, this.blockState, notifyBlocks, playSound);
                    blockSet = true;
                } else {
                    if (existingBlock instanceof BedBlock) {
                        world.removeBlock(this.p.getBlockPos(), false);
                    }
                    if (this.block instanceof BedBlock) {
                        WorldUtilities.setBlockAndMetadata(world, this.p, Blocks.AIR, 0, notifyBlocks, playSound);
                        Direction facing2 = (Direction)this.blockState.getValue(HorizontalDirectionalBlock.FACING);
                        if (facing2 == Direction.EAST) {
                            WorldUtilities.setBlockAndMetadata(world, this.p.getWest(), Blocks.AIR, 0, notifyBlocks, playSound);
                        } else if (facing2 == Direction.SOUTH) {
                            WorldUtilities.setBlockAndMetadata(world, this.p.getNorth(), Blocks.AIR, 0, notifyBlocks, playSound);
                        } else if (facing2 == Direction.WEST) {
                            WorldUtilities.setBlockAndMetadata(world, this.p.getEast(), Blocks.AIR, 0, notifyBlocks, playSound);
                        } else if (facing2 == Direction.NORTH) {
                            WorldUtilities.setBlockAndMetadata(world, this.p.getSouth(), Blocks.AIR, 0, notifyBlocks, playSound);
                        }
                    }
                    if (this.blockState != Blocks.DIRT.defaultBlockState() || existingBlock != Blocks.GRASS_BLOCK) {
                        WorldUtilities.setBlockstate(world, this.p, this.blockState, notifyBlocks, playSound);
                        blockSet = true;
                    }
                }
            }
            if (this.block instanceof DoorBlock) {
                if (this.blockState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    bs = this.blockState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
                    if (this.special == INVERTED_DOOR) {
                        bs = bs.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT);
                    }
                    WorldUtilities.setBlockstate(world, this.p.getAbove(), bs, notifyBlocks, playSound);
                }
            } else if (this.block instanceof BedBlock) {
                facing = (Direction)this.blockState.getValue(HorizontalDirectionalBlock.FACING);
                if (facing == Direction.EAST) {
                    WorldUtilities.setBlockstate(world, this.p.getWest(), this.blockState.setValue(BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), notifyBlocks, playSound);
                } else if (facing == Direction.SOUTH) {
                    WorldUtilities.setBlockstate(world, this.p.getNorth(), this.blockState.setValue(BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), notifyBlocks, playSound);
                } else if (facing == Direction.WEST) {
                    WorldUtilities.setBlockstate(world, this.p.getEast(), this.blockState.setValue(BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), notifyBlocks, playSound);
                } else if (facing == Direction.NORTH) {
                    WorldUtilities.setBlockstate(world, this.p.getSouth(), this.blockState.setValue(BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), notifyBlocks, playSound);
                }
            } else if (this.block == Blocks.WATER) {
                world.setBlock(this.p.getBlockPos(), Blocks.WATER.defaultBlockState(), 11);
            } else if (this.block == Blocks.NETHER_PORTAL) {
                PortalShape.findPortalShape(world, this.p.getBlockPos(), shape -> shape.isValid() && !shape.isComplete(), Direction.Axis.X).ifPresent(PortalShape::createPortalBlocks);
            } else if (this.block instanceof DoublePlantBlock) {
                bs = this.blockState.setValue(DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                WorldUtilities.setBlockstate(world, this.p.getAbove(), bs, notifyBlocks, playSound);            } else if (this.block instanceof MockBlockBannerHanging || this.block instanceof MockBlockBannerStanding) {
                int bannerType = this.block instanceof MockBlockBannerHanging hanging ? hanging.bannerType : ((MockBlockBannerStanding)this.block).bannerType;
                BlockEntity entity = world.getBlockEntity(this.p.getBlockPos());
                if (entity instanceof TileEntityMockBanner banner) {
                    try {
                        ItemStack pattern = townHall == null
                            ? ItemMockBanner.makeBanner(Item.byBlock(this.block), ItemMockBanner.BANNER_COLOURS[bannerType], TagParser.parseTag(ItemMockBanner.BANNER_DESIGNS[bannerType]))
                            : bannerType == ItemMockBanner.BANNER_VILLAGE ? townHall.getBannerStack() : townHall.culture.cultureBannerItemStack;
                        banner.func_175112_a(pattern, true);
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) { MillLog.printException(exception); }
                }
            }
        }
        return blockSet;
    }

    private boolean buildPicture(Level world) {
        EntityWallDecoration art = null;
        if (this.special == TAPESTRY) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 1);
        } else if (this.special == INDIANSTATUE) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 2);
        } else if (this.special == MAYANSTATUE) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 3);
        } else if (this.special == BYZANTINEICONSMALL) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 4);
        } else if (this.special == BYZANTINEICONMEDIUM) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 5);
        } else if (this.special == BYZANTINEICONLARGE) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 6);
        } else if (this.special == HIDEHANGING) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 7);
        } else if (this.special == WALLCARPETSMALL) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 8);
        } else if (this.special == WALLCARPETMEDIUM) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 9);
        } else if (this.special == WALLCARPETLARGE) {
            art = EntityWallDecoration.createWallDecoration(world, this.p, 10);
        }
        if (art.survives() && !world.isClientSide) {
            world.addFreshEntity((Entity)art);
            return true;
        }
        return false;
    }

    private boolean buildPreserveGround(Level world, boolean worldGeneration, boolean notifyBlocks, boolean playSound) {
        Material material;
        boolean surface;
        BlockState existingBlockState = WorldUtilities.getBlockState(world, this.p);
        boolean bl = surface = this.special == PRESERVEGROUNDSURFACE;
        if (!surface && existingBlockState.isSolidRender(world, this.p.getBlockPos()) && existingBlockState.isCollisionShapeFullBlock(world, this.p.getBlockPos()) && ((material = existingBlockState.getMaterial()) == Material.DIRT || material == Material.STONE || material == Material.SAND || material == Material.CLAY)) {
            return false;
        }
        BlockState targetGroundBlockState = WorldUtilities.getBlockStateValidGround(existingBlockState, surface);
        if (targetGroundBlockState == null) {
            Point below = this.p.getBelow();
            while (targetGroundBlockState == null && below.getiY() >= world.getMinBuildHeight()) {
                BlockState belowState = WorldUtilities.getBlockState(world, below);
                if (WorldUtilities.getBlockStateValidGround(belowState, surface) != null) {
                    targetGroundBlockState = WorldUtilities.getBlockStateValidGround(belowState, surface);
                }
                below = below.getBelow();
            }
            if (targetGroundBlockState == null) {
                targetGroundBlockState = Blocks.DIRT.defaultBlockState();
            }
        }
        if (targetGroundBlockState.getBlock() == Blocks.DIRT && worldGeneration && surface) {
            targetGroundBlockState = Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (targetGroundBlockState.getBlock() == Blocks.GRASS_BLOCK && !worldGeneration) {
            targetGroundBlockState = Blocks.DIRT.defaultBlockState();
        }
        if (targetGroundBlockState == null || targetGroundBlockState.getBlock() == Blocks.AIR) {
            targetGroundBlockState = worldGeneration && surface ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        if (targetGroundBlockState == existingBlockState) {
            return false;
        }
        if (existingBlockState.getBlock() == Blocks.GRASS_BLOCK && targetGroundBlockState.getBlock() == Blocks.DIRT) {
            return false;
        }
        WorldUtilities.setBlockstate(world, this.p, targetGroundBlockState, notifyBlocks, playSound);
        return true;
    }

    private boolean buildTreeSpawn(Level world, boolean worldGeneration) {
        if (!worldGeneration || !(world instanceof ServerLevel server)) return false;
        var position = this.p.getBlockPos();
        var random = MillCommonUtilities.random;
        if (this.special == APPLETREESPAWN) return new WorldGenAppleTree(true).func_180709_b(world, random, position);
        if (this.special == OLIVETREESPAWN) return new WorldGenOliveTree(true).func_180709_b(world, random, position);
        if (this.special == PISTACHIOTREESPAWN) return new WorldGenPistachio(true).func_180709_b(world, random, position);
        if (this.special == CHERRYTREESPAWN) return new WorldGenCherry(true).func_180709_b(world, random, position);
        if (this.special == SAKURATREESPAWN) return new WorldGenSakura(true).func_180709_b(world, random, position);
        var feature = this.special == OAKSPAWN ? TreeFeatures.OAK
            : this.special == PINESPAWN ? TreeFeatures.SPRUCE
            : this.special == BIRCHSPAWN ? TreeFeatures.BIRCH
            : this.special == JUNGLESPAWN ? TreeFeatures.JUNGLE_TREE_NO_VINE
            : this.special == ACACIASPAWN ? TreeFeatures.ACACIA
            : this.special == DARKOAKSPAWN ? TreeFeatures.DARK_OAK : null;
        return feature != null && feature.value().place(server, server.getChunkSource().getGenerator(), random, position);
    }

    public BlockState getBlockstate() {
        return this.blockState;
    }

    public byte getMeta() {
        return this.meta;
    }

    private Direction guessChestFurnaceFacing(Level world, Point p) {
        BlockState bsNorth = p.getNorth().getBlockActualState(world);
        BlockState bsSouth = p.getSouth().getBlockActualState(world);
        BlockState bsWest = p.getWest().getBlockActualState(world);
        BlockState bsEast = p.getEast().getBlockActualState(world);
        if (bsNorth.isSolidRender(world, p.getNorth().getBlockPos()) && bsNorth.getBlock() != Blocks.FURNACE && bsNorth.getBlock() != MillBlocks.LOCKED_CHEST && !bsSouth.isSolidRender(world, p.getSouth().getBlockPos())) {
            return Direction.SOUTH;
        }
        if (bsSouth.isSolidRender(world, p.getSouth().getBlockPos()) && bsSouth.getBlock() != Blocks.FURNACE && bsSouth.getBlock() != MillBlocks.LOCKED_CHEST && !bsNorth.isSolidRender(world, p.getNorth().getBlockPos())) {
            return Direction.NORTH;
        }
        if (bsWest.isSolidRender(world, p.getWest().getBlockPos()) && bsWest.getBlock() != Blocks.FURNACE && bsWest.getBlock() != MillBlocks.LOCKED_CHEST && !bsEast.isSolidRender(world, p.getEast().getBlockPos())) {
            return Direction.EAST;
        }
        if (bsEast.isSolidRender(world, p.getEast().getBlockPos()) && bsEast.getBlock() != Blocks.FURNACE && bsEast.getBlock() != MillBlocks.LOCKED_CHEST && !bsWest.isSolidRender(world, p.getWest().getBlockPos())) {
            return Direction.WEST;
        }
        if (!bsSouth.isSolidRender(world, p.getSouth().getBlockPos())) {
            return Direction.SOUTH;
        }
        if (!bsNorth.isSolidRender(world, p.getNorth().getBlockPos())) {
            return Direction.NORTH;
        }
        if (!bsEast.isSolidRender(world, p.getEast().getBlockPos())) {
            return Direction.EAST;
        }
        if (!bsWest.isSolidRender(world, p.getWest().getBlockPos())) {
            return Direction.WEST;
        }
        return Direction.NORTH;
    }

    public void pathBuild(Building th) {
        BlockState currentBlockState = this.p.getBlockActualState(th.world);
        if (!BlockItemUtilities.isPath(currentBlockState.getBlock()) && PathUtilities.canPathBeBuiltHere(currentBlockState)) {
            this.build(th.world, null, false, false);
        } else if (BlockItemUtilities.isPath(currentBlockState.getBlock())) {
            int targetPathLevel = 0;
            IBlockPath bp = (IBlockPath)this.block;
            for (int i = 0; i < th.villageType.pathMaterial.size(); ++i) {
                if (!BlockItemUtilities.isPath(this.block) || th.villageType.pathMaterial.get(i).getBlock() != bp.getSingleSlab() && th.villageType.pathMaterial.get(i).getBlock() != bp.getDoubleSlab()) continue;
                targetPathLevel = i;
            }
            int currentPathLevel = Integer.MAX_VALUE;
            IBlockPath currentPathBlock = (IBlockPath)currentBlockState.getBlock();
            for (int i = 0; i < th.villageType.pathMaterial.size(); ++i) {
                if (th.villageType.pathMaterial.get(i).getBlock() != currentPathBlock.getDoubleSlab() && th.villageType.pathMaterial.get(i).getBlock() != currentPathBlock.getSingleSlab()) continue;
                currentPathLevel = i;
            }
            if (currentPathLevel < targetPathLevel) {
                this.build(th.world, null, false, false);
            }
        }
    }

    public void setBlockstate(BlockState bs) {
        this.blockState = bs;
        this.block = bs.getBlock();
        this.meta = BuildingBlockStateIO.markerMetadata(bs);
    }

    public void setMeta(byte meta) {
        this.meta = meta;
        this.blockState = BuildingBlockStateIO.fromMarker(this.block, meta);
    }

    public void writeState(java.io.DataOutput output) throws IOException {
        BuildingBlockStateIO.write(output, this.blockState, this.meta, this.special);
    }

    public String toString() {
        return "(block: " + this.block + " meta: " + this.meta + " pos:" + this.p + ")";
    }
}


