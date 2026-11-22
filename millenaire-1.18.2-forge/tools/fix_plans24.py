from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';bk=r/'migration/before-plans24';bk.mkdir(exist_ok=False)
def edit(n,fn):
 p=base/n;q=bk/n;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q);p.write_text(fn(p.read_text()),encoding='utf-8')
(base/'buildingplan/SavedPlanReader.java').write_text('''package org.millenaire.common.buildingplan;
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
        return new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(file)));
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
            for(int i=0;i<n;i++) result[i]=new BuildingBlock(point(in),in);
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
                for(int j=0;j<size;j++) path.add(new BuildingBlock(point(in),in));
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
''',encoding='utf-8')
def construction(s):
 a=s.index('    public void readBblocks()');b=s.index('    public void setBblockPos',a)
 s=s[:a]+'''    public void readBblocks() {
        File file = new File(MillCommonUtilities.getBuildingsDir(this.townHall.world),
            this.townHall.getPos().getPathString() + "_bblocks_" + this.id + ".bin");
        try {
            if (file.exists()) {
                BuildingBlock[] loaded = org.millenaire.common.buildingplan.SavedPlanReader.blocks(file.toPath(), this.bblocksPos);
                this.bblocks = loaded;
            } else if (this.bblocksPos != 0) throw new IOException("Missing construction plan with nonzero progress");
        } catch (Exception failure) {
            this.townHall.protectPlanFiles(file, failure);
        }
    }

'''+s[b:]
 s=s.replace('    public void writeBblocks() {','    public void writeBblocks() {\n        this.townHall.requirePlanFilesWritable();')
 return s
edit('village/ConstructionIP.java',construction)
def building(s):
 a=s.index('   private void readPaths()');b=s.index('   public void readTownHall(',a)
 s=s[:a]+'''   private boolean planFilesProtected = false;

   public void protectPlanFiles(File file, Exception failure) {
      this.planFilesProtected = true;
      MillLog.printException("Village save protected after invalid plan file: " + file, failure);
   }

   public void requirePlanFilesWritable() {
      if (this.planFilesProtected) throw new IllegalStateException("Village save blocked: repair invalid plan files before saving");
   }

   private void readPaths() {
      File dir = MillCommonUtilities.getBuildingsDir(this.world);
      File paths = new File(dir, this.getPos().getPathString() + "_paths.bin");
      File clear = new File(dir, this.getPos().getPathString() + "_pathstoclear.bin");
      try {
         List<List<BuildingBlock>> loadedPaths = paths.exists()
            ? org.millenaire.common.buildingplan.SavedPlanReader.paths(paths.toPath(), this.pathsToBuildIndex, this.pathsToBuildPathIndex) : null;
         List<Point> loadedClear = clear.exists()
            ? org.millenaire.common.buildingplan.SavedPlanReader.points(clear.toPath(), this.oldPathPointsToClearIndex) : null;
         this.pathsToBuild = loadedPaths;
         this.oldPathPointsToClear = loadedClear;
      } catch (Exception failure) { this.protectPlanFiles(dir, failure); }
   }

'''+s[b:]
 s=s.replace('            cip.readBblocks();\n            cip.setBblockPos(nbttagcompound.getInt("bblocksPos_" + ixx));','            cip.setBblockPos(nbttagcompound.getInt("bblocksPos_" + ixx));\n            cip.readBblocks();')
 s=s.replace('   public void writeToNBT(CompoundTag nbttagcompound) {','   public void writeToNBT(CompoundTag nbttagcompound) {\n      this.requirePlanFilesWritable();')
 # Main snapshot must fail too, even when changed flags are false.
 assert 'this.requirePlanFilesWritable();' in s
 s=s.replace('   private void writePaths() {','   private void writePaths() {\n      this.requirePlanFilesWritable();')
 return s
edit('village/Building.java',building)
print('Bounded complete plan readers, progress validation, original file protection')
