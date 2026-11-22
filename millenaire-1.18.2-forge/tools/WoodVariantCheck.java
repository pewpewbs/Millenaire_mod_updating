import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.millenaire.common.block.WoodVariant;

public class WoodVariantCheck {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        for (WoodVariant wood : WoodVariant.values()) {
            check(WoodVariant.byMetadata(wood.getMetadata()) == wood, "legacy wood numbering");
            check(WoodVariant.fromState(wood.planks().defaultBlockState()) == wood, "plank family");
            check(WoodVariant.fromState(wood.sapling().defaultBlockState()) == wood, "sapling family");
        }
        var log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        var swapped = WoodVariant.JUNGLE.replace(log);
        check(swapped.is(Blocks.JUNGLE_LOG) && swapped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "horizontal log orientation");
        var stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP).setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.WATERLOGGED, true);
        var birch = WoodVariant.BIRCH.replace(stairs);
        check(birch.is(Blocks.BIRCH_STAIRS) && birch.getValue(StairBlock.HALF) == Half.TOP && birch.getValue(StairBlock.FACING) == Direction.WEST && birch.getValue(StairBlock.WATERLOGGED), "stair state preservation");
        var door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER).setValue(DoorBlock.OPEN, true);
        var spruce = WoodVariant.SPRUCE.replace(door);
        check(spruce.is(Blocks.SPRUCE_DOOR) && spruce.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER && spruce.getValue(DoorBlock.OPEN), "door state preservation");
        check(WoodVariant.ACACIA.replace(Blocks.STONE.defaultBlockState()).is(Blocks.STONE), "nonwood unchanged");
        System.out.println("Wood family checks passed");
    }
    private static void check(boolean success, String message) { if (!success) throw new AssertionError(message); }
}
