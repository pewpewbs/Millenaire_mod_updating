from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';b=r/'migration/before-entity-identity';b.mkdir(exist_ok=True)
p=base/'entity/MillVillager.java';s=p.read_text();shutil.copy2(p,b/p.name)
old='''    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MillVillager)) {
            return false;
        }
        MillVillager v = (MillVillager)obj;
        return this.getVillagerId() == v.villagerId;
    }
'''
assert s.count(old)==1;s=s.replace(old,'''    // Entity equality/hashCode must remain Minecraft's runtime entity identity.
    // Persistent villager IDs are used explicitly by MillWorldData's maps.
''')
old='''    public int hashCode() {
        return (int)this.getVillagerId();
    }
''';assert s.count(old)==1;s=s.replace(old,'')
old='''        this.mw.clearVillagerOfId(this.getVillagerId());
        super.remove(Entity.RemovalReason.DISCARDED);'''
new='''        // Removing a duplicate must not unregister the retained resident.
        if (this.mw.getVillagerById(this.getVillagerId()) == this) {
            this.mw.clearVillagerOfId(this.getVillagerId());
        }
        super.remove(Entity.RemovalReason.DISCARDED);'''
a=s.index('    public void despawnVillagerSilent()');z=s.index('    public void detrampleCrops()',a)
part=s[a:z];assert part.count(old)==1;s=s[:a]+part.replace(old,new)+s[z:];p.write_text(s)
p=base/'world/MillWorldData.java';s=p.read_text();shutil.copy2(p,b/p.name)
old='''            if (this.villagers.containsKey(villager.getVillagerId()) && MillConfigValues.LogVillagerSpawn >= 1) {
                MillLog.major(this, "Duplicate villager: " + villager);
                villagersToDespawn.add(villager);
            }'''
new='''            if (this.villagers.containsKey(villager.getVillagerId())) {
                if (MillConfigValues.LogVillagerSpawn >= 1) {
                    MillLog.major(this, "Duplicate villager: " + villager);
                }
                villagersToDespawn.add(villager);
                continue;
            }'''
assert s.count(old)==1;s=s.replace(old,new)
old='''            if (!(entity instanceof MillVillager)) continue;'''
assert s.count(old)==1;s=s.replace(old,'''            if (!(entity instanceof MillVillager) || entity.isRemoved()) continue;''');p.write_text(s)
print('Restored runtime entity identity and canonical duplicate cleanup.')
