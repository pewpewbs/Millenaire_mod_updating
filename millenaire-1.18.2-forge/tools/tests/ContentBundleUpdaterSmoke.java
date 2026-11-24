import org.millenaire.common.deployer.ContentBundleUpdater;
import java.nio.file.*;
import java.util.*;

/** Standalone Java 17 smoke test, no Minecraft or Forge required. */
public final class ContentBundleUpdaterSmoke {
    private static void put(Path root, String key, String value) throws Exception {
        Path file=root.resolve(key); Files.createDirectories(file.getParent()); Files.writeString(file,value);
    }
    private static String get(Path root, String key) throws Exception { return Files.readString(root.resolve(key)); }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        System.out.println("PASS " + name);
    }
    private static boolean backupContains(Path root,String suffix,String value) throws Exception {
        if (!Files.isDirectory(root)) return false;
        try (var paths=Files.walk(root)) {
            for (Path path:paths.filter(Files::isRegularFile).toList())
                if (path.toString().replace('\\','/').endsWith(suffix) && Files.readString(path).equals(value)) return true;
        }
        return false;
    }
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("mill-stock-smoke-");
        try {
            Path bundled=dir.resolve("bundle"), installed=dir.resolve("millenaire"), custom=dir.resolve("millenaire-custom"), backup=dir.resolve("millenaire-content-backups");
            for (String name:List.of("cultures","goals","villagerconfig","quests","help","languages")) Files.createDirectories(bundled.resolve(name));
            for (String name:List.of("blocklist.txt","blocktypes.txt","itemlist.txt","localization-version.txt")) put(bundled,name,"v1");
            put(bundled,"cultures/mayan/miner.txt","new stock");
            put(bundled,"goals/cook.txt","v1");
            put(bundled,"goals/obsolete.txt","obsolete");
            put(bundled,"goals/locally-edited.txt","v1");
            put(bundled,"goals/removed-customized.txt","v1");
            put(installed,"cultures/mayan/miner.txt","legacy stock");
            put(installed,"config.txt","user setting");
            put(installed,"config-server.txt","server setting");
            put(installed,"goals/untracked.txt","local addition");
            put(custom,"cultures/mayan/miner.txt","custom override");
            var log=new ArrayList<String>();
            var first=ContentBundleUpdater.update(bundled,installed,log::add);
            check(first.updated()==1 && get(installed,"cultures/mayan/miner.txt").equals("new stock"),"legacy same-version stock updated");
            check(backupContains(backup,"/original/cultures/mayan/miner.txt","legacy stock"),"legacy original backed up");
            check(get(installed,"config.txt").equals("user setting") && get(installed,"config-server.txt").equals("server setting") && get(custom,"cultures/mayan/miner.txt").equals("custom override"),"user and custom settings untouched");
            var again=ContentBundleUpdater.update(bundled,installed,log::add);
            check(again.installed()+again.updated()+again.removed()+again.conflicts()==0,"idempotent restart");
            put(installed,"goals/locally-edited.txt","my edit");
            put(installed,"goals/removed-customized.txt","my removed edit");
            put(bundled,"goals/cook.txt","v2");
            put(bundled,"goals/new.txt","new goal");
            Files.delete(bundled.resolve("goals/obsolete.txt"));
            Files.delete(bundled.resolve("goals/removed-customized.txt"));
            var upgrade=ContentBundleUpdater.update(bundled,installed,log::add);
            check(get(installed,"goals/cook.txt").equals("v2") && get(installed,"goals/new.txt").equals("new goal"),"automatic update and new goal");
            check(!Files.exists(installed.resolve("goals/obsolete.txt")) && backupContains(backup,"/removed/goals/obsolete.txt","obsolete"),"clean upstream removal backed up");
            check(get(installed,"goals/locally-edited.txt").equals("my edit") && get(installed,"goals/removed-customized.txt").equals("my removed edit") && get(installed,"goals/untracked.txt").equals("local addition"),"local modifications and untracked files preserved");
            put(bundled,"goals/locally-edited.txt","upstream v2");
            var conflict=ContentBundleUpdater.update(bundled,installed,log::add);
            check(conflict.conflicts()==1 && get(installed,"goals/locally-edited.txt").equals("my edit") && backupContains(backup,"/incoming/goals/locally-edited.txt","upstream v2"),"conflict preserves local and incoming");
            check(ContentBundleUpdater.update(bundled,installed,log::add).conflicts()==0,"conflict not repeatedly backed up");
            Files.delete(installed.resolve("goals/cook.txt"));
            check(ContentBundleUpdater.update(bundled,installed,log::add).installed()==1 && get(installed,"goals/cook.txt").equals("v2"),"missing stock restored");
            check(log.stream().anyMatch(s->s.startsWith("MILL-CONTENT-SYNC")) && log.stream().anyMatch(s->s.startsWith("MILL-CONTENT-CONFLICT")),"audit logs emitted");
        } finally {
            try (var paths=Files.walk(dir)) { for (Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        }
    }
}
