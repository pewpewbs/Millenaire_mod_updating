package org.millenaire.common.village;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.culture.VillagerType;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.InvItem;

/** Shared read-only production capability checks for the planner and map. */
public final class ProductionCapability {
    private ProductionCapability() {}
    private static final Map<BuildingPlan,Set<ResourceLocation>> SOILS=new WeakHashMap<>();

    private static ResourceLocation cropMarker(String marker) {
        if(marker==null)return null;
        return switch(marker.toLowerCase(Locale.ROOT)){
            case "soil" -> Mill.CROP_WHEAT;
            case "ricesoil" -> Mill.CROP_RICE;
            case "turmericsoil" -> Mill.CROP_TURMERIC;
            case "maizesoil" -> Mill.CROP_MAIZE;
            case "carrotsoil" -> Mill.CROP_CARROT;
            case "potatosoil" -> Mill.CROP_POTATO;
            case "flowersoil" -> Mill.CROP_FLOWER;
            case "vinesoil" -> Mill.CROP_VINE;
            case "cacaospot" -> Mill.CROP_CACAO;
            case "cottonsoil" -> Mill.CROP_COTTON;
            default -> null;
        };
    }

    private static synchronized Set<ResourceLocation> planCrops(BuildingPlan plan){
        return SOILS.computeIfAbsent(plan,key->{
            Set<ResourceLocation> found=new HashSet<>();
            for(BuildingPlan p=key;p!=null;){
                if(p.plan!=null)for(var floor:p.plan)if(floor!=null)
                    for(var row:floor)if(row!=null)for(var point:row)if(point!=null){
                        ResourceLocation crop=cropMarker(point.getSpecialType());
                        if(crop!=null)found.add(crop);
                    }
                if(p.level<=0)break;
                BuildingPlan previous=p.getPreviousBuildingPlan();
                if(previous==null||previous.level>=p.level)break;
                p=previous;
            }
            return Set.copyOf(found);
        });
    }

    public static boolean hasCrop(BuildingPlan plan,Building existing,ResourceLocation crop){
        if(crop==null)return false;
        if(existing!=null){
            var soils=existing.getResManager().getSoilPoints(crop);
            if(soils!=null&&!soils.isEmpty())return true;
            // Server has authoritative registered soil positions; client
            // building packets do not carry soils, so fall back to the plan.
            if(!existing.world.isClientSide)return false;
        }
        return plan!=null&&planCrops(plan).contains(crop);
    }

    /** Ordinary potential vanilla drops a configured worker actually collects.
     * Not a loot-table simulation: custom loot, fire, random and pickup vary. */
    public static List<InvItem> animalGoods(ResourceLocation animal,VillagerType worker){
        if(animal==null||worker==null||!animal.getNamespace().equals("minecraft"))return List.of();
        List<Item> possible=switch(animal.getPath()){
            case "chicken" -> List.of(Items.CHICKEN,Items.FEATHER,Items.EGG);
            case "cow","mooshroom" -> List.of(Items.BEEF,Items.LEATHER);
            case "pig" -> List.of(Items.PORKCHOP);
            case "sheep" -> List.of(Items.MUTTON);
            case "rabbit" -> List.of(Items.RABBIT,Items.RABBIT_HIDE,Items.RABBIT_FOOT);
            case "squid","glow_squid" -> List.of(animal.getPath().equals("squid")?Items.INK_SAC:Items.GLOW_INK_SAC);
            case "horse","donkey","mule","llama" -> List.of(Items.LEATHER);
            default -> List.of();
        };
        List<InvItem> result=new ArrayList<>();
        for(Item item:possible){
            InvItem good=InvItem.createInvItem(item);
            if(worker.collectGoods.stream().anyMatch(g->g.matches(good))
                    &&worker.bringBackHomeGoods.stream().anyMatch(g->g.matches(good)))result.add(good);
        }
        if(animal.getPath().equals("sheep"))for(var color:net.minecraft.world.item.DyeColor.values()){
            Item item=net.minecraft.core.Registry.ITEM.get(new ResourceLocation("minecraft",color.getName()+"_wool"));
            if(item==Items.AIR)continue;
            InvItem good=InvItem.createInvItem(item);
            if(worker.collectGoods.stream().anyMatch(g->g.matches(good))
                    &&worker.bringBackHomeGoods.stream().anyMatch(g->g.matches(good)))result.add(good);
        }
        return result;
    }
}
