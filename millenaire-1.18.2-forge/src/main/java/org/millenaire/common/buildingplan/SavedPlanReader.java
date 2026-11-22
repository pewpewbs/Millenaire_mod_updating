package org.millenaire.common.buildingplan;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.millenaire.common.utilities.Point;

/** Parse completely before publishing; count limits are checked before allocation. */
public final class SavedPlanReader {
    public static final int MAX_ENTRIES = 1_000_000;
    public static final long MAX_BYTES = 64L * 1024 * 1024;
    private SavedPlanReader() {}
    private static DataInputStream open(Path file) throws IOException {
        long size = Files.size(file);
        if (size < 4 || size > MAX_BYTES) throw new IOException("Invalid plan file size: " + size);
        try (InputStream input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes((int)MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Plan grew beyond file size limit");
            return new DataInputStream(new ByteArrayInputStream(bytes));
        }
    }
    private static int count(DataInputStream in, int minBytes, int limit) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > limit || n > in.available() / minBytes) throw new IOException("Invalid plan count: " + n);
        return n;
    }
    private static Point point(DataInputStream in) throws IOException {
        int x=in.readInt(), y=in.readShort(), z=in.readInt();
        if (x < -30000000 || x > 30000000 || z < -30000000 || z > 30000000)
            throw new IOException("Plan coordinate outside world limits");
        return new Point(x,y,z);
    }
    private static BuildingBlock block(DataInputStream in) throws IOException {
        try { return new BuildingBlock(point(in), in); }
        catch (RuntimeException malformed) { throw new IOException("Invalid saved block state", malformed); }
    }
    private static void end(DataInputStream in) throws IOException {
        if (in.read()!=-1) throw new IOException("Unexpected trailing plan data");
    }
    public static void validateProgress(int index, int size) throws IOException {
        if (index < 0 || index > size) throw new IOException("Plan progress " + index + " outside 0.." + size);
    }
    public static BuildingBlock[] blocks(Path file, int progress) throws IOException {
        try (DataInputStream in=open(file)) {
            int n=count(in,16,MAX_ENTRIES);validateProgress(progress,n);
            BuildingBlock[] result=new BuildingBlock[n];
            for(int i=0;i<n;i++) result[i]=block(in);
            end(in);return result;
        }
    }
    public static List<List<BuildingBlock>> paths(Path file, int pathIndex, int blockIndex) throws IOException {
        try (DataInputStream in=open(file)) {
            int n=count(in,4,MAX_ENTRIES);validateProgress(pathIndex,n);
            List<List<BuildingBlock>> result=new ArrayList<>();int total=0;
            for(int i=0;i<n;i++) {
                int size=count(in,16,MAX_ENTRIES-total);total+=size;
                List<BuildingBlock> path=new ArrayList<>();
                for(int j=0;j<size;j++) path.add(block(in));
                result.add(path);
            }
            if(pathIndex<n) validateProgress(blockIndex,result.get(pathIndex).size());
            else if(blockIndex!=0) throw new IOException("Progress after final path");
            end(in);return result;
        }
    }
    public static List<Point> points(Path file, int progress) throws IOException {
        try(DataInputStream in=open(file)) {
            int n=count(in,10,MAX_ENTRIES);validateProgress(progress,n);
            List<Point> result=new ArrayList<>();for(int i=0;i<n;i++)result.add(point(in));
            end(in);return result;
        }
    }
}
