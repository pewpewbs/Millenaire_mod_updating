/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockLiquid
 *  net.minecraft.block.SoundType
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.item.EntityXPOrb
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntityFurnace
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.SoundEvent
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package org.millenaire.common.utilities;

import java.util.List;
import java.util.UUID;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;

public class WorldUtilities {
    public static boolean checkChunksGenerated(Level world, int start_x, int start_z, int end_x, int end_z) {
        start_x >>= 4;
        start_z >>= 4;
        end_x >>= 4;
        end_z >>= 4;
        ++end_x;
        ++end_z;
        for (int k1 = start_x; k1 <= end_x; ++k1) {
            for (int l1 = start_z; l1 <= end_z; ++l1) {
                if (isChunkGenerated(world,k1,l1)) continue;
                return false;
            }
        }
        return true;
    }

    public static boolean isChunkGenerated(Level world,int x,int z) {
        if (world.hasChunk(x,z)) return true;
        if (!(world instanceof net.minecraft.server.level.ServerLevel server)) return false;
        try {
            // Inspect existing disk data without requesting terrain generation.
            net.minecraft.nbt.CompoundTag tag=server.getChunkSource().chunkMap.read(new net.minecraft.world.level.ChunkPos(x,z));
            if (tag==null) return false;
            if (tag.contains("Level",10)) tag=tag.getCompound("Level");
            String status=tag.getString("Status");
            return "full".equals(status) || "minecraft:full".equals(status) || tag.getBoolean("TerrainPopulated");
        } catch (java.io.IOException error) {
            MillLog.printException("Could not inspect existing village chunk "+x+", "+z,error);
            return false;
        }
    }

    public static int countBlocksAround(Level world, int x, int y, int z, int rx, int ry, int rz) {
        int counter = 0;
        for (int i = x - rx; i <= x + rx; ++i) {
            for (int j = y - ry; j <= y + ry; ++j) {
                for (int k = z - rz; k <= z + rz; ++k) {
                    if (WorldUtilities.getBlock(world, i, j, k) == null || !WorldUtilities.getBlockState(world, i, j, k).getMaterial().blocksMotion()) continue;
                    ++counter;
                }
            }
        }
        return counter;
    }

