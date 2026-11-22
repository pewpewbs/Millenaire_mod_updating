package org.millenaire.common.world;

import java.util.HashSet;
import java.util.Random;
import net.minecraft.world.level.block.LeavesBlock;
import org.millenaire.common.block.WoodVariant;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IPlantable;
import org.millenaire.common.block.MillBlocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.RotatedPillarBlock;

public class WorldGenAppleTree
extends MillTreeGenerator {
    private static final int MIN_TREE_HEIGHT = 5;
    private static final BlockState WOOD_BS = WoodVariant.OAK.log().defaultBlockState();
    private static final BlockState LEAVES_BS = MillBlocks.LEAVES_APPLETREE.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 7);

    public WorldGenAppleTree(boolean notify) {
        super(notify);
    }

    public boolean func_180709_b(Level worldIn, Random rand, BlockPos position) {
        int treeHeight = rand.nextInt(2) + 5;
        boolean obstacleMet = true;
        if (position.getY() > worldIn.getMinBuildHeight() && position.getY() + treeHeight + 1 <= worldIn.getMaxBuildHeight()) {
            for (int j = position.getY(); j <= position.getY() + 1 + treeHeight; ++j) {
                int k = 1;
                if (j == position.getY()) {
                    k = 0;
                }
                if (j >= position.getY() + 1 + treeHeight - 2) {
                    k = 2;
                }
                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
                for (int l = position.getX() - k; l <= position.getX() + k && obstacleMet; ++l) {
                    for (int i1 = position.getZ() - k; i1 <= position.getZ() + k && obstacleMet; ++i1) {
                        if (j >= worldIn.getMinBuildHeight() && j < worldIn.getMaxBuildHeight()) {
                            if (this.isReplaceable(worldIn, (BlockPos)blockpos$mutableblockpos.set(l, j, i1))) continue;
                            obstacleMet = false;
                            continue;
                        }
                        obstacleMet = false;
                    }
                }
            }
            if (!obstacleMet) {
                return false;
            }
            BlockState state = worldIn.getBlockState(position.below());
            if (state.getBlock().canSustainPlant(state, (BlockGetter)worldIn, position.below(), Direction.UP, (IPlantable)((SaplingBlock)Blocks.OAK_SAPLING)) && position.getY() < worldIn.getMaxBuildHeight() - treeHeight - 1) {
                prepareSoil(worldIn, position.below());
                for (int yPos = 0; yPos < 5; ++yPos) {
                    BlockPos upN = position.above(yPos);
                    state = worldIn.getBlockState(upN);
                    if (!state.isAir() && !state.is(BlockTags.LEAVES) && state.getMaterial() != Material.REPLACEABLE_PLANT) continue;
                    this.func_175903_a(worldIn, position.above(yPos), WOOD_BS);
                }
                HashSet<Direction> branchFacings = new HashSet<Direction>();
                branchFacings.add(Direction.Plane.HORIZONTAL.getRandomDirection(rand));
                for (int i = 0; i < 3; ++i) {
                    branchFacings.add(Direction.Plane.HORIZONTAL.getRandomDirection(rand));
                }
                for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
                    int branchStartY = 3 + rand.nextInt(1);
                    int horizontalSize = 3 - rand.nextInt(2);
                    int xPos = position.getX();
                    int zPos = position.getZ();
                    int yPos = position.getY() + branchStartY;
                    int curve = rand.nextDouble() < 0.5 ? 1 : -1;
                    for (int hPos = 0; hPos < horizontalSize; ++hPos) {
                        BlockPos blockpos;
                        if (yPos < position.getY() + treeHeight && rand.nextDouble() < 0.7) {
                            ++yPos;
                        }
                        if (enumfacing.getStepX() != 0) {
                            xPos += enumfacing.getStepX();
                            if (rand.nextDouble() < 0.15) {
                                zPos += curve;
                            }
                        } else {
                            zPos += enumfacing.getStepZ();
                            if (rand.nextDouble() < 0.15) {
                                xPos += curve;
                            }
                        }
                        if (!(state = worldIn.getBlockState(blockpos = new BlockPos(xPos, yPos, zPos))).isAir() && !state.is(BlockTags.LEAVES)) continue;
                        this.func_175903_a(worldIn, blockpos, WOOD_BS.setValue(RotatedPillarBlock.AXIS, enumfacing.getAxis()));
                        for (int dx = -1; dx < 2; ++dx) {
                            for (int dz = -1; dz < 2; ++dz) {
                                for (int dy = -1; dy < 2; ++dy) {
                                    BlockPos leavePos = blockpos.offset(dx, dy, dz);
                                    state = worldIn.getBlockState(leavePos);
                                    if (!state.isAir() || rand.nextInt(100) >= 50) continue;
                                    this.func_175903_a(worldIn, leavePos, LEAVES_BS);
                                }
                            }
                        }
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }
}

