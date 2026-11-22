package net.minecraftforge.registries;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.millenaire.common.block.*;
import org.millenaire.common.item.LegacyItemStacks;

public class VariantBlockCheck {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var blocks = (ForgeRegistry<Block>)ForgeRegistries.BLOCKS;
        var items = (ForgeRegistry<Item>)ForgeRegistries.ITEMS;
        blocks.unfreeze(); items.unfreeze();
        ((NamespacedDefaultedWrapper<Block>)Registry.BLOCK).unfreeze();
        ((NamespacedDefaultedWrapper<Item>)Registry.ITEM).unfreeze();
        var sod = new BlockSod("sod_check");
        blocks.register(sod);
        var sodItem = new BlockItem(sod, new Item.Properties());
        sodItem.setRegistryName(sod.getRegistryName());
        items.register(sodItem);
        sodItem.registerBlocks(Item.BY_BLOCK, sodItem);
        var creative = NonNullList.<ItemStack>create();
        sod.fillItemCategory(CreativeModeTab.TAB_BUILDING_BLOCKS, creative);
        check(creative.size() == 6, "all six sod variants");
        for (WoodVariant wood : WoodVariant.values()) {
            var state = sod.func_176203_a(wood.getMetadata());
            check(state.getValue(BlockSod.VARIANT) == wood, "placed sod variant");
            var dropped = sod.getDrops(state, null).get(0);
            check(dropped.is(sodItem) && LegacyItemStacks.metadata(dropped) == wood.getMetadata(), "drop retains variant");
            check(LegacyItemStacks.metadata(sod.getCloneItemStack(null, BlockPos.ZERO, state)) == wood.getMetadata(), "pick block retains variant");
        }
        var slab = new BlockMillSlab("slab_check", Blocks.STONE);
        blocks.register(slab);
        var bottom = slab.func_176203_a(0);
        var top = slab.func_176203_a(8);
        check(bottom.getValue(BlockHalfSlab.HALF) == Half.BOTTOM && top.getValue(BlockHalfSlab.HALF) == Half.TOP, "half metadata");
        check(slab.getShape(bottom, null, BlockPos.ZERO, CollisionContext.empty()).max(Direction.Axis.Y) == 0.5, "bottom collision height");
        check(slab.getShape(top, null, BlockPos.ZERO, CollisionContext.empty()).min(Direction.Axis.Y) == 0.5, "top collision height");
        check(slab.getBaseBlock() == Blocks.STONE, "combined block identity");
        check(LegacyItemStacks.metadata(top) == 0, "top half is same construction item");
        Block[] decorated = {new BlockDecorativeStone("stone_check"), new BlockDecorativeWood("wood_check"),
            new BlockDecorativeEarth("earth_check"), new BlockExtendedMudBrick("mudbrick_check"),
            new BlockSlabStone("stone_slab_check"), new BlockSlabWood("wood_slab_check"), new BlockMillStainedGlass("glass_check")};
        for (Block block : decorated) {
            blocks.register(block);
            BlockItem blockItem = new BlockItem(block, new Item.Properties());
            blockItem.setRegistryName(block.getRegistryName());
            items.register(blockItem);
            blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
            var codec = (LegacyVariantBlock)block;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                if (state.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)
                    && state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE) == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE) continue;
                int metadata = codec.func_176201_c(state);
                var restored = codec.func_176203_a(metadata);
                var property = block.getStateDefinition().getProperty("variant");
                check(property == null ? restored == state : state.getValue(property).equals(restored.getValue(property)), "decorative variant roundtrip");
                var drop = block.getDrops(state, null).get(0);
                check(drop.is(blockItem), "decorative drop identity");
                check(LegacyItemStacks.metadata(drop) == (block instanceof net.minecraft.world.level.block.SlabBlock ? metadata & 7 : metadata), "decorative drop variant");
                check(LegacyItemStacks.metadata(state) == LegacyItemStacks.metadata(drop), "construction cost matches dropped item variant");
            }
        }
        var road = new BlockPath("path_check", net.minecraft.world.level.material.MaterialColor.DIRT, net.minecraft.world.level.block.SoundType.GRAVEL);
        var roadSlab = new BlockPathSlab("path_check", net.minecraft.world.level.material.MaterialColor.DIRT, net.minecraft.world.level.block.SoundType.GRAVEL);
        blocks.register(road); blocks.register(roadSlab);
        check(road.getSingleSlab() == roadSlab && roadSlab.getDoubleSlab() == road, "road pair lookup");
        check(road.isFullPath() && !roadSlab.isFullPath(), "road size identity");
        for (Block path : new Block[] {road, roadSlab}) {
            var codec = (LegacyVariantBlock)path;
            for (var state : path.getStateDefinition().getPossibleStates()) {
                check(codec.func_176203_a(codec.func_176201_c(state)) == state, "road stability and half roundtrip");
                check(LegacyItemStacks.metadata(state) == 0, "road stability and half do not change inventory identity");
                var shape = path.getShape(state, null, BlockPos.ZERO, CollisionContext.empty());
                double min = path == roadSlab && state.getValue(BlockPathSlab.HALF) == Half.TOP ? 0.5 : 0;
                double max = path == road || min == 0.5 ? 15.0 / 16 : 7.0 / 16;
                check(shape.min(Direction.Axis.Y) == min && shape.max(Direction.Axis.Y) == max, "road collision height");
            }
        }
        for (BlockOrientedSlab tiles : new BlockOrientedSlab[] {
                new BlockOrientedSlab.BlockOrientedSlabDouble("full_tiles_check"),
                new BlockOrientedSlab.BlockOrientedSlabSlab("half_tiles_check"),
                new BlockOrientedSlabDoubleDecorated("decorated_tiles_check")}) {
            check(tiles.getStateDefinition().getProperty("variant") == null, "obsolete single-valued property removed");
            check(tiles.acceptsLegacyProperty("variant", "default"), "old tile building plans supported");
            for (var state : tiles.getStateDefinition().getPossibleStates()) {
                check(tiles.func_176203_a(tiles.func_176201_c(state)) == state, "oriented tile state roundtrip");
                check(LegacyItemStacks.metadata(state) == 0, "tile direction does not change material cost");
                var rotated = state;
                for (int turn = 0; turn < 4; turn++) rotated = tiles.rotate(rotated, net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
                check(rotated == state, "tile rotation roundtrip");
                var shape = tiles.getShape(state, null, BlockPos.ZERO, CollisionContext.empty());
                double expectedHeight = tiles.func_176552_j() ? 1 : 0.5;
                check(shape.max(Direction.Axis.Y) - shape.min(Direction.Axis.Y) == expectedHeight, "tile collision height");
            }
        }
        var wall = new BlockMillWall("wall_check", Blocks.STONE);
        net.minecraft.world.level.BlockGetter northSouth = new net.minecraft.world.level.BlockGetter() {
            public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) { return null; }
            public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos pos) {
                return pos.equals(BlockPos.ZERO.north()) || pos.equals(BlockPos.ZERO.south()) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
            }
            public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) { return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState(); }
            public int getHeight() { return 384; }
            public int getMinBuildHeight() { return -64; }
        };
        var connected = wall.func_176221_a(wall.defaultBlockState(), northSouth, BlockPos.ZERO);
        check(connected.getValue(BlockMillWall.NORTH) && connected.getValue(BlockMillWall.SOUTH)
            && !connected.getValue(BlockMillWall.EAST) && !connected.getValue(BlockMillWall.WEST), "wall attaches to solid neighbors");
        check(!connected.getValue(BlockMillWall.UP), "straight wall omits post");
        check(wall.getShape(connected, northSouth, BlockPos.ZERO, CollisionContext.empty()).max(Direction.Axis.Y) == 0.875, "straight wall outline");
        check(wall.getCollisionShape(connected, northSouth, BlockPos.ZERO, CollisionContext.empty()).max(Direction.Axis.Y) == 1.5, "wall prevents jumping");
        var stairs = new BlockMillStairs("stairs_check", Blocks.STONE.defaultBlockState());
        check(stairs.defaultBlockState().hasProperty(net.minecraft.world.level.block.StairBlock.FACING), "stairs retain directional placement");
        var pane = new BlockMillPane("pane_check", net.minecraft.world.level.material.Material.WOOD, net.minecraft.world.level.block.SoundType.WOOD);
        check(pane.attachesTo(wall.defaultBlockState(), false), "pane attaches to custom wall");
        var paneConnected = pane.func_176221_a(pane.defaultBlockState(), northSouth, BlockPos.ZERO);
        check(paneConnected.getValue(net.minecraft.world.level.block.IronBarsBlock.NORTH)
            && paneConnected.getValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH)
            && !paneConnected.getValue(net.minecraft.world.level.block.IronBarsBlock.EAST), "pane connections follow neighbors");
        var rosette = new BlockRosette("rosette_check", net.minecraft.world.level.material.Material.GLASS, net.minecraft.world.level.block.SoundType.GLASS);
        var patterned = rosette.defaultBlockState().setValue(BlockRosette.ROSETTE_NORTH, true).setValue(BlockRosette.ROSETTE_UP, true);
        var turned = rosette.rotate(patterned, net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        check(turned.getValue(BlockRosette.ROSETTE_EAST) && turned.getValue(BlockRosette.ROSETTE_UP) && !turned.getValue(BlockRosette.ROSETTE_NORTH), "rosette pattern rotates with block");
        var rosetteBars = new BlockRosetteBars("bars_rosette_check", net.minecraft.world.level.material.Material.WOOD, net.minecraft.world.level.block.SoundType.WOOD);
        for (int meta = 0; meta < 8; meta++) check(rosetteBars.func_176201_c(rosetteBars.func_176203_a(meta)) == meta, "rosette quadrant and facing roundtrip");
        var carpet = new BlockMillCarpet("carpet_check");
        check(carpet.getShape(carpet.defaultBlockState(), northSouth, BlockPos.ZERO, CollisionContext.empty()).max(Direction.Axis.Y) == 1.0 / 16, "carpet height");
        for (Block production : new Block[] {new BlockSilkWorm("silk_check"), new BlockSnailSoil("snail_check")}) {
            var codec = (LegacyVariantBlock)production;
            for (int meta = 0; meta < 4; meta++) {
                var state = codec.func_176203_a(meta);
                check(codec.func_176201_c(state) == meta, "production maturity roundtrip");
                check(codec.itemMetadata(state) == 0, "production progress does not change material cost");
            }
        }
        System.out.println("Variant, geometry, connection, rosette and production state checks passed");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
