from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common'
p=base/'village/Building.java';old=p.read_text(encoding='utf-8')
start=old.index('   public int nbGoodAvailable(InvItem ii,');end=old.index('   public int nbGoodAvailable(Item item,',start)
part=old[start:end]
part=part.replace('      } else {\n','      } else {\n           int nb = this.countGoods(ii.getItem(), ii.meta);\n           // Reservations cannot make an absent physical item available.\n           if (nb <= 0) return 0;\n',1)
part=part.replace('           int nb = this.countGoods(ii.getItem(), ii.meta);\n           if (this.isTownhall)', '           if (this.isTownhall)',1)
part=part.replace('if (vr.getHousePos() != null && vr.getHousePos().equals(this.getPos()) && vr.getType() != null) {','if (vr.getHousePos() != null && vr.getHousePos().equals(this.getPos())) {\n                    var residentType = vr.getType();\n                    if (residentType == null) continue;')
part=part.replace('vr.getType().requiredFoodAndGoods','residentType.requiredFoodAndGoods')
changes={p:old[:start]+part+old[end:]}
p=base/'goal/GoalGetGoodsForHousehold.java';old=p.read_text(encoding='utf-8');new=old.replace('                for (Building building : buildings) {\n','                for (Building building : buildings) {\n                    if (building == villager.getHouse()) continue;\n').replace('nbav <= 0 || building == villager.getHouse() ||','nbav <= 0 ||')
new=new.replace('                for (Building shop : shops) {\n','                for (Building shop : shops) {\n                    if (shop == villager.getHouse()) continue;\n').replace('nbav <= 0 || shop == villager.getHouse()','nbav <= 0')
changes[p]=new
backup=r/'migration/before-goods-scan-optimization';assert not backup.exists()
for p,new in changes.items():
 q=backup/p.relative_to(r);q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q);p.write_text(new,encoding='utf-8')
print('Optimized empty-stock and own-house scans without caching availability or changing construction timing')
