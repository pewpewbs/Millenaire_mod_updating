package org.millenaire.common.utilities;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.world.level.ChunkPos;

/** Read-only saved-chunk overview. A region entry can be a partially generated chunk. */
public final class RegionChunkIndex {
    private RegionChunkIndex() {}
    public static Set<Long> read(Path regionFolder,int startX,int startZ,int size) throws IOException {
        if (size<=0 || size>256) throw new IllegalArgumentException("Invalid overview size: "+size);
        Set<Long> result=new HashSet<>();
        int endX=startX+size-1,endZ=startZ+size-1;
        for (int rx=startX>>5;rx<=endX>>5;rx++) for (int rz=startZ>>5;rz<=endZ>>5;rz++) {
            Path file=regionFolder.resolve("r."+rx+"."+rz+".mca");
            try (FileChannel channel=FileChannel.open(file,StandardOpenOption.READ)) {
                ByteBuffer header=ByteBuffer.allocate(4096);
                // A newly created region may still have an incomplete header
                // while the server writes it. Keep other regions in the overview
                // and let the next refresh retry this file; do not modify it.
                if (channel.size() < 8192L) continue;
                while (header.hasRemaining()) {
                    if (channel.read(header) <= 0) break;
                }
                if (header.hasRemaining()) continue;
                header.flip();
                long fileSize=channel.size();
                for (int x=Math.max(startX,rx*32);x<=Math.min(endX,rx*32+31);x++)
                    for (int z=Math.max(startZ,rz*32);z<=Math.min(endZ,rz*32+31);z++) {
                        int location=header.getInt(4*((x&31)+(z&31)*32));
                        int sector=location>>>8,count=location&255;
                        if (sector>=2 && count>0 && ((long)sector+count)*4096<=fileSize) result.add(ChunkPos.asLong(x,z));
                    }
            } catch (NoSuchFileException absent) { /* Never create a region while inspecting it. */ }
        }
        return Set.copyOf(result);
    }
}
