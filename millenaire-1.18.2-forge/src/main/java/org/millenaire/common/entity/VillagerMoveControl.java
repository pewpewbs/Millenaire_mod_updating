package org.millenaire.common.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One movement owner per tick: ladder assistance or the normal ground controller. */
public final class VillagerMoveControl extends MoveControl {
    public VillagerMoveControl(Mob mob) { super(mob); }

    @Override public void tick() {
        if (this.mob.getNavigation() instanceof PathNavigateSimple navigation
                && navigation.isLadderMotionControlled()) {
            // A stale ground JUMPING operation otherwise survives until landing,
            // reasserting jump while the ladder controller requests descent.
            this.operation = Operation.WAIT;
            this.mob.setSpeed((float)(this.speedModifier
                    * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            this.mob.setXxa(0.0F);
            this.mob.setZza(0.0F);
            this.mob.setJumping(false);
            return;
        }
        super.tick();
    }
}
