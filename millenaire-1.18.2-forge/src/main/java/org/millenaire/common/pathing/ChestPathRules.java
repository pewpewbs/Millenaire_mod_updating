package org.millenaire.common.pathing;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import org.millenaire.common.block.BlockLockedChest;

public final class ChestPathRules {
    private ChestPathRules() {}
    public static boolean isChest(Block block) {
        return block instanceof ChestBlock || block instanceof EnderChestBlock
                || block instanceof BlockLockedChest;
    }
}
