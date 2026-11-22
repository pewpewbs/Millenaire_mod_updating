package org.millenaire.common.item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.*;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
public class ItemParchment extends ItemMill {
    private static final String NBT_VILLAGE_POS = "village_pos_";
    public static final int NORMAN_VILLAGERS = 1;
    public static final int NORMAN_BUILDINGS = 2;
    public static final int NORMAN_ITEMS = 3;
    public static final int VILLAGE_BOOK = 4;
    public static final int INDIAN_VILLAGERS = 5;
    public static final int INDIAN_BUILDINGS = 6;
    public static final int INDIAN_ITEMS = 7;
    public static final int MAYAN_VILLAGERS = 9;
    public static final int MAYAN_BUILDINGS = 10;
    public static final int MAYAN_ITEMS = 11;
    public static final int JAPANESE_VILLAGERS = 16;
    public static final int JAPANESE_BUILDINGS = 17;
    public static final int JAPANESE_ITEMS = 18;
    public static final int SADHU = 15;
    private final int[] textsId;
    private final boolean obsolete;
    public static ItemStack createParchmentForVillage(Building townHall) {
        ItemStack parchment=new ItemStack(MillItems.PARCHMENT_VILLAGE_SCROLL);
        townHall.getPos().write(parchment.getOrCreateTag(),NBT_VILLAGE_POS);return parchment;
    }
    public ItemParchment(String name,int text,boolean obsolete) { this(name,new int[]{text},obsolete); }
    public ItemParchment(String name,int[] texts,boolean obsolete) {
        super(name,new Item.Properties().stacksTo(1));
        if(texts.length==0)throw new IllegalArgumentException("Parchment requires at least one text");
        textsId=texts.clone();this.obsolete=obsolete;
    }
    @Override public void fillItemCategory(CreativeModeTab tab,NonNullList<ItemStack> items) {
        if(!obsolete)super.fillItemCategory(tab,items);
    }
    private void displayVillageBook(Player player,ItemStack stack) {
        if(player.level.isClientSide)return;
        Point pos=stack.hasTag()?Point.read(stack.getTag(),NBT_VILLAGE_POS):null;
        var data=Mill.getMillWorld(player.level);
        Building townHall=pos==null || data==null?null:data.getBuilding(pos);
        if(townHall==null) { ServerSender.sendTranslatedSentence(player,'6',"panels.invalidid");return; }
        if(!player.level.hasChunk(pos.getChunkX(),pos.getChunkZ()) || !townHall.isActive) {
            ServerSender.sendTranslatedSentence(player,'6',"panels.toofar");return;
        }
        ServerSender.displayVillageBookGUI(player,pos);
    }
    @Override public Component getName(ItemStack stack) {
        Component base=super.getName(stack);
        if(textsId[0]!=VILLAGE_BOOK || !stack.hasTag() || Mill.clientWorld==null)return base;
        Point pos=Point.read(stack.getTag(),NBT_VILLAGE_POS);
        Building townHall=pos==null?null:Mill.clientWorld.getBuilding(pos);
        return townHall==null?base:base.copy().append(": "+townHall.getVillageQualifiedName());
    }
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(textsId[0]==VILLAGE_BOOK) {
            if(!world.isClientSide)displayVillageBook(player,stack);
        } else if(world.isClientSide) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->org.millenaire.client.book.ParchmentClient.open(player,textsId));
        }
        return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
    }
}
