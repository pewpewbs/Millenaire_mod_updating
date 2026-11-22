package org.millenaire.common.entity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** The original quest mob is a sword-wielding Skeleton, despite its registry name. */
public class EntityTargetedWitherSkeleton extends Skeleton {
    public EntityTargetedWitherSkeleton(Level world) { this(MillQuestEntities.WITHER_SKELETON,world); }
    public EntityTargetedWitherSkeleton(EntityType<? extends Skeleton> type,Level world) {
        super(type,world);setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new FloatGoal(this));
        goalSelector.addGoal(5,new RandomStrollGoal(this,1));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        goalSelector.addGoal(4,new MeleeAttackGoal(this,.31,false));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,10,true,false,null));
    }
    // The quest always uses the original melee goal, including after equipment/NBT updates.
    @Override public void reassessWeaponGoal() {}
    @Override public void aiStep() { super.aiStep();clearFire(); }
    @Override public net.minecraft.network.protocol.Packet<?> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }
}
