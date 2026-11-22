package org.millenaire.common.ui.firepit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SlotFirePitOutput extends SlotItemHandler {
    private final Player player;
    private int removeCount;
    public SlotFirePitOutput(Player player, IItemHandler handler, int index, int x, int y) { super(handler, index, x, y); this.player = player; }
    @Override public ItemStack remove(int amount) { if (hasItem()) removeCount += Math.min(amount, getItem().getCount()); return super.remove(amount); }
    @Override public boolean mayPlace(ItemStack stack) { return false; }
    @Override protected void onQuickCraft(ItemStack stack, int amount) { removeCount += amount; checkTakeAchievements(stack); }
    @Override public void onTake(Player player, ItemStack stack) { checkTakeAchievements(stack); super.onTake(player, stack); }
    @Override protected void checkTakeAchievements(ItemStack stack) {
        if (removeCount <= 0) return;
        stack.onCraftedBy(player.level, player, removeCount);
        if (player.level instanceof ServerLevel level) {
            float experience = level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
                .filter(recipe -> ItemStack.isSameItemSameTags(recipe.getResultItem(), stack)).findFirst().map(recipe -> recipe.getExperience()).orElse(0F);
            float total = removeCount * experience; int count = (int)total;
            if (level.random.nextFloat() < total - count) count++;
            if (count > 0) ExperienceOrb.award(level, player.position(), count);
            ForgeEventFactory.firePlayerSmeltedEvent(player, stack);
        }
        removeCount = 0;
    }
}
