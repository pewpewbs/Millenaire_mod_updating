package org.millenaire.common.deployer;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/** Updates only the bundled language folders; user custom content and cultures stay in place. */
public final class LanguageBundleUpdater {
    private LanguageBundleUpdater() {}
    public static void update(Path bundled, Path installed) throws IOException {
        Path sourceMarker=bundled.resolve("localization-version.txt");
        if (!Files.isRegularFile(sourceMarker)) return;
        String revision=Files.readString(sourceMarker).trim();
        Path marker=installed.resolve("localization-version.txt");
        if (Files.isRegularFile(marker) && Files.readString(marker).trim().equals(revision)) return;
        Path backup=installed.resolveSibling("millenaire-language-backups").resolve(UUID.randomUUID().toString());
        for (String language : new String[]{"uk", "ru"}) {
            Path input=bundled.resolve("languages").resolve(language);
            if (!Files.isDirectory(input)) throw new IOException("Missing language bundle: "+language);
        }
        for (String language : new String[]{"uk", "ru"}) {
            Path input=bundled.resolve("languages").resolve(language);
            Path target=installed.resolve("languages").resolve(language);
            if (Files.exists(target)) { Files.createDirectories(backup); Files.move(target,backup.resolve(language)); }
            try (var paths=Files.walk(input)) {
                for (Path entry:paths.toList()) {
                    Path output=target;
                    for (Path part:input.relativize(entry)) output=output.resolve(part.toString());
                    if (Files.isDirectory(entry)) Files.createDirectories(output);
                    else { Files.createDirectories(output.getParent()); Files.copy(entry,output); }
                }
            }
        }
        Files.writeString(marker,revision+"\n");
    }
}
