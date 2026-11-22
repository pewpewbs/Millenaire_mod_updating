from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');p=r/'src/main/java/org/millenaire/common/village/buildingmanagers/PanelManager.java'
s=p.read_text(encoding='utf-8');start=s.index('    private void updateSignTHResources(');end=s.index('    private void updateSignTHVillageName(',start);part=s[start:end]
old='''            if (goalPlan != null) {
                lines.add(this.createFullLine("ui.resources", PanelManager.stackFromBlock((Block)Blocks.CHEST), PanelManager.stackFromBlock((Block)Blocks.CHEST)));'''
new='''            lines.add(this.createFullLine("ui.resources", PanelManager.stackFromBlock((Block)Blocks.CHEST), PanelManager.stackFromBlock((Block)Blocks.CHEST)));
            if (goalPlan == null) {
                lines.add(this.createEmptyLine());
                lines.add(this.createFullLine("ui.resources_no_project1", null, null));
                lines.add(this.createFullLine("ui.resources_no_project2", null, null));
            } else {'''
assert old in part;changes={p:s[:start]+part.replace(old,new,1)+s[end:]}
for lang,values in [('en',('No selected','project')),('uk',('Немає обраного','проєкту')),('ru',('Нет выбранного','проекта'))]:
 p=r/f'src/main/resources/todeploy/millenaire/languages/{lang}/strings.txt'
 text=p.read_text(encoding='utf-8');assert 'ui.resources_no_project1=' not in text
 changes[p]=text+'\nui.resources_no_project1='+values[0]+'\nui.resources_no_project2='+values[1]+'\n'
p=r/'src/main/resources/todeploy/millenaire/localization-version.txt';changes[p]='uk-ru-2026-11-16-3\n'
backup=r/'migration/before-resource-panel-empty-state';assert not backup.exists()
for p,text in changes.items():
 q=backup/p.relative_to(r);q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q);p.write_text(text,encoding='utf-8')
print('Resource panel empty state added; populated resource list unchanged')
