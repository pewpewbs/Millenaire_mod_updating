from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common'
b=r/'migration/before-audit19';b.mkdir(exist_ok=False)
def edit(rel,fn):
 p=base/rel;d=b/p.relative_to(r);d.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,d)
 p.write_text(fn(p.read_text(encoding='utf-8')),encoding='utf-8')
def replace(s,a,c):
 assert a in s,a
 return s.replace(a,c)
edit('utilities/WorldUtilities.java',lambda s:replace(s,'outputs.extractItem(stackNb, toTake, false)','outputs.extractItem(stackNb, toTake - taken, false)'))
edit('utilities/MillCommonUtilities.java',lambda s:replace(s,'            this.generatedFor.remove(id);','            this.generatedFor.remove(id);\n            this.buildingsTime.remove(id);\n            this.villagersTime.remove(id);'))
def config(s):
 s=replace(s,'if (field.getType() != Map.class) continue;','if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType() != Map.class) continue;')
 return replace(s,'return this.getBestItem(this.categories.get(categoryName), villager);','List<InvItem> items = this.categories.get(categoryName);\n        return items == null ? null : this.getBestItem(items, villager);')
edit('entity/VillagerConfig.java',config)
def language(s):
 s=replace(s,'s.replaceAll("\\\\$name", player.getName().getString())','s.replace("$name", player.getName().getString())')
 return s.replace('s.replaceAll("<" + pos + ">",','s.replace("<" + pos + ">",')
edit('utilities/LanguageUtilities.java',language)
def resident(s):
 a='''                                    item.discard();
                                    if (item.getItem().is(net.minecraft.tags.ItemTags.SAPLINGS)) {
                                        this.addToInv(item.getItem().getItem(), item.getItem().getDamageValue() & 3, 1);
                                    } else {
                                        this.addToInv(item.getItem().getItem(), item.getItem().getDamageValue(), 1);
                                    }'''
 s=replace(s,a,'                                    this.pickUpGoods(item);')
 anchor='    public void addToInv(Block block, int nb) {'
 return replace(s,anchor,'''    /** Commodity inventory cannot represent custom NBT; leave such drops intact. */
    public boolean pickUpGoods(ItemEntity entity) {
        if (entity == null || entity.isRemoved()) return false;
        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || stack.hasTag()) return false;
        int count = stack.getCount();
        if ((long)this.countInv(stack.getItem(), stack.getDamageValue()) + count > Integer.MAX_VALUE) return false;
        this.addToInv(stack.getItem(), stack.getDamageValue(), count);
        entity.discard();
        return true;
    }

'''+anchor)
edit('entity/MillVillager.java',resident)
edit('goal/GoalGatherGoods.java',lambda s:replace(s,'''            item.discard();
            villager.addToInv(item.getItem().getItem(), item.getItem().getDamageValue(), 1);
            return true;''','''            return villager.pickUpGoods(item);'''))
print('Applied confirmed audit fixes P0-06, P0-09, P0-16, P2-12 and literal substitutions from P2-11')
