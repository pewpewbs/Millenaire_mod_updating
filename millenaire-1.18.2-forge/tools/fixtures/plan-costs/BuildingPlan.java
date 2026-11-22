package org.millenaire.common.buildingplan;import java.util.*;import net.minecraft.world.item.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import org.millenaire.common.item.*;import org.millenaire.common.block.*;import org.millenaire.common.utilities.*;public class BuildingPlan{public int nbfloors=1,length=1,width;public PointType[][][] plan;public HashMap<InvItem,Integer> resCost;public void addToCost(Block block, int nb) {
      this.addToCost(block, 0, nb);
   }public void addToCost(Block block, int meta, int nb) {
      try {
         InvItem key = InvItem.createInvItem(block, meta);
         if (this.resCost.containsKey(key)) {
            nb += this.resCost.get(key);
            this.resCost.put(key, nb);
         } else {
            this.resCost.put(key, nb);
         }
      } catch (Exception var5) {
         MillLog.printException("Exception when calculating cost of: " + this, var5);
      }
   }public void addToCost(BlockState blockState, int nb) {
      this.addToCost(InvItem.createInvItem(blockState), nb);
   }public void addToCost(InvItem invitem, int nb) {
      try {
         if (this.resCost.containsKey(invitem)) {
            nb += this.resCost.get(invitem);
            this.resCost.put(invitem, nb);
         } else {
            this.resCost.put(invitem, nb);
         }
      } catch (Exception var4) {
         MillLog.printException("Exception when calculating cost of: " + this, var4);
      }
   }public void addToCost(Item item, int nb) {
      try {
         InvItem key = InvItem.createInvItem(item, 0);
         if (this.resCost.containsKey(key)) {
            nb += this.resCost.get(key);
            this.resCost.put(key, nb);
         } else {
            this.resCost.put(key, nb);
         }
      } catch (Exception var4) {
         MillLog.printException("Exception when calculating cost of: " + this, var4);
      }
   }}