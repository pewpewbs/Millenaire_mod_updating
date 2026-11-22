package org.millenaire.common.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public class TileEntityMillBed extends BedBlockEntity {
    public TileEntityMillBed(BlockPos pos,BlockState state) { super(pos,state,DyeColor.RED); }
    @Override public BlockEntityType<?> getType() { return MillBlockEntities.MILL_BED; }
}
