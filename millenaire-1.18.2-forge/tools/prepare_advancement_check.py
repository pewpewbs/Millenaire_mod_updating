from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[1];src=root/'src/main/java/org/millenaire';fixture=root/'tools/fixtures/advancements';fixture.mkdir(parents=True,exist_ok=True)
fixtures={
'MillConfigValues':'package org.millenaire.common.config; public class MillConfigValues { public static boolean DEV; public static int randomUid; public static java.util.Set<String> advancementsCreative=new java.util.HashSet<>(),advancementsSurvival=new java.util.HashSet<>(); public static void writeConfigFile(){} }',
'MillLog':'package org.millenaire.common.utilities; public class MillLog { public static void printException(String message,Exception error){throw new RuntimeException(message,error);} }',
'ServerSender':'package org.millenaire.common.network; public class ServerSender { public static void sendAdvancementEarned(net.minecraft.world.entity.player.Player player,String key){} }'}
for name,text in fixtures.items():(fixture/(name+'.java')).write_text(text,encoding='utf-8')
cp=(root/'build/migration-classpath.txt').read_text(encoding='utf-8').strip();out=root/'build/advancement-check';out.mkdir(exist_ok=True)
q=lambda s:'"'+str(s).replace('\\','/')+'"'
args=['-encoding','UTF-8','-classpath',cp,'-d',out]+list((src/'common/advancements').glob('*.java'))+list(fixture.glob('*.java'))+[root/'tools/AdvancementResourceCheck.java']
(root/'build/advancement-check-javac.args').write_text('\n'.join(q(v) for v in args),encoding='utf-8')
(root/'build/advancement-check-java.args').write_text('\n'.join(q(v) for v in ['-cp',str(out)+';'+cp,'net.minecraftforge.registries.AdvancementResourceCheck',root/'src/main/resources/data/millenaire/advancements']),encoding='utf-8')