    public static Point findRandomStandingPosAround(Level world, Point dest) {
        if (dest == null) {
            return null;
        }
        for (int i = 0; i < 50; ++i) {
            Point testdest = dest.getRelative(5 - MillCommonUtilities.randomInt(10), 5 - MillCommonUtilities.randomInt(20), 5 - MillCommonUtilities.randomInt(10));
            if (!BlockItemUtilities.isBlockWalkable(WorldUtilities.getBlock(world, testdest.getiX(), testdest.getiY() - 1, testdest.getiZ())) || BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, testdest.getiX(), testdest.getiY(), testdest.getiZ())) || BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, testdest.getiX(), testdest.getiY() + 1, testdest.getiZ()))) continue;
            return testdest;
        }
        return null;
    }

    public static int findSurfaceBlock(Level world, int x, int z) {
        BlockPos pos = new BlockPos(x, world.getMaxBuildHeight()-1, z);
        while (pos.getY() >= world.getMinBuildHeight() && !BlockItemUtilities.isBlockGround(WorldUtilities.getBlock(world, x, pos.getY(), z)) && !(WorldUtilities.getBlock(world, x, pos.getY(), z) instanceof LiquidBlock)) {
            pos = new BlockPos(x, pos.getY() - 1, z);
        }
        if (pos.getY() >= world.getMaxBuildHeight()-1) {
            pos = new BlockPos(x, world.getMaxBuildHeight()-2, z);
        }
        return pos.getY() + 1;
    }

    public static Point findTopNonPassableBlock(Level world, int x, int z) {
        for (int y = world.getMaxBuildHeight()-1; y >= world.getMinBuildHeight(); --y) {
            if (!WorldUtilities.getBlock(world, x, y, z).defaultBlockState().getMaterial().isSolid()) continue;
            return new Point(x, y, z);
        }
        return null;
    }

    public static int findTopSoilBlock(Level world, int x, int z) {
        BlockPos pos = world.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, world.getMinBuildHeight(), z));
        while (pos.getY() >= world.getMinBuildHeight() && !BlockItemUtilities.isBlockGround(WorldUtilities.getBlock(world, x, pos.getY(), z))) {
            pos = new BlockPos(x, pos.getY() - 1, z);
        }
        if (pos.getY() >= world.getMaxBuildHeight()-1) {
            pos = new BlockPos(x, world.getMaxBuildHeight()-2, z);
        }
        return pos.getY() + 1;
    }

    public static Point findVerticalStandingPos(Level world, Point dest) {
        int y;
        if (dest == null) {
            return null;
        }
        for (y = dest.getiY(); y < world.getMaxBuildHeight()-2 && (BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, dest.getiX(), y, dest.getiZ())) || BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, dest.getiX(), y + 1, dest.getiZ()))); ++y) {
        }
        while (y > world.getMinBuildHeight() && !BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, dest.getiX(), y - 1, dest.getiZ()))) {
            --y;
        }
        if (y >= world.getMaxBuildHeight()-2) {
            return null;
        }
        if (!BlockItemUtilities.isBlockWalkable(WorldUtilities.getBlock(world, dest.getiX(), y - 1, dest.getiZ()))) {
            return null;
        }
        return new Point(dest.getiX(), y, dest.getiZ());
    }

    public static Block getBlock(Level world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public static Block getBlock(Level world, Point p) {
        if (p.x < -3.2E7 || p.z < -3.2E7 || p.x >= 3.2E7 || p.z > 3.2E7) {
            return null;
        }
        if (p.y < world.getMinBuildHeight()) {
            return null;
        }
        if (p.y >= world.getMaxBuildHeight()) {
            return null;
        }
        return WorldUtilities.getBlock(world, p.getiX(), p.getiY(), p.getiZ());
    }

    public static int getBlockId(Block b) {
        return net.minecraft.core.Registry.BLOCK.getId(b);
    }

    public static int getBlockMeta(Level world, int i, int j, int k) {
        return WorldUtilities.getBlockMeta(world, new Point(i, j, k));
    }

    public static int getBlockMeta(Level world, Point p) {
        if (p.x < -3.2E7 || p.z < -3.2E7 || p.x >= 3.2E7 || p.z > 3.2E7) {
            return -1;
        }
        if (p.y < world.getMinBuildHeight()) {
            return -1;
        }
        if (p.y >= world.getMaxBuildHeight()) {
            return -1;
        }
        BlockState state = p.getBlockActualState(world);
        return LegacyWorldMetadata.read(state);
    }

    public static int getBlockMetadata(Level world, int x, int y, int z) {
        BlockPos bp = new BlockPos(x, y, z);
        BlockState bs = world.getBlockState(bp);
        return LegacyWorldMetadata.read(bs);
    }

    public static BlockState getBlockState(Level world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z));
    }

    public static BlockState getBlockState(Level world, Point p) {
        return world.getBlockState(new BlockPos(p.x, p.y, p.z));
    }

    public static BlockState getBlockStateValidGround(BlockState currentBlockState, boolean surface) {
        Block b = currentBlockState.getBlock();
        if (b == Blocks.BEDROCK) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (b == Blocks.STONE && surface) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (b == Blocks.STONE && !surface) {
            return currentBlockState;
        }
        if (b == Blocks.DIRT) {
            return currentBlockState;
        }
        if (b == Blocks.GRASS_BLOCK) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (b == Blocks.GRAVEL) {
            return currentBlockState;
        }
        if (b == Blocks.SAND) {
            return currentBlockState;
        }
        if (b == Blocks.SANDSTONE && surface) {
            return Blocks.SAND.defaultBlockState();
        }
        if (b == Blocks.SANDSTONE && !surface) {
            return currentBlockState;
        }
        if (b == Blocks.TERRACOTTA) {
            return currentBlockState;
        }
        return null;
    }

    public static Point getClosestBlock(Level world, Block[] blocks, Point pos, int rx, int ry, int rz) {
        return WorldUtilities.getClosestBlockMeta(world, blocks, -1, pos, rx, ry, rz);
    }

    public static Point getClosestBlockMeta(Level world, Block[] blocks, int meta, Point pos, int rx, int ry, int rz) {
        Point closest = null;
        double minDistance = 9.99999999E8;
        for (int i = pos.getiX() - rx; i <= pos.getiX() + rx; ++i) {
            for (int j = pos.getiY() - ry; j <= pos.getiY() + ry; ++j) {
                for (int k = pos.getiZ() - rz; k <= pos.getiZ() + rz; ++k) {
                    for (int l = 0; l < blocks.length; ++l) {
                        if (WorldUtilities.getBlock(world, i, j, k) != blocks[l] || meta != -1 && WorldUtilities.getBlockMeta(world, i, j, k) != meta) continue;
                        Point temp = new Point(i, j, k);
                        if (closest != null && !(temp.distanceTo(pos) < minDistance)) continue;
                        closest = temp;
                        minDistance = closest.distanceTo(pos);
                    }
                }
            }
        }
        if (minDistance < 9.99999999E8) {
            return closest;
        }
        return null;
    }

    public static ItemEntity getClosestItemVertical(Level world, Point p, List<InvItem> goods, int radius, int vertical) {
        List<Entity> list = WorldUtilities.getEntitiesWithinAABB(world, Entity.class, p, radius, vertical);
        double bestdist = Double.MAX_VALUE;
        ItemEntity citem = null;
        for (Entity ent : list) {
            if (ent.getClass() != ItemEntity.class) continue;
            ItemEntity item = (ItemEntity)ent;
            if (item.isRemoved()) continue;
            for (InvItem key : goods) {
                double dist;
                if (!org.millenaire.common.item.LegacyItemStacks.matches(item.getItem(), key.getItem(), key.meta) || !((dist = item.distanceToSqr(p.x, p.y, p.z)) < bestdist)) continue;
                bestdist = dist;
                citem = item;
            }
        }
        if (citem == null) {
            return null;
        }
        return citem;
    }

    public static List<Entity> getEntitiesWithinAABB(Level world, Class type, Point p, int hradius, int vradius) {
        AABB area = new AABB(p.x, p.y, p.z, p.x + 1.0, p.y + 1.0, p.z + 1.0).expandTowards((double)hradius, (double)vradius, (double)hradius).expandTowards((double)(-hradius), (double)(-vradius), (double)(-hradius));
        return world.getEntitiesOfClass(type, area);
    }

    public static List<Entity> getEntitiesWithinAABB(Level world, Class type, Point pstart, Point pend) {
        AABB area = new AABB(pstart.x, pstart.y, pstart.z, pend.x, pend.y, pend.z);
        return world.getEntitiesOfClass(type, area);
    }

    public static Entity getEntityByUUID(Level world, UUID uuid) {
        return org.millenaire.common.forge.Mill.proxy.getEntityByUUID(world,uuid);
    }

    public static int getItemsFromChest(Container chest, Block block, int meta, int toTake) {
        return WorldUtilities.getItemsFromChest(chest, Item.byBlock((Block)block), meta, toTake);
    }

    public static int getItemsFromChest(Container chest, BlockState blockState, int toTake) {
        return WorldUtilities.getItemsFromChest(chest, blockState.getBlock(), org.millenaire.common.item.LegacyItemStacks.metadata(blockState), toTake);
    }

    public static int getItemsFromChest(Container chest, Item item, int meta, int toTake) {
        if (chest == null) {
            return 0;
        }
        int nb = 0;
        int maxSlot = chest.getContainerSize() - 1;
        if (chest instanceof Inventory) {
            maxSlot -= 4;
        }
        for (int i = maxSlot; i >= 0 && nb < toTake; --i) {
            ItemStack stack = chest.getItem(i);
            if (org.millenaire.common.item.LegacyItemStacks.matches(stack, item, meta)) {
                if (stack.getCount() <= toTake - nb) {
                    nb += stack.getCount();
                    chest.setItem(i, ItemStack.EMPTY);
                } else {
                    chest.removeItem(i, toTake - nb);
                    nb = toTake;
                }
            }

        }
        return nb;
    }

    public static int getItemsFromFirePit(TileEntityFirePit firepit, Item item, int toTake) {
        if (firepit == null) {
            return 0;
        }
        int taken = 0;
        for (int stackNb = 0; stackNb < 3; ++stackNb) {
            ItemStack stack = firepit.outputs.getStackInSlot(stackNb);
            if (taken >= toTake || stack == null || stack.getItem() != item) continue;
            taken += firepit.outputs.extractItem(stackNb, toTake - taken, false).getCount();
        }
        return taken;
    }

    public static int getItemsFromFurnace(FurnaceBlockEntity furnace, Item item, int toTake) {
        if (furnace == null) {
            return 0;
        }
        int nb = 0;
        ItemStack stack = furnace.getItem(2);
        if (stack != null && stack.getItem() == item) {
            if (stack.getCount() <= toTake - nb) {
                nb += stack.getCount();
                furnace.setItem(2, ItemStack.EMPTY);
            } else {
                furnace.removeItem(2, toTake - nb);
                nb = toTake;
            }
        }
        return nb;
    }

    public static Direction guessPanelFacing(Level world, Point p) {
        boolean northOpen = true;
        boolean southOpen = true;
        boolean eastOpen = true;
        boolean westOpen = true;
        if (WorldUtilities.getBlockState(world, p.getNorth()).isCollisionShapeFullBlock(world,p.getNorth().getBlockPos())) {
            northOpen = false;
        }
        if (WorldUtilities.getBlockState(world, p.getEast()).isCollisionShapeFullBlock(world,p.getEast().getBlockPos())) {
            eastOpen = false;
        }
        if (WorldUtilities.getBlockState(world, p.getSouth()).isCollisionShapeFullBlock(world,p.getSouth().getBlockPos())) {
            southOpen = false;
        }
        if (WorldUtilities.getBlockState(world, p.getWest()).isCollisionShapeFullBlock(world,p.getWest().getBlockPos())) {
            westOpen = false;
        }
        if (!eastOpen) {
            return Direction.WEST;
        }
        if (!westOpen) {
            return Direction.EAST;
        }
        if (!southOpen) {
            return Direction.NORTH;
        }
        if (!northOpen) {
            return Direction.SOUTH;
        }
        return null;
    }

    public static boolean isBlockFullCube(Level world, int i, int j, int k) {
        BlockState bs = WorldUtilities.getBlockState(world, i, j, k);
        if (bs == null) {
            return false;
        }
        return bs.isCollisionShapeFullBlock(world,new BlockPos(i,j,k));
    }

    public static void playSound(Level world, Point p, SoundEvent sound, SoundSource category, float volume, float pitch) {
        if (world.isClientSide) {
            world.playLocalSound((double)((float)p.x + 0.5f), (double)((float)p.y + 0.5f), (double)((float)p.z + 0.5f), sound, category, volume, pitch, false);
        } else {
            world.playSound(null, (double)((float)p.x + 0.5f), (double)((float)p.y + 0.5f), (double)((float)p.z + 0.5f), sound, category, volume, pitch);
        }
    }

    public static void playSoundBlockBreaking(Level world, Point p, Block b, float volume) {
        if (b != null && b.defaultBlockState().getSoundType() != null) {
            WorldUtilities.playSound(world, p, b.defaultBlockState().getSoundType().getBreakSound(), SoundSource.BLOCKS, b.defaultBlockState().getSoundType().getVolume() * volume, b.defaultBlockState().getSoundType().getPitch());
        }
    }

    public static void playSoundBlockPlaced(Level world, Point p, Block b, float volume) {
        if (b != null && b.defaultBlockState().getSoundType() != null) {
            WorldUtilities.playSound(world, p, b.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, b.defaultBlockState().getSoundType().getVolume() * volume, b.defaultBlockState().getSoundType().getPitch());
        }
    }

    public static void playSoundByMillName(Level world, Point p, String soundMill, float volume) {
        if (soundMill.equals("metal")) {
            WorldUtilities.playSoundBlockPlaced(world, p, Blocks.IRON_BLOCK, volume);
        } else if (soundMill.equals("wood")) {
            WorldUtilities.playSoundBlockPlaced(world, p, Blocks.OAK_LOG, volume);
        } else if (soundMill.equals("wool")) {
            WorldUtilities.playSoundBlockPlaced(world, p, Blocks.WHITE_WOOL, volume);
        } else if (soundMill.equals("glass")) {
            WorldUtilities.playSoundBlockPlaced(world, p, Blocks.GLASS, volume);
        } else if (soundMill.equals("stone")) {
            WorldUtilities.playSoundBlockPlaced(world, p, Blocks.STONE, volume);
        } else if (soundMill.equals("earth")) {
            WorldUtilities.playSoundBlockPlaced(world, p, Blocks.DIRT, volume);
        } else if (soundMill.equals("sand")) {
            WorldUtilities.playSoundBlockPlaced(world, p, (Block)Blocks.SAND, volume);
        } else {
            MillLog.printException("Tried to play unknown sound: " + soundMill, new Exception());
        }
    }

    public static boolean setBlock(Level world, Point p, Block block) {
        return WorldUtilities.setBlock(world, p, block, true, false);
    }

    public static boolean setBlock(Level world, Point p, Block block, boolean notify, boolean playSound) {
        Block oldBlock;
        if (p.x < -3.2E7 || p.z < -3.2E7 || p.x >= 3.2E7 || p.z > 3.2E7) {
            return false;
        }
        if (p.y < world.getMinBuildHeight()) {
            return false;
        }
        if (p.y >= world.getMaxBuildHeight()) {
            return false;
        }
        if (playSound && block == Blocks.AIR && (oldBlock = WorldUtilities.getBlock(world, p.getiX(), p.getiY(), p.getiZ())) != null && oldBlock.defaultBlockState().getSoundType() != null) {
            if (oldBlock.defaultBlockState().getSoundType() == SoundType.GRAVEL) {
                WorldUtilities.playSoundBlockBreaking(world, p, oldBlock, 0.5f);
            } else {
                WorldUtilities.playSoundBlockBreaking(world, p, oldBlock, 1.0f);
            }
        }
        ConnectedBlockPlacement.place(world, p.getBlockPos(), block.defaultBlockState(), notify);
        if (playSound && block != Blocks.AIR && block.defaultBlockState().getSoundType() != null) {
            if (block.defaultBlockState().getSoundType() == SoundType.GRAVEL) {
                WorldUtilities.playSoundBlockBreaking(world, p, block, 0.5f);
            } else {
                WorldUtilities.playSoundBlockBreaking(world, p, block, 1.0f);
            }
        }
        return true;
    }

    public static boolean setBlockAndMetadata(Level world, int x, int y, int z, Block block, int metadata, boolean notify, boolean playSound) {
        if (block == null) return false;
        return setBlockstate(world, new Point(x, y, z), LegacyWorldMetadata.write(block, metadata), notify, playSound);
    }

    public static boolean setBlockAndMetadata(Level world, Point p, Block block, int metadata) {
        return WorldUtilities.setBlockAndMetadata(world, p, block, metadata, true, false);
    }

    public static boolean setBlockAndMetadata(Level world, Point p, Block block, int metadata, boolean notify, boolean playSound) {
        return WorldUtilities.setBlockAndMetadata(world, p.getiX(), p.getiY(), p.getiZ(), block, metadata, notify, playSound);
    }

    public static boolean setBlockMetadata(Level world, int x, int y, int z, int metadata, boolean notify) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return false;
        }
        if (y < world.getMinBuildHeight()) {
            return false;
        }
        if (y >= world.getMaxBuildHeight()) {
            return false;
        }
        Point p = new Point(x, y, z);
        BlockState state = p.getBlockActualState(world);
        state = LegacyWorldMetadata.write(state.getBlock(), metadata);
        return ConnectedBlockPlacement.place(world, p.getBlockPos(), state, notify);
    }

    public static boolean setBlockMetadata(Level world, Point p, int metadata) {
        return WorldUtilities.setBlockMetadata(world, p, metadata, true);
    }

    public static boolean setBlockMetadata(Level world, Point p, int metadata, boolean notify) {
        return WorldUtilities.setBlockMetadata(world, p.getiX(), p.getiY(), p.getiZ(), metadata, notify);
    }

    public static boolean setBlockstate(Level world, Point p, BlockState bs, boolean b, boolean c) {
        if (world.isOutsideBuildHeight(p.getBlockPos()) || !Level.isInSpawnableBounds(p.getBlockPos())) return false;
        BlockState previous = world.getBlockState(p.getBlockPos());
        boolean changed = ConnectedBlockPlacement.place(world, p.getBlockPos(), bs, b);
        if (changed && c) {
            if (!previous.isAir()) playSoundBlockBreaking(world,p,previous.getBlock(),1);
            if (!bs.isAir()) playSoundBlockPlaced(world,p,bs.getBlock(),2);
        }
        return changed;
    }

    public static void spawnExp(Level world, Point p, int nb) {
        int l;
        if (world.isClientSide) {
            return;
        }
        for (int j = nb; j > 0; j -= l) {
            l = ExperienceOrb.getExperienceValue((int)j);
            world.addFreshEntity(new ExperienceOrb(world, p.x + 0.5, p.y + 5.0, p.z + 0.5, l));
        }
    }

    public static ItemEntity spawnItem(Level world, Point p, ItemStack itemstack, float f) {
        if (world.isClientSide) {
            return null;
        }
        ItemEntity entityitem = new ItemEntity(world, p.x, p.y + (double)f, p.z, itemstack);
        entityitem.setDefaultPickUpDelay();
        if (!world.addFreshEntity(entityitem)) return null;
        return entityitem;
    }

    public static void spawnMobsAround(Level world, Point p, int radius, ResourceLocation mobType, int minNb, int extraNb) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel server)) return;
        net.minecraft.world.entity.EntityType<?> type=net.minecraftforge.registries.ForgeRegistries.ENTITIES.getValue(mobType);
        if (type==null) return;
        int nb = minNb;
        if (extraNb > 0) {
            nb += MillCommonUtilities.randomInt(extraNb);
        }
        for (int i = 0; i < nb; ++i) {
            Entity created=type.create(world);
            Mob entityliving=created instanceof Mob mob?mob:null;
            if (entityliving == null) continue;
            boolean spawned = false;
            for (int j = 0; j < 20 && !spawned; ++j) {
                double ez;
                double ey;
                double ex = p.x + (world.random.nextDouble() * 2.0 - 1.0) * (double)radius;
                Point ep = new Point(ex, ey = p.y + (double)world.random.nextInt(3) - 1.0, ez = p.z + (world.random.nextDouble() * 2.0 - 1.0) * (double)radius);
                if (!ep.getBelow().getBlockActualState(world).isFaceSturdy(world,ep.getBelow().getBlockPos(),Direction.UP)) continue;
                entityliving.moveTo(ex, ey, ez, world.random.nextFloat() * 360.0f, 0.0f);
                if (!entityliving.checkSpawnRules(world,net.minecraft.world.entity.MobSpawnType.EVENT) || !entityliving.checkSpawnObstruction(world)) continue;
                if (!world.addFreshEntity(entityliving)) continue;
                MillLog.major(null, "Entering world: " + entityliving.getClass().getName());
                spawned = true;
            }
            if (!spawned) {
                MillLog.major(null, "No valid space found.");
            }
            if (spawned) entityliving.spawnAnim();
        }
    }

    public static Entity spawnMobsSpawner(Level world, Point p, ResourceLocation mobType) {
        int z;
        int ez;
        int ey;
        if (!(world instanceof net.minecraft.server.level.ServerLevel)) return null;
        net.minecraft.world.entity.EntityType<?> type=net.minecraftforge.registries.ForgeRegistries.ENTITIES.getValue(mobType);
        Entity created=type==null?null:type.create(world);
        Mob entityliving=created instanceof Mob mob?mob:null;
        if (entityliving == null) {
            return null;
        }
        int x = MillCommonUtilities.randomInt(2) - 1;
        int ex = (int)(p.x + (double)x);
        if (WorldUtilities.getBlock(world, ex, ey = (int)p.y, ez = (int)(p.z + (double)(z = MillCommonUtilities.randomInt(2) - 1))) != Blocks.AIR && WorldUtilities.getBlock(world, ex, ey + 1, ez) != Blocks.AIR) {
            return null;
        }
        entityliving.moveTo((double)ex, (double)ey, (double)ez, world.random.nextFloat() * 360.0f, 0.0f);
        if (!world.addFreshEntity(entityliving)) return null;
        entityliving.spawnAnim();
        return entityliving;
    }
}

