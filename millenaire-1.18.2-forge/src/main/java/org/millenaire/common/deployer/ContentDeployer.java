/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.io.IOUtils
 */
package org.millenaire.common.deployer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.IOUtils;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.MillCommonUtilities;

public class ContentDeployer {
    /** Works with Forge's union filesystem as well as unpacked development resources. */
    public static void deployBundledContent(java.nio.file.Path source) {
        java.nio.file.Path mods = MillCommonUtilities.getModsDir().toPath();
        try {
            if (!Files.isDirectory(source.resolve("millenaire")))
                throw new IOException("Bundled Millenaire content is missing: " + source);
            Files.createDirectories(mods);
            for (String folder : new String[]{"millenaire", "millenaire-custom"}) {
                var destination = mods.resolve(folder);
                var version = destination.resolve("version.txt");
                if (Files.exists(destination)) {
                    if (folder.equals("millenaire-custom")) continue;
                    if (Files.isRegularFile(version) && Files.readString(version).trim().equals(Mill.VERSION_NUMBER)) continue;
                    // Preserve previous content rather than deleting a user's edited files.
                    Files.move(destination, mods.resolve(folder + "-backup-" + java.util.UUID.randomUUID()));
                }
                var input = source.resolve(folder);
                if (!Files.isDirectory(input)) throw new IOException("Missing bundled directory: " + input);
                try (var paths = Files.walk(input)) {
                    for (var entry : paths.toList()) {
                        var target = destination;
                        for (var part : input.relativize(entry)) target = target.resolve(part.toString());
                        if (Files.isDirectory(entry)) Files.createDirectories(target);
                        else { Files.createDirectories(target.getParent()); Files.copy(entry, target); }
                    }
                }
                if (folder.equals("millenaire")) Files.writeString(version, Mill.VERSION_NUMBER);
            }
            LanguageBundleUpdater.update(source.resolve("millenaire"), mods.resolve("millenaire"));
            updateMayanSandstoneGoal(source.resolve("millenaire"), mods.resolve("millenaire"));
            updateTavernFurnace(source.resolve("millenaire"), mods.resolve("millenaire"));
        } catch (IOException error) {
            throw new IllegalStateException("Unable to deploy Millenaire content", error);
        }
    }

