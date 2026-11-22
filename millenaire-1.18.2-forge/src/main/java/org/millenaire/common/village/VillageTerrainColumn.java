package org.millenaire.common.village;

import java.util.function.IntFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded column analysis, independent of chunk loading or thread scheduling. */
public final class VillageTerrainColumn {
    private VillageTerrainColumn() {}
    public interface Rules {
        boolean ground(Block block); boolean solid(Block block); boolean liquid(Block block);
        boolean forbidden(Block block); boolean dangerous(Block block); boolean water(Block block);
        boolean fence(Block block); boolean path(Block block); boolean tree(BlockState state);
        boolean explicitlyForbidden(Block block);
    }
    public record Result(int top, int clearance, boolean forbidden, boolean danger, boolean water,
                         boolean tree, boolean path, boolean groundFound) {}
    public static Result scan(IntFunction<BlockState> read, int baseline, int minimum, int maximum, Rules rules) {
        int min = Math.max(baseline - 25, minimum);
        int max = Math.min(baseline + 25, maximum - 2);
        int y = max;
        boolean forbidden = false;
        if (min > max) return new Result(Math.max(minimum + 1, Math.min(baseline, maximum - 1)), 0, false, false, false, false, false, false);
        Block block = read.apply(y).getBlock();
        while (y >= min && !rules.ground(block)) {
            forbidden |= rules.forbidden(block);
            if (--y >= min) block = read.apply(y).getBlock();
        }
        if (y < min) return new Result(min, 0, forbidden, false, false, false, false, false);
        boolean onGround = true;
        int lastLiquid = minimum;
        while (y <= max && (rules.solid(block) || rules.liquid(block) || !onGround)) {
            forbidden |= rules.forbidden(block);
            if (rules.liquid(block)) { onGround = false; lastLiquid = y; }
            else if (rules.solid(block)) onGround = true;
            if (++y <= max) block = read.apply(y).getBlock();
        }
        if (!onGround) y = lastLiquid;
        while (y <= max && (rules.solid(read.apply(y).getBlock()) || rules.solid(read.apply(y + 1).getBlock()))) y++;
        y = Math.max(minimum + 1, Math.min(y, maximum - 1));
        BlockState soilState = read.apply(y - 1);
        Block soil = soilState.getBlock();
        block = read.apply(y).getBlock();
        boolean water = rules.water(block);
        boolean danger = rules.dangerous(block) || rules.explicitlyForbidden(block) || rules.explicitlyForbidden(soil);
        boolean blocked = rules.fence(soil) || rules.solid(block) || water || rules.water(soil);
        int clearance = blocked ? 0 : 1;
        forbidden |= rules.forbidden(block);
        for (int above = y + 1; above < max; above++) {
            Block upper = read.apply(above).getBlock();
            if (!blocked && clearance < 3 && !rules.solid(upper)) clearance++;
            else blocked = true;
            forbidden |= rules.forbidden(upper);
        }
        if (danger) clearance = 0;
        return new Result(y, clearance, forbidden, danger, water, rules.tree(soilState), rules.path(soil), true);
    }
}
