import java.nio.file.*;import org.millenaire.common.deployer.LanguageBundleUpdater;
public class LanguageBundleCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void write(Path p,String value)throws Exception{Files.createDirectories(p.getParent());Files.writeString(p,value);}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("millenaire-language-check-");Path bundled=root.resolve("bundle"),installed=root.resolve("mods/millenaire");
  write(bundled.resolve("localization-version.txt"),"test-1");write(bundled.resolve("languages/uk/help/help_1.txt"),"Українська довідка");write(bundled.resolve("languages/ru/strings.txt"),"Русский текст");
  write(installed.resolve("languages/uk/strings.txt"),"User edits");write(installed.resolve("cultures/keep.txt"),"Culture unchanged");
  LanguageBundleUpdater.update(bundled,installed);
  check(Files.readString(installed.resolve("languages/uk/help/help_1.txt")).equals("Українська довідка"),"Unicode copy");
  check(Files.readString(installed.resolve("languages/ru/strings.txt")).equals("Русский текст"),"Both locales");
  check(Files.readString(installed.resolve("cultures/keep.txt")).equals("Culture unchanged"),"Cultures preserved");
  try(var paths=Files.walk(root.resolve("mods/millenaire-language-backups"))){check(paths.filter(Files::isRegularFile).anyMatch(path->{try{return Files.readString(path).equals("User edits");}catch(Exception e){return false;}}),"Old edits backed up");}
  write(installed.resolve("languages/ru/strings.txt"),"New edit");LanguageBundleUpdater.update(bundled,installed);check(Files.readString(installed.resolve("languages/ru/strings.txt")).equals("New edit"),"Same revision keeps later edits");
  Path invalid=root.resolve("invalid");write(invalid.resolve("localization-version.txt"),"test-2");write(invalid.resolve("languages/uk/strings.txt"),"Only one locale");
  try{LanguageBundleUpdater.update(invalid,installed);throw new AssertionError("Missing locale accepted");}catch(java.io.IOException expected){}
  check(Files.readString(installed.resolve("languages/ru/strings.txt")).equals("New edit"),"Preflight failure leaves installed data intact");
  System.out.println("PASS: actual filesystem language updater: both locales, Unicode, nested files, backups, preserved cultures, revision idempotence and preflight validation. Temporary fixture: "+root);
 }
}