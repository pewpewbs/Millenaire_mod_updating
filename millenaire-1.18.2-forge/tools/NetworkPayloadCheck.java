package org.millenaire.common.network;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
public class NetworkPayloadCheck {
    static FriendlyByteBuf buffer(){return new FriendlyByteBuf(Unpooled.buffer());}
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static void reject(Runnable work,String reason){try{work.run();throw new AssertionError(reason);}catch(DecoderException expected){}}
    public static void main(String[] args){
        FriendlyByteBuf source=buffer();source.writeByte(9);source.writeInt(205);source.writeUtf("culture:українська");source.readByte();
        int reader=source.readerIndex();var snapshot=MillPacketPayload.copy(source,MillPacketPayload.MAX_SERVERBOUND);
        check(source.readerIndex()==reader,"copy must preserve source cursor");source.setInt(reader,999);source.release();
        FriendlyByteBuf encoded=buffer();snapshot.encode(encoded);var decoded=MillPacketPayload.decode(encoded,MillPacketPayload.MAX_SERVERBOUND);encoded.release();
        FriendlyByteBuf payload=decoded.open();check(payload.readInt()==205 && payload.readUtf().equals("culture:українська") && !payload.isReadable(),"payload survives original buffer release and modification");payload.release();
        for(int size:new int[]{4,32760,1048570}){
            var data=buffer();data.writeZero(size);int max=size>32760?MillPacketPayload.MAX_CLIENTBOUND:MillPacketPayload.MAX_SERVERBOUND;
            var copy=MillPacketPayload.copy(data,max);var wire=buffer();copy.encode(wire);var result=MillPacketPayload.decode(wire,max).open();
            check(result.readableBytes()==size,"boundary packet preserved");data.release();wire.release();result.release();
        }
        FriendlyByteBuf shortPacket=buffer();shortPacket.writeVarInt(3);shortPacket.writeZero(3);reject(()->MillPacketPayload.decode(shortPacket,32760),"missing packet id");shortPacket.release();
        FriendlyByteBuf large=buffer();large.writeVarInt(32761);reject(()->MillPacketPayload.decode(large,32760),"oversized packet before allocation");large.release();
        FriendlyByteBuf truncated=buffer();truncated.writeVarInt(100);truncated.writeZero(4);reject(()->MillPacketPayload.decode(truncated,32760),"truncated payload");truncated.release();
        FriendlyByteBuf trailing=buffer();trailing.writeVarInt(4);trailing.writeZero(5);reject(()->MillPacketPayload.decode(trailing,32760),"trailing payload");trailing.release();
        System.out.println("PASS: owned network payload roundtrip, UTF-8, buffer lifetime, cursor preservation and size/truncation validation");
    }
}
