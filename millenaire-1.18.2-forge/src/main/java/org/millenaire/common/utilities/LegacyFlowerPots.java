package org.millenaire.common.utilities;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Ordinals in the original building palette, not vanilla block metadata. */
public final class LegacyFlowerPots {
    private LegacyFlowerPots() {}
    private static final Block[] POTS = {
        Blocks.FLOWER_POT,
        Blocks.POTTED_POPPY,
        Blocks.POTTED_BLUE_ORCHID,
        Blocks.POTTED_ALLIUM,
        Blocks.POTTED_AZURE_BLUET,
        Blocks.POTTED_RED_TULIP,
        Blocks.POTTED_ORANGE_TULIP,
        Blocks.POTTED_WHITE_TULIP,
        Blocks.POTTED_PINK_TULIP,
        Blocks.POTTED_OXEYE_DAISY,
        Blocks.POTTED_DANDELION,
        Blocks.POTTED_OAK_SAPLING,
        Blocks.POTTED_SPRUCE_SAPLING,
        Blocks.POTTED_BIRCH_SAPLING,
        Blocks.POTTED_JUNGLE_SAPLING,
        Blocks.POTTED_ACACIA_SAPLING,
        Blocks.POTTED_DARK_OAK_SAPLING,
        Blocks.POTTED_RED_MUSHROOM,
        Blocks.POTTED_BROWN_MUSHROOM,
        Blocks.POTTED_DEAD_BUSH,
        Blocks.POTTED_FERN,
        Blocks.POTTED_CACTUS
    };
    public static BlockState fromPlan(int ordinal) {
        if (ordinal == 255 || ordinal == -1) ordinal = 0;
        if (ordinal < 0 || ordinal >= POTS.length)
            throw new IllegalArgumentException("Unknown legacy flowerpot ordinal: " + ordinal);
        return POTS[ordinal].defaultBlockState();
    }
    /** Empty uses 255 as in blocklist.txt. Unknown modern plants are not silently erased. */
    public static int toPlan(Block block) {
        if (block == Blocks.FLOWER_POT) return 255;
        for (int i = 1; i < POTS.length; i++) if (POTS[i] == block) return i;
        throw new IllegalArgumentException("Flowerpot has no legacy palette entry: " + block);
    }
}
