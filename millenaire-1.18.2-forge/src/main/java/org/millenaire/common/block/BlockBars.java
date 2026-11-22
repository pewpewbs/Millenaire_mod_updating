package org.millenaire.common.block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class BlockBars extends BlockMillPane {
    protected BlockBars(String name) { super(name, Properties.of(Material.WOOD).strength(0.3F, 6.0F).sound(SoundType.WOOD).noOcclusion()); }
    public boolean canPaneConnectTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos other = pos.relative(direction); BlockState state = level.getBlockState(other);
        return attachesTo(state, state.isFaceSturdy(level, other, direction.getOpposite()));
    }
    public BlockState func_176221_a(BlockState state, BlockGetter level, BlockPos pos) {
        boolean northSouth = canPaneConnectTo(level, pos, Direction.NORTH) || canPaneConnectTo(level, pos, Direction.SOUTH);
        boolean eastWest = canPaneConnectTo(level, pos, Direction.EAST) || canPaneConnectTo(level, pos, Direction.WEST);
        return state.setValue(NORTH, northSouth).setValue(SOUTH, northSouth).setValue(EAST, eastWest).setValue(WEST, eastWest);
    }
}
