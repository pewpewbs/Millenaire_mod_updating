package org.millenaire.common.buildingplan;import java.util.*;import net.minecraft.world.item.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import org.millenaire.common.item.*;import org.millenaire.common.block.*;import org.millenaire.common.utilities.*;public class CostHarness{public static void computeCost(BuildingPlan buildingPlan) throws MillLog.MillenaireException {
        InvItem redSlabInvItem;
        InvItem greenSlabInvItem;
        InvItem graySlabInvItem;
        InvItem byzSlabInvItem;
        buildingPlan.resCost = new HashMap();
        for (int i = 0; i < buildingPlan.nbfloors; ++i) {
            for (int j = 0; j < buildingPlan.length; ++j) {
                for (int k = 0; k < buildingPlan.width; ++k) {
                    PointType p = buildingPlan.plan[i][j][k];
                    if (p.getCostQuantity() <= 0) continue;
                    if (p.getCostInvItem().getItem() == Items.AIR) {
                        MillLog.error(p, "cost in air!");
                    }
                    buildingPlan.addToCost(p.getCostInvItem(), p.getCostQuantity());
                }
            }
        }
        InvItem stickInvItem = InvItem.createInvItem(Items.STICK);
        if (buildingPlan.resCost.containsKey(stickInvItem)) {
            int stickQuantity = buildingPlan.resCost.get(stickInvItem);
            buildingPlan.resCost.remove(stickInvItem);
            buildingPlan.addToCost(Blocks.OAK_LOG, -1, (int)Math.max(Math.ceil((double)stickQuantity * 1.0 / 4.0), 1.0));
        }
        for (WoodVariant plankType : WoodVariant.values()) {
            InvItem plankInvItem = InvItem.createInvItem(plankType.planks().defaultBlockState());
            if (!buildingPlan.resCost.containsKey(plankInvItem)) continue;
            int plankQuantity = buildingPlan.resCost.get(plankInvItem);
            buildingPlan.addToCost(plankType.log().defaultBlockState(), (int)Math.max(Math.ceil((double)plankQuantity / 4.0), 1.0));
            buildingPlan.resCost.remove(plankInvItem);
        }
        InvItem glassPaneInvItem = InvItem.createInvItem(Blocks.GLASS_PANE);
        if (buildingPlan.resCost.containsKey(glassPaneInvItem)) {
            int paneQuantity = buildingPlan.resCost.get(glassPaneInvItem);
            buildingPlan.addToCost(Blocks.GLASS.defaultBlockState(), (int)Math.max(Math.ceil((double)paneQuantity * 6.0 / 16.0), 1.0));
            buildingPlan.resCost.remove(glassPaneInvItem);
        }
        if (buildingPlan.resCost.containsKey(byzSlabInvItem = InvItem.createInvItem((Block)MillBlocks.BYZANTINE_TILES_SLAB, 0))) {
            int slabQuantity = buildingPlan.resCost.get(byzSlabInvItem);
            buildingPlan.addToCost((Block)MillBlocks.BYZANTINE_TILES, 0, (int)Math.max(Math.ceil((double)slabQuantity / 2.0), 1.0));
            buildingPlan.resCost.remove(byzSlabInvItem);
        }
        if (buildingPlan.resCost.containsKey(graySlabInvItem = InvItem.createInvItem((Block)MillBlocks.GRAY_TILES_SLAB, 0))) {
            int slabQuantity = buildingPlan.resCost.get(graySlabInvItem);
            buildingPlan.addToCost((Block)MillBlocks.GRAY_TILES, 0, (int)Math.max(Math.ceil((double)slabQuantity / 2.0), 1.0));
            buildingPlan.resCost.remove(graySlabInvItem);
        }
        if (buildingPlan.resCost.containsKey(greenSlabInvItem = InvItem.createInvItem((Block)MillBlocks.GREEN_TILES_SLAB, 0))) {
            int slabQuantity = buildingPlan.resCost.get(greenSlabInvItem);
            buildingPlan.addToCost((Block)MillBlocks.GRAY_TILES, 0, (int)Math.max(Math.ceil((double)slabQuantity / 2.0), 1.0));
            buildingPlan.resCost.remove(greenSlabInvItem);
        }
        if (buildingPlan.resCost.containsKey(redSlabInvItem = InvItem.createInvItem((Block)MillBlocks.RED_TILES_SLAB, 0))) {
            int slabQuantity = buildingPlan.resCost.get(redSlabInvItem);
            buildingPlan.addToCost((Block)MillBlocks.GRAY_TILES, 0, (int)Math.max(Math.ceil((double)slabQuantity / 2.0), 1.0));
            buildingPlan.resCost.remove(redSlabInvItem);
        }
    }}