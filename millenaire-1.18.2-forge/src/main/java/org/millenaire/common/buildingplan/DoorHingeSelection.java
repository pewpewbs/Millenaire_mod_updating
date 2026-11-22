package org.millenaire.common.buildingplan;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.core.BlockPos;

public final class DoorHingeSelection {
    private DoorHingeSelection() {}
    private static boolean door(BlockState s) { return s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER; }
    private static int solid(BlockState s) { return s.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) ? 1 : 0; }
    public static DoorHingeSide choose(BlockState state, BlockState left, BlockState right, BlockState aboveLeft, BlockState aboveRight) {
        boolean leftDoor=door(left), rightDoor=door(right);
        if (leftDoor != rightDoor) return leftDoor ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
        int balance=solid(right)+solid(aboveRight)-solid(left)-solid(aboveLeft);
        if (balance!=0) return balance>0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
        return state.getValue(DoorBlock.HINGE);
    }
}
