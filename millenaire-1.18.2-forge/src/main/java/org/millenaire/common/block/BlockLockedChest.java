package org.millenaire.common.block;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkHooks;
import org.millenaire.common.entity.*;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.ui.*;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

public class BlockLockedChest extends BaseEntityBlock implements LegacyVariantBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public BlockLockedChest(String name) {
        super(Properties.of(Material.WOOD).strength(50,1200).sound(SoundType.WOOD).noOcclusion());
        setRegistryName(name);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new TileEntityLockedChest(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,MillBlockEntities.LOCKED_CHEST,(world,pos,block,chest)->chest.tick());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public List<ItemStack> getDrops(BlockState state,LootContext.Builder context) { return List.of(); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return func_176196_c(context.getLevel(),context.getClickedPos())?defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()):null;
    }
    public static Container getInventory(TileEntityLockedChest chest,Level world,int x,int y,int z) {
        if(chest==null)return null;
        BlockPos pos=new BlockPos(x,y,z);
        return world.getBlockState(pos).getBlock() instanceof BlockLockedChest block?block.getContainer(world,pos,true):null;
    }
    public static ContainerLockedChest createContainer(Level world,int x,int y,int z,Player player) {
        if(!(world.getBlockEntity(new BlockPos(x,y,z)) instanceof TileEntityLockedChest chest))return null;
        Container inventory=getInventory(chest,world,x,y,z);if(inventory==null)return null;
        var data=Mill.getMillWorld(world);
        Building building=data==null || chest.buildingPos==null?null:data.getBuilding(chest.buildingPos);
        return new ContainerLockedChest(player.getInventory(),inventory,player,building,isLocked(inventory,player));
    }
    public static boolean isLocked(Container inventory,Player player) {
        return inventory instanceof TileEntityLockedChest chest?chest.isLockedFor(player):
            inventory instanceof TileEntityLockedChest.InventoryLockedLargeChest pair?pair.isLockedFor(player):true;
    }
    public static void openChest(Player player,BlockPos pos) {
        if(!(player instanceof ServerPlayer server) || !player.level.hasChunkAt(pos)
            || player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64
            || !player.level.mayInteract(player,pos))return;
        if(!(player.level.getBlockState(pos).getBlock() instanceof BlockLockedChest block))return;
        Container inventory=block.getContainer(player.level,pos,false);
        if(inventory instanceof MenuProvider provider) {
            boolean locked=isLocked(inventory,player);
            NetworkHooks.openGui(server,provider,buffer->{buffer.writeVarInt(inventory.getContainerSize()/9);buffer.writeBoolean(locked);});
        }
    }
    @Nullable public Container getContainer(Level world,BlockPos pos,boolean allowBlocking) {
        if(!(world.getBlockEntity(pos) instanceof TileEntityLockedChest chest) || (!allowBlocking && isBlocked(world,pos)))return null;
        for(Direction direction:Direction.Plane.HORIZONTAL) {
            BlockPos next=pos.relative(direction);
            if(!world.hasChunkAt(next) || !world.getBlockState(next).is(this))continue;
            if(!allowBlocking && isBlocked(world,next))return null;
            if(world.getBlockEntity(next) instanceof TileEntityLockedChest other) {
                boolean first=direction==Direction.WEST || direction==Direction.NORTH;
                return new TileEntityLockedChest.InventoryLockedLargeChest(chest.getInvLargeName(),first?other:chest,first?chest:other);
            }
        }
        return chest;
    }
    @Nullable public Container getLockableContainer(Level world,BlockPos pos) { return getContainer(world,pos,false); }
    private boolean isBlocked(Level world,BlockPos pos) {
        if(world.getBlockState(pos.above()).isRedstoneConductor(world,pos.above()))return true;
        for(Cat cat:world.getEntitiesOfClass(Cat.class,new AABB(pos.above())))if(cat.isInSittingPose())return true;
        return false;
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {
        if(world.getBlockState(pos.north()).is(this))return Block.box(1,0,0,15,14,15);
        if(world.getBlockState(pos.south()).is(this))return Block.box(1,0,1,15,14,16);
        if(world.getBlockState(pos.west()).is(this))return Block.box(0,0,1,15,14,15);
        if(world.getBlockState(pos.east()).is(this))return Block.box(1,0,1,16,14,15);
        return Block.box(1,0,1,15,14,15);
    }
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!world.isClientSide)GuiActions.activateMillChest(player,new Point(pos));
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
    @Override public void neighborChanged(BlockState state,Level world,BlockPos pos,Block block,BlockPos from,boolean moving) {
        super.neighborChanged(state,world,pos,block,from,moving);
        if(world.getBlockEntity(pos) instanceof TileEntityLockedChest chest)chest.func_145836_u();
    }
    @Override public void onPlace(BlockState state,Level world,BlockPos pos,BlockState old,boolean moving) {
        if(old.is(this))return;
        checkForSurroundingChests(world,pos,state);
        for(Direction direction:Direction.Plane.HORIZONTAL) {
            BlockPos next=pos.relative(direction);BlockState adjacent=world.getBlockState(next);
            if(adjacent.is(this))checkForSurroundingChests(world,next,adjacent);
        }
    }
    public boolean func_176196_c(Level worldIn, BlockPos pos) {
        int i = 0;
        BlockPos blockpos = pos.west();
        BlockPos blockpos1 = pos.east();
        BlockPos blockpos2 = pos.north();
        BlockPos blockpos3 = pos.south();
        if (worldIn.getBlockState(blockpos).getBlock() == this) {
            if (this.isDoubleChest(worldIn, blockpos)) {
                return false;
            }
            ++i;
        }
        if (worldIn.getBlockState(blockpos1).getBlock() == this) {
            if (this.isDoubleChest(worldIn, blockpos1)) {
                return false;
            }
            ++i;
        }
        if (worldIn.getBlockState(blockpos2).getBlock() == this) {
            if (this.isDoubleChest(worldIn, blockpos2)) {
                return false;
            }
            ++i;
        }
        if (worldIn.getBlockState(blockpos3).getBlock() == this) {
            if (this.isDoubleChest(worldIn, blockpos3)) {
                return false;
            }
            ++i;
        }
        return i <= 1;
    }

    public BlockState checkForSurroundingChests(Level worldIn, BlockPos pos, BlockState state) {
        if (worldIn.isClientSide) {
            return state;
        }
        BlockState iblockstate = worldIn.getBlockState(pos.north());
        BlockState iblockstate1 = worldIn.getBlockState(pos.south());
        BlockState iblockstate2 = worldIn.getBlockState(pos.west());
        BlockState iblockstate3 = worldIn.getBlockState(pos.east());
        Direction enumfacing = (Direction)state.getValue(FACING);
        if (iblockstate.getBlock() != this && iblockstate1.getBlock() != this) {
            boolean flag = iblockstate.isCollisionShapeFullBlock(worldIn, pos.north());
            boolean flag1 = iblockstate1.isCollisionShapeFullBlock(worldIn, pos.south());
            if (iblockstate2.getBlock() == this || iblockstate3.getBlock() == this) {
                BlockPos blockpos1 = iblockstate2.getBlock() == this ? pos.west() : pos.east();
                BlockState iblockstate7 = worldIn.getBlockState(blockpos1.north());
                BlockState iblockstate6 = worldIn.getBlockState(blockpos1.south());
                enumfacing = Direction.SOUTH;
                Direction enumfacing2 = iblockstate2.getBlock() == this ? (Direction)iblockstate2.getValue(FACING) : (Direction)iblockstate3.getValue(FACING);
                if (enumfacing2 == Direction.NORTH) {
                    enumfacing = Direction.NORTH;
                }
                if ((flag || iblockstate7.isCollisionShapeFullBlock(worldIn, blockpos1.north())) && !flag1 && !iblockstate6.isCollisionShapeFullBlock(worldIn, blockpos1.south())) {
                    enumfacing = Direction.SOUTH;
                }
                if ((flag1 || iblockstate6.isCollisionShapeFullBlock(worldIn, blockpos1.south())) && !flag && !iblockstate7.isCollisionShapeFullBlock(worldIn, blockpos1.north())) {
                    enumfacing = Direction.NORTH;
                }
            }
        } else {
            BlockPos blockpos = iblockstate.getBlock() == this ? pos.north() : pos.south();
            BlockState iblockstate4 = worldIn.getBlockState(blockpos.west());
            BlockState iblockstate5 = worldIn.getBlockState(blockpos.east());
            enumfacing = Direction.EAST;
            Direction enumfacing1 = iblockstate.getBlock() == this ? (Direction)iblockstate.getValue(FACING) : (Direction)iblockstate1.getValue(FACING);
            if (enumfacing1 == Direction.WEST) {
                enumfacing = Direction.WEST;
            }
            if ((iblockstate2.isCollisionShapeFullBlock(worldIn, pos.west()) || iblockstate4.isCollisionShapeFullBlock(worldIn, blockpos.west())) && !iblockstate3.isCollisionShapeFullBlock(worldIn, pos.east()) && !iblockstate5.isCollisionShapeFullBlock(worldIn, blockpos.east())) {
                enumfacing = Direction.EAST;
            }
            if ((iblockstate3.isCollisionShapeFullBlock(worldIn, pos.east()) || iblockstate5.isCollisionShapeFullBlock(worldIn, blockpos.east())) && !iblockstate2.isCollisionShapeFullBlock(worldIn, pos.west()) && !iblockstate4.isCollisionShapeFullBlock(worldIn, blockpos.west())) {
                enumfacing = Direction.WEST;
            }
        }
        state = state.setValue(FACING, enumfacing);
        worldIn.setBlock(pos, state, 3);
        return state;
    }

    public BlockState correctFacing(Level worldIn, BlockPos pos, BlockState state) {
        Direction enumfacing = null;
        for (Direction enumfacing1 : Direction.Plane.HORIZONTAL) {
            BlockState iblockstate = worldIn.getBlockState(pos.relative(enumfacing1));
            if (iblockstate.getBlock() == this) {
                return state;
            }
            if (!iblockstate.isCollisionShapeFullBlock(worldIn, pos.relative(enumfacing1))) continue;
            if (enumfacing != null) {
                enumfacing = null;
                break;
            }
            enumfacing = enumfacing1;
        }
        if (enumfacing != null) {
            return state.setValue(FACING, enumfacing.getOpposite());
        }
        Direction enumfacing2 = (Direction)state.getValue(FACING);
        if (worldIn.getBlockState(pos.relative(enumfacing2)).isCollisionShapeFullBlock(worldIn,pos.relative(enumfacing2))) {
            enumfacing2 = enumfacing2.getOpposite();
        }
        if (worldIn.getBlockState(pos.relative(enumfacing2)).isCollisionShapeFullBlock(worldIn,pos.relative(enumfacing2))) {
            enumfacing2 = enumfacing2.getClockWise();
        }
        if (worldIn.getBlockState(pos.relative(enumfacing2)).isCollisionShapeFullBlock(worldIn,pos.relative(enumfacing2))) {
            enumfacing2 = enumfacing2.getOpposite();
        }
        return state.setValue(FACING, enumfacing2);
    }

    public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)this.getLockableContainer(worldIn, pos));
    }

    public int func_176201_c(BlockState state) {
        return ((Direction)state.getValue(FACING)).get3DDataValue();
    }

    public BlockState func_176203_a(int meta) {
        Direction enumfacing = Direction.from3DDataValue((int)meta);
        if (enumfacing.getAxis() == Direction.Axis.Y) {
            enumfacing = Direction.NORTH;
        }
        return this.defaultBlockState().setValue(FACING, enumfacing);
    }

    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    private boolean isDoubleChest(Level worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() != this) {
            return false;
        }
        for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
            if (worldIn.getBlockState(pos.relative(enumfacing)).getBlock() != this) continue;
            return true;
        }
        return false;
    }

    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockEntity tileentity;
        boolean flag3;
        Direction enumfacing = Direction.from2DDataValue((int)(Mth.floor((double)((double)(placer.getYRot() * 4.0f / 360.0f) + 0.5)) & 3)).getOpposite();
        state = state.setValue(FACING, enumfacing);
        BlockPos blockpos = pos.north();
        BlockPos blockpos1 = pos.south();
        BlockPos blockpos2 = pos.west();
        BlockPos blockpos3 = pos.east();
        boolean flag = this == worldIn.getBlockState(blockpos).getBlock();
        boolean flag1 = this == worldIn.getBlockState(blockpos1).getBlock();
        boolean flag2 = this == worldIn.getBlockState(blockpos2).getBlock();
        boolean bl = flag3 = this == worldIn.getBlockState(blockpos3).getBlock();
        if (!(flag || flag1 || flag2 || flag3)) {
            worldIn.setBlock(pos, state, 3);
        } else if (enumfacing.getAxis() != Direction.Axis.X || !flag && !flag1) {
            if (enumfacing.getAxis() == Direction.Axis.Z && (flag2 || flag3)) {
                if (flag2) {
                    worldIn.setBlock(blockpos2, state, 3);
                } else {
                    worldIn.setBlock(blockpos3, state, 3);
                }
                worldIn.setBlock(pos, state, 3);
            }
        } else {
            if (flag) {
                worldIn.setBlock(blockpos, state, 3);
            } else {
                worldIn.setBlock(blockpos1, state, 3);
            }
            worldIn.setBlock(pos, state, 3);
        }
        if (stack.hasCustomHoverName() && (tileentity = worldIn.getBlockEntity(pos)) instanceof TileEntityLockedChest) {
            ((TileEntityLockedChest)tileentity).setCustomName(stack.getHoverName());
        }
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
    }

}
