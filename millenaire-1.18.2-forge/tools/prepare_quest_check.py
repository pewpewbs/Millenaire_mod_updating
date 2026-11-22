from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[1];src=root/'src/main/java/org/millenaire';fixtures=root/'tools/fixtures/quest';fixtures.mkdir(parents=True,exist_ok=True)
(fixtures/'Point.java').write_text('''package org.millenaire.common.utilities;
public class Point {
    public double x,y,z;
    public double distanceTo(net.minecraft.world.entity.Entity entity){return Math.sqrt(entity.distanceToSqr(x,y,z));}
    public static Point read(net.minecraft.nbt.CompoundTag tag,String key){return null;}
    public void write(net.minecraft.nbt.CompoundTag tag,String key){}
}
''',encoding='utf-8')
cp=(root/'build/migration-classpath.txt').read_text(encoding='utf-8').strip();out=root/'build/quest-check';out.mkdir(exist_ok=True)
files=['common/entity/EntityTargetedBlaze','common/entity/EntityTargetedGhast','common/entity/EntityTargetedWitherSkeleton','common/entity/MillQuestEntities','client/render/QuestEntityRenderers']
q=lambda s:'"'+str(s).replace('\\','/')+'"'
args=['-encoding','UTF-8','-classpath',cp,'-d',out]+[src/(f+'.java') for f in files]+[fixtures/'Point.java',root/'tools/QuestEntityRegistryCheck.java']
(root/'build/quest-check-javac.args').write_text('\n'.join(q(v) for v in args),encoding='utf-8')
(root/'build/quest-check-java.args').write_text('\n'.join(q(v) for v in ['-cp',str(out)+';'+cp,'net.minecraftforge.registries.QuestEntityRegistryCheck']),encoding='utf-8')
