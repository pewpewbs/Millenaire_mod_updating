package org.millenaire.common.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.block.MillBlocks;

/** Millenaire food keeps its original 64-durability-per-serving convention. */
public class ItemFoodMultiple extends Item {
    public static final int DAMAGE_PER_PORTION = 64;
    public static final int FULL_STACK_LIMIT = 64;
    public static final int MULTI_SERVING_STACK_LIMIT = 16;
    private final int healthAmount;
    private final int regenerationDuration;
    private final int drunkDuration;
    private final int nutrition;
    private final float saturation;
    private final boolean drink;
    private final boolean alwaysEdible;
    private MobEffectInstance potionId;
    private float effectProbability;
    private boolean clearEffects;

    public ItemFoodMultiple(String name, int health, int regeneration, int nutrition,
                            float saturation, boolean drink, int drunkenness,
                            int durability, int stackSize, boolean alwaysEdible) {
        super(properties(nutrition, saturation, durability, stackSize));
        this.healthAmount = health;
        this.regenerationDuration = regeneration;
        this.drunkDuration = drunkenness;
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.drink = drink;
        this.alwaysEdible = alwaysEdible || health > 0;
        setRegistryName("millenaire", name);
    }

    private static Properties properties(int nutrition, float saturation, int durability, int stackSize) {
        Properties properties = new Properties().tab(MillBlocks.tabMillenaire)
            .stacksTo(stackSize)
            .food(new FoodProperties.Builder().nutrition(nutrition).saturationMod(saturation).build());
        if (durability > 0) properties.durability(durability);
        return properties;
    }

    /** Forge 1.18.2 stack-sensitive limit. Existing saved counts are not
     * rewritten or clamped; only subsequent insertion/merging uses this limit. */
    @Override
    public int getItemStackLimit(ItemStack stack) {
        if (stack.getDamageValue() > 0) return 1;
        return drink || stack.getMaxDamage() > DAMAGE_PER_PORTION
                ? MULTI_SERVING_STACK_LIMIT : FULL_STACK_LIMIT;
    }

    @Override
    public FoodProperties getFoodProperties() {
        FoodProperties.Builder builder = new FoodProperties.Builder()
            .nutrition(nutrition).saturationMod(saturation);
        if (alwaysEdible || clearEffects || potionId != null) builder.alwaysEat();
        if (potionId != null) builder.effect(() -> new MobEffectInstance(potionId), effectProbability);
        return builder.build();
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
        return getFoodProperties();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.canEat(getFoodProperties().canAlwaysEat())) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) { return 32; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return drink ? UseAnim.DRINK : UseAnim.EAT; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || stack.isEmpty()) return stack;
        if (entity instanceof Player player) {
            if (clearEffects) player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
            player.getFoodData().eat(nutrition, saturation);
            player.heal(healthAmount);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F,
                level.random.nextFloat() * 0.1F + 0.9F);
            if (potionId != null && level.random.nextFloat() < effectProbability)
                player.addEffect(new MobEffectInstance(potionId));
            player.awardStat(Stats.ITEM_USED.get(this));
            if (drink) MillAdvancements.CHEERS.grant(player);
            if (regenerationDuration > 0)
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenerationDuration * 20, 0));
            if (drunkDuration > 0)
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, drunkDuration * 20, 0));
            if (player instanceof ServerPlayer serverPlayer)
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }
        // A serving changes only one item. Keep the opened food in hand, and
        // transfer the unopened remainder after the held stack is damaged so
        // Inventory.add cannot merge the remainder back into the held stack.
        int nextDamage = stack.getDamageValue() + DAMAGE_PER_PORTION;
        if (nextDamage < stack.getMaxDamage()) {
            ItemStack remainder = stack.getCount() > 1
                    ? stack.split(stack.getCount() - 1) : ItemStack.EMPTY;
            stack.setDamageValue(nextDamage);
            if (!remainder.isEmpty()) {
                if (entity instanceof Player player) {
                    player.getInventory().add(remainder);
                    // Never discard unopened food if there is no inventory room.
                    if (!remainder.isEmpty()) player.drop(remainder, false);
                } else {
                    entity.spawnAtLocation(remainder);
                }
            }
        } else {
            // Single-portion foods and the last serving of an opened item.
            stack.shrink(1);
        }
        return stack;
    }

    public ItemFoodMultiple setClearEffects(boolean value) { clearEffects = value; return this; }
    public ItemFoodMultiple withEffect(MobEffectInstance effect, float probability) {
        potionId = new MobEffectInstance(effect);
        effectProbability = probability;
        return this;
    }
    public int getDrunkDuration() { return drunkDuration; }
    public int getHealthAmount() { return healthAmount; }
    public int getRegenerationDuration() { return regenerationDuration; }
    public MobEffectInstance getPotionId() { return potionId; }
    public boolean isClearEffects() { return clearEffects; }
    public boolean isDrink() { return drink; }
}
