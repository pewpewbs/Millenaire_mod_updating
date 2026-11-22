package net.minecraftforge.registries;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.items.CapabilityItemHandler;
import org.millenaire.common.block.BlockFirePit;
import org.millenaire.common.entity.MillBlockEntities;
import org.millenaire.common.entity.TileEntityFirePit;

/** Isolated registry fixture; the production registration and live cooking need integration testing. */
public class FirePitStateCheck {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); net.minecraftforge.common.ForgeHooks.updateBurns();
        ((ForgeRegistry<net.minecraft.world.level.block.Block>)ForgeRegistries.BLOCKS).unfreeze();
        ((NamespacedDefaultedWrapper<net.minecraft.world.level.block.Block>)net.minecraft.core.Registry.BLOCK).unfreeze();
        BlockFirePit block = new BlockFirePit("firepit_state_check");
        MillBlockEntities.FIRE_PIT = BlockEntityType.Builder.of(TileEntityFirePit::new, block).build(null);
        for (int meta = 0; meta < 4; meta++) {
            var state = block.func_176203_a(meta);
            check(block.func_176201_c(state) == meta, "fire and alignment metadata roundtrip");
            check(state.getLightEmission() == ((meta & 1) == 0 ? 0 : 15), "lit state emits light");
        }
        var pit = new TileEntityFirePit(BlockPos.ZERO, block.defaultBlockState());
        pit.inputs.setStackInSlot(0, new ItemStack(Items.PORKCHOP, 11));
        pit.inputs.setStackInSlot(2, new ItemStack(Items.POTATO, 5));
        pit.outputs.setStackInSlot(1, new ItemStack(Items.COOKED_BEEF, 7));
        var side = pit.fuel;
        check(side.getSlots() == 1 && side.insertItem(0, new ItemStack(Items.COAL, 6), false).isEmpty(), "sides insert fuel");
        check(!side.insertItem(0, new ItemStack(Items.DIRT), false).isEmpty(), "sides reject nonfuel");
        var below = pit.outputs;
        check(!below.insertItem(0, new ItemStack(Items.COAL), false).isEmpty(), "outputs reject inserted items");
        check(below.extractItem(1, 2, false).getCount() == 2, "outputs allow extraction");
        check(pit.inputs.getSlots() == 3, "three input slots");
        pit.setBurnTime(1300); pit.setTotalBurnTime(1600); pit.setCookTime(0, 80); pit.setCookTime(2, 151);
        CompoundTag saved = pit.getUpdateTag();
        check(!saved.getCompound("Inventory").contains("Size"), "legacy inventory tag format retained");
        var loaded = new TileEntityFirePit(BlockPos.ZERO, block.defaultBlockState()); loaded.load(saved);
        check(loaded.inputs.getStackInSlot(0).getCount() == 11 && loaded.inputs.getStackInSlot(2).getCount() == 5, "inputs survive save/load");
        check(loaded.outputs.getStackInSlot(1).getCount() == 5 && loaded.fuel.getStackInSlot(0).getCount() == 6, "output and fuel survive save/load");
        check(loaded.getBurnTime() == 1300 && loaded.getTotalBurnTime() == 1600 && loaded.getCookTime(0) == 80 && loaded.getCookTime(2) == 151, "all timers survive save/load");
        check(TileEntityFirePit.fuelTime(new ItemStack(Items.LAVA_BUCKET)) > 0 && new ItemStack(Items.LAVA_BUCKET).getContainerItem().is(Items.BUCKET), "lava fuel returns bucket");
        System.out.println("Fire pit state, inventory persistence and slot validation checks passed (isolated registry fixture; capability routing requires Forge launcher)");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
