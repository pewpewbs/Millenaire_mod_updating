from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');b=r/'migration/before-review13';b.mkdir(exist_ok=False)
base=r/'src/main/java/org/millenaire/common';res=r/'src/main/resources/todeploy/millenaire'
def edit(p,fn):
 dest=b/p.relative_to(r);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest);s=p.read_text(encoding='utf-8');p.write_text(fn(s),encoding='utf-8')
for p in (res/'languages/uk').glob('*buildings.txt'):
 edit(p,lambda s:s.replace('=Кварі','=Каменоломня').replace('=Армійна форга','=Військова кузня').replace('=Дом охоронника','=Дім охоронця').replace('=Дом\n','=Дім\n'))
p=res/'languages/uk/japanese_buildings.txt'
repl={'Лучное стрільбище':'Стрільбище для лучників','Купальний будинок':'Лазня','Куриня ферма':'Куряча ферма','Дом ферми':'Фермерський будинок','Рыбний басейн':'Рибний ставок','Рівніцтво':'Рибне господарство','Коваль':'Кузня','Гроув':'Гай','Имперский офіс':'Імператорська канцелярія','Середний наділ':'Середня ділянка','Остань':'Ставок','Півоварення саке':'Броварня саке','Ну, добре.':'Криниця','Форт бандити':'Форт бандитів','Одинокий японський шахта':'Віддалена японська шахта'}
# Already backed up above; keep that original backup.
s=p.read_text(encoding='utf-8')
for old,new in repl.items():s=s.replace('='+old+'\n','='+new+'\n')
p.write_text(s,encoding='utf-8')
def quest(s):
 lines=s.splitlines();key='fishingfrenzy_0_description='
 assert sum(x.startswith(key) for x in lines)==1
 return '\n'.join(key+'Привіт! Мене звати $startvillager_villagername$. Риболовля — нелегка справа, і я не впевнений, що сьогодні наловлю достатньо риби. Можеш мені допомогти?' if x.startswith(key) else x for x in lines)+'\n'
edit(res/'languages/uk/quests_others.txt',quest)
edit(res/'languages/uk/japanese_sentences.txt',lambda s:s.replace('Буду садити ці саджанці.','Посаджу ці паростки.'))
for lang in ['uk','ru']:
 def strings(s,lang=lang):
  if lang=='uk':s=s.replace('Садить саджанці','Садить паростки').replace('panels.notyetbuilt=ще не побудовані','panels.notyetbuilt=Ще не збудовано')
  forms=['Залишилася <0> модернізація','Залишилося <0> модернізації','Залишилося <0> модернізацій'] if lang=='uk' else ['Осталась <0> модернизация','Осталось <0> модернизации','Осталось <0> модернизаций']
  return s.rstrip()+'\n'+'\n'.join('panels.nbupgradesleft.'+k+'='+v for k,v in zip(['one','few','many'],forms))+'\n'
 edit(res/f'languages/{lang}/strings.txt',strings)
edit(res/'localization-version.txt',lambda s:'uk-ru-2026-11-17-2\n')
p=base/'utilities/LanguageUtilities.java'
def language(s):
 marker='    public static String string(String key) {'
 assert marker in s
 return s.replace(marker,'''    public static String countedString(String key, int count) {
        String suffix = CountedForms.suffix(MillConfigValues.effective_language, count);
        String variant = key + suffix;
        if (!suffix.isEmpty() && MillConfigValues.mainLanguage != null
                && MillConfigValues.mainLanguage.strings.containsKey(variant)) {
            return string(variant, Integer.toString(count));
        }
        return string(key, Integer.toString(count));
    }

'''+marker)
edit(p,language)
(base/'utilities/CountedForms.java').write_text('''package org.millenaire.common.utilities;
public final class CountedForms {
    private CountedForms() {}
    public static String suffix(String language, int count) {
        if (language == null) return "";
        String code = language.toLowerCase(java.util.Locale.ROOT).replace('-', '_').split("_")[0];
        if (!code.equals("uk") && !code.equals("ru")) return "";
        long n = Math.abs((long)count), last = n % 10, lastTwo = n % 100;
        if (last == 1 && lastTwo != 11) return ".one";
        if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) return ".few";
        return ".many";
    }
}
''',encoding='utf-8')
def panel(s):
 old='''LanguageUtilities.string(
                        "panels.nbupgradesleft", "" + (project.getLevelsNumber(project.location.getVariation()) - project.location.level - 1)'''
 new='''LanguageUtilities.countedString(
                        "panels.nbupgradesleft", (project.getLevelsNumber(project.location.getVariation()) - project.location.level - 1)'''
 assert s.count(old)==1;return s.replace(old,new)
edit(base/'village/buildingmanagers/PanelContentGenerator.java',panel)
(base/'pathing/PartialPathRecovery.java').write_text('''package org.millenaire.common.pathing;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
public final class PartialPathRecovery {
    private PartialPathRecovery() {}
    public static boolean canUse(Path path, Vec3 current, Vec3 target) {
        if (path == null || path.getNodeCount() < 2 || path.isDone() || path.getEndNode() == null) return false;
        Vec3 end = Vec3.atBottomCenterOf(path.getEndNode().asBlockPos());
        // Partial routes are useful only if they make measurable progress toward the destination.
        return end.distanceTo(target) + 1.0 < current.distanceTo(target);
    }
}
''',encoding='utf-8')
def villager(s):
 old='''            if (fallback != null && fallback.canReach() && fallback.getNodeCount() > 0
                    && new Point(fallback.getEndNode()).horizontalDistanceTo(dest) < range) {'''
 new='''            boolean reached = fallback != null && fallback.canReach() && fallback.getNodeCount() > 0
                    && new Point(fallback.getEndNode()).horizontalDistanceTo(dest) < range;
            boolean progressing = org.millenaire.common.pathing.PartialPathRecovery.canUse(fallback,
                    this.position(), new Vec3(dest.x, dest.y, dest.z));
            if (reached || progressing) {'''
 assert s.count(old)==1;return s.replace(old,new)
edit(base/'entity/MillVillager.java',villager)
print('Updated reported translations, counted forms and progressing partial recovery routes.')
