from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';backup=r/'migration/before-saves21';backup.mkdir(exist_ok=False)
def edit(rel,fn):
 p=base/rel;d=backup/p.relative_to(r);d.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,d);p.write_text(fn(p.read_text(encoding='utf-8')),encoding='utf-8')
p=base/'utilities/AtomicSaveFile.java';assert not p.exists()
p.write_text('''package org.millenaire.common.utilities;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Never opens the last good file for truncation. Unsupported atomic moves fail closed. */
public final class AtomicSaveFile {
    private static final ThreadLocal<Batch> CAPTURE = new ThreadLocal<>();
    public static final class Batch implements AutoCloseable {
        private final Map<Path, byte[]> files = new LinkedHashMap<>();
        private final List<Runnable> retries = new ArrayList<>();
        private Batch() { if (CAPTURE.get()!=null) throw new IllegalStateException("Nested save capture"); CAPTURE.set(this); }
        public void commit() throws IOException {
            for (var entry : files.entrySet()) {
                if (entry.getValue()==null) AtomicSaveFile.delete(entry.getKey());
                else AtomicSaveFile.write(entry.getKey(), out -> out.write(entry.getValue()));
            }
        }
        public void retryOnServerThread() { for (Runnable retry : retries) retry.run(); }
        @Override public void close() { CAPTURE.remove(); }
    }
    public static Batch capture() { return new Batch(); }
    public static void retryIfFailed(Runnable retry) { if (CAPTURE.get()!=null) CAPTURE.get().retries.add(retry); }
    @FunctionalInterface public interface Writer { void write(OutputStream stream) throws IOException; }
    private AtomicSaveFile() {}
    public static void write(Path target, Writer writer) throws IOException {
        if (CAPTURE.get()!=null) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            writer.write(bytes); CAPTURE.get().files.put(target, bytes.toByteArray()); return;
        }
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temp = Files.createTempFile(target.toAbsolutePath().getParent(), target.getFileName()+".", ".tmp");
        try {
            try (FileOutputStream stream = new FileOutputStream(temp.toFile())) {
                // Some serializers close their stream. Keep ownership here for flush/fsync.
                writer.write(new FilterOutputStream(stream) {
                    @Override public void close() throws IOException { flush(); }
                });
                stream.flush(); stream.getFD().sync();
            }
            if (Files.exists(target)) {
                Path previous = target.resolveSibling(target.getFileName()+".bak");
                Path copy = Files.createTempFile(target.toAbsolutePath().getParent(), "backup-", ".tmp");
                try {
                    Files.copy(target, copy, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(copy, previous, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(copy); }
            }
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    public static void delete(Path target) throws IOException {
        if (CAPTURE.get()!=null) { CAPTURE.get().files.put(target, null); return; }
        if (Files.exists(target)) {
            Path backup=target.resolveSibling(target.getFileName()+".bak");
            AtomicSaveFile.write(backup, out -> Files.copy(target, out));
            Files.delete(target);
        }
    }
}
''',encoding='utf-8')
def building(s):
 start=s.index('   private void writePaths() {');end=s.index('   public void writeToNBT(',start)
 s=s[:start]+'''   private void writePaths() {
      File dir = MillCommonUtilities.getBuildingsDir(this.world);
      Path paths = new File(dir, this.getPos().getPathString()+"_paths.bin").toPath();
      Path clear = new File(dir, this.getPos().getPathString()+"_pathstoclear.bin").toPath();
      try {
         if (this.pathsToBuild == null) org.millenaire.common.utilities.AtomicSaveFile.delete(paths);
         else org.millenaire.common.utilities.AtomicSaveFile.write(paths, stream -> {
            DataOutputStream ds = new DataOutputStream(stream);
            ds.writeInt(this.pathsToBuild.size());
            for (List<BuildingBlock> path : this.pathsToBuild) {
               ds.writeInt(path.size());
               for (BuildingBlock block : path) {
                  ds.writeInt(block.p.getiX()); ds.writeShort(block.p.getiY()); ds.writeInt(block.p.getiZ());
                  block.writeState(ds);
               }
            }
            ds.flush();
         });
         if (this.oldPathPointsToClear == null) org.millenaire.common.utilities.AtomicSaveFile.delete(clear);
         else org.millenaire.common.utilities.AtomicSaveFile.write(clear, stream -> {
            DataOutputStream ds = new DataOutputStream(stream);
            ds.writeInt(this.oldPathPointsToClear.size());
            for (Point point : this.oldPathPointsToClear) {
               ds.writeInt(point.getiX()); ds.writeShort(point.getiY()); ds.writeInt(point.getiZ());
            }
            ds.flush();
         });
         org.millenaire.common.utilities.AtomicSaveFile.retryIfFailed(() -> this.pathsChanged = true);
         this.pathsChanged = false;
      } catch (IOException error) { throw new java.io.UncheckedIOException("Cannot save paths", error); }
   }

'''+s[end:]
 s=s.replace('   private Building.SaveWorker saveWorker = null;','''   private SaveWorker saveWorker = null;
   private long saveRevision = 0L;
   private long nextSaveAttempt = 0L;''')
 s=s.replace('this.saveNeeded = true;', 'this.markSaveNeeded();')
 start=s.index('   public void saveTownHall(String reason) {');end=s.index('   public void sendBuildingPacket',start)
 s=s[:start]+'''   public void markSaveNeeded() {
      this.saveNeeded = true;
      ++this.saveRevision;
      if (!this.isTownhall && this.mw != null && this.townHallPos != null) {
         Building th = this.mw.getBuilding(this.townHallPos);
         if (th != null && th != this) th.markSaveNeeded();
      }
   }

   public void finishPendingSave() {
      SaveWorker worker = this.saveWorker;
      if (worker == null || worker.isAlive()) return;
      this.saveWorker = null;
      if (!worker.succeeded) {
         worker.batch.retryOnServerThread();
         this.saveNeeded = true;
         this.nextSaveAttempt = this.world.getGameTime() + 100L;
         MillLog.printException("Town hall save failed; changes retained for retry: " + worker.target,
            worker.failure == null ? new IOException("Save worker stopped before commit") : worker.failure);
      } else {
         this.lastSaved = this.world.getGameTime();
         this.nextSaveAttempt = this.world.getGameTime() + 100L;
         if (this.saveRevision == worker.revision) {
            this.saveNeeded = false;
            this.saveReason = null;
         }
         if (MillConfigValues.LogHybernation >= 1) MillLog.major(this, "Saved town hall: " + worker.reason);
      }
   }

   public void saveTownHall(String reason) {
      if (this.world.isClientSide || !this.isTownhall) return;
      if (!this.world.getServer().isSameThread()) {
         this.world.getServer().execute(() -> this.saveTownHall(reason));
         return;
      }
      this.finishPendingSave();
      if (this.saveWorker != null) {
         // An explicit save request may represent changes newer than the snapshot.
         this.markSaveNeeded();
         return;
      }
      if (this.world.getGameTime() < this.nextSaveAttempt) return;
      this.saveNeeded = true;
      org.millenaire.common.utilities.AtomicSaveFile.Batch batch = org.millenaire.common.utilities.AtomicSaveFile.capture();
      try (batch) {
         CompoundTag snapshot = new CompoundTag();
         ListTag list = new ListTag();
         for (Point point : new ArrayList<Point>(this.buildings)) {
            if (point == null) throw new IllegalStateException("Null building position during save");
            Building building = this.mw.getBuilding(point);
            if (building == null) throw new IllegalStateException("Missing building during save: " + point);
            CompoundTag tag = new CompoundTag();
            building.writeToNBT(tag);
            list.add(tag);
         }
         snapshot.put("buildings", list);
         Path target = new File(new File(this.mw.millenaireDir, "buildings"), this.getPos().getPathString()+".gz").toPath();
         this.saveWorker = new SaveWorker(snapshot.copy(), target, this.saveRevision, reason, batch);
         this.saveWorker.start();
      } catch (Exception failure) {
         batch.retryOnServerThread();
         this.saveWorker = null;
         this.nextSaveAttempt = this.world.getGameTime() + 100L;
         MillLog.printException("Could not create complete town hall snapshot; will retry", failure);
      }
   }

   public void saveTownHallAndWait(String reason) {
      if (this.world.isClientSide || !this.isTownhall) return;
      if (!this.world.getServer().isSameThread()) throw new IllegalStateException("Synchronous save must run on server thread");
      try {
         if (this.saveWorker != null) this.saveWorker.join();
         this.finishPendingSave();
         this.nextSaveAttempt = 0L;
         this.saveTownHall(reason); // Fresh final snapshot, including changes during previous write.
         if (this.saveWorker != null) this.saveWorker.join();
         this.finishPendingSave();
      } catch (InterruptedException interrupted) {
         Thread.currentThread().interrupt();
         this.markSaveNeeded();
         MillLog.printException("Interrupted while waiting for town hall save", interrupted);
      }
   }

'''+s[end:]
 s=s.replace('   public void updateBuildingServer() {\n','   public void updateBuildingServer() {\n      if (!this.world.isClientSide) this.finishPendingSave();\n')
 s=s.replace('this.world.getDayTime() - this.lastSaved > 1000L','this.world.getGameTime() - this.lastSaved > 1000L')
 # The scheduler must not make a new revision on every tick of an in-flight save.
 s=s.replace('if (this.saveNeeded) {\n                        this.saveTownHall', 'if (this.saveNeeded && this.saveWorker == null) {\n                        this.saveTownHall')
 s=s.replace('} else if (this.world.getGameTime() - this.lastSaved > 1000L) {','} else if (this.saveWorker == null && this.world.getGameTime() - this.lastSaved > 1000L) {')
 s=s.replace('   public void invalidateInventoryCache() {\n      this.inventoryCache = null;','   public void invalidateInventoryCache() {\n      this.inventoryCache = null;\n      if (this.world != null && !this.world.isClientSide) this.markSaveNeeded();')
 s=s.replace('MillLog.error(this, "Null location. Skipping write.");','throw new IllegalStateException("Cannot save building with null location");')
 old='''            MillLog.error(this, "Exception in Villager.onUpdate(): ");
            MillLog.printException(var11);'''
 assert old in s;s=s.replace(old,'''            MillLog.printException(var11);
            throw new IllegalStateException("Cannot serialize complete building snapshot", var11);''')
 start=s.index('   private class SaveWorker extends Thread {')
 s=s[:start]+'''   private static final class SaveWorker extends Thread {
      private final CompoundTag snapshot;
      private final Path target;
      private final long revision;
      private final String reason;
      private volatile Exception failure;
      private volatile boolean succeeded;
      private final org.millenaire.common.utilities.AtomicSaveFile.Batch batch;
      private SaveWorker(CompoundTag snapshot, Path target, long revision, String reason,
            org.millenaire.common.utilities.AtomicSaveFile.Batch batch) {
         super("Millenaire-save");
         this.snapshot = snapshot;
         this.target = target;
         this.revision = revision;
         this.reason = reason;
         this.batch = batch;
      }
      @Override public void run() {
         try {
            this.batch.commit();
            org.millenaire.common.utilities.AtomicSaveFile.write(this.target,
               stream -> NbtIo.writeCompressed(this.snapshot, stream));
            this.succeeded = true;
         } catch (Exception error) { this.failure = error; }
      }
   }
}
'''
 return s
