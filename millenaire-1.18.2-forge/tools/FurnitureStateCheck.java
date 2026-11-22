package net.minecraftforge.registries;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.millenaire.common.block.BlockMillBed;
import org.millenaire.common.entity.*;

public class FurnitureStateCheck {
    public static void main(String[] args) throws Exception {
        BannerStateCheck.main(args);
        var bed = new BlockMillBed("bed_state_check",4); ForgeRegistries.BLOCKS.register(bed);
        var item = new BlockItem(bed,new Item.Properties()); item.setRegistryName("millenaire","bed_state_check"); ForgeRegistries.ITEMS.register(item); item.registerBlocks(Item.BY_BLOCK,item);
        MillBlockEntities.MILL_BED=BlockEntityType.Builder.of(TileEntityMillBed::new,bed).build(null);
        for (int meta=0;meta<16;meta++) {
            var state=bed.func_176203_a(meta);
            check(bed.func_176201_c(state)==meta,"legacy bed orientation, part and occupancy survive codec");
            check(bed.itemMetadata(state)==0,"bed occupancy does not change construction material identity");
            check(bed.getShape(state,net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO,CollisionContext.empty()).max(Direction.Axis.Y)==0.25,"custom bed height retained");
            check(bed.getDrops(state,null).size()==(state.getValue(BedBlock.PART)==BedPart.HEAD?1:0),"only head drops an item, matching vanilla paired destruction");
        }
        var entity=new TileEntityMillBed(BlockPos.ZERO,bed.defaultBlockState());
        check(entity.getType()==MillBlockEntities.MILL_BED,"custom bed type retained");
        System.out.println("Bed state, height, construction identity and single-item drop checks passed (live placement and sleep require Forge launch)");
    }
    private static void check(boolean ok,String reason) { if (!ok) throw new AssertionError(reason); }
}
