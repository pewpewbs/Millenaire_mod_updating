from pathlib import Path
import shutil,re
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
backup=r/'migration/before-work-clock-fix'
changes={}
java=r/'src/main/java/org/millenaire/common'
for p in [java/'entity/MillVillager.java',*(java/'goal').rglob('*.java')]:
 old=p.read_text(encoding='utf-8');lines=old.splitlines(keepends=True);new=[]
 for line in lines:
  # All writers and readers of these transient elapsed-time fields use one clock.
  if any(x in line for x in ['actionStart','goalStarted','lastGoalTime']):line=line.replace('.getDayTime()', '.getGameTime()')
  if p.name=='MillVillager.java' and '%' in line and ('hashCode()' in line or 'getVillagerId()' in line):line=line.replace('.getDayTime()', '.getGameTime()')
  new.append(line)
 updated=''.join(new)
 if updated!=old:changes[p]=updated
for lang,value in [('uk','Деревина будь-якого виду'),('ru','Древесина любого вида')]:
 p=r/f'src/main/resources/todeploy/millenaire/languages/{lang}/strings.txt'
 old=p.read_text(encoding='utf-8');new,count=re.subn(r'^travelbook\.anywood=.*$', 'travelbook.anywood='+value,old,flags=re.M)
 assert count==1
 changes[p]=new
marker=r/'src/main/resources/todeploy/millenaire/localization-version.txt'
changes[marker]='uk-ru-2026-11-16-2\n'
assert not backup.exists(),'Already applied; inspect before retry'
for p,text in changes.items():
 q=backup/p.relative_to(r);q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
 p.write_text(text,encoding='utf-8')
print('Updated',len(changes),'files; transient AI timers use game time; wood label repaired')
