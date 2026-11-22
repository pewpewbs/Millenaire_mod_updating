package org.millenaire.common.network;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

/** An owned snapshot: enqueueing a packet must not retain a Netty event buffer. */
public final class MillPacketPayload {
    public static final int MAX_CLIENTBOUND=1048570, MAX_SERVERBOUND=32760;
    private final byte[] bytes;
    private MillPacketPayload(byte[] bytes) { this.bytes=bytes; }
    public static MillPacketPayload copy(FriendlyByteBuf source,int max) {
        int length=source.readableBytes();validate(length,max);
        byte[] bytes=new byte[length];source.getBytes(source.readerIndex(),bytes);return new MillPacketPayload(bytes);
    }
    public static MillPacketPayload decode(FriendlyByteBuf source,int max) {
        int length=source.readVarInt();validate(length,max);
        if(length>source.readableBytes())throw new DecoderException("Truncated Millenaire packet");
        byte[] bytes=new byte[length];source.readBytes(bytes);
        if(source.isReadable())throw new DecoderException("Trailing Millenaire packet data");
        return new MillPacketPayload(bytes);
    }
    private static void validate(int length,int max) {
        if(length<4 || length>max)throw new DecoderException("Invalid Millenaire packet length: "+length);
    }
    public void encode(FriendlyByteBuf target) { target.writeByteArray(bytes); }
    public FriendlyByteBuf open() { return new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)); }
}
