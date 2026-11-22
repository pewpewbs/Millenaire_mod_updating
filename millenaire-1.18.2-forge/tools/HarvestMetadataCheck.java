import net.minecraft.world.level.Level;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.resources.ResourceLocation;import net.minecraft.core.Direction;import net.minecraft.world.item.*;import net.minecraft.world.item.alchemy.*;import net.minecraft.world.level.block.state.properties.*;import org.millenaire.common.utilities.LegacyWorldMetadata;import org.millenaire.common.item.LegacyItemStacks;
public class HarvestMetadataCheck {
 static class Point {BlockState state;Point(BlockState s){state=s;}Point getAbove(){return this;}BlockState getBlockActualState(Level w){return state;}}
 BlockState harvestBlockState;ResourceLocation cropType;static int getCropBlockRipeMeta(ResourceLocation id){return 7;}
    private boolean isValidHarvestSoil(Level world, Point p) {
        if (p == null) return false;
        BlockState state = p.getAbove().getBlockActualState(world);
        if (this.harvestBlockState != null) return state.equals(this.harvestBlockState);
        if (this.cropType == null) return false;
        Block crop = net.minecraft.core.Registry.BLOCK.getOptional(this.cropType).orElse(null);
        if (crop == null || state.getBlock() != crop) return false;
        if (crop instanceof net.minecraft.world.level.block.CropBlock plant) return plant.isMaxAge(state);
        return crop instanceof org.millenaire.common.block.LegacyVariantBlock legacy
            && legacy.func_176201_c(state) == getCropBlockRipeMeta(this.cropType);
    }


 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var test=new HarvestMetadataCheck();int checked=0;
 for(Block crop:new Block[]{Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES,Blocks.BEETROOTS}){test.cropType=net.minecraft.core.Registry.BLOCK.getKey(crop);CropBlock c=(CropBlock)crop;for(int age=0;age<=c.getMaxAge();age++){var state=c.getStateForAge(age);check(test.isValidHarvestSoil(null,new Point(state))==(age==c.getMaxAge()),"Maturity "+state);check(LegacyWorldMetadata.read(state)==age,"Crop age codec "+state);checked++;}}
 test.cropType=new ResourceLocation("missing:crop");check(!test.isValidHarvestSoil(null,new Point(Blocks.WHEAT.defaultBlockState())),"Unknown crop rejected");test.cropType=null;check(!test.isValidHarvestSoil(null,new Point(Blocks.WHEAT.defaultBlockState())),"Unset crop rejected");test.harvestBlockState=Blocks.WHEAT.defaultBlockState();check(test.isValidHarvestSoil(null,new Point(test.harvestBlockState)),"Explicit state override");check(!test.isValidHarvestSoil(null,null),"Missing soil");
 for(Direction d:Direction.Plane.HORIZONTAL){var sign=Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,d);check(LegacyWorldMetadata.write(Blocks.OAK_WALL_SIGN,LegacyWorldMetadata.read(sign)).equals(sign),"Sign orientation");var chest=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,d);check(LegacyWorldMetadata.read(chest)==d.get3DDataValue(),"Chest facing");for(BedPart part:BedPart.values())for(boolean occupied:new boolean[]{false,true}){var bed=Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,d).setValue(BedBlock.PART,part).setValue(BedBlock.OCCUPIED,occupied);check(LegacyWorldMetadata.read(bed)==(d.get2DDataValue()|(part==BedPart.HEAD?8:0)|(occupied?4:0)),"Bed metadata");}}
 for(int age=0;age<=3;age++)check(LegacyWorldMetadata.read(Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE,age))==age,"Wart age");
 var water=LegacyItemStacks.stack(Items.POTION,1,0);var awkward=LegacyItemStacks.stack(Items.POTION,1,16);check(PotionUtils.getPotion(water)==Potions.WATER&&PotionUtils.getPotion(awkward)==Potions.AWKWARD,"Legacy potion IDs");check(LegacyItemStacks.metadata(awkward)==16,"Awkward stock identity");check(!LegacyItemStacks.matches(awkward,Items.POTION,0)&&LegacyItemStacks.matches(awkward,Items.POTION,16)&&LegacyItemStacks.matches(awkward,Items.POTION,-1),"Potion stock matching");
 boolean rejected=false;try{LegacyWorldMetadata.read(Blocks.SCULK_SENSOR.defaultBlockState());}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Unknown old metadata not silently zeroed");
 System.out.println("PASS: extracted harvest predicate for "+checked+" vanilla crop ages, explicit/unknown/missing targets; actual metadata codec for signs/chests/beds/warts and actual potion stock conversion/matching. No live harvest or world write test.");}}
