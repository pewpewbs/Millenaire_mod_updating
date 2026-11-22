from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');p=r/'src/main/java/org/millenaire/common/network/StreamReadWrite.java'
b=r/'migration/before-audit19'/p.relative_to(r);b.parent.mkdir(parents=True,exist_ok=True);assert not b.exists();shutil.copy2(p,b)
s=p.read_text(encoding='utf-8')
for kind,name in [('Boolean','readBooleanList'),('ItemStack','readItemStackList'),('Point','readPointList'),('String','readStringList')]:
 start=s.index('    public static CopyOnWriteArrayList<'+kind+'> '+name+'(');end=s.index('\n    }',start)+6
 old=s[start:end];new=old.replace('CopyOnWriteArrayList<'+kind+'> v = new CopyOnWriteArrayList<'+kind+'>();','ArrayList<'+kind+'> v = new ArrayList<'+kind+'>();').replace('return v;','return new CopyOnWriteArrayList<>(v);')
 assert new!=old;s=s[:start]+new+s[end:]
p.write_text(s,encoding='utf-8')
print('Four network list readers now construct CopyOnWriteArrayList once; wire unchanged')
