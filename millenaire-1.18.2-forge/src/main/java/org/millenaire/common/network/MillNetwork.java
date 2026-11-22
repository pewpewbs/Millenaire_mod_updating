package org.millenaire.common.network;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import org.millenaire.common.utilities.Point;

@Mod.EventBusSubscriber(modid="millenaire",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MillNetwork {
    private static final String PROTOCOL="1.18.2-2";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("millenaire","main"),()->PROTOCOL,PROTOCOL::equals,PROTOCOL::equals);
    private record ToClient(MillPacketPayload payload) {}
    private record ToServer(MillPacketPayload payload) {}
    private static boolean registered;
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) { register(); }
    public static synchronized void register() {
        if(registered)return;
        CHANNEL.registerMessage(0,ToClient.class,(message,buffer)->message.payload.encode(buffer),buffer->new ToClient(MillPacketPayload.decode(buffer,MillPacketPayload.MAX_CLIENTBOUND)),MillNetwork::handleClient,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1,ToServer.class,(message,buffer)->message.payload.encode(buffer),buffer->new ToServer(MillPacketPayload.decode(buffer,MillPacketPayload.MAX_SERVERBOUND)),MillNetwork::handleServer,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        registered=true;
    }
    private static void handleClient(ToClient message,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context=supplier.get();
        context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->org.millenaire.client.network.ClientReceiver.receive(message.payload)));
        context.setPacketHandled(true);
    }
    private static void handleServer(ToServer message,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context=supplier.get();ServerPlayer sender=context.getSender();
        if(sender!=null)context.enqueueWork(()->ServerReceiver.receive(sender,message.payload));
        context.setPacketHandled(true);
    }
    public static void sendToPlayer(FriendlyByteBuf buffer,ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ToClient(MillPacketPayload.copy(buffer,MillPacketPayload.MAX_CLIENTBOUND)));
    }
    public static void sendNear(FriendlyByteBuf buffer,Level level,Point pos,int range) {
        CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(pos.x,pos.y,pos.z,range,level.dimension())),new ToClient(MillPacketPayload.copy(buffer,MillPacketPayload.MAX_CLIENTBOUND)));
    }
    public static void sendToServer(FriendlyByteBuf buffer) {
        CHANNEL.sendToServer(new ToServer(MillPacketPayload.copy(buffer,MillPacketPayload.MAX_SERVERBOUND)));
    }
}
