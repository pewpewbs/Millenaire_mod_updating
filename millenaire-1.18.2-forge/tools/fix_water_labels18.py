from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
b=r/'migration/before-water-labels18'
b.mkdir(exist_ok=False)
def edit(rel,fn):
 p=r/rel; d=b/rel; d.parent.mkdir(parents=True,exist_ok=True)
 shutil.copy2(p,d); p.write_text(fn(p.read_text(encoding='utf-8')),encoding='utf-8')
def villager(s):
 anchor='    public void aiStep() {'
 assert s.count(anchor)==1 and 'void registerGoals()' not in s
 return s.replace(anchor,'''    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Route permission alone does not make a mob swim towards the surface.
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
    }

'''+anchor)
edit('src/main/java/org/millenaire/common/entity/MillVillager.java',villager)
def renderer(s):
 start=s.index('        if (villager.shouldLieDown) {',s.index('public void doRenderVillagerName'))
 end=s.index('        Minecraft minecraft',start)
 s=s[:start]+'''        // Labels are rendered after the model pose is restored. Keep their anchor
        // at the entity instead of applying the old model-space sleeping offset.
'''+s[end:]
 return s.replace('height += 0.25f','height += 0.32f')
edit('src/main/java/org/millenaire/client/render/RenderMillVillager.java',renderer)
def dialogue(s):
 assert 'К захисті поселення!' in s
 return s.replace('К захисті поселення!','На захист поселення!')
edit('src/main/resources/todeploy/millenaire/languages/uk/japanese_sentences.txt',dialogue)
edit('src/main/resources/todeploy/millenaire/localization-version.txt',lambda s:'uk-ru-2026-11-17-6\n')
print('Applied swimming, label spacing/anchor and Japanese battle dialogue fixes')
