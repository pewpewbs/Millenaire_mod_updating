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
                    if (!Files.isDirectory(destination) || Files.isSymbolicLink(destination))
                        throw new IOException("Invalid deployed directory " + destination);
                    // Reconcile stock files below, even across version changes.
                    // Never replace an existing user configuration or custom directory.
                    continue;
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
            // Reconcile the entire managed stock bundle, even when VERSION_NUMBER
            // has not changed. The SHA-256 baseline preserves later local edits.
            var bundled = source.resolve("millenaire");
            var installed = mods.resolve("millenaire");
            ContentBundleUpdater.update(bundled, installed, message -> Mill.LOGGER.info(message));
            Files.writeString(installed.resolve("version.txt"), Mill.VERSION_NUMBER);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to deploy Millenaire content", error);
        }
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

