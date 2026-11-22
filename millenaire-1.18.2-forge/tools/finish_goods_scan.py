from pathlib import Path
p=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge/src/main/java/org/millenaire/common/village/Building.java')
s=p.read_text(encoding='utf-8');a=s.index('   public int nbGoodAvailable(InvItem ii,');b=s.index('   public int nbGoodAvailable(Item item,',a)
t=s[a:b];needle='         int nb = this.countGoods(ii.getItem(), ii.meta);\n         if (this.isTownhall)'
assert needle in t;t=t.replace(needle,'         if (this.isTownhall)',1)
assert t.count('int nb =')==1
p.write_text(s[:a]+t+s[b:],encoding='utf-8')
