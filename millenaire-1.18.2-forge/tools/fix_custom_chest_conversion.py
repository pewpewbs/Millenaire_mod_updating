from pathlib import Path
import shutil

root = Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
source = root/'src/main/java/org/millenaire/common/buildingplan/BuildingCustomPlan.java'
backup = root/'migration/before-chest-conversion/BuildingCustomPlan.java'
backup.parent.mkdir(parents=True, exist_ok=True)
if not backup.exists(): shutil.copy2(source, backup)
text = source.read_text(encoding='utf-8')
old = '''                    int meta = chestP.getMeta(building.world);
                    chestP.setBlock(building.world, (Block)MillBlocks.LOCKED_CHEST, meta, false, false);'''
new = '''                    if (!org.millenaire.common.utilities.ChestConversion.convert(building.world, chestP.getBlockPos())) {
                        MillLog.error(this, "Could not convert chest at " + chestP + "; original contents preserved.");
                        continue;
                    }'''
assert text.count(old) == 1
source.write_text(text.replace(old,new), encoding='utf-8')
helper = root/'src/main/java/org/millenaire/common/utilities/ChestConversion.java'
helper.write_text('''package org.millenaire.common.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.block.BlockLockedChest;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.entity.TileEntityLockedChest;

/** Converts one half at a time, retaining inventory, names and unopened loot tables. */
public final class ChestConversion {
    private ChestConversion() {}

    public static boolean convert(Level world, BlockPos pos) {
        if (world.isClientSide || !world.hasChunkAt(pos)) return false;
        BlockState previous = world.getBlockState(pos);
        if (!previous.is(Blocks.CHEST) || !(world.getBlockEntity(pos) instanceof ChestBlockEntity original)) return false;
        CompoundTag saved = original.saveWithoutMetadata();
        BlockState replacement = MillBlocks.LOCKED_CHEST.defaultBlockState()
            .setValue(BlockLockedChest.FACING, previous.getValue(ChestBlock.FACING));
        // ChestBlock.onRemove drops the old inventory. Detach it first so the items
        // can be transferred exactly once, including the untouched loot-table seed.
        world.removeBlockEntity(pos);
        boolean converted = false;
        try {
            if (world.setBlock(pos, replacement, 3)
                && world.getBlockEntity(pos) instanceof TileEntityLockedChest target) {
                target.load(saved);
                target.setChanged();
                converted = true;
            }
            return converted;
        } finally {
            if (!converted) {
                // Restore the original object even if replacement failed before a
                // new block entity was created. No items were cleared from it.
                world.removeBlockEntity(pos);
                world.setBlock(pos, previous, 3);
                original.clearRemoved();
                world.setBlockEntity(original);
                original.setChanged();
            }
        }
    }
}
''',encoding='utf-8')
print('Custom chest conversion now preserves full container NBT and restores failures.')
