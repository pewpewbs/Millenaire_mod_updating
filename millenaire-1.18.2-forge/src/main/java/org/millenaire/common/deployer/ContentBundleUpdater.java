package org.millenaire.common.deployer;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

/** Stock content reconciliation, independent of the mod version. Never walks custom content. */
public final class ContentBundleUpdater {
    private ContentBundleUpdater() {}
    private static final String MANIFEST = ".millenaire-stock-content.properties";
    private static final List<String> ROOTS = List.of("cultures", "goals", "villagerconfig", "quests", "help", "languages");
    private static final List<String> FILES = List.of("blocklist.txt", "blocktypes.txt", "itemlist.txt", "localization-version.txt");
    public record Report(int installed, int updated, int removed, int conflicts, int preserved) {}
    private static String hash(Path path) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(path)) {
                byte[] buffer = new byte[16384];
                for (int n; (n = in.read(buffer)) >= 0;) if (n > 0) digest.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String key(Path relative) {
        var names = new ArrayList<String>();
        for (Path part : relative) names.add(part.toString());
        return String.join("/", names);
    }
    private static boolean managed(String key) {
        if (FILES.contains(key)) return true;
        return ROOTS.stream().anyMatch(root -> key.startsWith(root + "/"));
    }
    private static Path safePath(Path root, String key) throws IOException {
        Path result = root;
        for (String part : key.split("/", -1)) {
            if (part.isBlank() || part.equals(".") || part.equals("..") || part.contains("\\") || part.contains(":"))
                throw new IOException("Invalid managed content path: " + key);
            result = result.resolve(part);
            if (Files.isSymbolicLink(result)) throw new IOException("Symbolic link in content path: " + result);
        }
        return result;
    }
    private static void replace(Path input, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), ".mill-stock-", ".tmp");
        try {
            Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING);
            move(tmp, target);
        } finally { Files.deleteIfExists(tmp); }
    }
    private static void move(Path input, Path target) throws IOException {
        try { Files.move(input, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(input, target, StandardCopyOption.REPLACE_EXISTING); }
    }
    public static Report update(Path bundled, Path installed, Consumer<String> log) throws IOException {
        if (!Files.isDirectory(bundled) || !Files.isDirectory(installed) || Files.isSymbolicLink(installed))
            throw new IOException("Missing or unsafe content directory");
        Path manifest = safePath(installed, MANIFEST);
        Properties previous = new Properties(), next = new Properties();
        boolean bootstrap = !Files.isRegularFile(manifest);
        if (!bootstrap) try (var in = Files.newInputStream(manifest)) { previous.load(in); }
        var current = new TreeMap<String,Path>();
        for (String name : ROOTS) {
            Path root = bundled.resolve(name);
            if (!Files.isDirectory(root)) throw new IOException("Missing stock directory " + name);
            try (var stream = Files.walk(root)) {
                for (Path path : stream.filter(Files::isRegularFile).toList()) {
                    String key = key(bundled.relativize(path));
                    current.put(key, safePath(bundled, key));
                }
            }
        }
        for (String name : FILES) {
            Path file = safePath(bundled, name);
            if (!Files.isRegularFile(file)) throw new IOException("Missing stock file " + name);
            current.put(name, file);
        }
        Path backup = installed.resolveSibling("millenaire-content-backups").resolve("stock-" + UUID.randomUUID());
        int added=0, changed=0, removed=0, conflicts=0, preserved=0;
        for (var entry : current.entrySet()) {
            String key = entry.getKey();
            Path input = entry.getValue(), target = safePath(installed, key);
            String incoming = hash(input), before = previous.getProperty(key);
            next.setProperty(key, incoming);
            if (!Files.exists(target)) {
                replace(input, target); added++; continue;
            }
            if (!Files.isRegularFile(target)) throw new IOException("Expected regular file: " + target);
            String local = hash(target);
            if (local.equals(incoming)) continue;
            if (bootstrap || (before != null && local.equals(before))) {
                replace(target, safePath(backup.resolve("original"), key));
                replace(input, target); changed++;
            } else if (incoming.equals(before)) {
                preserved++;
            } else {
                // Both upstream and local changed, or a new upstream file collides
                // with an existing local addition. Never silently overwrite it.
                replace(target, safePath(backup.resolve("original"), key));
                replace(input, safePath(backup.resolve("incoming"), key));
                conflicts++;
                log.accept("MILL-CONTENT-CONFLICT: " + key + " (local retained; incoming: " + backup.resolve("incoming").resolve(key) + ")");
            }
        }
        // These three retired stock paths predate the manifest. On a first
        // sync, remove only known unmodified copies (and always back them up).
        // Otherwise an old recipe can keep overwriting the shared global ID.
        var retired = Map.of(
                "goals/genericcrafting/seljuk/MakeSandstone.txt", Set.of(
                        "455cd8bfcd8d2f409663183bc13d96a96685210b7eba4f419ceb562c17d1e0d7",
                        "28c87956096c8c93b686d73beb2d986b0108a42f30b109d1b70dbae4c1220ece"),
                "goals/genericcrafting/seljuk/MakeSugar.txt", Set.of(
                        "400c8eae9da15edff0829859ac43ed4946a65d0dc32456d675d3c438b7333395",
                        "3f34cff9a201a465db68b1e8900ce99fed7fa7510ee412a5e4af88a029396deb"));
        for (var entry : retired.entrySet()) {
            String key = entry.getKey();
            if (current.containsKey(key) || previous.containsKey(key)) continue;
            Path target = safePath(installed, key);
            if (!Files.isRegularFile(target)) continue;
            if (entry.getValue().contains(hash(target))) {
                replace(target, safePath(backup.resolve("removed"), key));
                Files.delete(target); removed++;
            } else {
                preserved++;
                log.accept("MILL-CONTENT-CONFLICT: retired locally modified goal " + key + "; retained, review duplicate ID manually");
            }
        }
        String duplicate = "goals/genericcrafting/inuit/makebonemeal.txt";
        String canonical = "goals/genericcrafting/makebonemeal.txt";
        if (!current.containsKey(duplicate) && !previous.containsKey(duplicate) && current.containsKey(canonical)) {
            Path target = safePath(installed, duplicate);
            if (Files.isRegularFile(target)) {
                if (hash(target).equals(hash(current.get(canonical)))) {
                    replace(target, safePath(backup.resolve("removed"), duplicate));
                    Files.delete(target); removed++;
                } else {
                    preserved++;
                    log.accept("MILL-CONTENT-CONFLICT: retired locally modified goal " + duplicate + "; retained, review duplicate ID manually");
                }
            }
        }
        for (String key : previous.stringPropertyNames()) {
            if (!managed(key) || current.containsKey(key)) continue;
            Path target = safePath(installed, key);
            if (!Files.exists(target)) continue;
            if (!Files.isRegularFile(target)) { preserved++; continue; }
            if (hash(target).equals(previous.getProperty(key))) {
                replace(target, safePath(backup.resolve("removed"), key));
                Files.delete(target); removed++;
            } else {
                // Removed upstream but locally edited: preserve; no longer managed.
                preserved++;
                log.accept("MILL-CONTENT-CONFLICT: upstream removed locally modified " + key + "; retained");
            }
        }
        if (bootstrap || !next.equals(previous)) {
            Path tmp = Files.createTempFile(installed, ".mill-stock-manifest-", ".tmp");
            try {
                try (var out = Files.newOutputStream(tmp)) { next.store(out, "Millenaire bundled SHA-256 baseline"); }
                move(tmp, manifest);
            } finally { Files.deleteIfExists(tmp); }
        }
        Report report = new Report(added,changed,removed,conflicts,preserved);
        log.accept("MILL-CONTENT-SYNC: bootstrap=" + bootstrap + " " + report + (changed+removed+conflicts>0 ? " backup=" + backup : ""));
        return report;
    }
}
