package org.millenaire.common.utilities;
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
            if (CAPTURE.get()!=null) throw new IllegalStateException("Close capture before commit");
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
                    @Override public void write(byte[] bytes, int offset, int length) throws IOException { out.write(bytes, offset, length); }
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