edit('village/Building.java',building)
def construction(s):
 start=s.index('    public void writeBblocks() {')
 return s[:start]+'''    public void writeBblocks() {
        File dir = MillCommonUtilities.getBuildingsDir(this.townHall.world);
        java.nio.file.Path target = new File(dir, this.townHall.getPos().getPathString()+"_bblocks_"+this.id+".bin").toPath();
        BuildingBlock[] blocks = this.getBblocks();
        try {
            if (blocks == null) org.millenaire.common.utilities.AtomicSaveFile.delete(target);
            else org.millenaire.common.utilities.AtomicSaveFile.write(target, stream -> {
                DataOutputStream ds = new DataOutputStream(stream);
                ds.writeInt(blocks.length);
                for (BuildingBlock block : blocks) {
                    ds.writeInt(block.p.getiX()); ds.writeShort(block.p.getiY()); ds.writeInt(block.p.getiZ());
                    block.writeState(ds);
                }
                ds.flush();
            });
            org.millenaire.common.utilities.AtomicSaveFile.retryIfFailed(() -> this.bblocksChanged = true);
            this.bblocksChanged = false;
        } catch (IOException error) { throw new java.io.UncheckedIOException("Cannot save construction blocks", error); }
    }
}
'''
edit('village/ConstructionIP.java',construction)
def world(s):
 s=s.replace('if (!b.isTownhall || !b.isActive) continue;\n            b.saveTownHall("world save");','if (!b.isTownhall) continue;\n            b.saveTownHallAndWait("world save");')
 return s
edit('world/MillWorldData.java',world)
def events(s):
 a='''        if (Mill.serverWorlds != null) Mill.serverWorlds.removeIf(mw -> mw.world == event.getWorld());'''
 assert a in s
 return s.replace(a,'''        if (Mill.serverWorlds != null) {
            for (MillWorldData mw : Mill.serverWorlds) {
                if (mw.world == event.getWorld() && !mw.world.isClientSide && !Mill.startupError)
                    for (org.millenaire.common.village.Building b : mw.allBuildings())
                        if (b.isTownhall) b.saveTownHallAndWait("world unload");
            }
            Mill.serverWorlds.removeIf(mw -> mw.world == event.getWorld());
        }''')
edit('forge/MillEventController.java',events)
print('Main snapshots now built on server thread; worker only writes immutable snapshot; revision/failure and final flush handling added')
