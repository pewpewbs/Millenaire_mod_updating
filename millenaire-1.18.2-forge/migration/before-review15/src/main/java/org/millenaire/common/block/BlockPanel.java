package org.millenaire.common.block;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.millenaire.common.entity.TileEntityPanel;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.Point;

public class BlockPanel extends BaseEntityBlock implements LegacyVariantBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public BlockPanel(String name) { super(Properties.of(Material.WOOD).strength(1.0F).noCollission().noOcclusion()); setRegistryName(name); registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new TileEntityPanel(pos,state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return switch (state.getValue(FACING)) { case EAST -> Block.box(0,0,0,2,16,16); case WEST -> Block.box(14,0,0,16,16,16); case SOUTH -> Block.box(0,0,0,16,16,2); default -> Block.box(0,0,14,16,16,16); };
    }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) { return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).getMaterial().isSolid(); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) if (direction.getAxis().isHorizontal()) {
            BlockState state = defaultBlockState().setValue(FACING,direction.getOpposite()); if (canSurvive(state,context.getLevel(),context.getClickedPos())) return state;
        }
        return null;
    }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        return side==state.getValue(FACING).getOpposite() && !canSurvive(state,level,pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state,side,neighbor,level,pos,neighborPos);
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootContext.Builder builder) { return List.of(); }
    @Override public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) { return ItemStack.EMPTY; }
    @Override public int func_176201_c(BlockState state) { return state.getValue(FACING).get3DDataValue(); }
    @Override public BlockState func_176203_a(int meta) { Direction facing=Direction.from3DDataValue(meta); return defaultBlockState().setValue(FACING,facing.getAxis().isHorizontal()?facing:Direction.NORTH); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public BlockState rotate(BlockState state,Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state,Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityPanel panel) || panel.panelType==0 || panel.buildingPos==null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var building=Mill.getMillWorld(level).getBuilding(panel.buildingPos); if (building==null) return InteractionResult.PASS;
        if (panel.panelType==4 && building.controlledBy(player)) ServerSender.displayControlledProjectGUI(player,building);
        else if (panel.panelType==13 && building.controlledBy(player)) ServerSender.displayControlledMilitaryGUI(player,building);
        else ServerSender.displayPanel(player,new Point(pos));
        return InteractionResult.CONSUME;
    }
}
