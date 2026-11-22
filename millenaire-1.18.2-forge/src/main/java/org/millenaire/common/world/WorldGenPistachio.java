package org.millenaire.common.world;

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

public class WorldGenPistachio
extends MillTreeGenerator {
    private final int minTreeHeight;
    private final BlockState metaWood = WoodVariant.OAK.log().defaultBlockState();
    private final BlockState metaLeaves = MillBlocks.LEAVES_PISTACHIO.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 7);

    public WorldGenPistachio(boolean notify) {
        super(notify);
        this.minTreeHeight = 6;
    }

    public boolean func_180709_b(Level worldIn, Random rand, BlockPos position) {
        int treeHeight = rand.nextInt(3) + this.minTreeHeight;
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
                for (int yPos = position.getY() + 3; yPos <= position.getY() + treeHeight; ++yPos) {
                    int leavesRadius = 4;
                    if (yPos < position.getY() + 5) {
                        leavesRadius -= position.getY() + 5 - yPos;
                    } else if (yPos > position.getY() + treeHeight - 3) {
                        leavesRadius -= yPos - (position.getY() + treeHeight - 3);
                    }
                    for (int xPos = position.getX() - leavesRadius; xPos <= position.getX() + leavesRadius; ++xPos) {
                        int distanceFromTrunkX = xPos - position.getX();
                        for (int zPos = position.getZ() - leavesRadius; zPos <= position.getZ() + leavesRadius; ++zPos) {
                            BlockPos blockpos;
                            int distanceFromTrunkZ = zPos - position.getZ();
                            int chanceOn100 = 100;
                            if (Math.abs(distanceFromTrunkX) == leavesRadius && Math.abs(distanceFromTrunkZ) == leavesRadius) {
                                chanceOn100 = 0;
                            } else if (Math.abs(distanceFromTrunkX) == leavesRadius || Math.abs(distanceFromTrunkZ) == leavesRadius) {
                                chanceOn100 = 80;
                            }
                            if (rand.nextInt(100) >= chanceOn100 || !(state = worldIn.getBlockState(blockpos = new BlockPos(xPos, yPos, zPos))).isAir() && !state.is(BlockTags.LEAVES) && state.getMaterial() != Material.REPLACEABLE_PLANT) continue;
                            this.func_175903_a(worldIn, blockpos, this.metaLeaves);
                        }
                    }
                }
                for (int j3 = 0; j3 < treeHeight; ++j3) {
                    BlockPos upN = position.above(j3);
                    state = worldIn.getBlockState(upN);
                    if (!state.isAir() && !state.is(BlockTags.LEAVES) && state.getMaterial() != Material.REPLACEABLE_PLANT) continue;
                    this.func_175903_a(worldIn, position.above(j3), this.metaWood);
                }
                for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
                    if (rand.nextInt(100) >= 70) continue;
                    int branchMaxY = treeHeight - rand.nextInt(2);
                    int branchMinY = 3 + rand.nextInt(2);
                    int horizontalOffset = 3 - rand.nextInt(2);
                    int xPos = position.getX();
                    int zPos = position.getZ();
                    for (int yPos = 0; yPos < branchMaxY; ++yPos) {
                        BlockPos blockpos;
                        int i2 = position.getY() + yPos;
                        if (yPos >= branchMinY && horizontalOffset > 0) {
                            xPos += enumfacing.getStepX();
                            zPos += enumfacing.getStepZ();
                            --horizontalOffset;
                        }
                        if (!(state = worldIn.getBlockState(blockpos = new BlockPos(xPos, i2, zPos))).isAir() && !state.is(BlockTags.LEAVES)) continue;
                        this.func_175903_a(worldIn, blockpos, this.metaWood);
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }
}

