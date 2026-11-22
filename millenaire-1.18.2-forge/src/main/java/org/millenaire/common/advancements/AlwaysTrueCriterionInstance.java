package org.millenaire.common.advancements;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
public class AlwaysTrueCriterionInstance extends AbstractCriterionTriggerInstance {
    public AlwaysTrueCriterionInstance(ResourceLocation id) { this(id,EntityPredicate.Composite.ANY); }
    public AlwaysTrueCriterionInstance(ResourceLocation id,EntityPredicate.Composite player) { super(id,player); }
    public boolean test() { return true; }
}