    /**
     * Same-version ports keep their deployed content. Migrate just the missing
     * stock Mayan miner task, without replacing the rest of the culture or
     * touching millenaire-custom. Safe to run again after every startup.
     */
    private static void updateMayanSandstoneGoal(java.nio.file.Path bundled,
            java.nio.file.Path installed) throws IOException {
        var relative = java.nio.file.Path.of("cultures", "mayan", "villagers", "mayan_miner.txt");
        var target = installed.resolve(relative);
        var stock = bundled.resolve(relative);
        var goalRelative = java.nio.file.Path.of("goals", "genericmining", "minesandstone.txt");
        if (!Files.isRegularFile(stock) || !Files.isRegularFile(target)) return;
        String stockText = Files.readString(stock);
        if (!stockText.lines().anyMatch(line -> line.trim().equalsIgnoreCase("goal=minesandstone"))) return;
        String original = Files.readString(target);
        if (original.lines().anyMatch(line -> line.trim().equalsIgnoreCase("goal=minesandstone"))) return;
        // Do not alter a locally rewritten miner lacking the standard stone task.
        var anchor = java.util.regex.Pattern.compile("(?im)^[ \\t]*goal[ \\t]*=[ \\t]*minestone[ \\t]*(?=\\r?$)").matcher(original);
        if (!anchor.find()) {
            Mill.LOGGER.warn("Mayan sandstone migration skipped: deployed miner has no standard minestone goal");
            return;
        }
        String newline = original.contains("\r\n") ? "\r\n" : "\n";
        String updated = original.substring(0, anchor.end()) + newline
                + "goal=minesandstone" + original.substring(anchor.end());
        var installedGoal = installed.resolve(goalRelative);
        if (!Files.isRegularFile(installedGoal)) {
            var bundledGoal = bundled.resolve(goalRelative);
            if (!Files.isRegularFile(bundledGoal)) {
                Mill.LOGGER.warn("Mayan sandstone migration skipped: minesandstone goal definition missing");
                return;
            }
            Files.createDirectories(installedGoal.getParent());
            Files.copy(bundledGoal, installedGoal);
        }
        var backup = installed.resolveSibling("millenaire-content-backups")
                .resolve(java.util.UUID.randomUUID().toString()).resolve(relative);
        Files.createDirectories(backup.getParent());
        Files.copy(target, backup);
        var staged = Files.createTempFile(target.getParent(), "mayan-miner-", ".tmp");
        try {
            Files.writeString(staged, updated);
            try {
                Files.move(staged, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(staged, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(staged);
        }
        Mill.LOGGER.info("MAYAN-SANDSTONE-MIGRATION: installed missing minesandstone goal; original backed up");
    }

    /** Same-version migration of the bundled tavern cook; custom overrides are untouched. */
    private static void updateTavernFurnace(java.nio.file.Path bundled,
            java.nio.file.Path installed) throws IOException {
        var relative = java.nio.file.Path.of("cultures", "byzantines", "villagers",
                "artisans", "hostesse_byzantine.txt");
        var target = installed.resolve(relative);
        var stock = bundled.resolve(relative);
        if (!Files.isRegularFile(target) || !Files.isRegularFile(stock)) return;
        String original = Files.readString(target);
        String stockText = Files.readString(stock);
        if (!stockText.lines().anyMatch(line -> line.trim().equalsIgnoreCase("goal=tendfurnace"))) return;
        var anchor = java.util.regex.Pattern.compile("(?im)^[ \\t]*goal[ \\t]*=[ \\t]*cooksteak[ \\t]*(?=\\r?$)").matcher(original);
        if (!anchor.find()) return;
        String newline = original.contains("\r\n") ? "\r\n" : "\n";
        var additions = new java.util.ArrayList<String>();
        if (!java.util.regex.Pattern.compile("(?im)^\\s*goal\\s*=\\s*tendfurnace\\s*$").matcher(original).find())
            additions.add("goal=tendfurnace");
        for (String wood : new String[]{"wood", "wood_pine", "wood_birch", "wood_jungle", "wood_acacia", "wood_darkoak"}) {
            String key = "requiredGood=" + wood + ",4";
            if (!stockText.lines().anyMatch(line -> line.trim().equalsIgnoreCase(key))) continue;
            if (!java.util.regex.Pattern.compile("(?im)^[ \\t]*requiredGood[ \\t]*=[ \\t]*"
                    + wood + "[ \\t]*,").matcher(original).find()) additions.add(key);
        }
        if (additions.isEmpty()) return;
        String updated = original.substring(0, anchor.end()) + newline
                + String.join(newline, additions) + original.substring(anchor.end());
        var backup = installed.resolveSibling("millenaire-content-backups")
                .resolve(java.util.UUID.randomUUID().toString()).resolve(relative);
        Files.createDirectories(backup.getParent());
        Files.copy(target, backup);
        var staged = Files.createTempFile(target.getParent(), "tavern-cook-", ".tmp");
        try {
            Files.writeString(staged, updated);
            try {
                Files.move(staged, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(staged, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(staged);
        }
        Mill.LOGGER.info("TAVERN-FURNACE-MIGRATION: added missing maintenance/fuel entries; original backed up");
    }

    private static final String DEV_VERSION_NUMBER = "@VERSION@";

    private static void copyFolder(String modJarPath, String deployLocation, String folder, File destDir) throws IOException {
        if (!destDir.exists() && !destDir.mkdir()) {
            Mill.LOGGER.warn("Failed to create dest dir");
        }
        JarFile file = new JarFile(modJarPath);
        Enumeration<JarEntry> e = file.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
            String jarEntryName = entry.getName();
            if (!jarEntryName.startsWith(deployLocation + folder)) continue;
            File destination = new File(destDir, jarEntryName.substring(deployLocation.length(), jarEntryName.length()));
            if (entry.isDirectory()) {
                if (destination.mkdirs()) continue;
                Mill.LOGGER.warn("Failed to create dest dirs");
                continue;
            }
            InputStream stream = file.getInputStream(entry);
            FileOutputStream out = new FileOutputStream(destination);
            IOUtils.copy((InputStream)stream, (OutputStream)out);
            stream.close();
            ((OutputStream)out).close();
        }
        file.close();
    }

    public static void deployContent(File ourJar) {
        if (!ContentDeployer.class.getResource("ContentDeployer.class").toString().startsWith("jar")) {
            Mill.LOGGER.warn("No need to redeploy Mill\u00e9naire as we are in a dev environment.");
            return;
        }
        File modsDir = MillCommonUtilities.getModsDir();
        try {
            boolean redeployMillenaire = false;
            File millenaireDir = new File(modsDir, "millenaire");
            if ("8.1.2".equals(DEV_VERSION_NUMBER)) {
                redeployMillenaire = true;
                Mill.LOGGER.warn("Deploying millenaire/ as we are using a dev version and can't test whether it has changed.");
            } else if (!millenaireDir.exists()) {
                redeployMillenaire = true;
                Mill.LOGGER.warn("Deploying millenaire/ to version 8.1.2 as it can't be found.");
            } else {
                File versionFile = new File(millenaireDir, "version.txt");
                if (!versionFile.exists()) {
                    redeployMillenaire = true;
                    MillCommonUtilities.deleteDir(millenaireDir);
                    Mill.LOGGER.warn("Redeploying millenaire/ to version 8.1.2 as it has no version file.");
                } else {
                    BufferedReader reader = MillCommonUtilities.getReader(versionFile);
                    String versionString = reader.readLine();
                    if (!versionString.equals("8.1.2")) {
                        redeployMillenaire = true;
                        MillCommonUtilities.deleteDir(millenaireDir);
                        Mill.LOGGER.warn("Redeploying millenaire/ to version 8.1.2 as it has version " + versionString + ".");
                    } else {
                        Mill.LOGGER.warn("No need to redeploy Mill\u00e9naire as the millenaire folder is already at vesion " + versionString + ".");
                    }
                }
            }
            if (redeployMillenaire) {
                try {
                    long startTime = System.currentTimeMillis();
                    ContentDeployer.copyFolder(ourJar.getAbsolutePath(), "todeploy/", "millenaire/", modsDir);
                    Files.write(Paths.get(modsDir.getAbsolutePath() + "/millenaire/version.txt", new String[0]), "8.1.2".getBytes(), new OpenOption[0]);
                    Mill.LOGGER.warn("Deployed millenaire folder in " + (System.currentTimeMillis() - startTime) + " ms.");
                }
                catch (IOException e) {
                    Mill.LOGGER.error("Error when checking existing millenaire dir: ", (Throwable)e);
                }
            }
        }
        catch (Exception e) {
            Mill.LOGGER.error("Error when unzipping millenaire: ", (Throwable)e);
        }
        try {
            File millenaireCustomDir = new File(modsDir, "millenaire-custom");
            if (!millenaireCustomDir.exists()) {
                Mill.LOGGER.warn("Deploying millenaire-custom/ .");
                try {
                    long startTime = System.currentTimeMillis();
                    ContentDeployer.copyFolder(ourJar.getAbsolutePath(), "todeploy/", "millenaire-custom/", modsDir);
                    Mill.LOGGER.warn("Deployed millenaire-custom folder in " + (System.currentTimeMillis() - startTime) + " ms.");
                }
                catch (IOException e) {
                    Mill.LOGGER.error("Error when checking existing millenaire-custom dir: ", (Throwable)e);
                }
            }
        }
        catch (Exception e) {
            Mill.LOGGER.error("Error when unzipping millenaire-custom: ", (Throwable)e);
        }
    }
}

