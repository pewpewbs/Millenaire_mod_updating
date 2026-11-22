package net.minecraftforge.registries;import org.millenaire.common.utilities.*;import org.millenaire.common.block.*;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.*;import net.minecraft.core.*;
public class CustomMarkerCheck{
 enum TypeRes{CHEST,CRAFT,SIGN,FIELD,SPAWN,SAPLING,STALL,MINING,FURNACE,FIRE_PIT,MUDBRICK,SUGAR,FISHING,SILK,SQUID,CACAO}
    private TypeRes identifyRes(Level world, Point p) {
        Block b = p.getBlock(world);
        if (b.equals(Blocks.CHEST) || b.equals((Object)MillBlocks.LOCKED_CHEST)) {
            return TypeRes.CHEST;
        }
        if (b.equals(Blocks.CRAFTING_TABLE)) {
            return TypeRes.CRAFT;
        }
        if (b.equals(Blocks.OAK_WALL_SIGN) || b.equals((Object)MillBlocks.PANEL)) {
            return TypeRes.SIGN;
        }
        if (b.equals(Blocks.FARMLAND)) {
            return TypeRes.FIELD;
        }
        if (b.equals(Blocks.HAY_BLOCK)) {
            return TypeRes.SPAWN;
        }
        if (LegacyBlockStates.matchesName(b, "minecraft:sapling") || (LegacyBlockStates.matchesName(b, "minecraft:log") || LegacyBlockStates.matchesName(b, "minecraft:log2")) && p.getBelow().getBlock(world).equals(Blocks.DIRT)) {
            return TypeRes.SAPLING;
        }
        if (b.equals(Blocks.YELLOW_WOOL)) {
            return TypeRes.STALL;
        }
        if ((LegacyBlockStates.matchesName(b, "minecraft:stone") || LegacyBlockStates.matchesName(b, "minecraft:sandstone") || LegacyBlockStates.matchesName(b, "minecraft:sand") || b.equals(Blocks.GRAVEL) || b.equals(Blocks.CLAY)) && (p.getAbove().getBlock(world).equals(Blocks.AIR) || p.getRelative(1.0, 0.0, 0.0).getBlock(world).equals(Blocks.AIR) || p.getRelative(-1.0, 0.0, 0.0).getBlock(world).equals(Blocks.AIR) || p.getRelative(0.0, 0.0, 1.0).getBlock(world).equals(Blocks.AIR) || p.getRelative(0.0, 0.0, -1.0).getBlock(world).equals(Blocks.AIR))) {
            return TypeRes.MINING;
        }
        if (b.equals(Blocks.FURNACE)) {
            return TypeRes.FURNACE;
        }
        if (b.equals((Object)MillBlocks.FIRE_PIT)) {
            return TypeRes.FIRE_PIT;
        }
        if (b.equals(MillBlocks.WET_BRICK) && p.getBlockActualState(world).getValue(BlockWetBrick.PROGRESS) == BlockWetBrick.EnumType.WETBRICK0) {
            return TypeRes.MUDBRICK;
        }
        if (b.equals(Blocks.SUGAR_CANE) && !p.getBelow().getBlock(world).equals(Blocks.SUGAR_CANE)) {
            return TypeRes.SUGAR;
        }
        if (b.equals(Blocks.LIGHT_BLUE_WOOL)) {
            return TypeRes.FISHING;
        }
        if (b.equals(Blocks.WHITE_WOOL)) {
            return TypeRes.SILK;
        }
        if (b.equals(Blocks.BLUE_WOOL)) {
            Point[] neighbours = new Point[]{p.getRelative(1.0, 0.0, 0.0), p.getRelative(-1.0, 0.0, 0.0), p.getRelative(0.0, 0.0, 1.0), p.getRelative(0.0, 0.0, -1.0)};
            boolean waterAround = true;
            for (Point p2 : neighbours) {
                if (p2.getBlock(world).equals(Blocks.WATER)) continue;
                waterAround = false;
            }
            if (waterAround) {
                return TypeRes.SQUID;
            }
        }
        if (b.equals(Blocks.COCOA)) {
            return TypeRes.CACAO;
        }
        return null;
    }


 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();((ForgeRegistry<?>)ForgeRegistries.BLOCKS).unfreeze();((NamespacedDefaultedWrapper<?>)Registry.BLOCK).unfreeze();var checker=new CustomMarkerCheck();var p=new Point(-5,-40,9);
  for(var wood:WoodVariant.values()){
   Point.cells.clear();Point.cells.put(p.pos,wood.sapling().defaultBlockState());check(checker.identifyRes(null,p)==TypeRes.SAPLING,"Sapling "+wood);
   Point.cells.put(p.pos,wood.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));check(checker.identifyRes(null,p)!=TypeRes.SAPLING,"Log requires dirt");Point.cells.put(p.pos.below(),Blocks.DIRT.defaultBlockState());check(checker.identifyRes(null,p)==TypeRes.SAPLING,"Log on dirt "+wood);
   Point.cells.put(p.pos,Registry.BLOCK.get(new net.minecraft.resources.ResourceLocation(wood.getSerializedName()+"_wood")).defaultBlockState());check(checker.identifyRes(null,p)==TypeRes.SAPLING,"Bark log on dirt "+wood);Point.cells.put(p.pos,wood.planks().defaultBlockState());check(checker.identifyRes(null,p)!=TypeRes.SAPLING,"Planks excluded");
  }
  Block[] wool={Blocks.YELLOW_WOOL,Blocks.LIGHT_BLUE_WOOL,Blocks.WHITE_WOOL};TypeRes[] types={TypeRes.STALL,TypeRes.FISHING,TypeRes.SILK};for(int i=0;i<wool.length;i++){Point.cells.clear();Point.cells.put(p.pos,wool[i].defaultBlockState());check(checker.identifyRes(null,p)==types[i],"Wool marker "+types[i]);}
  Point.cells.clear();Point.cells.put(p.pos,Blocks.BLUE_WOOL.defaultBlockState());check(checker.identifyRes(null,p)==null,"Squid requires four water neighbors");for(var d:Direction.Plane.HORIZONTAL)Point.cells.put(p.pos.relative(d),Blocks.WATER.defaultBlockState());check(checker.identifyRes(null,p)==TypeRes.SQUID,"Squid pool marker");
  for(Block block:new Block[]{Blocks.STONE,Blocks.GRANITE,Blocks.POLISHED_ANDESITE,Blocks.SAND,Blocks.RED_SAND,Blocks.CHISELED_SANDSTONE,Blocks.GRAVEL,Blocks.CLAY}){Point.cells.clear();Point.cells.put(p.pos,block.defaultBlockState());check(checker.identifyRes(null,p)==TypeRes.MINING,"Flattened quarry material "+block);for(var d:Direction.values())Point.cells.put(p.pos.relative(d),Blocks.DIRT.defaultBlockState());check(checker.identifyRes(null,p)==null,"Buried material is not a source");}
  for(var stage:BlockWetBrick.EnumType.values()){Point.cells.clear();Point.cells.put(p.pos,MillBlocks.WET_BRICK.defaultBlockState().setValue(BlockWetBrick.PROGRESS,stage));check(checker.identifyRes(null,p)==(stage==BlockWetBrick.EnumType.WETBRICK0?TypeRes.MUDBRICK:null),"Brick maturity "+stage);}
  System.out.println("PASS: extracted production identifyRes method: six sapling/log/bark species, dirt requirement, plank exclusion, wool markers, squid water enclosure, flattened quarry variants/exposure, wet-brick stages, negative coordinates. Point/world/custom blocks are fixtures; complete building registration untested.");
 }
}