package org.millenaire.common.advancements;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.millenaire.common.network.ServerSender;

public class GenericAdvancement extends SimpleCriterionTrigger<AlwaysTrueCriterionInstance> {
    private final String key;
    private final ResourceLocation id;
    public GenericAdvancement(String key) { this.key=key;id=new ResourceLocation("millenaire",key); }
    @Override public ResourceLocation getId() { return id; }
    public String getKey() { return key; }
    @Override protected AlwaysTrueCriterionInstance createInstance(JsonObject json,EntityPredicate.Composite player,DeserializationContext context) {
        return new AlwaysTrueCriterionInstance(id,player);
    }
    public void grant(Player player) {
        if(player==null)return;
        if(player instanceof ServerPlayer server) {
            trigger(server,AlwaysTrueCriterionInstance::test);
            ServerSender.sendAdvancementEarned(server,key);
        }
        MillAdvancements.addToStats(player,key);
    }
}
