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
        if (entity instanceof Player player && !level.isClientSide) {
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
        // Do not invoke Item.finishUsingItem: it would consume the entire stack item
        // rather than a single portion of the original multi-use food.
        if (stack.getDamageValue() + DAMAGE_PER_PORTION < stack.getMaxDamage()) {
            stack.setDamageValue(stack.getDamageValue() + DAMAGE_PER_PORTION);
        } else {
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
